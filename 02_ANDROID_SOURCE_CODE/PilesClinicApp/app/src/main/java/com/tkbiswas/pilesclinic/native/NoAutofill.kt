package com.tkbiswas.pilesclinic.native

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.View

/**
 * 🔵🔒 V418 (TK-নির্দেশ, ১৭.০৮.২০২৬) — *"এখানে অটো সাজেশ কেন থাকবে"*
 *
 * সমস্যা: "Add Collection"-এর Cash ঘরে চাপ দিলে ফোনের নিজের **Autofill**
 * পুরনো সেভ করা লেখা (যেমন মোবাইল নম্বর 9002003540 / 8001080080) সাজেশন
 * হিসেবে দেখাচ্ছিল। ওটা অ্যাপের নিজের সাজেশন নয় — Android-এর/Google-এর
 * Autofill সেবা। টাকার ঘরে ভুল করে ফোন নম্বর বসে যাওয়ার আসল ঝুঁকি ছিল।
 *
 * সমাধান: প্রতিটা পর্দার **মূল বাক্সে** একবার বলে দেওয়া হয় — "এখানে Autofill
 * লাগবে না"। `NO_EXCLUDE_DESCENDANTS` মানে ওই পর্দার **ভিতরের সব ঘরেই**
 * নিয়মটা খাটে, তাই ৩৪টা পর্দার একটাতেও আলাদা কোড লাগেনি, আর ভবিষ্যতে নতুন
 * পর্দা যোগ হলেও নিজে থেকেই ঢেকে যাবে।
 * (`NoBengali.hookApp` ঠিক এই প্রমাণিত পথেই বসানো আছে।)
 *
 * ⛔ কীবোর্ড, টাইপ করা, সেভ করা — কিছুই বদলায় না। শুধু ফোনের নিজের পুরনো
 *    লেখা আর ভেসে ওঠে না।
 * ⛔ Android 8 (API 26)-এর আগে Autofill নেই, তাই সেখানে কিছুই করা হয় না।
 * ⛔ পুরোটা try/catch-এ মোড়া — এখানে কিছু ভুল হলেও কোনো পর্দা ভাঙবে না।
 */
object NoAutofill {

