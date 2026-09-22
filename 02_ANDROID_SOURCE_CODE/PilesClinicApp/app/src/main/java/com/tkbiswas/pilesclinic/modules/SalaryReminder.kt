package com.tkbiswas.pilesclinic.modules

import android.content.Context

/**
 * 🟢 B629 (11.08.2026, TK-নির্দেশ): "Salary Due" মনে করানো।
 *
 * যে স্টাফের Salary **enabled** ও **Salary Date (day of month)** দেওয়া আছে,
 * আজকের দিন সেই দিন-বা-তার-পরে এসে গেছে, অথচ **এই মাসের** স্যালারি এখনো
 * পুরো দেওয়া হয়নি — শুধু তাদেরই তালিকা/সংখ্যা ফেরে। Master ও Doctor-এর
 * ঘণ্টায় (BellCounter) ও Briefing-এ মনে করানোর জন্য।
 *
 * ⛔ বিদ্যমান `hr.salary_config` + `hr.salary_payments` (+ নাম/ব্রাঞ্চের জন্য
 *    `hr.staff_profiles`) পড়েই হিসাব — কোনো নতুন টেবিল/SQL নেই।
 * ⛔ ছোট টেক্সট-টেবিল, ছবি টানে না; পড়া ব্যর্থ হলে খালি ফেরে (কিছু ভাঙে না)।
 */
object SalaryReminder {

    data class Due(
        val code: String,
        val name: String,
        val branch: String,
        val amount: Double,
        val salaryDay: String,
        val forMonth: String = "",          // 💰 V1198 — কোন মাসের বেতন
        val forMonthLabel: String = ""      // 💰 V1198 — "August 2026"
    )

