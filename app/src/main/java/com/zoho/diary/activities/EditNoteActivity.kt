package com.zoho.diary.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zoho.diary.adapters.SimpleLabelAdapter
import com.zoho.diary.adapters.ToDosAdapter
import com.zoho.diary.dbutils.DBHandler
import com.zoho.diary.extensions.getAllContent
import com.zoho.diary.extensions.isEmpty
import com.zoho.diary.notes.Label
import com.zoho.diary.notes.Note
import com.zoho.diary.notes.ToDo
import com.zoho.takenote.utils.ColorOption
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.text.DateFormat
import java.text.FieldPosition
import java.util.*
import kotlin.collections.ArrayList


class EditNoteActivity : AppCompatActivity() {

    private lateinit var daObject: DBHandler
    private lateinit var etTitle: EditText
    private lateinit var tvLastEdited: TextView
    private lateinit var currentNote: Note
    private var isDeleted = false
    private lateinit var clEditNote: CoordinatorLayout
    private lateinit var rvMarkedLabels: RecyclerView
    private lateinit var markedLabels: ArrayList<Label>
    private lateinit var toolbar: Toolbar
    private lateinit var etContent: EditText
    private lateinit var rvToDosContent: RecyclerView
    private lateinit var flToDos: FrameLayout
    private lateinit var pbChangeContent: ProgressBar
    private val textHistory: Stack<Pair<String, Boolean>> = Stack()//string, isFromNoteContent
    var isTextContent = true
    val scopeForSaving = CoroutineScope(IO)

    private fun initProperties() {
        daObject = DBHandler(this)
        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)
        tvLastEdited = findViewById(R.id.tvLastEdited)
        clEditNote = findViewById(R.id.clNewNote)
        rvMarkedLabels = findViewById(R.id.rvAddedLabels)
        rvToDosContent = findViewById(R.id.rvToDosContent)
        flToDos = findViewById(R.id.flToDosContent)
        pbChangeContent = findViewById(R.id.pbChangeContent)
        markedLabels = ArrayList()

//        collectData()
        rvMarkedLabels.adapter = SimpleLabelAdapter(markedLabels)
        rvMarkedLabels.layoutManager = LinearLayoutManager(this)
        rvToDosContent.layoutManager = LinearLayoutManager(this)

