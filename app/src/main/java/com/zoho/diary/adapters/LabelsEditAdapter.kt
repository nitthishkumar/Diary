package com.zoho.diary.adapters

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zoho.diary.activities.EditLabelsActivity
import com.zoho.diary.activities.R
import com.zoho.diary.notes.Label

class LabelsEditAdapter(
    context: Context,
    private val mLabelsList: ArrayList<Label>
) : RecyclerView.Adapter<LabelsEditAdapter.LabelViewHolder>() {

    private var changedHolder: LabelViewHolder? = null
    private val mActivity = context as EditLabelsActivity

    class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){//@SelectLabel
        val ivLabelIcon: ImageView = itemView.findViewById(R.id.ivViewLabel2)
        val ivDeleteLabel: ImageView = itemView.findViewById(R.id.ivDeleteLabel2)

        val tvLabelTitle: TextView = itemView.findViewById(R.id.tvLabelTitle)
        val etLabelTitle: EditText = itemView.findViewById(R.id.etLabelTitle)

        val ivSaveLabel: ImageView = itemView.findViewById(R.id.ivSaveLabel2)
        val ivEditLabel: ImageView = itemView.findViewById(R.id.ivEditLabel2)
    }

    fun resetLastOpenedView() : Boolean{
        if(changedHolder == null){
            return true
        }
        val holder = changedHolder!!

        val mTitle = holder.etLabelTitle.text.toString().lowercase()
        if (mTitle.isEmpty()){
            holder.etLabelTitle.error = "Label name cannot be empty"
            return false

        } else if(mActivity.mTitleSet.contains(mTitle) && mActivity.mTitleSet.indexOf(mTitle) != holder.adapterPosition) {
            holder.etLabelTitle.error = "Label already exists @set: ${mActivity.mTitleSet.indexOf(mTitle)} @rv ${holder.adapterPosition}"
            return false
        }

        holder.etLabelTitle.visibility = View.GONE
        holder.tvLabelTitle.text = holder.etLabelTitle.text
        holder.tvLabelTitle.visibility = View.VISIBLE

        holder.ivSaveLabel.visibility = View.GONE
        holder.ivEditLabel.visibility = View.VISIBLE

        holder.ivDeleteLabel.visibility = View.GONE
        holder.ivLabelIcon.visibility = View.VISIBLE
        mLabelsList[holder.adapterPosition].labelTitle = holder.etLabelTitle.text.toString()//mLine that saves the et text
        return true
    }

    private fun setViewToEdit(holder: LabelViewHolder){
//        Log.d("LEA", "abc setViewToEdit() for ${holder.etLabelTitle.text} 1")
        if (!resetLastOpenedView()){
            return
        }
        mActivity.resetView()
        changedHolder = holder
        holder.ivLabelIcon.visibility = View.GONE
        holder.ivDeleteLabel.visibility = View.VISIBLE

        holder.ivEditLabel.visibility = View.GONE
        holder.ivSaveLabel.visibility = View.VISIBLE

        holder.tvLabelTitle.visibility = View.GONE
        holder.etLabelTitle.visibility = View.VISIBLE
    }

    override fun getItemCount() = mLabelsList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val itemView: View = inflater.inflate(R.layout.item_label_edit, parent, false)
        val holder = LabelViewHolder(itemView)
        holder.ivEditLabel.setOnClickListener {
            setViewToEdit(holder)
        }
        holder.tvLabelTitle.setOnClickListener{
            setViewToEdit(holder)
        }

        holder.ivSaveLabel.setOnClickListener{
            resetLastOpenedView()

        }
        return holder
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        holder.tvLabelTitle.text = mLabelsList[position].labelTitle
        holder.etLabelTitle.text = SpannableStringBuilder(mLabelsList[position].labelTitle)
        holder.etLabelTitle.setText(mLabelsList[position].labelTitle)
        holder.ivDeleteLabel.setOnClickListener{
            mActivity.confirmAndDelete(holder, position)
        }
    }
}