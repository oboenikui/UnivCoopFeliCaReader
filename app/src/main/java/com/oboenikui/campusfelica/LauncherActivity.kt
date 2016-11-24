package com.oboenikui.campusfelica

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

class LauncherActivity : AppCompatActivity() {
    private lateinit var mAdapter: NfcAdapter
    private var mPendingIntent: PendingIntent? = null
    private var mFilters: Array<IntentFilter>? = null
    private var mTechLists: Array<Array<String>>? = null
    private var showAlways: Boolean = false
    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showAlways = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_always", true)
        mAdapter = NfcAdapter.getDefaultAdapter(this)
        mPendingIntent = PendingIntent.getActivity(this, 0, Intent(this,
                javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        tech.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        mFilters = arrayOf(tech)
        mTechLists = arrayOf(arrayOf<String>(NfcF::class.java.name))
    }

    public override fun onResume() {
        mAdapter.enableForegroundDispatch(
                this, mPendingIntent, mFilters, mTechLists)
        super.onResume()
    }

    public override fun onPause() {
        if (this.isFinishing) {
            mAdapter.disableForegroundDispatch(this)
        }
        super.onPause()
    }

    public override fun onNewIntent(intent: Intent) {
        if (intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            intent.setClass(this, NfcActivity2::class.java)
            if (intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG).techList[0] == "android.nfc.tech.NfcF") {
                startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.nfc, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val showAlways = menu.findItem(R.id.show_always)
        showAlways.isChecked = this.showAlways
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_always -> {
                showAlways = !showAlways
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("show_always", showAlways).apply()
                item.isChecked = showAlways
                onClickShowAlways(showAlways)
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onClickShowAlways(showAlways: Boolean) {
        packageManager.setComponentEnabledSetting(ComponentName(this, NfcActivity::class.java),
                if (showAlways) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
    }
}
