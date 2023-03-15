package com.zoho.diary.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.zoho.diary.adapters.LabelsEditAdapter
import com.zoho.diary.dbutils.DBHandler
import com.zoho.diary.notes.Label
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditLabelsActivity : AppCompatActivity(), TextWatcher {

    private lateinit var rvLabels: RecyclerView
    private lateinit var btnAddLabel: ImageView
    private lateinit var btnDone: ImageView
    private lateinit var btnCancel: ImageView
    private lateinit var daObject: DBHandler
    private lateinit var etLabelTitle: TextInputEditText
    private lateinit var tvNoLabel: TextView
    private lateinit var clCreateLabel: CoordinatorLayout
    lateinit var mTitleSet: LinkedHashSet<String>
    private lateinit var mLabelsList: ArrayList<Label>
    private lateinit var removedList: ArrayList<Label>
    private lateinit var scopeForIO: CoroutineScope


    private fun initProps(){
        rvLabels = findViewById(R.id.rvLabelsListEL)
        etLabelTitle = findViewById(R.id.etLabelEl)
        tvNoLabel = findViewById(R.id.tvNoLabelEL)
        btnDone = findViewById(R.id.btnDoneEL)
        btnAddLabel = findViewById(R.id.btnAddLabelEL)
        btnCancel = findViewById(R.id.btnCancelLabelEL2)
        clCreateLabel = findViewById(R.id.clCreateLabel)
        mTitleSet = LinkedHashSet()
        val toolbar = findViewById<Toolbar>(R.id.toolbarEditLabel)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        daObject = DBHandler(this)
        scopeForIO = CoroutineScope(IO)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setButtonClicks() {

        clCreateLabel.setOnClickListener{
            if(!btnDone.isVisible) {
                setViewForEdit()
            }
        }

        btnCancel.setOnClickListener{
            etLabelTitle.setText("")
            resetView()
        }

        etLabelTitle.setOnTouchListener { _, _ ->
            setViewForEdit()
            true
        }

        btnAddLabel.setOnClickListener{
            setViewForEdit()
        }

        btnDone.setOnClickListener{
            Log.d("ELA", "clicked done btn abc")
            scopeForIO.launch {
                val resultCode = addNewLabel()
                withContext(Main) {
                    when(resultCode) {
                        0 -> {
                            etLabelTitle.error = "Label name cannot be empty"
                        }
                        1 -> {
                            etLabelTitle.error = "Label already exists"
                        }
                        2 -> {
                            etLabelTitle.setText("")
                            resetView()
                            loadLabels()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ResourceAsColor", "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_labels)
        initProps()
        window.statusBarColor = Color.parseColor(getString(R.color.status_yellow))
        supportActionBar?.title = "All Labels"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setButtonClicks()
        mLabelsList = daObject.getAllLabels()
        removedList = ArrayList()

        if(mLabelsList.isEmpty()) {
            tvNoLabel.visibility = View.VISIBLE
        }

        for(mLabel in mLabelsList) {
            mTitleSet.add(mLabel.labelTitle.lowercase())
        }

        loadLabels()
        etLabelTitle.addTextChangedListener(this)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_empty, menu)
        return true
    }

    private fun addNewLabel() : Int {
        addRemovedLabels()//add search filtered labels before adding new one
        val enteredTitle = etLabelTitle.text.toString().trim()
        return when {
            enteredTitle.isEmpty() -> 0
            mTitleSet.contains(enteredTitle.lowercase()) -> 1
            else -> {
                var addedLabel = Label(enteredTitle = enteredTitle)
                mTitleSet.add(enteredTitle.lowercase())
                addedLabel = daObject.writeToDB(addedLabel)
                mLabelsList.add(addedLabel)
                2
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadLabels(){
        if(mLabelsList.isNotEmpty()) {
            tvNoLabel.visibility = View.GONE
        }
        rvLabels.adapter = LabelsEditAdapter(this, mLabelsList)
        rvLabels.layoutManager = LinearLayoutManager(this@EditLabelsActivity)
    }

    private fun addRemovedLabels() {
        for(item in removedList) {
            mLabelsList.add(item)
        }
        removedList.clear()
    }

    private suspend fun filterLabels(query: String) {
        addRemovedLabels()
        val tempList = ArrayList<Label>(mLabelsList)
        for(labelItem in tempList) {
            if (!labelItem.labelTitle.lowercase().contains(query.lowercase())) {
                removedList.add(labelItem)
                mLabelsList.remove(labelItem)
            }
        }
        withContext(Main) {
            loadLabels()
        }
    }

    override fun onResume() {
        super.onResume()
        scopeForIO.launch {
            loadLabels()
        }
    }

    override fun onPause() {
        super.onPause()
        scopeForIO.launch {
            for(labelItem in mLabelsList){
                daObject.writeToDB(labelItem)
            }
        }
    }

    fun confirmAndDelete(holder: LabelsEditAdapter.LabelViewHolder, position: Int) {
        val confirmDelete: AlertDialog.Builder = AlertDialog.Builder(this@EditLabelsActivity)
        confirmDelete.setIcon(R.drawable.ic_black_delete_24)
        confirmDelete.setTitle("Confirm Deletion")
        confirmDelete.setMessage("Are you sure you want to delete this Label?")
        confirmDelete.setPositiveButton("YES") { _, _ ->
            holder.etLabelTitle.setText("")
            val mLabel = mLabelsList[position]
            scopeForIO.launch {
                daObject.deleteLabel(mLabel)
                mLabelsList.remove(mLabel)
                mTitleSet.remove(mLabel.labelTitle.lowercase())
                withContext(Main) {
                    rvLabels.adapter = LabelsEditAdapter(this@EditLabelsActivity, mLabelsList)
                    if(mLabelsList.isEmpty()) {
                        tvNoLabel.visibility = View.VISIBLE
                    }
                }
            }
        }
        confirmDelete.setNegativeButton("NO") { dialog, _ ->
            dialog?.cancel()
        }
        confirmDelete.show()
    }

    private fun setViewForEdit(){
        (rvLabels.adapter as LabelsEditAdapter).resetLastOpenedView()
        etLabelTitle.requestFocus()
        btnAddLabel.visibility = View.GONE
        btnCancel.visibility = View.VISIBLE
        btnDone.visibility = View.VISIBLE
    }

    fun resetView() {
        btnCancel.visibility = View.GONE
        btnAddLabel.visibility = View.VISIBLE
        btnDone.visibility = View.GONE
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if(s != null) {
            scopeForIO.launch{
                filterLabels(s.toString())
            }
        }
    }

    override fun afterTextChanged(s: Editable?) {}

}