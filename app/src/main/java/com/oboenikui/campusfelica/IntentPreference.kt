package com.oboenikui.campusfelica

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.preference.CheckBoxPreference
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton

class IntentPreference(private val mContext: Context) : CheckBoxPreference(mContext) {

    override fun onBindView(view: View) {
        super.onBindView(view)
        (view.findViewById(android.R.id.checkbox) as CheckBox).setOnCheckedChangeListener { buttonView, isChecked ->
            mContext.packageManager.setComponentEnabledSetting(ComponentName(mContext, NfcActivity::class.java),
                    if (isChecked) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }

    }
}
