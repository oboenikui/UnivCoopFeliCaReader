package com.oboenikui.campusfelica

import android.app.ActivityManager.TaskDescription
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.oboenikui.campusfelica.CampusFeliCa.CampusFeliCaHistory
import java.text.SimpleDateFormat
import java.util.*


open class NfcActivity : AppCompatActivity() {
    private val POLLING_COMMAND = byteArrayOf(0x06.toByte(), 0x00.toByte(), 0xFE.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0F.toByte())
    private lateinit var adapter: NfcAdapter
    private var pendingIntent: PendingIntent? = null
    private var filters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private var showAlways: Boolean = false
    private lateinit var toolbar: Toolbar
    private lateinit var collapsingToolbarLayout: CollapsingToolbarLayout
    private lateinit var appBarLayout: AppBarLayout

    private lateinit var recyclerView: RecyclerView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(android.R.id.content).systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            val bm = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher)
            val taskDesc = TaskDescription(getString(R.string.app_name), bm, resources.getColor(R.color.colorPrimary))
            setTaskDescription(taskDesc)
        }
        toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar) as CollapsingToolbarLayout
        appBarLayout = findViewById(R.id.appbarLayout) as AppBarLayout
        drawerLayout = findViewById(R.id.drawerLayout) as DrawerLayout
        recyclerView = findViewById(R.id.recyclerView) as RecyclerView

        drawerToggle = object : ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name) {
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                invalidateOptionsMenu()
            }

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu()
            }
        }
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.isDrawerIndicatorEnabled = true

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        showAlways = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_always", true)
        setupNavigationDrawer()

        adapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(this, 0, Intent(this,
                javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        tech.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        filters = arrayOf(tech)
        techLists = arrayOf(arrayOf<String>(NfcF::class.java.name))
        val intent = intent
        val action = intent.action
        if (action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            ReadTask(this).execute(intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG))
        } else {
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = CardRecyclerAdapter(this, listOf<CampusFeliCaHistory>(), R.string.tutorial)

            collapsingToolbarLayout.title = getString(R.string.app_name)
            collapsingToolbarLayout.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            collapsingToolbarLayout.setContentScrimResource(R.drawable.background_balance)
            toggleAppBar(false)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    fun setupNavigationDrawer() {
        val navigationView = (findViewById(R.id.navigationView) as NavigationView)
        val menu = navigationView.menu
        val menuItem = menu.findItem(R.id.menu_show_always)
        val actionView = MenuItemCompat.getActionView(menuItem)
        val switch = actionView.findViewById(R.id.switchCompat) as SwitchCompat
        switch.isChecked = showAlways
        navigationView.setNavigationItemSelectedListener {
            onItemSelected(it)
        }
        switch.setOnCheckedChangeListener { compoundButton, b ->
            showAlways = b
            onClickShowAlways(showAlways)
        }
    }

    fun onItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_license -> {
                drawerLayout.closeDrawer(GravityCompat.START)
                LibsBuilder()
                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                        .withAboutVersionShown(true)
                        .withLibraries("kotlin")
                        .withAboutDescription("description")
                        .withActivityTitle(getString(R.string.open_source_license))
                        .start(this)
            }
            R.id.menu_show_always -> {
                drawerLayout.closeDrawer(GravityCompat.START)
                (item.actionView.findViewById(R.id.switchCompat) as SwitchCompat).isChecked = !showAlways
            }
        }
        return false
    }

    fun onClickShowAlways(showAlways: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("show_always", showAlways).apply()
        packageManager.setComponentEnabledSetting(ComponentName(this, NfcActivity::class.java),
                if (showAlways) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
    }

    public override fun onResume() {
        adapter.enableForegroundDispatch(
                this, pendingIntent, filters, techLists)
        super.onResume()
    }

    public override fun onPause() {
        if (this.isFinishing) {
            adapter.disableForegroundDispatch(this)
        }
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        val action = intent.action
        if (action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            ReadTask(this).execute(intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG))
        }
    }

    private inner class ReadTask(val context: AppCompatActivity) : AsyncTask<Tag, Void, Pair<CampusFeliCa.CampusFeliCaInformation, List<CampusFeliCa.CampusFeliCaHistory>>?>() {
        lateinit var dialog: MaterialDialog

        override fun onPreExecute() {
            dialog = MaterialDialog.Builder(context)
                    .title(R.string.reading)
                    .content(R.string.dont_move)
                    .progress(true, 0)
                    .build()
            dialog.show()
        }

        override fun doInBackground(vararg params: Tag): Pair<CampusFeliCa.CampusFeliCaInformation, List<CampusFeliCa.CampusFeliCaHistory>>? {
            return CampusFeliCa(params[0]).readData()
        }

        override fun onPostExecute(data: Pair<CampusFeliCa.CampusFeliCaInformation, List<CampusFeliCa.CampusFeliCaHistory>>?) {
            if (data == null) {
                recyclerView.layoutManager = LinearLayoutManager(context)
                recyclerView.adapter = CardRecyclerAdapter(context, arrayListOf())

                collapsingToolbarLayout.title = getString(R.string.error)
                collapsingToolbarLayout.setBackgroundColor(resources.getColor(R.color.warningBackground))
                collapsingToolbarLayout.setContentScrimResource(R.drawable.background_warn)
                toggleAppBar(false)
                dialog.dismiss()
                return
            }
            val (info, histories) = data

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = CardRecyclerAdapter(context, histories)
            collapsingToolbarLayout.title = getString(R.string.balance) + toPriceText(info.balance)
            if (info.balance > 1000) {
                collapsingToolbarLayout.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                collapsingToolbarLayout.setContentScrimResource(R.drawable.background_balance)
            } else {
                collapsingToolbarLayout.setBackgroundColor(resources.getColor(R.color.warningBackground))
                collapsingToolbarLayout.setContentScrimResource(R.drawable.background_warn)
            }

            (findViewById(R.id.coop_id) as TextView).text = getString(if(info.isMemberId) R.string.member_no else R.string.manage_no).format(info.coopId)

            (findViewById(R.id.point_balance) as TextView).text = getString(R.string.point_format).format(info.point)

            val mealUsedText = findViewById(R.id.meal_used) as TextView

            if(info.isMealUser && System.currentTimeMillis() - info.lastMealDate.timeInMillis < 30L * 24 * 60 * 60 * 1000 ) {
                mealUsedText.text = getString(R.string.meal_card_format).format(toPriceText(info.mealUsed.toLong()))
            } else {
                mealUsedText.visibility = View.GONE
            }

            dialog.dismiss()
            toggleAppBar(true)
        }
    }

    private fun toggleAppBar(enable: Boolean) {
        appBarLayout.setExpanded(enable, true)
        ViewCompat.setNestedScrollingEnabled(recyclerView, enable)
    }

    private inner class CardRecyclerAdapter(val context: Context, private val list: List<CampusFeliCa.CampusFeliCaHistory>, private val errorId: Int = R.string.not_found) : RecyclerView.Adapter<ViewHolder>() {
        private val inflater: LayoutInflater

        init {
            this.inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        override fun getItemViewType(position: Int): Int {
            return if (list.isEmpty()) R.layout.error_card else R.layout.list_item
        }

        override fun getItemCount(): Int {
            return if (list.isEmpty()) 1 else list.size
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            when (viewHolder) {
                is CardViewHolder -> {
                    val history = list[position]
                    viewHolder.priceTextView.text = (if (history.isPayment) '-' else '+') + toPriceText(history.price.toLong())
                    viewHolder.priceTextView.setTextColor(resources.getColor(if (history.isPayment) R.color.payment else R.color.charge))
                    viewHolder.modeTextView.setText(if (history.isPayment) R.string.payment else R.string.charge)
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
                    viewHolder.calendarTextView.text = sdf.format(history.calendar.time)
                    viewHolder.modeTag.setBackgroundResource(if (history.isPayment) R.drawable.background_pay else R.drawable.background_charge)
                    viewHolder.layout.setOnClickListener {
                    }

                    if (position === itemCount - 1)
                        (viewHolder.cardView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { it.setMargins(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin * 2) }

                    if (position === 0)
                        (viewHolder.cardView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { it.setMargins(it.leftMargin, it.topMargin * 2, it.rightMargin, it.bottomMargin) }
                }

                is ErrorViewHolder -> {
                    viewHolder.errorText.setText(errorId)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder? {
            val layoutInflater = LayoutInflater.from(context)
            val v = layoutInflater.inflate(viewType, parent, false)
            when (viewType) {
                R.layout.list_item -> return CardViewHolder(v)
                R.layout.error_card -> return ErrorViewHolder(v)
                else -> return null
            }
        }

        internal inner class CardViewHolder(view: View) : ViewHolder(view) {
            val calendarTextView: TextView
            val priceTextView: TextView
            val modeTextView: TextView
            val modeTag: View
            val layout: LinearLayout
            val cardView: CardView

            init {
                calendarTextView = view.findViewById(R.id.calendarTextView) as TextView
                priceTextView = view.findViewById(R.id.priceTextView) as TextView
                modeTextView = view.findViewById(R.id.modeTextView) as TextView
                layout = view.findViewById(R.id.layout) as LinearLayout
                modeTag = view.findViewById(R.id.mode_tag)
                cardView = view.findViewById(R.id.cardView) as CardView
            }
        }

        internal inner class ErrorViewHolder(view: View) : ViewHolder(view) {
            val errorText: TextView
            val layout: LinearLayout

            init {
                errorText = view.findViewById(R.id.error_text) as TextView
                layout = view.findViewById(R.id.layout) as LinearLayout
            }
        }
    }

    private fun dp2px(dp: Int): Int {
        val scale = this.resources.displayMetrics.density
        return (16.0f * scale + 0.5f).toInt()
    }

    companion object {

        private fun bytesToText(bytes: ByteArray): String {
            val buffer = StringBuilder()
            for (b in bytes) {
                val hex = "%02X".format(b)
                buffer.append(hex).append(" ")
            }

            val text = buffer.toString().trim { it <= ' ' }
            return text
        }

        private fun toPriceText(price: Long): String {
            val priceStr = price.toString()
            val builder = StringBuilder("¥")
            var i = (priceStr.length - 1) % 3 + 1
            builder.append(priceStr.substring(0..i - 1))
            while (i < priceStr.length) {
                builder.append(',')
                builder.append(priceStr.substring(i..i + 2))
                i += 3
            }
            return builder.toString()
        }

    }

}

class NfcActivity2 : NfcActivity()
