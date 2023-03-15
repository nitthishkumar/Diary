package com.zoho.diary.notes


import java.io.Serializable

data class Label (
    val labelId: Int = -1,
    private var enteredTitle: String = "No Title",
//    val notesContained: ArrayList<Note> = ArrayList()
) : Serializable {

    var labelTitle = if(enteredTitle.length < 30) enteredTitle else enteredTitle.substring(0,30)
    set(value) {
        field = if(value.length < 30) value else value.substring(0,30)
    }

    override fun equals(other: Any?): Boolean {
        return if(other == null || other !is Label){
            false
        } else {
            this.labelId == other.labelId
        }
    }

    override fun hashCode(): Int {
        return labelId
    }
}