package com.zoho.diary.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.zoho.diary.activities.EditNoteActivity
import com.zoho.diary.activities.R
import com.zoho.takenote.utils.ColorOption

class ColorAdapter(
    context: Context,
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {
    private val activity = context as EditNoteActivity
    private val colorsList: ArrayList<ColorOption> = ArrayList()

    init {
        for(i in ColorOption.values()){
            colorsList.add(i)
        }
        colorsList.remove(ColorOption.BLACK)
    }

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val context: Context = parent.context
        val inflater = LayoutInflater.from(context)
        val itemView: CardView = inflater.inflate(R.layout.item_color, parent, false) as CardView
        return ColorViewHolder(itemView)
    }

    @SuppressLint("ResourceAsColor", "SetTextI18n")
    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        (holder.itemView as CardView).setCardBackgroundColor(Color.parseColor(colorsList[position].rgb))

        holder.itemView.setOnClickListener {
            activity.changeCurrentNoteColor(colorsList[position])
        }
    }

    override fun getItemCount(): Int {
        return colorsList.size
    }

}