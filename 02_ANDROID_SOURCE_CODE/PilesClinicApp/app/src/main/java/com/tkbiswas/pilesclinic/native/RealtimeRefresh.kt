package com.tkbiswas.pilesclinic.native

import android.os.Handler
import android.os.Looper
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🟢🔒 V1519 (17.09.2026) — lightweight Supabase Realtime invalidation.
 *
 * This object NEVER writes clinic data. It listens only for change notices on the
 * four core tables that the existing LiveRefresh screens already read. A notice
 * clears only the read caches that could hide the remote change, then wakes the
 * foreground screen. The old 30-second HEAD/count poll remains the fallback.
 *
 * Protocol: Supabase Realtime websocket, Phoenix JSON protocol v1.0.0.
 * Only id/branch/mobile/patientId/updatedAt are requested in change payloads so
 * photos, notes and money-history blobs are never streamed over the socket.
 */
object RealtimeRefresh {

    data class Change(
        val table: String,
        val branch: String?,
        val mobile: String?,
        val patientId: String?
    )

    private data class Subscriber(
        val tables: Set<String>,
        val callback: (Change) -> Unit
    )

    private const val TOPIC = "realtime:android-live-refresh"
    private const val HEARTBEAT_MS = 25_000L
    private val reconnectDelays = longArrayOf(1_000L, 2_000L, 5_000L, 10_000L)

    private val lock = Any()
    private val main = Handler(Looper.getMainLooper())
    private val subscribers = LinkedHashMap<Long, Subscriber>()
    private var nextSubscriberId = 1L
    private var nextRef = 1L
    private var socket: WebSocket? = null
    private var connecting = false
    private var reconnectIndex = 0
    private var reconnectPosted = false
    // 🔴🔒 V1522: the 30-second fallback may slow down only after the actual
    // postgres_changes channel join is acknowledged. Heartbeat OK by itself is
    // NOT enough, because a live websocket without a joined channel would miss
    // database changes. lastServerAckAt also prevents a silent/stale socket from
    // being treated as healthy forever.
    private var channelJoined = false
    private var activeJoinRef: String? = null
    private var lastServerAckAt = 0L
    private const val HEALTH_ACK_MAX_AGE_MS = 75_000L

    private val heartbeat = object : Runnable {
        override fun run() {
            val ws = synchronized(lock) { socket }
            if (ws == null || !hasSubscribers() || !LiveRefresh.awake()) {
                if (!LiveRefresh.awake()) stopSocketOnly()
                return
            }
            try {
                val ref = newRef()
                val msg = JSONObject()
                    .put("topic", "phoenix")
                    .put("event", "heartbeat")
                    .put("payload", JSONObject())
                    .put("ref", ref)
                    .put("join_ref", JSONObject.NULL)
                if (!ws.send(msg.toString())) reconnect(ws)
            } catch (_: Throwable) {
                reconnect(ws)
                return
            }
            main.postDelayed(this, HEARTBEAT_MS)
        }
    }

    /** Register a foreground screen. Callback is always delivered on main thread. */
    fun subscribe(tables: Collection<String>, callback: (Change) -> Unit): Long {
        val clean = tables.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (clean.isEmpty()) return 0L
        val id = synchronized(lock) {
            val n = nextSubscriberId++
            subscribers[n] = Subscriber(clean, callback)
            n
        }
        poke()
        return id
    }

    fun unsubscribe(id: Long) {
        if (id == 0L) return
        val empty = synchronized(lock) {
            subscribers.remove(id)
            subscribers.isEmpty()
        }
        if (empty) {
            main.removeCallbacks(heartbeat)
            stopSocketOnly()
        }
    }

    /** Called by the 30-second fallback too, so a screen left open across 6 AM reconnects. */
    fun poke() {
        main.post {
            if (!hasSubscribers() || !LiveRefresh.awake()) {
                if (!LiveRefresh.awake()) stopSocketOnly()
                return@post
            }
            ensureConnected()
        }
    }

    private fun hasSubscribers(): Boolean = synchronized(lock) { subscribers.isNotEmpty() }