    fun hookApp(app: Application) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) { apply(activity) }
            override fun onActivityResumed(activity: Activity) { apply(activity) }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) { removeNet(activity) }
        })
    }

    /** ⛔ একবারই — বারবার ডাকার দরকার নেই। */
    @Volatile private var serviceOff = false

    /**
     * 🔴🔒 V758 (২৭.০৮.২০২৬, TK-লাইভ রিপোর্ট: *"মোবাইল নম্বর সাজেস্ট এখনও করছে"*)
     *
     * **আসল কারণ (গভীরে গিয়ে ধরা):** এতদিন শুধু ঘরে-ঘরে "এখানে Autofill
     * লাগবে না" বলা হচ্ছিল (`importantForAutofill`)। কিন্তু Android-এর
     * **Autofill সেবাটা নিজে চালুই ছিল** — তাই Google/Samsung-এর জমানো
     * নম্বর-পাসওয়ার্ড তবু ভেসে উঠত।
     *
     * 🔴🔴 **V772-এ নিজের ভুল ধরা পড়ল — সৎভাবে লিখে রাখছি।**
     *    V758-এ আমি লিখেছিলাম *"এটাই আসল সমাধান"*। **সেটা ভুল ছিল।**
     *    Android-এর ভিতরে `disableAutofillServices()` আসলে ডাকে
     *    `disableOwnedAutofillServices()` — অর্থাৎ **যে অ্যাপ নিজেই একটা
     *    Autofill সেবা**, শুধু তারই সেবা বন্ধ হয়। আমাদের অ্যাপ Autofill
     *    সেবা নয় ⇒ **এই ডাকটা কার্যত কিছুই করে না।**
     *    ⇒ তাই TK-কে "সমাধান হয়ে গেছে" বলা ঠিক হয়নি। ডাকটা রাখা হলো
     *      (ক্ষতি নেই), কিন্তু এটাকে আর ভরসা করা হচ্ছে না।
     *    ⇒ আসল কাজটা করে — `importantForAutofill` (নিচে) + `cancel()` +
     *      **V772-এর নতুন জাল** (`netForEveryWindow`) + সবচেয়ে বড় কথা,
     *      ক্লিপবোর্ডের গোপন-পতাকা (`Clip.copy`, ClipboardUtil.kt)।
     *
     * ⛔ শুধু **এই অ্যাপে** — ফোনের অন্য অ্যাপে কোনো প্রভাব নেই।
     * ⛔ পুরোটা try/catch-এ, তাই এখানে কিছু ভুল হলেও কোনো পর্দা ভাঙে না।
     */
    private fun killAutofillService(activity: Activity) {
        if (serviceOff || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val am = activity.getSystemService(android.view.autofill.AutofillManager::class.java)
            if (am != null) {
                try { am.cancel() } catch (_: Throwable) {}          // এখন খোলা থাকলে বন্ধ করো
                if (am.isEnabled) am.disableAutofillServices()        // আর কখনো এসো না
            }
        } catch (_: Throwable) {}
        serviceOff = true
    }

    /** পর্দার মূল বাক্স খুঁজে নিয়ে Autofill বন্ধ করে দেয়। */
    private fun apply(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        killAutofillService(activity)
        try {
            // 🔴 V758 — শুধু `android.R.id.content` নয়, **পুরো উইন্ডোর বাইরের
            //    বাক্সেও** (decorView) বসানো হয়। content-এর উপরে আরও কিছু
            //    থাকতে পারে, সেগুলোও তখন ঢেকে যায়।
            try {
                val decor = activity.window?.decorView
                if (decor != null &&
                    decor.importantForAutofill != View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS) {
                    decor.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                }
            } catch (_: Throwable) {}
            val root = activity.findViewById<View>(android.R.id.content) ?: return
            if (root.importantForAutofill != View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS) {
                root.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            }
            scrub(root)
            keepScrubbing(activity.window?.decorView)   // 🔴 V774 — আসল ফাঁকটা এখানেই
            watchFocus(activity)
            netForEveryWindow(activity)
        } catch (_: Throwable) {}
    }

    /** কোন উইন্ডোতে পাহারাদার বসানো হয়ে গেছে — দুবার বসানো ঠেকাতে। */
    private val sweeping = java.util.WeakHashMap<View, Boolean>()

    /**
     * 🔴🔴🔒 V774 (২৮.০৮.২০২৬) — **এতদিনের আসল ফাঁক, এবার প্রমাণসহ ধরা।**
     *
     * TK: *"যেকোনো পর্দাতে মোবাইল নাম্বার সাজেস্ট… এই সমস্যা নিয়ে আমি বেশ
     * কয়েকবার আপনাকে কমপ্লেন করেছি।"* — TK ঠিক বলেছেন, এতদিন সারেনি।
     *
     * ═══════════════════════════════════════════════════════════════════
     * **কেন সারেনি — কোড ধরে যাচাই (আন্দাজ নয়)**
     *
     * ১) **এটা Autofill নয়** — প্রমাণ: `activity_registration.xml`-এর
     *    মোবাইলের ঘরে XML-এই `importantForAutofill="no"` লেখা আছে, আর
     *    পর্দার decorView-তেও পাহারা বসে। তবু TK-এর ফোনে **ওই ঘরেও**
     *    সাজেশন আসে। ⇒ সাজেশনটা **কীবোর্ডের নিজের** পট্টি।
     *
     * ২) কীবোর্ডকে থামানোর একমাত্র পতাকা `IME_FLAG_NO_PERSONALIZED_LEARNING`
     *    — আর এটা **প্রতিটা ঘরে আলাদা করে** বসাতে হয়; উপরের বাক্সে বসালে
     *    ভিতরের ঘরে নামে না (`importantForAutofill`-এর মতো উত্তরাধিকার নেই)।
     *
     * ৩) 🔴 **`scrub()` চলত শুধু পর্দা খোলার সময়** (onStart/onResume) —
     *    অর্থাৎ **ওই মুহূর্তে যে ঘরগুলো ছিল** কেবল সেগুলোতেই পতাকা বসত।
     *    কিন্তু এই অ্যাপের প্রায় **সব ঘরই পরে তৈরি হয়** — তথ্য আসার পরে,
     *    বোতাম চাপার পরে, তালিকা আঁকার পরে। ⇒ সেই ঘরগুলোতে পতাকা
     *    **কখনোই** বসত না। এই কারণেই "যেকোনো পর্দাতে, যেকোনো ঘরে"।
     * ═══════════════════════════════════════════════════════════════════
     *
     * **সমাধান:** `NoBengali`-র বহুদিনের প্রমাণিত পথ — **প্রতিটা layout-এর
     * পরে** আবার মিলিয়ে দেখা। ফলে পরে বসানো ঘরও, চোখে পড়ার আগেই, পতাকা
     * পেয়ে যায়। ⇒ কোনো ঝিলিক নেই, ব্যবহারকারী কিছুই টের পান না।
     *
     * ⛔ ইতিমধ্যে পতাকা-পাওয়া ঘর আবার ছোঁয়া হয় না (`hardened` চিহ্ন), তাই
     *    বারবার কাজ হয় না — খরচ নগণ্য।
     * ⛔ টাইপ করা · সেভ · কীবোর্ডের ভাষা · `inputType` — কিছুই বদলায় না।
     * ⛔ প্রতি উইন্ডোতে একবারই বসে (WeakHashMap), পুরোটা try/catch-এ।
     */
    private fun keepScrubbing(decor: View?) {
        val d = decor ?: return
        if (sweeping.containsKey(d)) { scrub(d); return }
        try {
            sweeping[d] = true
            d.viewTreeObserver.addOnGlobalLayoutListener { try { scrub(d) } catch (_: Throwable) {} }
            scrub(d)
        } catch (_: Throwable) {}
    }

    /** কোন পর্দায় জাল বসানো হয়ে গেছে — দুবার বসানো ঠেকাতে। */
    private val netted = java.util.WeakHashMap<Activity, Any>()

    /**
     * 🕸️🔒 V772 (২৮.০৮.২০২৬) — **চার নম্বর স্তর: প্রতিটা উইন্ডোর জন্য জাল।**
     *
     * **কেন লাগল (কোড গুনে দেখা):** পর্দার পাহারা বসে `decorView`-এ। কিন্তু
     * **প্রতিটা পপ-আপের নিজের আলাদা উইন্ডো** — তাই ওখানে পৌঁছায় না।
     * `PremiumAlert.paint()` দিয়ে বেশিরভাগ পপ-আপ ঢাকা পড়ে, কিন্তু গুনে
     * দেখা গেল **৬৫টা পপ-আপ** PremiumAlert দিয়ে যায় না (MedicinePicker ·
     * DoctorCheckup · Registration · Payment · FollowUp …)। ওগুলোতে ফাঁক
     * থেকে যাচ্ছিল।
     *
     * ৬৫টা ফাইল হাতে বদলানোর বদলে **একটাই জাল** — `AutofillCallback`।
     * Autofill-এর সাজেশন যদি অ্যাপের **যেকোনো** ঘরে ভেসে ওঠে, Android
     * নিজেই এখানে খবর দেয়; তখন ওই ঘরটাকে চিরতরে "Autofill নয়" চিহ্ন দিয়ে
     * চলতি সাজেশনটা বন্ধ করে দেওয়া হয়।
     *
     * ⛔ কোনো ঘরের নিজের কাজ · listener · টাইপ করা — কিচ্ছু ছোঁয়া হয় না।
     * ⛔ পর্দা বন্ধ হলে জাল খুলে নেওয়া হয় (`removeNet`), তাই স্মৃতি জমে না।
     * ⛔ পুরোটা try/catch — এখানে ভুল হলেও কোনো পর্দা ভাঙবে না।
     */
    private fun netForEveryWindow(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (netted.containsKey(activity)) return
        try {
            val am = activity.getSystemService(android.view.autofill.AutofillManager::class.java)
                ?: return
            val cb = object : android.view.autofill.AutofillManager.AutofillCallback() {
                override fun onAutofillEvent(view: View, event: Int) {
                    try {
                        if (event == EVENT_INPUT_SHOWN) {
                            view.importantForAutofill =
                                View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                            am.cancel()
                        }
                    } catch (_: Throwable) {}
                }
                override fun onAutofillEvent(view: View, virtualId: Int, event: Int) {
                    try { if (event == EVENT_INPUT_SHOWN) am.cancel() } catch (_: Throwable) {}
                }
            }
            am.registerCallback(cb)
            netted[activity] = cb
        } catch (_: Throwable) {}
    }

    /** পর্দা বন্ধ হলে জাল খুলে নেওয়া — নইলে স্মৃতি ধরে রাখত। */
    private fun removeNet(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val cb = netted.remove(activity)
                as? android.view.autofill.AutofillManager.AutofillCallback ?: return
            activity.getSystemService(android.view.autofill.AutofillManager::class.java)
                ?.unregisterCallback(cb)
        } catch (_: Throwable) {}
    }

    /** কোন পর্দায় পাহারাদার বসানো হয়ে গেছে — দুবার বসানো ঠেকাতে। */
    private val focusWatched = java.util.WeakHashMap<Activity, Boolean>()

    /**
     * 🔴🔒 V761 (২৭.০৮.২০২৬, TK: *"সম্পূর্ণ প্রজেক্টের সমস্ত জায়গায় আসতেছে"*)
     *
     * **তিন নম্বর স্তর — সবচেয়ে জোরালো।** আগের দুটো (ঘরের পতাকা + সেবা বন্ধ)
     * TK-এর ফোনে যথেষ্ট হয়নি। তাই এখন **যতবার কোনো লেখার ঘরে চাপ পড়ে**,
     * ততবার `AutofillManager.cancel()` ডেকে চলতি সাজেশন **জোর করে বন্ধ** করা হয়।
     *
     * ⛔ **কোনো ঘরের নিজের listener ছোঁয়া হয় না** — `OnGlobalFocusChangeListener`
     *    পর্দার সবার উপরে বসে, তাই অ্যাপের কোনো পুরনো কাজ ভাঙে না।
     *    (`setOnFocusChangeListener` ব্যবহার করলে পুরনো listener মুছে যেত —
     *     সেটা ইচ্ছে করেই এড়ানো হলো।)
     * ⛔ প্রতি পর্দায় একবারই বসে (WeakHashMap), তাই বারবার জমে না।
     * ⛔ পুরোটা try/catch — কিছু ভুল হলেও পর্দা ভাঙে না।
     *
     * ⚠️ সৎ কথা: এটাও **Android-এর Autofill** থামায়। সাজেশনটা যদি
     *    কীবোর্ডের **নিজের** হয় (Gboard/ফোনের কীবোর্ডের সেটিং), তাহলে
     *    কোনো অ্যাপের পক্ষেই সেটা বন্ধ করা সম্ভব নয় — ফোনের Settings থেকে
     *    বন্ধ করতে হয়।
     */
    private fun watchFocus(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (focusWatched.containsKey(activity)) return
        try {
            val decor = activity.window?.decorView ?: return
            val am = activity.getSystemService(android.view.autofill.AutofillManager::class.java)
                ?: return
            decor.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
                if (newFocus is android.widget.EditText) {
                    try { am.cancel() } catch (_: Throwable) {}
                }
            }
            focusWatched[activity] = true
        } catch (_: Throwable) {}
    }

    /**
     * 🔴🔒 V752 (২৭.০৮.২০২৬, TK-রিপোর্ট ছবিসহ: *"এরকম যেন সাজেস্ট না করে"*)
     *
     * **আসল কারণ (কোড ধরে যাচাই):** V418-এ শুধু পর্দার মূল বাক্সে
     * `importantForAutofill` বসানো হয়েছিল — সেটা **Google-এর Autofill**
     * থামায়, কিন্তু **কীবোর্ডের নিজের সাজেশন** থামায় না। ওটা থামাতে
     * প্রতিটা ঘরে আলাদা করে `IME_FLAG_NO_PERSONALIZED_LEARNING` লাগে।
     * এতদিন সেটা **শুধু মোবাইলের ঘরে** ছিল (`MobileInput.attach`), তাই
     * নাম · টাকা · অন্য সব ঘরে পুরনো লেখা ভেসে উঠত।
     *
     * এখন পর্দার **প্রতিটা লেখার ঘরে** একবার করে বসিয়ে দেওয়া হয়।
     * ⛔ টাইপ করা · সেভ · কীবোর্ডের ভাষা — কিছুই বদলায় না। `inputType`-এ
     *    হাত দেওয়া হয়নি (নইলে বাংলা লেখার সুবিধা নষ্ট হত)।
     * ⛔ অ্যাপের নিজের সাজেশন (যেমন "Select Saved RMP") অক্ষত — সেগুলো
     *    কীবোর্ডের নয়, অ্যাপের নিজের তালিকা।
     * ⚠️ সৎ কথা: `NO_PERSONALIZED_LEARNING` একটা **অনুরোধ** — প্রায় সব
     *    কীবোর্ড মানে, কিন্তু কোনো কীবোর্ড না মানলে অ্যাপের আর কিছু করার নেই।
     */
    private fun scrub(v: View) {
        if (v is android.widget.EditText) {
            // 🔴 V774 — এখন এক জায়গা দিয়েই যায় (`harden`), তাই পর্দা ও পপ-আপে
            //    নিয়ম হুবহু এক থাকে, আর যা আগেই হয়ে গেছে তা আবার ছোঁয়া হয় না।
            harden(v)
            return
        }
        if (v is android.view.ViewGroup) {
            for (i in 0 until v.childCount) {
                try { scrub(v.getChildAt(i)) } catch (_: Throwable) {}
            }
        }
    }

    /**
     * 🔧 V756 — **কোড দিয়ে বানানো একটা ঘরে** সাজেশন বন্ধ করার এক-লাইনের হাতিয়ার।
     *
     * ⚠️ কেন লাগল: `scrub()` চলে পর্দা **খোলার সময়** (onStart/onResume)। কিন্তু
     *    Module-এর পর্দাগুলো (Staff Profiles · বেতন · Add Person …) ঘরগুলো
     *    বানায় **তার পরে**, বোতাম চাপার পর। তাই ওখানে পাহারা পৌঁছাত না —
     *    নিজের কাজ যাচাই করতে গিয়ে ধরা পড়েছে (২৭.০৮.২০২৬)।
     * ⛔ কখনো ব্যতিক্রম ছোড়ে না; Android 8-এর আগে কিছুই করে না।
     */
    /* 🔴🔒 V1505 (১৫.০৯.২০২৬, TK-নির্দেশ, স্টাফ-রিপোর্ট — "অন্য অ্যাপে ভয়েস
       কাজ করে, এই অ্যাপে করে না") — যাচাই করে ধরা পড়ল: `IME_FLAG_NO_
       PERSONALIZED_LEARNING` পতাকাটাই Gboard-এর ভয়েস-টাইপিং বন্ধ করে
       দিচ্ছিল, আর এই পতাকা এই অ্যাপের **প্রতিটা** লেখার ঘরে বসানো হতো
       (V774)। TK-র নির্দেশ: "ভয়েস ফিরিয়ে দিন, পুরনো সাজেশনের ঝুঁকি
       নেব না একদম — সঠিকভাবে করতে পারলে করুন, নাহলে বাদ দিন।"

       ⇒ এই পতাকাটা এখান (`harden()`, সাধারণ সব ঘরে প্রযোজ্য) থেকে সরানো
       হলো, তাই সাধারণ ঘরে (নাম/মন্তব্য/সার্চ) ভয়েস আবার কাজ করবে।
       ⛔ **আসল ঝুঁকিটা (V758-এর ফোন-নম্বর সাজেশন) এখনো ঠিক আগের মতোই
       বন্ধ থাকছে**, কারণ:
       ① মোবাইল নম্বরের ঘরে (`MobileInput.attach()`) এই পতাকা **আলাদাভাবে,
          অক্ষত** রাখা হয়েছে — ফোন-নম্বরের কীবোর্ডে এমনিতেই ভয়েস বোতাম
          থাকে না, তাই ওখানে হারানোর কিছু নেই।
       ② Android-এর নিজস্ব Autofill Framework-এর (যেটাই আসল "সাজেশন-বার"
          দেখাত) বাকি চারটে স্তর (`importantForAutofill` · `disable
          AutofillServices()` · ফোকাস-বদলে `cancel()` · প্রতি-উইন্ডো জাল
          `netForEveryWindow`) এক অক্ষরও বদলানো হয়নি — সব ঘরেই আগের মতোই
          সক্রিয়।
       ⛔ যা বদলায়নি: টাইপ করা, সেভ, `inputType`, বাংলা/হিন্দি লেখা। */
    fun harden(et: android.widget.EditText) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                et.importantForAutofill != View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS) {
                et.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                et.setAutofillHints(null as String?)
            }
        } catch (_: Throwable) {}
    }

    /**
     * 🪟 V752 — **পপ-আপের নিজের আলাদা উইন্ডো থাকে**, তাই পর্দার পাহারা
     * ওখানে পৌঁছায় না (TK-এর ছবিতে "Cash" ঘরে সাজেশন — ঠিক এই কারণেই)।
     * `PremiumAlert.paint()` প্রজেক্টের **সব** পপ-আপে ডাকা হয়, তাই সেখান
     * থেকে একবার এটা ডাকলেই সবক'টা ঢেকে যায়।
     * ⛔ কখনো ব্যতিক্রম ছোড়ে না — পপ-আপ ভাঙার ঝুঁকি নেই।
     */
    /**
     * 🪟🔒 V774 — **যেকোনো পপ-আপ** (Dialog / AlertDialog / BottomSheet) সরাসরি
     * দেওয়া যায়; ভিতরের উইন্ডোটা এখান থেকেই বার করে নেওয়া হয়।
     *
     * ⛔ ইচ্ছে করেই `Any?` — প্রজেক্টে পপ-আপের ক্লাস তিন রকম (androidx
     *    `AlertDialog` · framework `android.app.AlertDialog` · `Dialog`)।
     *    একটাই ডাক সবগুলোতে চলে, আর ভুল কিছু দিলে চুপচাপ কিছুই হয় না।
     * ⛔ পপ-আপ **দেখানোর পরে** ডাকতে হয় (তখনই উইন্ডো তৈরি হয়)।
     */
    fun scrubAnyDialog(d: Any?) {
        try {
            val w = (d as? android.app.Dialog)?.window ?: return
            scrubDialogWindow(w)
        } catch (_: Throwable) {}
    }

    fun scrubDialogWindow(window: android.view.Window?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val root = window?.decorView ?: return
            root.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            // 🔴 V774 — পপ-আপেও **প্রতিটা layout-এর পরে** মিলিয়ে দেখা হয়:
            //    পপ-আপের ভিতরেও ঘর পরে যোগ হয় (তালিকা · সারি · ওষুধের ঘর)।
            keepScrubbing(root)
        } catch (_: Throwable) {}
    }
}
