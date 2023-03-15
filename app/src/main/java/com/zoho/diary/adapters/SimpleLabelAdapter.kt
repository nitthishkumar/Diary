package com.zoho.diary.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zoho.diary.activities.R
import com.zoho.diary.notes.Label

class SimpleLabelAdapter(
    private var selectedLabels: ArrayList<Label>
) : RecyclerView.Adapter<SimpleLabelAdapter.LabelTitleHolder>() {

    class LabelTitleHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvLabelTitle: TextView = itemView.findViewById(R.id.tvLabelItemTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelTitleHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_simple_label, parent, false)
        val v = LabelTitleHolder(view)
        v.tvLabelTitle.setOnClickListener{
//            (context as EditNoteActivity).startActivity(Intent(context, SelectLabelActivity::class.java))
        }
        return v
    }

    override fun onBindViewHolder(holder: LabelTitleHolder, position: Int) {
        holder.tvLabelTitle.text = selectedLabels[position].labelTitle
    }

    override fun getItemCount(): Int {
        return selectedLabels.size
    }

}