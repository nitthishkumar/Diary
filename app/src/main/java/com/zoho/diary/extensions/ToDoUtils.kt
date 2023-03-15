package com.zoho.diary.extensions

import com.zoho.diary.notes.ToDo

fun ToDo.isEmpty() : Boolean{
    return this.jobDescription.trim().isEmpty()
}