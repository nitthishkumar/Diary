package com.zoho.diary.notes

import java.io.Serializable

data class ToDo(
    val jobId: Long = -1,
    var jobDescription: String = "",
    var isCompleted: Boolean = false
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if(other !is ToDo) {
            return false
        } else {
            return this.jobId == other.jobId
        }
    }

    override fun hashCode(): Int {
        return jobId.hashCode()
    }

    override fun toString(): String {
        val sb = StringBuffer("")
        sb.append("$jobDescription:")
        if(isCompleted) {
            sb.append("T ;")
        } else {
            sb.append("F ;")
        }
        return sb.toString()

    }
}