    fun dueList(context: Context): List<Due> {
        return try {
            ModuleAuth.signInCurrentSession(context)
            val cfg = ModuleAuth.getRows(
                "hr", "salary_config",
                "select=person_code,salary_enabled,salary_amount,salary_date"
            )
            if (cfg.length() == 0) return emptyList()

            val cal = java.util.Calendar.getInstance()
            val cy = cal.get(java.util.Calendar.YEAR)
            val cm = cal.get(java.util.Calendar.MONTH) + 1
            val today = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val cur = "%04d-%02d".format(cy, cm)

            /* 💰🔒 V1198 (০৮.০৯.২০২৬, TK-নির্দেশ ও কোডে মিলিয়ে দেখা, হুবহু):
                 *"আগস্টের স্যালারি সেপ্টেম্বরে দেয়া হয়, সেপ্টেম্বরের স্যালারি
                  অক্টোবরে দেয়া হবে — তাহলে নোটিফিকেশনে কেন বারবার স্যালারি ডিউ
                  লেখা আসে"*
               🔴 কারণ: নিয়ম বসানো ছিল **"চলতি মাসের বেতন চলতি মাসেই বাকি"** —
                  তাই সেপ্টেম্বরে সেপ্টেম্বরের বেতন খুঁজে না পেয়ে চিরকাল Due
                  দেখাত। ⇒ এখন বেতনের দিন এলে **আগের মাসের** বেতন বাকি কিনা দেখে।
               ⛔ টাকার অঙ্ক · কোথায় জমা · কে দেখবে — কিছুই বদলায়নি, শুধু
                  **কোন মাসটা খোঁজা হবে** সেটা। */
            val dueMonth = run {
                val c2 = java.util.Calendar.getInstance()
                c2.add(java.util.Calendar.MONTH, -1)
                "%04d-%02d".format(c2.get(java.util.Calendar.YEAR), c2.get(java.util.Calendar.MONTH) + 1)
            }

            val payR = ModuleAuth.getRows(
                "hr", "salary_payments",
                "select=person_code,amount,for_month&for_month=eq.$dueMonth"
            )
            val paid = HashMap<String, Double>()
            for (i in 0 until payR.length()) {
                val p = payR.getJSONObject(i)
                val c = p.optString("person_code")
                paid[c] = (paid[c] ?: 0.0) + p.optDouble("amount", 0.0)
            }

            // 🔴 V404 (16.08.2026): `active` ঘরটাও টানা হচ্ছে — বাদ-দেওয়া কর্মীর
            //    নাম যেন আর কখনো "বেতন বাকি"-তে না ওঠে। আগে উঠত: SWAPNA ADHIKARI
            //    কাজ ছেড়ে দেওয়ার পরেও তাঁর নাম প্রতি মাসে দেখানোর কথা ছিল।
            /* \U0001f534\U0001f512 V1140 (০৬.০৯.২০২৬, TK-রিপোর্ট) — TK: *"KISHAN-10, KISHAN-11
               আজকের থেকে জয়েন করেছে, তাহলে তাদের স্যালারি ডিউ কেন দেখাচ্ছে?
               ১ মাস যাবে তারপর তো তাদের স্যালারি দেবো।"*
               \U0001f534 কারণ (কোডে মেপে পাওয়া): নিয়ম ছিল শুধু দুটো — বেতনের দিন এসে
                  গেছে কিনা, আর এই মাসে দেওয়া হয়েছে কিনা। **জয়েনিং তারিখ কখনো
                  দেখাই হত না**, তাই আজ জয়েন করা কর্মীও আজই "বেতন বাকি"-তে উঠত।
               ⇒ এখন `join_date`-ও টানা হয়: যিনি **এই মাসে (বা পরে) জয়েন করেছেন**,
                 তাঁর নাম এ মাসে ওঠে না — প্রথম বেতন পরের মাস থেকে।
               ⛔ তারিখ ফাঁকা/অচেনা হলে আগের মতোই ধরা হয় — পুরনো কারও নাম হারায় না। */
            val prof = ModuleAuth.getRows(
                "hr", "staff_profiles",
                "select=person_code,full_name,branch,active,join_date"
            )
            val info = HashMap<String, Pair<String, String>>()
            val removedCodes = HashSet<String>()
            val notYetDue = HashSet<String>()
            for (i in 0 until prof.length()) {
                val pr = prof.getJSONObject(i)
                info[pr.optString("person_code")] = Pair(pr.optString("full_name"), pr.optString("branch"))
                // ⛔ ঘরটা না থাকলে (পুরনো সারি) ডিফল্ট true ⇒ কেউ ভুলে বাদ পড়বে না।
                if (!pr.optBoolean("active", true)) removedCodes.add(pr.optString("person_code"))
                val jm = pr.optString("join_date").trim().take(7)   // "yyyy-MM"
                /* 🔴 V1140 + V1198 — যিনি **যে মাসের বেতন বাকি** সেই মাসের পরে
                   জয়েন করেছেন, তাঁর নাম ওঠে না (আগস্টে জয়েন করলে সেপ্টেম্বরে
                   আগস্টের বেতন ঠিকই বাকি দেখাবে)। */
                if (jm.length == 7 && jm > dueMonth) notYetDue.add(pr.optString("person_code"))
            }

            val out = ArrayList<Due>()
            for (i in 0 until cfg.length()) {
                val c = cfg.getJSONObject(i)
                if (!c.optBoolean("salary_enabled", false)) continue
                val amount = c.optDouble("salary_amount", 0.0)
                if (amount <= 0) continue
                val sday = c.optString("salary_date").trim().toIntOrNull() ?: continue
                if (today < sday) continue                        // স্যালারির দিন এখনো আসেনি
                val code = c.optString("person_code")
                if (removedCodes.contains(code)) continue           // 🔴 V404: বাদ-দেওয়া কর্মী
                if (notYetDue.contains(code)) continue              // 🔴 V1140: এই মাসেই জয়েন
                if ((paid[code] ?: 0.0) >= amount) continue        // এ মাসে দেওয়া হয়ে গেছে
                val nb = info[code] ?: Pair(code, "")
                out.add(Due(code, nb.first.ifBlank { code }, nb.second, amount, sday.toString(),
                    dueMonth, monthLabel(dueMonth)))
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** "2026-08" → "August 2026"; চেনা না গেলে যা আছে তাই। */
    fun monthLabel(ym: String): String = try {
        val p = ym.split("-")
        val names = arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        names[p[1].toInt() - 1] + " " + p[0]
    } catch (_: Throwable) { ym }

    fun dueCount(context: Context): Int = dueList(context).size
}
