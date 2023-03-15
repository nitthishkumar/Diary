package com.zoho.diary.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.zoho.diary.activities.R
import com.zoho.diary.notes.Label

class LabelSelectionAdapter(
    private val isLabelled: HashMap<Label, Boolean>,
    private var labelsList: ArrayList<Label>
) : RecyclerView.Adapter<LabelSelectionAdapter.LabelSelectionViewHolder>(){


    class LabelSelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val llSelectLabel: CoordinatorLayout = itemView.findViewById(R.id.clSelectLabel)
        val checkBox: CheckBox = itemView.findViewById(R.id.cbSelectLabel)
        val tvLabelTitle: TextView = itemView.findViewById(R.id.tvSelectLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelSelectionViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val itemView: View = inflater.inflate(R.layout.item_mark_label, parent, false)
        val childViewHolder = LabelSelectionViewHolder(itemView)
        childViewHolder.checkBox.setOnClickListener{
            invertCheckBox(childViewHolder)
        }
        childViewHolder.llSelectLabel.setOnClickListener{
            childViewHolder.checkBox.isChecked = !childViewHolder.checkBox.isChecked
            invertCheckBox(childViewHolder)
        }

        return childViewHolder
    }

    override fun onBindViewHolder(holder: LabelSelectionViewHolder, position: Int) {
        holder.tvLabelTitle.text = labelsList[position].labelTitle
        holder.checkBox.isChecked = isLabelled[labelsList[position]] == true
    }

    private fun invertCheckBox(childViewHolder: LabelSelectionViewHolder) {
        val labelSelected = labelsList[childViewHolder.adapterPosition]
        isLabelled[labelSelected] = childViewHolder.checkBox.isChecked
    }

    override fun getItemCount(): Int {
        return isLabelled.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun reloadLabels(newLabelsList: ArrayList<Label>) {
//        Log.d("LSAd", "abc reloading ${labelsList.size}")
        labelsList = newLabelsList
        notifyDataSetChanged()
    }

}