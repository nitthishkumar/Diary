package com.zoho.diary.extensions

import com.zoho.diary.notes.Note

fun Note.isEmpty(): Boolean{
    return this.noteTitle.trim().isEmpty() &&
            this.noteContent.trim().isEmpty() &&
            this.todosList.isEmpty()
}

fun Note.titlePreview(): String{
    return if(noteTitle.length > 12){
        "${noteTitle.substring(0, 10)}..."
    } else if(noteTitle.isNotEmpty()){
        noteTitle
    } else{
        "(no title)"
    }
}


fun Note.contentPreview(): String{
    var retNote = when {
        noteContent.length > 50 -> {
            "${noteContent.substring(0, 50)}..."
        }
        noteContent.isNotEmpty() -> {
            noteContent
        }
        else -> {
            "(no content)"
        }
    }
    val splitString = retNote.split("\n")
    retNote = ""
    for(i in 0 until 9.coerceAtMost(splitString.size)) {
        retNote = "$retNote\n${splitString[i]}"
    }
    if(splitString.size > 10) {
        retNote = "$retNote\n..."
    }
    return retNote
}

fun Note.getAllContent(): String {
    val contentBuffer = StringBuffer("${this.noteTitle.lowercase()} ${this.noteContent.lowercase()}")
    for(item in this.todosList) {
        contentBuffer.append(item.jobDescription+" ")
    }
    return contentBuffer.toString()
}