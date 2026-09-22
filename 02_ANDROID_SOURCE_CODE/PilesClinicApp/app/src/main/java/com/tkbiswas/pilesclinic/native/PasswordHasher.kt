package com.tkbiswas.pilesclinic.native

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * V216 (§4, 31.07.2026) — Password hashing (PBKDF2-HMAC-SHA256).
 *
 * ⛔ কেন এবং কীভাবে (সৎভাবে):
 *  আগে সব password **খোলা (plaintext)** ছিল — DB-তে, config-এ, source-এ। এটা
 *  একবারে সব বদলে দিলে login ভেঙে যেত (login plaintext মেলাত)। তাই এই ধাপটা
 *  **backward-compatible**: hash থাকলে hash দিয়ে মেলানো হয়, না থাকলে আগের মতোই
 *  plaintext; আর সঠিক plaintext দিয়ে login সফল হলে তখনই hash বসিয়ে দেওয়া হয়
 *  (lazy migration) — তাই সময়ের সঙ্গে সব custom password DB-তে hash হয়ে যায়,
 *  কিছু না ভেঙেই।
 *
 * সংরক্ষণ ফরম্যাট (এক লাইনে, DB-র password_hash কলামে):
 *   pbkdf2_sha256$<iterations>$<salt-base64>$<hash-base64>
 */
object PasswordHasher {

    private const val ALGO = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LEN = 256           // bits
    private const val PREFIX = "pbkdf2_sha256"

    /** এই স্ট্রিংটা কি আমাদের hash ফরম্যাট? (plaintext থেকে আলাদা করার জন্য) */
    fun isHash(stored: String?): Boolean =
        stored != null && stored.startsWith("$PREFIX\$")

    /** নতুন salt দিয়ে password hash — DB-তে বসানোর মতো এক-লাইন স্ট্রিং। */
    fun hash(password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val dk = pbkdf2(password, salt, ITERATIONS, KEY_LEN)
        val b64 = { b: ByteArray -> Base64.encodeToString(b, Base64.NO_WRAP) }
        return "$PREFIX\$$ITERATIONS\$${b64(salt)}\$${b64(dk)}"
    }

    /** password ও সংরক্ষিত hash মেলানো (constant-time তুলনা)। ভুল ফরম্যাটে false। */
    fun verify(password: String, stored: String): Boolean {
        return try {
            if (!isHash(stored)) return false
            val parts = stored.split("$")
            if (parts.size != 4) return false
            val iters = parts[1].toIntOrNull() ?: return false
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val expected = Base64.decode(parts[3], Base64.NO_WRAP)
            val actual = pbkdf2(password, salt, iters, expected.size * 8)
            constantTimeEquals(expected, actual)
        } catch (_: Throwable) { false }
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int, keyLenBits: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLenBits)
        return SecretKeyFactory.getInstance(ALGO).generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].toInt() xor b[i].toInt())
        return r == 0
    }
}
