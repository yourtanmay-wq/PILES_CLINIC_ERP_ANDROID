package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import java.io.File

/**
 * TK-REQUESTED ADDITION (2026-07-16): "Export to Excel" (CSV) feature so TK
 * has a portable copy of clinic data outside the app/Supabase, for backup
 * or accounting -- independent of this project entirely (reads via the
 * existing SupabaseClient.fetchList, does not touch any repository/table
 * logic used elsewhere).
 *
 * WHY CSV, NOT A REAL .xlsx FILE: a true Excel (.xlsx) writer library
 * (e.g. Apache POI) is heavy for Android (large app size, known dependency
 * conflicts) and was not already part of this project. Plain CSV opens
 * directly in Excel/Google Sheets with zero extra libraries, zero new
 * dependency-conflict risk, and no app-size increase -- the safer choice
 * for an already-large project. If TK specifically wants a true .xlsx
 * file later, that is a separate, bigger request.
 */
object CsvExportHelper {

    /** Converts a Supabase JSONArray of rows into a CSV file (all columns
     *  across all rows as the header, comma/quote/newline properly escaped
     *  per the standard CSV rules) and returns the written file. */
    fun writeCsv(context: Context, tableName: String, rows: JSONArray): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "$tableName.csv")

        // Union of every key seen across every row, in first-seen order --
        // so no column is silently dropped even if one row has extra fields.
        val columns = LinkedHashSet<String>()
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            row.keys().forEach { columns.add(it) }
        }

        fun escape(raw: String): String {
            val needsQuoting = raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r")
            val escaped = raw.replace("\"", "\"\"")
            return if (needsQuoting) "\"$escaped\"" else escaped
        }

        val sb = StringBuilder()
        sb.append(columns.joinToString(",") { escape(it) }).append("\r\n")
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            sb.append(columns.joinToString(",") { col ->
                escape(if (row.isNull(col)) "" else row.optString(col, ""))
            }).append("\r\n")
        }

        file.writeText(sb.toString())
        return file
    }
}