        toolbar = findViewById(R.id.toolbarEditNote)
        toolbar.setTitleTextColor(Color.parseColor(ColorOption.WHITE.rgb))
        setSupportActionBar(toolbar)

    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)
        window.statusBarColor = Color.parseColor(getString(R.color.status_yellow))
        initProperties()
        addTextChangeListener()

        if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            if ("text/plain" == intent.type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                currentNote = Note(noteContent = sharedText?: "...")
                updateExistingNote()
            }
        } else {
            try {
                currentNote = intent.extras!!.get("mNote") as Note
                updateExistingNote()
            } catch (e: NullPointerException) {
                currentNote = Note()
                etTitle.requestFocus()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        scopeForSaving.launch {
            loadMarkedLabels()
        }
        changeCurrentNoteColor()
    }

    @SuppressLint("ResourceType")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d("ENA", "abc onCreateOptionsMenu() ###1")
        Log.d("ENA", "abc onCreateOptionsMenu() >${currentNote.getAllContent()}< ###")
        menuInflater.inflate(R.menu.menu_edit_note, menu)
        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        try {
            actionBar?.title = when {
                currentNote.noteId == -3L -> "Clone Note"
                currentNote.getAllContent().trim().isNotEmpty() -> "Edit Note"
                else -> "New Note"

            }
        } catch(e: Exception){
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.btnUndo2 -> undoTextChange()
            R.id.btnMore2 -> {
                scopeForSaving.launch {
                    saveNoteToDB(false)
                    while (currentNote.noteId < 1 && !currentNote.isEmpty()) {
                        delay(1)
                    }
                    withContext(Main){
                        val bottomSheet = BottomSheetEditNote(currentNote)
                        bottomSheet.show(supportFragmentManager, "ModalBottomSheet")
                    }
                }
            }
        }
        return true
    }

    override fun onBackPressed() {
        Log.d("ENA", "abc onBackPressed() $isDeleted")
        if(!isDeleted){
            scopeForSaving.launch{
                saveNoteToDB(true)
            }
        }
        super.onBackPressed()
    }

    fun confirmAndDelete() {
        val confirmDelete: AlertDialog.Builder = AlertDialog.Builder(this@EditNoteActivity)
        confirmDelete.setIcon(R.drawable.ic_black_delete_24)
        confirmDelete.setTitle("Confirm Deletion")
        confirmDelete.setMessage("Are you sure you want to delete this note?")
        confirmDelete.setPositiveButton("YES") { _, _ ->
            scopeForSaving.launch {
                daObject.deleteNote(currentNote)
                isDeleted = true
            }
            finish()
        }
        confirmDelete.setNegativeButton("NO") { dialog, _ -> dialog?.cancel() }
        confirmDelete.show()
    }

    suspend fun cloneNote(){
        val cloneNote = Note(-3)
        cloneNote.noteTitle = etTitle.text.toString().trim()
        cloneNote.noteContent = etContent.text.toString().trim()
        cloneNote.lastEdited = Date()
        cloneNote.todosList = ArrayList()
        for(todoItem in currentNote.todosList) {
            cloneNote.todosList.add(todoItem.copy(jobId = -1))
        }
        cloneNote.color = currentNote.color

        if(cloneNote.isEmpty()){
            withContext(Main){
                makeToast(getString(R.string.empty_note))
            }
            return
        }
        val ii = Intent(this, EditNoteActivity::class.java)
        ii.putExtra("mNote", cloneNote)
        startActivity(ii)
        finish()
    }

    private fun makeToast(message: String) {
        Toast.makeText(this@EditNoteActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun addTextChangeListener() {
        etContent.addTextChangedListener( object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(textHistory.size < 1 || s.toString() != textHistory.peek().first) {
                    textHistory.push(Pair(s.toString(), true))
                }
            }

            override fun afterTextChanged(s: Editable?) {}

        })
        etTitle.addTextChangedListener( object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(textHistory.size < 1 || s.toString() != textHistory.peek().first) {
                    textHistory.push(Pair(s.toString(), false))
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    @SuppressLint("SetTextI18n")
    private fun undoTextChange(){
        if(textHistory.empty()){
            return
        }
        var topPair = textHistory.pop()
        var old = topPair.first
        var isContent = topPair.second
        if(isContent && old == etContent.text.toString() && textHistory.size > 0) {
            topPair =  textHistory.pop()
            old = topPair.first
            isContent = topPair.second
        } else if ( !isContent && old == etTitle.text.toString()  && textHistory.size > 0) {
            topPair =  textHistory.pop()
            old = topPair.first
            isContent = topPair.second

        }
        if(isContent) {
            etContent.setText(old)
//            etContent.requestFocus()
            etContent.setSelection(etContent.text.toString().length)
        } else {
            etTitle.setText(old)
//            etTitle.requestFocus()
            etTitle.setSelection(etTitle.text.toString().length)
        }
    }

    private suspend fun saveNoteToDB(warnIfEmpty: Boolean) {
        val title: String = etTitle.text.toString().trim()
        val content: String = etContent.text.toString().trim()
        currentNote.noteTitle = title
        currentNote.noteContent = if(currentNote.todosList.isEmpty()) content else " "
        currentNote.lastEdited = Date()
        if (currentNote.isEmpty() && currentNote.todosList.isEmpty()) {
            daObject.deleteNote(currentNote)
            if(warnIfEmpty) {
                withContext(Main) { makeToast("Discarding empty note") }
            }
        } else if(currentNote.noteTitle.isEmpty() && currentNote.todosList.size == 1 && currentNote.todosList[0].isEmpty() && warnIfEmpty) {
            daObject.deleteNote(currentNote)
            if(warnIfEmpty) {
                withContext(Main) { makeToast("Discarding empty note") }
            }
        }
        else {
            currentNote = daObject.writeToDB(currentNote)
        }
    }

    @SuppressLint("ResourceType")
    private suspend fun loadMarkedLabels(){
        if(currentNote.noteId < 0){
            return
        }
        markedLabels.clear()
        val ii = daObject.getAllLabelNoteSet()
        for(pairItem in ii){
            if(pairItem.mNote == currentNote){
                markedLabels.add(pairItem.mLabel)
            }
        }
        withContext(Main){
            rvMarkedLabels.post{
                rvMarkedLabels.adapter = SimpleLabelAdapter(markedLabels)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateExistingNote(){
        etTitle.setText(currentNote.noteTitle)
        val lastEdited = currentNote.lastEdited
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(lastEdited)
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(lastEdited)
        tvLastEdited.text = "Last Edited: $date $time"
        if(currentNote.todosList.size < 1) {
            etContent.setText(currentNote.noteContent)
            etContent.requestFocus()
        } else {
            loadToDos()
        }
    }

    private fun loadToDos() {
        isTextContent = false
        etContent.visibility = View.GONE
        rvToDosContent.adapter = ToDosAdapter(this, currentNote.todosList)
        pbChangeContent.visibility = View.GONE
        flToDos.visibility = View.VISIBLE
    }

    fun shareNote(){
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Insert Subject here")
        val sharedText = StringBuffer("${etTitle.text}\n${etContent.text}")
        for(mToDo in currentNote.todosList) {
            sharedText.append("${mToDo.jobDescription}\n")
        }
        shareIntent.putExtra(Intent.EXTRA_TEXT, sharedText.toString())
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun changeCurrentNoteColor(){
        val bgColor = Color.parseColor(currentNote.color.rgb)
        etContent.setBackgroundColor(bgColor)
        clEditNote.setBackgroundColor(bgColor)
    }

    fun changeCurrentNoteColor(colorClicked: ColorOption){
        currentNote.color = colorClicked
        changeCurrentNoteColor()
    }

    suspend fun textToCheckList() {
        withContext(Main){
            pbChangeContent.visibility = View.VISIBLE
            etContent.visibility = View.GONE
        }
        val jobDescArray = etContent.text.toString().split("\n")
        for(descItem in jobDescArray) {
            if(descItem.isEmpty()) {
                continue
            }
            currentNote.todosList.add(ToDo(jobDescription = descItem))
        }
        etContent.setText("")
        currentNote.noteContent = " "
        withContext(Main) {
            loadToDos()
        }
    }

    fun confirmDeleteCheckBoxes() : Boolean {
        var isChanged = false
        if(currentNote.todosList.isEmpty()) {
            scopeForSaving.launch {
                checkListToText()
            }
            return true
        }
        val confirmDeleteToDos: AlertDialog.Builder = AlertDialog.Builder(this)
        confirmDeleteToDos.setIcon(R.drawable.ic_black_delete_24)
        confirmDeleteToDos.setTitle("Confirm Remove")
        confirmDeleteToDos.setMessage(resources.getString(R.string.remove_checkbox))
        confirmDeleteToDos.setPositiveButton("REMOVE") { _, _ ->
            scopeForSaving.launch {
                checkListToText()
                isChanged = true
            }
        }
        confirmDeleteToDos.setNegativeButton("CANCEL") { dialog, _ -> dialog?.cancel() }
        confirmDeleteToDos.show()
        return isChanged
    }

    private suspend fun checkListToText() {
        isTextContent = true
        withContext(Main) {
            pbChangeContent.visibility = View.VISIBLE
            flToDos.visibility = View.GONE
//            collectData()
        }
        val content = StringBuilder("")
        for(todoItem in currentNote.todosList) {
            content.append("${todoItem.jobDescription}\n")
        }//at last
        withContext(Main) {
            etContent.setText(content.toString())
            pbChangeContent.visibility = View.GONE
            etContent.visibility = View.VISIBLE
        }
        daObject.deleteToDosOfNote(currentNote)
        currentNote.todosList.clear()
    }

    fun removeToDo(position: Int) {
        val mTodo = currentNote.todosList[position]
        Log.d("ENA", "abc before removal ${currentNote.todosList}")
        Log.d("ENA", "abc removing $mTodo @${currentNote.todosList.indexOf(mTodo)}")
        currentNote.todosList.removeAt(position)
        Log.d("ENA", "abc after removal ${currentNote.todosList}")
        scopeForSaving.launch {
            daObject.deleteToDo(mTodo)
            withContext(Main) {
                rvToDosContent.adapter = ToDosAdapter(this@EditNoteActivity, currentNote.todosList)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addToDo(v: View) {
        v.visibility
        if(currentNote.todosList.isNotEmpty() && currentNote.todosList[currentNote.todosList.size-1].jobDescription.trim().isEmpty()) {
            return
        }
        Log.d("ENA", "abc before adding ${currentNote.todosList}")
        currentNote.todosList.add(currentNote.todosList.size, ToDo())
        Log.d("ENA", "abc after adding ${currentNote.todosList}")
        (rvToDosContent.adapter as ToDosAdapter).addNewToDo()
    }


    /*fun collectData() {
        val mAdapter = rvToDosContent.adapter as ToDosAdapter
        for(i in 0 until mAdapter.itemCount) {
            val childViewHolder = rvToDosContent.getChildViewHolder(rvToDosContent.getChildAt(i)) as ToDosAdapter.ToDoViewHolder
            mAdapter.orderedList[i].jobDescription = childViewHolder.etTodo.text.toString()
        }
    }*/

}