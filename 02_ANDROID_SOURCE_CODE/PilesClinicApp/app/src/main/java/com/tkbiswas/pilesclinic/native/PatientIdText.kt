package com.tkbiswas.pilesclinic.native

/**
 * 🆔 TK-এর স্থায়ী নিয়ম (28.07.2026 ১.১০ pm, ফটো-প্রুফসহ):
 * **যেখানেই রোগীর নাম ও মোবাইল একসাথে দেখানো হয়, সেখানে Patient ID-ও থাকতে হবে।**
 *
 * সব পর্দা যাতে একই রকম দেখায়, তাই লেখাটা এই এক জায়গা থেকেই তৈরি হয়।
 *
 * ⚠️ গুরুত্বপূর্ণ শর্ত: **এনকোয়ারির রেকর্ডে Patient ID থাকে না** (তখনো
 * রেজিস্ট্রেশন হয়নি)। তাই ID ফাঁকা হলে কিছুই বাড়তি দেখানো হয় না — শুধু
 * নাম ও মোবাইল, ঠিক আগের মতো। ⛔ কোথাও খালি লেবেল বা ফাঁকা ঘর দেখানো যাবে না।
 *
 * ⛔ এখানে কোনো নেটের কাজ নেই — যে তথ্য ডাকার সময় হাতে আছে শুধু তাই সাজানো হয়।
 * তাই এতে অ্যাপ এক মুহূর্তও ধীর হয় না।
 */
object PatientIdText {

    /** শুধু দশ অঙ্কের নম্বরটুকু, যাতে সব পর্দায় একই রকম দেখায়। */
    private fun digits(mobile: String): String =
        mobile.filter { it.isDigit() }.takeLast(10)

    /**
     * এক লাইনে: `NAME · 9876543210 · KNE-0012`
     * ID না থাকলে: `NAME · 9876543210`
     */
    fun line(name: String, mobile: String, patientId: String, phoneIcon: Boolean = false): String {
        val d = digits(mobile)
        val who = name.trim().ifBlank { d }
        val sb = StringBuilder(who)
        if (d.isNotBlank() && !who.equals(d, ignoreCase = true)) {
            sb.append("  ·  ")
            if (phoneIcon) sb.append("\uD83D\uDCDE ")
            sb.append(d)
        }
        val pid = patientId.trim()
        if (pid.isNotBlank()) sb.append("  ·  \uD83C\uDD94 ").append(pid)
        return sb.toString()
    }

    /**
     * যেখানে নাম আলাদা ঘরে আর মোবাইল আলাদা ঘরে আছে, সেখানে মোবাইলের ঘরে
     * বসানোর জন্য: `9876543210 · KNE-0012` (ID না থাকলে শুধু নম্বর)।
     */
    fun mobileWithId(mobile: String, patientId: String, phoneIcon: Boolean = false): String {
        val d = digits(mobile).ifBlank { mobile.trim() }
        val sb = StringBuilder()
        if (phoneIcon && d.isNotBlank()) sb.append("\uD83D\uDCDE ")
        sb.append(d)
        val pid = patientId.trim()
        if (pid.isNotBlank()) sb.append("  ·  \uD83C\uDD94 ").append(pid)
        return sb.toString()
    }

    /**
     * যে লেখাটা আগে থেকেই ঠিক আছে (যেমন `+919876543210`) তার শেষে শুধু
     * ID জুড়ে দেয়। ID ফাঁকা হলে লেখাটা এক অক্ষরও বদলায় না।
     */
    fun append(text: String, patientId: String): String {
        val pid = patientId.trim()
        return if (pid.isBlank()) text else "$text  ·  \uD83C\uDD94 $pid"
    }

    /** শুধু ID-এর ব্যাজ, আলাদা ঘরে বসানোর জন্য। ফাঁকা হলে খালি স্ট্রিং। */
    fun badge(patientId: String): String {
        val pid = patientId.trim()
        return if (pid.isBlank()) "" else "\uD83C\uDD94 $pid"
    }
}
