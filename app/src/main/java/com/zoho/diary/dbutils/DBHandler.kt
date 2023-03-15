package com.zoho.diary.dbutils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns._ID
import android.util.Log
import com.zoho.diary.notes.Label
import com.zoho.diary.notes.LabelNotePair
import com.zoho.diary.extensions.getAllContent
import com.zoho.diary.extensions.titlePreview
import com.zoho.diary.notes.Note
import com.zoho.diary.notes.ToDo
import com.zoho.takenote.utils.ColorOption
import java.text.DateFormat.getDateTimeInstance
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class DBHandler(context: Context?) : SQLiteOpenHelper(
    context,
    DiaryDBContract.DB_NAME, null,
    DiaryDBContract.DB_VERSION
){

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DiaryDBContract.NotesTable.CREATE_TABLE)
        db.execSQL(DiaryDBContract.LabelsTable.CREATE_TABLE)
        db.execSQL(DiaryDBContract.NoteLabelEntry.CREATE_TABLE)
        db.execSQL(DiaryDBContract.ToDosTable.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(DiaryDBContract.NotesTable.DELETE_TABLE)
        db.execSQL(DiaryDBContract.LabelsTable.DELETE_TABLE)
        db.execSQL(DiaryDBContract.NoteLabelEntry.DELETE_TABLE)
        db.execSQL(DiaryDBContract.ToDosTable.DELETE_TABLE)
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys=ON;")
        }
    }

    private fun isExistingNote(note : Note): Boolean{
        val db = this.readableDatabase
        val selectQuery = "SELECT $_ID FROM ${DiaryDBContract.NotesTable.TABLE_NAME}"
        val cursor = db.rawQuery(selectQuery, null)
        var existingNote = false
        if(cursor.count > 0){
            cursor.moveToFirst()
            do {
                if(cursor.getLong(cursor.getColumnIndex(_ID)) == note.noteId){
                    existingNote = true
                    break
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return existingNote && note.noteId > 0
    }

    fun writeToDB(note: Note): Note {
        return if (note.noteId > 0) {
            updateNote(note)
        } else {
            addNewNote(note)
        }
    }

    private fun changesMade(editedCopy: Note, retrievedCopy: Note) : Boolean{
        when{
            editedCopy.getAllContent() != retrievedCopy.getAllContent() -> return true
            editedCopy.color != retrievedCopy.color -> return true
            editedCopy.todosList.size != retrievedCopy.todosList.size -> return true
        }
        for(i in 0 until editedCopy.todosList.size) {
            val rToDo = retrievedCopy.todosList[i]
            val mToDo = editedCopy.todosList[i]
            when {
                rToDo.jobId != mToDo.jobId -> return true
                rToDo.isCompleted != mToDo.isCompleted -> return true
                rToDo.jobDescription != mToDo.jobDescription -> return true
            }
        }
        return false
    }

    private fun updateNote(currentCopy: Note): Note {
        val lastSavedCopy: Note = getNote(currentCopy.noteId)!!
        if(!changesMade(currentCopy, lastSavedCopy)){
                return currentCopy
        }
        val cvUpdateNote = ContentValues()
        cvUpdateNote.put(_ID, currentCopy.noteId)
        cvUpdateNote.put(DiaryDBContract.NotesTable.TITLE_COL, currentCopy.noteTitle)
        cvUpdateNote.put(DiaryDBContract.NotesTable.CONTENT_COL, currentCopy.noteContent)
        cvUpdateNote.put(DiaryDBContract.NotesTable.COLOR_COL, currentCopy.color.rgb)
        cvUpdateNote.put(DiaryDBContract.NotesTable.LAST_EDITED_COL, currentCopy.lastEdited.toLocaleString())
        val db = this.writableDatabase
        db.beginTransaction()
        db.update(DiaryDBContract.NotesTable.TABLE_NAME, cvUpdateNote, "$_ID = ?", arrayOf(currentCopy.noteId.toString()))
        db.setTransactionSuccessful()
        db.endTransaction()
        updateToDosOfNote(currentCopy)
        return currentCopy
    }

    private fun addNewNote(note: Note): Note {
        note.lastEdited = Date()
        val db = this.writableDatabase
        db.beginTransaction()
        val newNoteEntry = ContentValues()
        newNoteEntry.put(DiaryDBContract.NotesTable.TITLE_COL, note.noteTitle)
        newNoteEntry.put(DiaryDBContract.NotesTable.CONTENT_COL, note.noteContent)
        newNoteEntry.put(DiaryDBContract.NotesTable.COLOR_COL, note.color.rgb)
        newNoteEntry.put(
            DiaryDBContract.NotesTable.LAST_EDITED_COL,
            note.lastEdited.toLocaleString()
        )
        val newId = db.insert(DiaryDBContract.NotesTable.TABLE_NAME, null, newNoteEntry)
        db.setTransactionSuccessful()
        db.endTransaction()
        val addedNote = note.copy(noteId = newId)
        if(addedNote.todosList.size > 0) {
            updateToDosOfNote(addedNote)
        }
        Log.d("DBH", "abc addNewNote() ${note.noteTitle} W $newId added")
        return addedNote
    }

    fun deleteNote(note: Note) {
        val deleteQuery = "DELETE FROM ${DiaryDBContract.NotesTable.TABLE_NAME} WHERE $_ID = ${note.noteId}"
        Log.d("DBH", "deleting $deleteQuery abc")
        val db = this.writableDatabase
        db.beginTransaction()
        db.execSQL(deleteQuery)
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    private fun dateHelper(s: String): Date {
        val formatter: SimpleDateFormat = getDateTimeInstance() as SimpleDateFormat
        return formatter.parse(s)!!
    }

    private fun getNote(givenId: Long): Note?{
        val note = Note(givenId)
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM ${DiaryDBContract.NotesTable.TABLE_NAME} WHERE $_ID = $givenId"
        val cursor: Cursor = db.rawQuery(selectQuery, null)
        val mNote = if(cursor.count > 0){
            cursor.moveToFirst()
            do{
                note.noteTitle = cursor.getString(1)
                note.noteContent = cursor.getString(2)
                note.color = ColorOption.getColorByValue(cursor.getString(3))
                note.lastEdited = dateHelper(cursor.getString(4))
                note.todosList = getToDosOfNote(note)
            } while (cursor.moveToNext())
            note.also {
                cursor.close()
            }
        } else {
            cursor.close()
            null
        }
        return mNote
    }

    fun getAllNotes(): ArrayList<Note>{
        val notesList: ArrayList<Note> = ArrayList()
        val db = this.readableDatabase
        db.beginTransaction()
        val cursor = db.rawQuery("SELECT * FROM ${DiaryDBContract.NotesTable.TABLE_NAME}", null)
        db.setTransactionSuccessful()
        db.endTransaction()
        if(cursor.count > 0){
            cursor.moveToFirst()
            do {
                val mNote =  Note(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    dateHelper(cursor.getString(4)),
                    ColorOption.getColorByValue(cursor.getString(3)),
                )
                mNote.todosList = getToDosOfNote(mNote)
                notesList.add(mNote)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return notesList
    }

    /*------------------------------------------------------------------------------------------------------------------------*/

    private fun isExistingLabel(label: Label): Boolean{
        val db = this.readableDatabase
        db.beginTransaction()
        val selectQuery = "SELECT $_ID FROM ${DiaryDBContract.LabelsTable.TABLE_NAME}"
        val cursor = db.rawQuery(selectQuery, null)
        var existingLabel = false
        if(cursor.count > 0){
            cursor.moveToFirst()
            do {
                if(cursor.getInt(cursor.getColumnIndex(_ID)) == label.labelId){
                    existingLabel = true
                    break
                }
            } while (cursor.moveToNext())
        }
        db.setTransactionSuccessful()
        db.endTransaction()
        cursor.close()
        return existingLabel && label.labelId > 0
    }

    fun writeToDB(passedLabel: Label): Label {//done
        return if(isExistingLabel(passedLabel)){
            updateLabel(passedLabel)
            passedLabel
        } else {
            addNewLabel(passedLabel)
        }
    }

    private fun addNewLabel(passedLabel: Label): Label {
        val newLabelEntry = ContentValues()
        newLabelEntry.put(DiaryDBContract.LabelsTable.TITLE_COL, passedLabel.labelTitle)
        val db = this.writableDatabase
        db.beginTransaction()
        val newLabelId = db.insert(DiaryDBContract.LabelsTable.TABLE_NAME, null, newLabelEntry).toInt()
        db.setTransactionSuccessful()
        db.endTransaction()
        return passedLabel.copy(labelId = newLabelId)
    }

    private fun updateLabel(currentCopy: Label){
        val lastChangedCopy: Label = getLabel(currentCopy.labelId)!!
        when {
            lastChangedCopy.labelTitle == currentCopy.labelTitle -> return

            currentCopy.labelTitle.isEmpty() -> deleteLabel(currentCopy)

            else -> {
                val db = this.writableDatabase
                db.beginTransaction()
                val updateLabelQuery = "UPDATE ${DiaryDBContract.LabelsTable.TABLE_NAME} SET " +
                        "${DiaryDBContract.LabelsTable.TITLE_COL} = '${currentCopy.labelTitle}' " +
                        "WHERE $_ID = ${currentCopy.labelId}"
                db.execSQL(updateLabelQuery)
                db.setTransactionSuccessful()
                db.endTransaction()
            }
        }
    }

    private fun getLabel(givenId: Int): Label? {//done
        val label = Label(givenId)
        val selectQuery = "SELECT * FROM ${DiaryDBContract.LabelsTable.TABLE_NAME} WHERE $_ID = $givenId"
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery(selectQuery, null)
        val returnLabel =  if (cursor.count > 0) {
            cursor.moveToFirst()
            do {
                label.labelTitle = cursor.getString(1)
            } while(cursor.moveToNext())
            label
        } else{
            null
        }
        cursor.close()
        return returnLabel
    }

    fun getAllLabels(): ArrayList<Label>{
        val mLabelsList: ArrayList<Label> = ArrayList()
        val db = this.readableDatabase
        db.beginTransaction()
        val cursor = db.rawQuery("SELECT * FROM ${DiaryDBContract.LabelsTable.TABLE_NAME}", null)
        if(cursor.count > 0){
            cursor.moveToFirst()
            do{
                mLabelsList += Label(
                    cursor.getInt(0),
                    cursor.getString(1)
                )
            } while (cursor.moveToNext())
        }
        db.setTransactionSuccessful()
        db.endTransaction()
        cursor.close()
        return  mLabelsList
    }

    @SuppressLint("Recycle")
    fun getNotesUnderLabel(givenLabel: Label): ArrayList<Note>{
        val mNotes = ArrayList<Note>()
        val db = this.readableDatabase
        db.beginTransaction()
        val cursor = db.rawQuery("SELECT ${DiaryDBContract.NoteLabelEntry.NOTE_ID_COL} FROM ${DiaryDBContract.NoteLabelEntry.TABLE_NAME} " +
                "WHERE ${DiaryDBContract.NoteLabelEntry.LABEL_ID_COL} = ${givenLabel.labelId}", null)
        db.setTransactionSuccessful()
        db.endTransaction()
        if(cursor.count > 0){
            cursor.moveToFirst()
            do{
                val tNote = getNote(cursor.getLong(0))!!
                mNotes.add(tNote)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return mNotes
    }

    @SuppressLint("Recycle")
    fun getAllLabelNoteSet(): HashSet<LabelNotePair>{
        val linkSet: HashSet<LabelNotePair> = HashSet()
        val db = this.readableDatabase
        db.beginTransaction()
        val cursor = db.rawQuery("SELECT * FROM ${DiaryDBContract.NoteLabelEntry.TABLE_NAME}", null)
        if(cursor.count > 0) {
            cursor.moveToFirst()
            do {
                val tNote: Note = getNote(cursor.getLong(0))!!
                val tLabel: Label = getLabel(cursor.getInt(1))!!
                linkSet.add(LabelNotePair(tLabel, tNote))
            } while (cursor.moveToNext())
        }
        db.setTransactionSuccessful()
        db.endTransaction()
        cursor.close()
        return linkSet
    }

    fun addLabelsToNote(givenNote: Note, labelsSelected: ArrayList<Label>) {
        deleteExistingLabelMarks(givenNote)
        val db = this.writableDatabase
        db.beginTransaction()
        for(labelItem in labelsSelected) {
            Log.d("DBH", "abc ${givenNote.titlePreview()} :: $labelItem")
            val newPairEntry = ContentValues()
            newPairEntry.put(DiaryDBContract.NoteLabelEntry.LABEL_ID_COL, labelItem.labelId)
            newPairEntry.put(DiaryDBContract.NoteLabelEntry.NOTE_ID_COL, givenNote.noteId)
            db.insert(DiaryDBContract.NoteLabelEntry.TABLE_NAME, null, newPairEntry)
        }
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    fun deleteLabel(label: Label){
        val deleteQuery = "DELETE FROM ${DiaryDBContract.LabelsTable.TABLE_NAME} WHERE $_ID = ${label.labelId}"
        val db = this.writableDatabase
        db.beginTransaction()
        db.execSQL(deleteQuery)
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    private fun deleteExistingLabelMarks(note: Note){
        val db = this.writableDatabase
        db.beginTransaction()
        db.delete(DiaryDBContract.NoteLabelEntry.TABLE_NAME, "${DiaryDBContract.NoteLabelEntry.NOTE_ID_COL} = ?", arrayOf(note.noteId.toString()))
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    /*------------------------------------------------------------------------------------------------------------------------*/

    fun addToDo(parentNote: Note, todoItem: ToDo) : ToDo {
        val todoEntry = ContentValues()
        val completedValue = if(todoItem.isCompleted) 1 else 0
        todoEntry.put(DiaryDBContract.ToDosTable.DESCRIPTION_COL, todoItem.jobDescription)
        todoEntry.put(DiaryDBContract.ToDosTable.COMPLETION_STATUS_COL, completedValue)
        todoEntry.put(DiaryDBContract.ToDosTable.PARENT_NOTE_ID_COL, parentNote.noteId)
        val db = this.writableDatabase
        db.beginTransaction()
        val newId = db.insert(DiaryDBContract.ToDosTable.TABLE_NAME,  null, todoEntry)
        db.setTransactionSuccessful()
        db.endTransaction()
        return todoItem.copy(jobId = newId)
    }

    private fun updateToDosOfNote(parentNote : Note) {
        Log.d("DBH","abc updateToDosOfNote()")
        val db = this.writableDatabase
        db.beginTransaction()
        for(i in 0 until parentNote.todosList.size) {
            val todoItem = parentNote.todosList[i]
//            if(todoItem.jobDescription.trim().isEmpty()) {
//                continue
//            }
            val todoEntry = ContentValues()
            val completedValue = if(todoItem.isCompleted) 1 else 0
            todoEntry.put(DiaryDBContract.ToDosTable.DESCRIPTION_COL, todoItem.jobDescription.trim())
            todoEntry.put(DiaryDBContract.ToDosTable.COMPLETION_STATUS_COL, completedValue)
            todoEntry.put(DiaryDBContract.ToDosTable.PARENT_NOTE_ID_COL, parentNote.noteId)
//            Log.d("DBH","abc writing \n$todoEntry")
            if (todoItem.jobId > 0) {
                todoEntry.put(_ID, todoItem.jobId)
                db.update(DiaryDBContract.ToDosTable.TABLE_NAME, todoEntry, "$_ID = ?", arrayOf(todoItem.jobId.toString()))
            } else {
                val addedId = db.insert(DiaryDBContract.ToDosTable.TABLE_NAME, null, todoEntry)
                parentNote.todosList[i] = todoItem.copy(jobId = addedId)
            }
        }
        db.setTransactionSuccessful()
        db.endTransaction()
//        for(todoItem in parentNote.todosList) {
//            if(todoItem.jobDescription.trim().isEmpty()) {
//                deleteToDo(todoItem)
//            }
//        }
    }

    @SuppressLint("Recycle")
    private fun getToDosOfNote(note: Note) : ArrayList<ToDo>{
        val mToDos = ArrayList<ToDo>()
        val selectQuery = "SELECT * FROM ${DiaryDBContract.ToDosTable.TABLE_NAME} WHERE ${DiaryDBContract.ToDosTable.PARENT_NOTE_ID_COL} = ${note.noteId}"
        val db = this.readableDatabase
        db.beginTransaction()
        val cursor = db.rawQuery(selectQuery, null)
        db.setTransactionSuccessful()
        db.endTransaction()
        if (cursor.count > 0) {
            cursor.moveToFirst()
            do {
                mToDos += ToDo(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getInt(2) > 0
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return mToDos
    }

    fun deleteToDo(todo: ToDo) {
        val deleteQuery = "DELETE FROM ${DiaryDBContract.ToDosTable.TABLE_NAME} WHERE $_ID = ${todo.jobId}"
        val db = this.writableDatabase
        db.beginTransaction()
        db.execSQL(deleteQuery)
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    fun deleteToDosOfNote(note: Note) {
        val deleteQuery = "DELETE FROM ${DiaryDBContract.ToDosTable.TABLE_NAME} WHERE ${DiaryDBContract.ToDosTable.PARENT_NOTE_ID_COL} = ${note.noteId}"
        val db = this.writableDatabase
        db.beginTransaction()
        db.execSQL(deleteQuery)
        db.setTransactionSuccessful()
        db.endTransaction()
    }

}

//fun main() {
//    val m = Date()
//    val a: String = m.toLocaleString()
//
//}

//fun Date.myLocaleString(): String {
//    val s = "12-Aug-2021, 4:19:32 PM"
//    val sf = SimpleDateFormat("dd-MMM-yyyy ")
//}