    /** V1522 — true only when the DB-change channel is joined and server ack is recent. */
    fun healthy(): Boolean = synchronized(lock) {
        socket != null && channelJoined && lastServerAckAt > 0L &&
            System.currentTimeMillis() - lastServerAckAt <= HEALTH_ACK_MAX_AGE_MS
    }

    private fun newRef(): String = synchronized(lock) { (nextRef++).toString() }

    private fun ensureConnected() {
        synchronized(lock) {
            if (socket != null || connecting || subscribers.isEmpty() || !LiveRefresh.awake()) return
            connecting = true
        }

        val url = SupabaseClient.URL.replaceFirst("https://", "wss://") +
            "/realtime/v1/websocket?apikey=" + java.net.URLEncoder.encode(SupabaseClient.KEY, "UTF-8") +
            "&vsn=1.0.0"
        val request = Request.Builder().url(url).build()

        try {
            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    synchronized(lock) {
                        // Assign here as well as after newWebSocket() returns: this closes the
                        // tiny race where an unusually fast callback could arrive first.
                        socket = webSocket
                        connecting = false
                        reconnectIndex = 0
                        reconnectPosted = false
                        channelJoined = false
                        activeJoinRef = null
                        lastServerAckAt = 0L
                    }
                    if (!LiveRefresh.awake() || !hasSubscribers()) {
                        stopSocketOnly()
                        return
                    }
                    sendJoin(webSocket)
                    main.removeCallbacks(heartbeat)
                    main.postDelayed(heartbeat, HEARTBEAT_MS)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (synchronized(lock) { socket !== webSocket }) return
                    handleMessage(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    try { webSocket.close(code, reason) } catch (_: Throwable) { }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    socketEnded(webSocket)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    socketEnded(webSocket)
                }
            }
            val created = SupabaseClient.http.newWebSocket(request, listener)
            synchronized(lock) {
                // A callback can theoretically beat newWebSocket() returning.
                // - onOpen may already have assigned this exact socket: keep it.
                // - onFailure may already have cleared connecting: never resurrect that dead socket.
                // - onPause may have removed the last subscriber: cancel it.
                if (socket === created) {
                    // already opened successfully
                } else if (!connecting || subscribers.isEmpty() || !LiveRefresh.awake()) {
                    connecting = false
                    try { created.cancel() } catch (_: Throwable) { }
                } else {
                    socket = created
                }
            }
        } catch (_: Throwable) {
            synchronized(lock) { connecting = false }
            scheduleReconnect()
        }
    }

    private fun sendJoin(ws: WebSocket) {
        try {
            val changes = JSONArray()
            changes.put(changeSpec("payments", true))
            changes.put(changeSpec("patients", true))
            changes.put(changeSpec("followups", true))
            changes.put(changeSpec("enquiries", false))

            val config = JSONObject()
                .put("broadcast", JSONObject().put("ack", false).put("self", false))
                .put("presence", JSONObject().put("enabled", false))
                .put("postgres_changes", changes)
                .put("private", false)

            val ref = newRef()
            synchronized(lock) {
                activeJoinRef = ref
                channelJoined = false
                lastServerAckAt = 0L
            }
            val msg = JSONObject()
                .put("topic", TOPIC)
                .put("event", "phx_join")
                .put("payload", JSONObject().put("config", config))
                .put("ref", ref)
                .put("join_ref", ref)
            if (!ws.send(msg.toString())) reconnect(ws)
        } catch (_: Throwable) {
            reconnect(ws)
        }
    }

    private fun changeSpec(table: String, hasPatientId: Boolean): JSONObject {
        val select = JSONArray().put("id").put("branch").put("mobile")
        if (hasPatientId) select.put("patientId")
        select.put("updatedAt")
        return JSONObject()
            .put("event", "*")
            .put("schema", "public")
            .put("table", table)
            .put("select", select)
    }

    private fun handleMessage(text: String) {
        try {
            val msg = JSONObject(text)
            when (msg.optString("event")) {
                "postgres_changes" -> {
                    val data = msg.optJSONObject("payload")?.optJSONObject("data") ?: return
                    val table = data.optString("table", "").trim()
                    if (table !in setOf("payments", "patients", "followups", "enquiries")) return
                    val record = data.optJSONObject("record")
                    val old = data.optJSONObject("old_record")
                    fun field(name: String): String? {
                        val a = record?.optString(name, "")?.trim().orEmpty()
                        if (a.isNotBlank() && a != "null") return a
                        val b = old?.optString(name, "")?.trim().orEmpty()
                        return b.takeIf { it.isNotBlank() && it != "null" }
                    }
                    invalidate(table)
                    dispatch(Change(table, field("branch"), field("mobile"), field("patientId")))
                }
                "phx_error", "phx_close" -> synchronized(lock) { socket }?.let { reconnect(it) }
                "phx_reply" -> {
                    val payload = msg.optJSONObject("payload")
                    val status = payload?.optString("status").orEmpty()
                    val replyRef = msg.optString("ref", "")
                    val replyTopic = msg.optString("topic", "")
                    if (status == "error") {
                        synchronized(lock) {
                            channelJoined = false
                            lastServerAckAt = 0L
                        }
                        synchronized(lock) { socket }?.let { reconnect(it) }
                    } else if (status == "ok") {
                        synchronized(lock) {
                            val now = System.currentTimeMillis()
                            // Only the matching channel-join reply can declare the
                            // postgres_changes subscription ready. A heartbeat reply
                            // merely refreshes liveness after the channel is joined.
                            if (replyTopic == TOPIC && replyRef.isNotBlank() && replyRef == activeJoinRef) {
                                channelJoined = true
                                lastServerAckAt = now
                            } else if (replyTopic == "phoenix" && channelJoined) {
                                lastServerAckAt = now
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Malformed/non-change server messages are ignored. 30s fallback stays active.
        }
    }

    private fun invalidate(table: String) {
        // Remote writes do not pass through SupabaseClient's local write invalidation.
        // Without these clears a Realtime wake-up could immediately repaint a 20/60s cache.
        try { CloudReadDedupe.clear(table) } catch (_: Throwable) { }
        try { CloudListRevalidate.clear(table) } catch (_: Throwable) { }
        try { CloudReadCache.clear() } catch (_: Throwable) { }
    }

    private fun dispatch(change: Change) {
        val targets = synchronized(lock) {
            subscribers.values.filter { change.table in it.tables }.map { it.callback }
        }
        if (targets.isEmpty()) return
        main.post {
            for (cb in targets) {
                try { cb(change) } catch (_: Throwable) { }
            }
        }
    }

    private fun reconnect(ws: WebSocket) {
        try { ws.cancel() } catch (_: Throwable) { }
        socketEnded(ws)
    }

    private fun socketEnded(ws: WebSocket) {
        val wasCurrent = synchronized(lock) {
            // If failure happens before newWebSocket() returns, socket may still be null
            // while connecting=true. Treat that callback as the current connection too.
            if (socket !== ws && !(socket == null && connecting)) false
            else {
                socket = null
                connecting = false
                channelJoined = false
                activeJoinRef = null
                lastServerAckAt = 0L
                true
            }
        }
        if (!wasCurrent) return
        main.removeCallbacks(heartbeat)
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (!hasSubscribers() || !LiveRefresh.awake()) return
        val delay = synchronized(lock) {
            if (reconnectPosted) return
            reconnectPosted = true
            reconnectDelays[reconnectIndex.coerceAtMost(reconnectDelays.lastIndex)].also {
                reconnectIndex = (reconnectIndex + 1).coerceAtMost(reconnectDelays.lastIndex)
            }
        }
        main.postDelayed({
            synchronized(lock) { reconnectPosted = false }
            ensureConnected()
        }, delay)
    }

    private fun stopSocketOnly() {
        val old = synchronized(lock) {
            val s = socket
            socket = null
            connecting = false
            reconnectPosted = false
            reconnectIndex = 0
            channelJoined = false
            activeJoinRef = null
            lastServerAckAt = 0L
            s
        }
        main.removeCallbacks(heartbeat)
        try { old?.close(1000, "inactive") } catch (_: Throwable) { try { old?.cancel() } catch (_: Throwable) { } }
    }
}
