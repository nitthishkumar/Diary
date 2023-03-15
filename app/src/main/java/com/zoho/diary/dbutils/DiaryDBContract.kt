package com.zoho.diary.dbutils

import android.provider.BaseColumns
import android.provider.BaseColumns._ID


object DiaryDBContract {
    const val DB_NAME = "Diary.db"
    const val DB_VERSION = 7

    object NotesTable : BaseColumns {
        const val TABLE_NAME = "notes"
        const val TITLE_COL = "note_title"
        const val CONTENT_COL = "note_content"
        const val COLOR_COL = "note_color"
        const val LAST_EDITED_COL = "last_edited"

        const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                "$_ID INTEGER PRIMARY KEY, " +
                "$TITLE_COL TEXT, " +
                "$CONTENT_COL TEXT, " +
                "$COLOR_COL TEXT, " +
                "$LAST_EDITED_COL TEXT)"

        const val DELETE_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    object LabelsTable : BaseColumns {
        const val TABLE_NAME = "labels"
        const val TITLE_COL = "label_title"

        const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                "$_ID INTEGER PRIMARY KEY, " +
                "$TITLE_COL TEXT" +
                ");"

        const val DELETE_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    object NoteLabelEntry : BaseColumns {
        const val TABLE_NAME = "labelled_notes"
        const val LABEL_ID_COL = "label_id"
        const val NOTE_ID_COL = "note_id"

        const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                "$NOTE_ID_COL INTEGER, " +
                "$LABEL_ID_COL INTEGER, " +
                "FOREIGN KEY($NOTE_ID_COL) REFERENCES ${NotesTable.TABLE_NAME}(${_ID}) ON DELETE CASCADE, " +
                "FOREIGN KEY($LABEL_ID_COL) REFERENCES ${LabelsTable.TABLE_NAME}(${_ID}) ON DELETE CASCADE, " +
                "PRIMARY KEY($NOTE_ID_COL, $LABEL_ID_COL)" +
                ");"

        const val DELETE_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    object ToDosTable : BaseColumns {
        const val TABLE_NAME = "todos"
        const val DESCRIPTION_COL = "job_desc"
        const val COMPLETION_STATUS_COL = "is_completed"
        const val PARENT_NOTE_ID_COL = "parent_note_id"

        const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                "$_ID INTEGER PRIMARY KEY, " +
                "$DESCRIPTION_COL TEXT, " +
                "$COMPLETION_STATUS_COL INTEGER, " +// 1 completed, 0 no
                "$PARENT_NOTE_ID_COL INTEGER, " +
                "FOREIGN KEY ($PARENT_NOTE_ID_COL) REFERENCES ${NotesTable.TABLE_NAME}(${_ID}) ON DELETE CASCADE" +
                ");"

        const val DELETE_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    //A cursor from a SQLite database in Android references columns from 0.
}