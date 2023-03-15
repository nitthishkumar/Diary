package com.zoho.diary.activities

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.zoho.diary.adapters.NotesAdapter
import com.zoho.diary.dbutils.DBHandler
import com.zoho.diary.fragments.NotesFragment
import com.zoho.diary.notes.Label
import com.zoho.takenote.utils.ColorOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.core.view.MenuItemCompat


class HomeActivity : AppCompatActivity(),NavigationView.OnNavigationItemSelectedListener {

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawer: DrawerLayout
    private lateinit var toolbarClick: Toolbar
    var setForSelection = false
    lateinit var loadedFragment: NotesFragment
    lateinit var searchItem: MenuItem
    lateinit var shareMultipleItem: MenuItem
    lateinit var deleteMultipleItem: MenuItem
    lateinit var cancelSelectionItem: MenuItem
    private lateinit var loadAllNoteItem: MenuItem
    lateinit var navigationView: NavigationView
    private lateinit var daObject: DBHandler
    private lateinit var labelsList: ArrayList<Label>
    private lateinit var preMenuIDs: ArrayList<Int>
    private lateinit var navMenu: Menu
    lateinit var searchView: SearchView

    @SuppressLint("ResourceType")
    private fun initProps(){
        daObject = DBHandler(applicationContext)
        window.statusBarColor = Color.parseColor(getString(R.color.status_yellow))
        toolbarClick = findViewById(R.id.toolbarHome)
        setSupportActionBar(toolbarClick)
        loadedFragment = NotesFragment()
        toolbarClick.setTitleTextColor(Color.parseColor(ColorOption.WHITE.rgb))
        drawer = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navMenu = navigationView.menu
        navigationView.menu.getItem(0).isChecked = true
        preMenuIDs = ArrayList()
        toggle = ActionBarDrawerToggle(this, drawer, toolbarClick, R.string.nav_open, R.string.nav_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        Log.d("HA", "abc onCreate() abc")
        initProps()
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        loadAllNoteItem = navMenu.findItem(R.id.nav_notes)
        onNavigationItemSelected(loadAllNoteItem)
//        searchView.maxWidth = Int.MAX_VALUE
//        searchView.setOnKeyListener(object : View.OnKeyListener {
//            private var extended = false
//            override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
//                if (!extended && event.action === KeyEvent.ACTION_DOWN) {
//                    extended = true
//                    val lp: ViewGroup.LayoutParams = v.layoutParams
//                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT
//                }
//                return false
//            }
//        })
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(IO).launch {
            clearLoadedLabels()
            loadLabelsOnNavDrawer()
        }
        if(this::searchView.isInitialized){
            searchView.onActionViewCollapsed()
        }
    }

    @SuppressLint("ServiceCast")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_notes_list_nf, menu)
        searchItem = menu?.findItem(R.id.btnSearchNF)!!
        deleteMultipleItem = menu.findItem(R.id.btnDeleteMultipleNF)
        shareMultipleItem = menu.findItem(R.id.btnShareMultipleNF)
        cancelSelectionItem = menu.findItem(R.id.btnCancelMultipleNF)
        searchItem.setShowAsActionFlags(
            MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
                    or MenuItem.SHOW_AS_ACTION_ALWAYS
        )
        searchView = searchItem.actionView as SearchView
        searchItem.expandActionView()
        searchView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

        searchView.setOnQueryTextListener ( object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false

            override fun onQueryTextChange(newText: String): Boolean {
                if(loadedFragment.notesList.isEmpty()) {
                    if(newText.trim().length == 1){
                        makeToast("No notes added")
                    }
                    return true
                }
                loadedFragment.filterNotes(newText)
                return true
            }

        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.btnDeleteMultipleNF -> loadedFragment.confirmAndDeleteMultiple()
            R.id.btnShareMultipleNF -> loadedFragment.shareMultiple()
            R.id.btnCancelMultipleNF -> {
                (loadedFragment.rvNotes.adapter as NotesAdapter).resetView()
            }
        }
        return true
    }

    override fun onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawer(GravityCompat.START)
        } else if(!searchView.isIconified){
            searchView.onActionViewCollapsed()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_notes  -> {
                loadedFragment = NotesFragment()
                loadFragment(loadedFragment)
            }
            R.id.nav_labels  -> {
                val labelIntent = Intent(baseContext, EditLabelsActivity::class.java)
                startActivity(labelIntent)
            }
            R.id.nav_settings -> {
                startActivity(Intent(this@HomeActivity, SettingsActivity::class.java))
            }
            else -> {
                val addedLabelPosition = preMenuIDs.indexOf(item.itemId)
                val addedLabel = labelsList[addedLabelPosition]
                val targetFragment = NotesFragment(addedLabel)
                loadFragment(targetFragment)
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadFragment(targetFragment: NotesFragment){
        loadedFragment = targetFragment
        supportFragmentManager.beginTransaction().replace(R.id.list_notes_fragment_container, targetFragment).commit()
        supportActionBar?.title = if(targetFragment.underLabel != null){
            targetFragment.underLabel!!.labelTitle
        } else {
            "All Notes"
        }
    }

    private suspend fun clearLoadedLabels() {
        withContext(Main){
            for(loadedId in preMenuIDs){
                navMenu.removeItem(loadedId)
            }
            preMenuIDs.clear()
        }
    }

    fun makeToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private suspend fun loadNewLabels(){
        withContext(Main){
            for(labelItem in labelsList) {
                val newMenuItem = navMenu.add(0, labelItem.labelId+999, 0, labelItem.labelTitle)
                preMenuIDs.add(newMenuItem.itemId)
                newMenuItem.isCheckable = true
            }
        }
    }

    private suspend fun loadLabelsOnNavDrawer(){
        clearLoadedLabels()
        labelsList = daObject.getAllLabels()
        loadNewLabels()
    }

}
