package com.zoho.diary.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.zoho.diary.activities.EditNoteActivity
import com.zoho.diary.activities.HomeActivity
import com.zoho.diary.activities.R
import com.zoho.diary.extensions.contentPreview
import com.zoho.diary.extensions.findYear
import com.zoho.diary.extensions.getAllContent
import com.zoho.diary.extensions.titlePreview
import com.zoho.diary.notes.Note
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet


class NotesAdapter(
    private val childItemId: Int,
    val context: Context,
    private var notesList: ArrayList<Note>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val selectedNotes: HashSet<Note> = HashSet()
    private val homeActivity = context as HomeActivity
    private var oldTitle: String? = null

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cv: CardView = itemView.findViewById(R.id.cvNoteCardItem)
        val tvTitle: TextView = itemView.findViewById(R.id.tvCardItemTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvCardItemContent)
    }

    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cv: CardView = itemView.findViewById(R.id.cvlvNoteCardItem)
        val tvTitle: TextView = itemView.findViewById(R.id.tvListItemTitleExt)
        val tvDate: TextView = itemView.findViewById(R.id.tvListItemDateExt)
        val tvTime: TextView = itemView.findViewById(R.id.tvListItemTimeExt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context: Context = parent.context
        val inflater: LayoutInflater = LayoutInflater.from(context)
        return if(childItemId == R.layout.item_note_card){
            val itemView: View = inflater.inflate(R.layout.item_note_card, parent, false)
            CardViewHolder(itemView)
        } else{
            val itemView: View = inflater.inflate(R.layout.item_note_list_card, parent, false)
            ListViewHolder(itemView)
        }

    }

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentNote: Note = notesList[position]
        val bgColor = Color.parseColor(currentNote.color.rgb)
        when(holder){
            is CardViewHolder -> {
                holder.tvTitle.text = currentNote.titlePreview()
                holder.tvContent.text = currentNote.contentPreview()
                if(currentNote.todosList.size > 0) {
                    addToDosPreview(holder.tvContent, currentNote)
                }
            }
            is ListViewHolder -> {
                holder.tvTitle.text = currentNote.titlePreview()
                when {
                    isToday(currentNote.lastEdited) -> {
                        holder.tvDate.text = "Today"
                        holder.tvTime.text = DateFormat.getTimeInstance(DateFormat.SHORT).format(currentNote.lastEdited)
                    }
                    currentNote.lastEdited.findYear() == Date().findYear() -> {
                        val f = SimpleDateFormat("dd, MMM")
                        holder.tvDate.text = f.format(currentNote.lastEdited)
                        holder.tvTime.text = DateFormat.getTimeInstance(DateFormat.SHORT).format(currentNote.lastEdited)
                    }
                    else -> {
                        holder.tvDate.text = DateFormat.getDateInstance(DateFormat.SHORT).format(currentNote.lastEdited)
                    }
                }
            }
        }

        if(holder.itemView is CardView){
            (holder.itemView as CardView).setCardBackgroundColor(bgColor)
        }

        holder.itemView.setOnClickListener {
            when {
                selectedNotes.size < 1 -> {
                    openClickedNote(holder, position)
                }
                selectedNotes.contains(notesList[position]) -> {
                    unselectNote(holder, position)
                }
                else -> {
                    selectNote(holder, position)
                }
            }
        }

        holder.itemView.setOnLongClickListener {
            Log.d("NA", "abc onLongPressed()")
            selectNote(holder, position)
            true
        }

    }

    private fun addToDosPreview(tvContent: TextView, note: Note) {
        val ssb = SpannableStringBuilder(" ")
        var completed = 0
        var incomplete = 0
        for(todoItem in note.todosList) {
            if(todoItem.isCompleted) {
                completed += 1
            } else {
                incomplete += 1
            }
        }

        if(incomplete > 0) {
            for (todoItem in note.todosList) {
                val imgUnchecked = ImageSpan(context, R.drawable.ic_baseline_unchecked_20)
                val positionToPlaceImageAt = ssb.length
                if (todoItem.isCompleted) {
                    completed += 1
                    continue
                } else {
                    ssb.setSpan(imgUnchecked, positionToPlaceImageAt - 1, positionToPlaceImageAt, 0)
                    ssb.append("\t${todoItem.jobDescription}\n ")
                }
            }
        }
        if(completed > 0) {
            for (todoItem in note.todosList) {
                val imgChecked = ImageSpan(context, R.drawable.ic_baseline_checked_20)
                val positionToPlaceImageAt = ssb.length
                if (todoItem.isCompleted) {
                    ssb.setSpan(imgChecked, positionToPlaceImageAt - 1, positionToPlaceImageAt, 0)
                    ssb.append("\t${todoItem.jobDescription}\n ")
                } else {
                    continue
                }
            }
        }
        tvContent.text = ssb
    }

    private fun openClickedNote(holder: RecyclerView.ViewHolder, position: Int) {
        val ii = Intent(holder.itemView.context, EditNoteActivity::class.java)
        ii.putExtra("mNote", notesList[position])
        holder.itemView.context.startActivity(ii)
    }

    @SuppressLint("ResourceAsColor")
    private fun selectNote(holder: RecyclerView.ViewHolder, position: Int) {
        if(selectedNotes.isEmpty()) {
            setViewForSelection()
        }
        selectedNotes.add(notesList[position])
//        when(holder) {
//            is CardViewHolder -> holder.cv.setCardBackgroundColor(R.color.background_grey)
//            is ListViewHolder -> holder.cv.setCardBackgroundColor(R.color.background_grey)
//            else -> Log.d("NA", "abc nun")
//        }
        (holder.itemView as CardView).setBackgroundColor(Color.parseColor("#777777"))
        homeActivity.supportActionBar?.title = "${selectedNotes.size} Selected"

    }

    private fun unselectNote(holder: RecyclerView.ViewHolder, position: Int) {
        selectedNotes.remove(notesList[position])
        (holder.itemView as CardView).setBackgroundColor(Color.parseColor(notesList[position].color.rgb))
        if(selectedNotes.isEmpty()) {
            resetView()
        }
    }

    private fun setViewForSelection() {
        oldTitle = homeActivity.supportActionBar?.title.toString()
        homeActivity.loadedFragment.fabNewNote.visibility = View.GONE
        homeActivity.setForSelection = true
        homeActivity.searchItem.isVisible = false
        homeActivity.deleteMultipleItem.isVisible = true
        homeActivity.shareMultipleItem.isVisible = true
        homeActivity.cancelSelectionItem.isVisible = true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun resetView() {
        homeActivity.supportActionBar?.title = oldTitle
        homeActivity.loadedFragment.fabNewNote.visibility = View.VISIBLE
        homeActivity.setForSelection = false
        homeActivity.navigationView.visibility = View.VISIBLE
        homeActivity.searchItem.isVisible = true
        homeActivity.shareMultipleItem.isVisible = false
        homeActivity.deleteMultipleItem.isVisible = false
        homeActivity.cancelSelectionItem.isVisible = false
        notifyDataSetChanged()
        for(noteItem in selectedNotes) {
            val adapterPosition = notesList.indexOf(noteItem)
            val holder = (homeActivity.loadedFragment.rvNotes).findViewHolderForLayoutPosition(adapterPosition)!!
//            val holder = (homeActivity.loadedFragment.rvNotes).findViewHolderForPosition(adapterPosition)     //deprecated
            holder.itemView.setBackgroundColor(Color.parseColor(noteItem.color.rgb))
        }
        selectedNotes.clear()
    }

    override fun getItemCount() = notesList.size

    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filteredList: ArrayList<Note>){
        notesList = filteredList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun sortByContent(){
        for(i in notesList.indices){
            for(j in i+1 until notesList.size)
            if(notesList[i].getAllContent() > notesList[j].getAllContent()){
                val temp = notesList[i]
                notesList[i] = notesList[j]
                notesList[j] = temp
            }
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun sortByDate(){
        for(i in notesList.indices){
            for(j in i+1 until notesList.size){
                if(notesList[i].lastEdited.before(notesList[j].lastEdited)){
                    val temp = notesList[i]
                    notesList[i] = notesList[j]
                    notesList[j] = temp
                }
            }
        }
        notifyDataSetChanged()
    }

    @SuppressLint("SimpleDateFormat")
    private fun isToday(givenDate: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd")
        return fmt.format(givenDate).equals(fmt.format(Date()))
    }

}