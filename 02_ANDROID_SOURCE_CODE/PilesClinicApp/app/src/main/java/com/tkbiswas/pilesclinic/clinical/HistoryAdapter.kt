package com.tkbiswas.pilesclinic.clinical

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.native.DateUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private val items: List<ClinicalVisit>) :
    RecyclerView.Adapter<HistoryAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvType: TextView = itemView.findViewById(R.id.tvType)
        val tvRole: TextView = itemView.findViewById(R.id.tvRole)
        val tvSummary: TextView = itemView.findViewById(R.id.tvSummary)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_entry, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val visit = items[position]
        holder.tvType.text = visit.type
        holder.tvRole.text = if (visit.doneByRole == UserRole.DOCTOR) "by Doctor" else "by Staff"
        holder.tvSummary.text = visit.summary.ifBlank { "(no details recorded)" }
        holder.tvTimestamp.text = DateUtil.displayWithTime(Date(visit.timestamp))
    }
}
