/*package com.zoho.diary.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.zoho.diary.activities.EditNoteActivity
import com.zoho.diary.activities.R
import com.zoho.diary.notes.ToDo
import java.util.*
import kotlin.collections.ArrayList

class ToDosAdapter(
    val context: Context,
    private var todoList: ArrayList<ToDo>
) : RecyclerView.Adapter<ToDosAdapter.ToDoViewHolder>() {

    private val mActivity: EditNoteActivity = context as EditNoteActivity
    private val orderedList: LinkedList<ToDo> = LinkedList()
    private var lastIncompleteIndex = 0

    init {
        for(todoItem in todoList) {
            if(todoItem.isCompleted) {
                continue
            } else {
                orderedList.add(todoItem)
                lastIncompleteIndex += 1
            }
        }

        for (todoItem in todoList) {
            if(todoItem.isCompleted) {
                orderedList.add(todoItem)
            }
        }
    }


    class ToDoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbTodo: CheckBox = itemView.findViewById(R.id.cbToDo)
        val etTodo: EditText = itemView.findViewById(R.id.etToDo)
        val ivCancelToDO: ImageView = itemView.findViewById(R.id.ivCancelToDo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_todo, parent, false)
        return ToDoViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ToDoViewHolder, position: Int) {
        val mTodo = orderedList[position]
        holder.cbTodo.isChecked = mTodo.isCompleted
        holder.etTodo.setText(mTodo.jobDescription)
        strikeTextIfCompleted(holder)

        if(position+1 == orderedList.size) {
            holder.etTodo.requestFocus()
        }

        holder.ivCancelToDO.setOnClickListener{
            mActivity.removeToDo(mTodo)
        }

        holder.cbTodo.setOnClickListener{
            mTodo.isCompleted = holder.cbTodo.isChecked
            strikeTextIfCompleted(holder)
            reorderItems(holder)
        }

        holder.etTodo.setOnTouchListener { _, _ ->
            false
        }
        holder.etTodo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mTodo.jobDescription = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun reorderItems(holder: ToDoViewHolder) {
        val itemPosition = holder.adapterPosition
        val currentItem = orderedList.removeAt(itemPosition)
        Log.d("TDA", "abc acted $currentItem")
        notifyItemRemoved(itemPosition)
        notifyItemRangeChanged(itemPosition, orderedList.size)
        if(currentItem.isCompleted) {
            orderedList.add(orderedList.size, currentItem)
            Log.d("TDA", "abc move from $itemPosition to ${orderedList.size}")
            Log.d("TDA", "abc $orderedList")
            notifyItemInserted(orderedList.size)
            notifyItemRangeChanged(orderedList.size, orderedList.size)
            lastIncompleteIndex -= 1
        } else {
            orderedList.add(lastIncompleteIndex, currentItem)
            Log.d("TDA", "abc move from $itemPosition to $lastIncompleteIndex")
            Log.d("TDA", "abc $orderedList")
            notifyItemInserted(lastIncompleteIndex)
            notifyItemRangeChanged(lastIncompleteIndex, orderedList.size)
            lastIncompleteIndex += 1
        }
    }

    private fun strikeTextIfCompleted(holder: ToDoViewHolder) {
        val mToDo = orderedList[holder.adapterPosition]
        if(mToDo.isCompleted) {
            holder.etTodo.paintFlags = holder.etTodo.paintFlags or Paint.STRIKE_TH RU_TEXT_FLAG
        } else {
            holder.etTodo.paintFlags = 0
        }

    }
    override fun getItemCount() = orderedList.size

    override fun getItemViewType(position: Int): Int {
        return position
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addNewToDo() { //for segregated list
        val addedItem = todoList[todoList.size-1]
        orderedList.add(lastIncompleteIndex, addedItem)
        lastIncompleteIndex += 1
        notifyItemInserted(orderedList.size-1)
    }

}*/

