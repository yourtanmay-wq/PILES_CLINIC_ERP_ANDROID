package com.tkbiswas.pilesclinic.clinical

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.R

class PrescriptionAdapter(
    private val items: MutableList<MedicineEntry>,
    private val editable: Boolean,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<PrescriptionAdapter.VH>() {

    inner class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val etName: EditText = itemView.findViewById(R.id.etMedName)
        val etDosage: EditText = itemView.findViewById(R.id.etDosage)
        val etFrequency: EditText = itemView.findViewById(R.id.etFrequency)
        val etDuration: EditText = itemView.findViewById(R.id.etDuration)
        val etInstructions: EditText = itemView.findViewById(R.id.etInstructions)
        val tvRemove: TextView = itemView.findViewById(R.id.tvRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prescription_medicine, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]

        /* 🔵🔒 V548 (২২.০৮.২০২৬) — **আসল বাগ, কোড ধরে ধরা**:
           নিচের লাইনগুলোর মন্তব্যে লেখা ছিল *"re-setting text clears the previous
           watchers"* — কিন্তু লেখা বসালে পুরোনো watcher **মোছে না, উল্টে চলে**।
           তাই RecyclerView যখন একটা সারি আবার ব্যবহার করত, **আগের ওষুধের**
           watcher নতুন ওষুধের লেখা পেয়ে —
             • আগের ওষুধের `entry.dosage` বদলে দিত, আর
             • `rememberRxDose(আগের নাম, নতুন ডোজ)` — অর্থাৎ **এক ওষুধের ডোজ
               আরেক ওষুধের চিরস্থায়ী ডিফল্ট** হয়ে বসত (এভাবেই "After Food"
               হারিয়ে গিয়ে When ফাঁকা ছাপা হত)।
           এখন লেখা বসানোর **আগেই** পুরোনো watcher খুলে ফেলা হয়।
           ⛔ নিচের বাকি সব — মাপ · ঘর · সেভের নিয়ম — এক অক্ষরও বদলায়নি। */
        holder.etName.clearSimpleWatcher()
        holder.etDosage.clearSimpleWatcher()
        holder.etFrequency.clearSimpleWatcher()
        holder.etDuration.clearSimpleWatcher()
        holder.etInstructions.clearSimpleWatcher()

        holder.etName.setTextKeepState(entry.name)
        holder.etDosage.setTextKeepState(entry.dosage)
        holder.etFrequency.setTextKeepState(entry.frequency)
        holder.etDuration.setTextKeepState(entry.duration)
        holder.etInstructions.setTextKeepState(entry.instructions)

        val fields = listOf(
            holder.etName, holder.etDosage, holder.etFrequency,
            holder.etDuration, holder.etInstructions
        )
        for (f in fields) {
            f.isEnabled = editable
        }

        holder.etName.addSimpleWatcher { entry.name = it }
        holder.etDosage.addSimpleWatcher { entry.dosage = it; ClinicalRepository.rememberRxDose(entry.name, it) }
        // 🍯 V956 (TK-নির্দেশ ০১.০৯.২০২৬): এই ঘরে "After Food with Honey" লিখলে
        //   সেটাই ওই ওষুধের চিরস্থায়ী When হয়ে যায় — Dose/Type/Days-এর মতোই।
        holder.etFrequency.addSimpleWatcher { entry.frequency = it; ClinicalRepository.rememberRxWhen(entry.name, it) }
        holder.etDuration.addSimpleWatcher { entry.duration = it }
        holder.etInstructions.addSimpleWatcher { entry.instructions = it }

        holder.tvRemove.visibility = if (editable) android.view.View.VISIBLE else android.view.View.GONE
        holder.tvRemove.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onRemove(pos)
        }
    }

    /** 🔵 V548: সারিটা আবার ব্যবহার হওয়ার আগে পুরোনো watcher খুলে ফেলা। */
    private fun EditText.clearSimpleWatcher() {
        (tag as? TextWatcher)?.let { removeTextChangedListener(it) }
        tag = null
    }

    private fun EditText.addSimpleWatcher(onChanged: (String) -> Unit) {
        // Remove any tagged watcher from a recycled view first.
        (tag as? TextWatcher)?.let { removeTextChangedListener(it) }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onChanged(s?.toString().orEmpty())
            }
        }
        addTextChangedListener(watcher)
        tag = watcher
    }
}
