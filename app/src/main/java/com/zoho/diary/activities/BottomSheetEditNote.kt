package com.zoho.diary.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zoho.diary.adapters.ColorAdapter
import com.zoho.diary.extensions.isEmpty
import com.zoho.diary.notes.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class BottomSheetEditNote(private val currentNote: Note) : BottomSheetDialogFragment() {

    private lateinit var llRemoveTodos: LinearLayout
    private lateinit var llGenerateTodos: LinearLayout
    private lateinit var llDelete: LinearLayout
    private lateinit var llClone: LinearLayout
    private lateinit var llShare: LinearLayout
    private lateinit var llMarkLabel: LinearLayout
    private lateinit var editActivity: EditNoteActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_edit_note, container, false)
        llRemoveTodos = view.findViewById(R.id.llRemoveToDos)
        llGenerateTodos = view.findViewById(R.id.llGenerateToDos)
        llDelete = view.findViewById(R.id.llDelete)
        llClone= view.findViewById(R.id.llClone)
        llShare = view.findViewById(R.id.llShare)
        llMarkLabel = view.findViewById(R.id.llMarkLabel)
        editActivity = activity as EditNoteActivity
        val rvColors: RecyclerView = view.findViewById(R.id.rvColorsOnFragmentV2)
        rvColors.adapter = ColorAdapter(context = editActivity)
        rvColors.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

        if(editActivity.isTextContent) {
            llGenerateTodos.visibility = View.VISIBLE
            llRemoveTodos.visibility = View.GONE
        } else {
            llGenerateTodos.visibility = View.GONE
            llRemoveTodos.visibility = View.VISIBLE
        }

        setClickFunctions()
        return view
    }

    private fun setClickFunctions() {
        llRemoveTodos.setOnClickListener{
            editActivity.confirmDeleteCheckBoxes()
            if(currentNote.todosList.isEmpty()) {
                llRemoveTodos.visibility = View.GONE
                llGenerateTodos.visibility = View.VISIBLE
            }
            this.dismiss()
        }

        llGenerateTodos.setOnClickListener{
            llGenerateTodos.visibility = View.GONE
            llRemoveTodos.visibility = View.VISIBLE
            this.dismiss()
            CoroutineScope(IO).launch{
                editActivity.textToCheckList()
            }
        }

        llDelete.setOnClickListener{
            editActivity.confirmAndDelete()
            this.dismiss()
        }

        llClone.setOnClickListener{
            editActivity.scopeForSaving.launch {
                editActivity.cloneNote()
            }
        }

        llShare.setOnClickListener{
            if(currentNote.isEmpty()) {
                Toast.makeText(context, "Note is Empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            editActivity.shareNote()
        }

        llMarkLabel.setOnClickListener{
            if(currentNote.isEmpty()) {
                Toast.makeText(context, "Note is Empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectLabelIntent = Intent(activity, MarkLabelActivity::class.java)
            selectLabelIntent.putExtra("mNote", currentNote)
            this.dismiss()
            editActivity.startActivity(selectLabelIntent)
        }
    }

}