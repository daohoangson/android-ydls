package com.daohoangson.ydls.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

import com.daohoangson.ydls.R

import androidx.lifecycle.MutableLiveData

internal class PrefLiveData(application: Application, keyResId: Int) : MutableLiveData<String>(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val key: String
    private val p: SharedPreferences

    init {
        val c = application.applicationContext
        this.key = c.getString(keyResId)
        this.p = c.getSharedPreferences(c.getString(R.string.pref_file_key), Context.MODE_PRIVATE)
    }

    override fun onActive() {
        super.onActive()

        syncValue()
        p.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onInactive() {
        super.onInactive()

        p.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun postValue(value: String) {
        syncValue(value, true)
    }

    override fun setValue(value: String) {
        super.setValue(value)

        syncValue(value, false)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == this.key) {
            syncValue()
        }
    }

    private fun syncValue() {
        val newValue = p.getString(key, "")!!
        if (newValue != value) {
            setValue(newValue)
        }
    }

    private fun syncValue(value: String, apply: Boolean) {
        val editor = p.edit()
        editor.putString(key, value)

        if (apply) {
            editor.apply()
        } else {
            editor.commit()
        }
    }
}
