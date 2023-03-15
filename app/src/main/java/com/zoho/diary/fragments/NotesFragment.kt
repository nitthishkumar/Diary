package com.zoho.diary.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.zoho.diary.activities.EditNoteActivity
import com.zoho.diary.activities.HomeActivity
import com.zoho.diary.activities.R
import com.zoho.diary.adapters.NotesAdapter
import com.zoho.diary.dbutils.DBHandler
import com.zoho.diary.extensions.getAllContent
import com.zoho.diary.notes.Label
import com.zoho.diary.notes.Note
import com.zoho.takenote.utils.ListOption
import com.zoho.takenote.utils.SortOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class NotesFragment() : Fragment() {

    lateinit var tvEmptyText: TextView
    lateinit var fabNewNote: FloatingActionButton
    lateinit var rvNotes: RecyclerView
    private lateinit var notesAdapter: NotesAdapter
    lateinit var notesList: ArrayList<Note>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sortOptionSaved: SortOption
    private lateinit var listOptionSaved: ListOption
    private lateinit var loadingBar: ProgressBar
    private lateinit var flNoNoteFound: FrameLayout
    private lateinit var daObject: DBHandler
    var underLabel: Label? = null
    private lateinit var scopeForIO: CoroutineScope

    constructor(clickedLabel: Label): this(){
        underLabel = clickedLabel
    }

    @SuppressLint("SetTextI18n")
    private fun initViews(v: View){
        rvNotes = v.findViewById(R.id.rvNotesListNF)
        tvEmptyText = v.findViewById(R.id.tvEmptyNoteNF)
        fabNewNote = v.findViewById(R.id.fabNewNoteNF)
        flNoNoteFound = v.findViewById(R.id.flNoNotes)
        loadingBar = v.findViewById(R.id.progressBarMainNF)
        daObject = DBHandler(requireContext())
        if(underLabel == null) {
            fabNewNote.visibility = View.VISIBLE
        } else{
            fabNewNote.visibility = View.GONE
            tvEmptyText.text = "No Notes found under ${underLabel!!.labelTitle}"
        }
        fabNewNote.setOnClickListener {
            val editIntent = Intent(requireContext(), EditNoteActivity::class.java)
            startActivity(editIntent)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_notes, container, false)
        initViews(v)
        scopeForIO = CoroutineScope(IO)
        return v
    }

    override fun onResume() {
        super.onResume()
        loadSavedPreferences()
        scopeForIO.launch {
            loadNotes()
        }
    }

    private fun loadSavedPreferences() {
        val spf = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
        val sortBy = spf.getString("sortBy", "${SortOption.LAST_EDITED_FIRST}")!!
        val listBy = spf.getString("listBy", "${ListOption.CARD_VIEW}")!!
        sortOptionSaved = SortOption.valueOf(sortBy)
        listOptionSaved = ListOption.valueOf(listBy)
    }

    private fun loadGivenNotesOnUI(){
        rvNotes.layoutManager = when(listOptionSaved){
            ListOption.CARD_VIEW -> {
                notesAdapter = NotesAdapter(R.layout.item_note_card,requireContext(), notesList)
                StaggeredGridLayoutManager(2, 1)
            }
            else -> {
                notesAdapter = NotesAdapter(R.layout.item_note_list_card,requireContext(), notesList)
                LinearLayoutManager(requireContext())
            }
        }
        rvNotes.adapter = notesAdapter
        loadingBar.visibility = View.INVISIBLE

        if(notesAdapter.itemCount < 1){
            tvEmptyText.visibility = View.VISIBLE
        } else{
            tvEmptyText.visibility = View.INVISIBLE
        }
        sortChanged()
    }

    override fun onPause() {
        super.onPause()
        underLabel = null

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return true
    }

    private fun sortChanged(){
        when(sortOptionSaved){
            SortOption.ALPHABETICAL -> notesAdapter.sortByContent()
            SortOption.LAST_EDITED_FIRST -> notesAdapter.sortByDate()
            else -> return
        }
    }

    fun filterNotes(queryText: String) {

        if(queryText.isNotEmpty()) {
            fabNewNote.visibility = View.GONE
            tvEmptyText.visibility = View.GONE
        } else {
            fabNewNote.visibility = View.VISIBLE
        }

        val filteredList: ArrayList<Note> = ArrayList()
        for (item in notesList) {
            if (item.getAllContent().lowercase().contains(queryText.lowercase())) {
                filteredList.add(item)
            }
        }
        if (filteredList.size < 1) {
            rvNotes.visibility = View.GONE
            flNoNoteFound.visibility = View.VISIBLE
        } else {
            rvNotes.visibility = View.VISIBLE
            flNoNoteFound.visibility = View.GONE
            notesAdapter.filterList(filteredList)
        }
    }

    fun confirmAndDeleteMultiple() {
        var str = "abc "
        for(noteItem in (rvNotes.adapter as NotesAdapter).selectedNotes) {
            str = "$str ${noteItem.noteTitle}"
        }
        val confirmDelete: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        confirmDelete.setIcon(R.drawable.ic_black_delete_24)
        confirmDelete.setTitle(getString(R.string.confirm_deletion))
        confirmDelete.setMessage("Are you sure you want to delete selected Notes?")
        confirmDelete.setPositiveButton("YES") { _, _ ->
            CoroutineScope(IO).launch {
                deleteMultiple()
                withContext(Main) {
                    (rvNotes.adapter as NotesAdapter).resetView()
                }
            }
        }
        confirmDelete.setNegativeButton("NO") { dialog, _ ->
            dialog?.cancel()
            (rvNotes.adapter as NotesAdapter).resetView()
        }
        confirmDelete.show()
    }


    private suspend fun deleteMultiple() {
        for(noteItem in (rvNotes.adapter as NotesAdapter).selectedNotes) {
            daObject.deleteNote(noteItem)
        }
        notesList = daObject.getAllNotes()
        withContext(Main){
            (rvNotes.adapter as NotesAdapter).resetView()
            loadGivenNotesOnUI()
        }
    }

    fun shareMultiple() {
        val sharedText = StringBuffer("")
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Insert Subject here")
        for(noteItem in (rvNotes.adapter as NotesAdapter).selectedNotes) {
            sharedText.append("${noteItem.noteTitle}\n${noteItem.noteContent}\n\n")
        }
        shareIntent.putExtra(Intent.EXTRA_TEXT, sharedText.toString())
        startActivity(Intent.createChooser(shareIntent, "Share via"))
        (rvNotes.adapter as NotesAdapter).resetView()
    }
    
    private suspend fun loadNotes() {
        notesList = daObject.getAllNotes()
        notesList = if (underLabel == null) daObject.getAllNotes() else daObject.getNotesUnderLabel(underLabel!!)
        withContext(Main){
            loadingBar.visibility = View.VISIBLE
            loadGivenNotesOnUI()
        }
    }

}