/*package com.zoho.diary.adapters

import android.content.Context
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.zoho.diary.activities.EditNoteActivity
import com.zoho.diary.activities.R
import com.zoho.diary.notes.ToDo
import java.util.*

class ToDosAdapter(
    val context: Context,
    private var todoList: ArrayList<ToDo>
) : RecyclerView.Adapter<ToDosAdapter.ToDoViewHolder>() {

    private val mActivity: EditNoteActivity = context as EditNoteActivity
    val orderedList: LinkedList<ToDo> = LinkedList()
    private var lastIncompleteIndex = 0

    init {
        for(todoItem in todoList) {
            if(todoItem.isCompleted) {
                continue
            } else {
                orderedList.add(todoItem)
                lastIncompleteIndex += 1
            }
        }

        for (todoItem in todoList) {
            if(todoItem.isCompleted) {
                orderedList.add(todoItem)
            }
        }
        Log.d("TDA", "abc $orderedList")
    }


    class ToDoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbTodo: CheckBox = itemView.findViewById(R.id.cbToDo)
        val etTodo: EditText = itemView.findViewById(R.id.etToDo)
        val ivCancelToDO: ImageView = itemView.findViewById(R.id.ivCancelToDo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_todo, parent, false)
        return ToDoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToDoViewHolder, position: Int) {
        val mTodo = todoList[position]
        holder.cbTodo.isChecked = mTodo.isCompleted
        holder.etTodo.setText(mTodo.jobDescription)
        strikeTextIfCompleted(holder)

        if(position+1 == orderedList.size) {
            holder.etTodo.requestFocus()
        }

        holder.ivCancelToDO.setOnClickListener {
            mActivity.collectData()
            orderedList.remove(mTodo)
            notifyItemRemoved(holder.adapterPosition)
            mActivity.removeToDo(mTodo)
        }

        holder.cbTodo.setOnClickListener{
            mTodo.isCompleted = holder.cbTodo.isChecked
            strikeTextIfCompleted(holder)
            mActivity.collectData()
            reorderItems(holder)
        }

    }

    private fun strikeTextIfCompleted(holder: ToDoViewHolder) {
        val mToDo = todoList[holder.adapterPosition]
        if(mToDo.isCompleted) {
            holder.etTodo.paintFlags = holder.etTodo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.etTodo.paintFlags = 0
        }

    }

    private fun reorderItems(holder: ToDoViewHolder) {
        val itemPosition = holder.adapterPosition
        val currentItem = orderedList.removeAt(itemPosition)
        Log.d("TDA", "abc acted $currentItem")
        notifyItemRemoved(itemPosition)
//        notifyItemRangeChanged(itemPosition, orderedList.size)
        if(currentItem.isCompleted) {
            orderedList.add(orderedList.size, currentItem)
            Log.d("TDA", "abc move from $itemPosition to ${orderedList.size}")
            Log.d("TDA", "abc $orderedList")

            notifyItemInserted(orderedList.size)
//            notifyItemRangeChanged(orderedList.size, orderedList.size)
            lastIncompleteIndex -= 1
        } else {
            orderedList.add(lastIncompleteIndex, currentItem)
            Log.d("TDA", "abc move from $itemPosition to $lastIncompleteIndex")
            Log.d("TDA", "abc $orderedList")
            notifyItemInserted(lastIncompleteIndex)
//            notifyItemRangeChanged(lastIncompleteIndex, orderedList.size)
            lastIncompleteIndex += 1
        }
    }

    override fun getItemCount() = todoList.size

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun addNewToDo() { //for segregated list
        val addedItem = todoList[todoList.size-1]
        orderedList.add(lastIncompleteIndex, addedItem)
        lastIncompleteIndex += 1
        notifyItemInserted(orderedList.size-1)
    }

}*/

package com.zoho.diary.adapters

import android.content.Context
import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.zoho.diary.activities.EditNoteActivity
import com.zoho.diary.activities.R
import com.zoho.diary.notes.ToDo
import kotlin.collections.ArrayList

class ToDosAdapter(
    val context: Context,
    private var todoList: ArrayList<ToDo>
) : RecyclerView.Adapter<ToDosAdapter.ToDoViewHolder>() {

    private val mActivity: EditNoteActivity = context as EditNoteActivity

    class ToDoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbTodo: CheckBox = itemView.findViewById(R.id.cbToDo)
        val etTodo: EditText = itemView.findViewById(R.id.etToDo)
        val ivCancelToDO: ImageView = itemView.findViewById(R.id.ivCancelToDo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_todo, parent, false)
        return ToDoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToDoViewHolder, position: Int) {
        val mTodo = todoList[holder.adapterPosition]
        holder.cbTodo.isChecked = mTodo.isCompleted
        holder.etTodo.setText(mTodo.jobDescription)
        strikeCompletedText(holder)

        if(position+1 == todoList.size) {
            holder.etTodo.requestFocus()
        }

        holder.ivCancelToDO.setOnClickListener{
            mActivity.removeToDo(holder.adapterPosition)
        }

        holder.cbTodo.setOnClickListener{
            mTodo.isCompleted = holder.cbTodo.isChecked
            strikeCompletedText(holder)
        }

        holder.etTodo.addTextChangedListener( object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("TDA", "abc onTextChange for ${holder.adapterPosition}")
                todoList[holder.adapterPosition].jobDescription = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}

        })
    }

    private fun strikeCompletedText(holder: ToDoViewHolder) {
        val mToDo = todoList[holder.adapterPosition]
        if(mToDo.isCompleted) {
            holder.etTodo.paintFlags = holder.etTodo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.etTodo.paintFlags = 0
        }

    }

    override fun getItemCount() = todoList.size

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun addNewToDo() {
        notifyItemInserted(todoList.size-1)
    }

}