package com.tkbiswas.pilesclinic.clinical

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.R

class InvestigationAdapter(
    private val items: MutableList<InvestigationEntry>,
    private val canToggle: Boolean
) : RecyclerView.Adapter<InvestigationAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cb: CheckBox = itemView.findViewById(R.id.cbTest)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_investigation_test, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]
        holder.cb.setOnCheckedChangeListener(null)
        holder.cb.text = entry.name
        holder.cb.isChecked = entry.isSelected
        holder.cb.isEnabled = canToggle

        holder.tvStatus.text = when {
            entry.isDoctorApproved -> "Advised"
            entry.isSelected -> "Requested"
            else -> ""
        }
        holder.tvStatus.setTextColor(
            holder.itemView.resources.getColor(
                if (entry.isDoctorApproved) R.color.clinic_accent_green else R.color.clinic_accent_amber,
                null
            )
        )

        holder.cb.setOnCheckedChangeListener { _, isChecked ->
            entry.isSelected = isChecked
            if (!isChecked) entry.isDoctorApproved = false
            notifyItemChanged(holder.bindingAdapterPosition)
        }
    }
}
