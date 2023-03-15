package com.zoho.diary.notes

import com.zoho.takenote.utils.ColorOption
import java.io.Serializable
import java.util.*

data class Note(
    val noteId: Long = -1,
    var noteTitle: String = "",
    var noteContent: String = "",
    var lastEdited: Date = Date(),
    var color: ColorOption = ColorOption.WHITE,
    var todosList: ArrayList<ToDo> = ArrayList()
) : Serializable {

    override fun equals(other: Any?): Boolean {
        return if(other !is Note) {
            false
        } else {
            other.noteId == this.noteId
        }
    }

    override fun hashCode(): Int {
        return noteId.hashCode()
    }
}