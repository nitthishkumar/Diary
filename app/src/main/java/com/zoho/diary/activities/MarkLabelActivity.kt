package com.zoho.diary.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zoho.diary.adapters.LabelSelectionAdapter
import com.zoho.diary.dbutils.DBHandler
import com.zoho.diary.notes.Label
import com.zoho.diary.notes.LabelNotePair
import com.zoho.diary.notes.Note
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main


class MarkLabelActivity : AppCompatActivity() {

    private lateinit var clickedNote : Note
    private lateinit var rvLabels: RecyclerView

    private lateinit var addedLabelsMap: HashMap<Label, Boolean>
    private lateinit var removedOnSearchLabelsMap: HashMap<Label, Boolean>
    private lateinit var mTitlesSet: HashSet<String>

    private lateinit var searchLabel: SearchView
    private lateinit var llUnknownLabel: LinearLayout
    private lateinit var tvUnknownLabel: TextView
    private lateinit var scrollLabels: ScrollView

    private val scopeForIO = CoroutineScope(Dispatchers.IO)
    private lateinit var daObject: DBHandler
    private lateinit var allLabelNotePair: HashSet<LabelNotePair>

    private fun initProps(){
        searchLabel = findViewById(R.id.svSelection)
        rvLabels = findViewById(R.id.rvLabelSelection)
        clickedNote = intent.extras!!.get("mNote") as Note
        daObject = DBHandler(this)
        llUnknownLabel = findViewById(R.id.llUnknownLabel)
        tvUnknownLabel = findViewById(R.id.tvUnknownLabel)
        scrollLabels = findViewById(R.id.scrollLabelSelection)

        val toolbar = findViewById<Toolbar>(R.id.toolbarSL)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24)

    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark_label)
        initProps()
        window.statusBarColor = Color.parseColor(getString(R.color.status_yellow))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        rvLabels.layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)

        searchLabel.setOnQueryTextListener( object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if(newText != null) {
                    scopeForIO.launch {
                        filterLabels(newText)
                    }
                }
                return true
            }

        })

        scopeForIO.launch {
            loadLabelsStatus()
            withContext(Main) {
                rvLabels.adapter = LabelSelectionAdapter(
                    addedLabelsMap,
                    ArrayList(addedLabelsMap.keys)
                )
            }
        }

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_select_label, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.btnDoneSL -> scopeForIO.launch {
                saveSelectedLabels()
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadLabelsStatus() {
        val labelsList = daObject.getAllLabels()
        allLabelNotePair = daObject.getAllLabelNoteSet()
        mTitlesSet = HashSet()
        addedLabelsMap = HashMap()
        removedOnSearchLabelsMap = HashMap()
        for(label in labelsList){
            addedLabelsMap[label] = false
            mTitlesSet.add(label.labelTitle.trim().lowercase())
        }
        val itr = allLabelNotePair.iterator()//from allLabel, marking the label for the clicked as checked or not
        while (itr.hasNext()) {
            val currentPair = itr.next()
            if(currentPair.mNote== clickedNote){
                addedLabelsMap[currentPair.mLabel] = true
            }
        }
    }

    private fun saveSelectedLabels() {
        val selectedLabels = ArrayList<Label>()
        for(key in addedLabelsMap.keys){
            if(addedLabelsMap[key] == true){
                Log.d("SLA", "adding label ${key.labelTitle}")
                selectedLabels.add(key)
            } else {
                Log.d("SLA", "NOT adding label ${key.labelTitle}")
            }
        }
        daObject.addLabelsToNote(clickedNote, selectedLabels)
        daObject.close()
    }

    private fun addRemovedLabels(){
        val tempRemovedLabelSet: HashSet<Label> = HashSet(removedOnSearchLabelsMap.keys)
        for (removedItem in tempRemovedLabelSet) {
            addedLabelsMap[removedItem] = removedOnSearchLabelsMap[removedItem]!!
            removedOnSearchLabelsMap.remove(removedItem)
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private suspend fun filterLabels(queryText: String) {
        addRemovedLabels()
        val queryTextFormatted = queryText.trim().lowercase()
        val unfilteredLabelSet: HashSet<Label> = HashSet(addedLabelsMap.keys)
        for(labelItem in unfilteredLabelSet) {
            if(!labelItem.labelTitle.lowercase().contains(queryTextFormatted)){
                Log.d("MLA","abc $labelItem removed for $queryTextFormatted")
                removedOnSearchLabelsMap[labelItem] = addedLabelsMap[labelItem]!!
                addedLabelsMap.remove(labelItem)
            } else {
                Log.d("MLA","abc $labelItem added for $queryTextFormatted W ${addedLabelsMap.keys.contains(labelItem)}")
            }
        }
        withContext(Main){
//            (rvLabels.adapter as LabelSelectionAdapter).reloadLabels(ArrayList(addedLabelsMap.keys))
            rvLabels.adapter = LabelSelectionAdapter(addedLabelsMap, ArrayList(addedLabelsMap.keys))
//            Log.d("MLA", "abc $mTitlesSet @ filter")
            if(!mTitlesSet.contains(queryText.lowercase()) && queryText.trim().isNotEmpty()) {
                val s = '"'
                tvUnknownLabel.text = "$s$queryText$s"
                llUnknownLabel.visibility = View.VISIBLE
            } else {//if(llUnknownLabel.visibility == View.VISIBLE) {
                llUnknownLabel.visibility = View.GONE
            }
        }
    }


    fun createUnknownLabel(v: View){
        v.visibility = View.GONE
        var title = findViewById<TextView>(R.id.tvUnknownLabel).text.toString()
        searchLabel.onActionViewCollapsed()
        title = title.substring(1, title.length-1)
        val unknownLabel = Label(enteredTitle = title)
        scopeForIO.launch {
            val addedLabel = daObject.writeToDB(unknownLabel)
            addedLabelsMap[addedLabel] = true
            mTitlesSet.add(addedLabel.labelTitle.lowercase())
            filterLabels("")
        }
    }

    fun setSearchView(v: View) {
        searchLabel.focusable = View.FOCUSABLE
    }

}