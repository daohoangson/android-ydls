package com.daohoangson.ydls.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import android.webkit.URLUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

class ClipboardUrlLiveData internal constructor(application: Application, private val mMediaUrl: LiveData<String>) : LiveData<String>(), Observer<String> {

    private val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    override fun onActive() {
        super.onActive()

        mMediaUrl.observeForever(this)
        value = null

        val clipItem = clipboard?.primaryClip?.getItemAt(0)
        clipItem ?: return

        val text = clipItem.text.toString()
        if (TextUtils.isEmpty(text) || !URLUtil.isValidUrl(text) || text == mMediaUrl.value) {
            return
        }

        value = text
    }

    override fun onChanged(mediaUrl: String?) {
        if (mediaUrl == value) {
            value = null
        }
    }

    override fun onInactive() {
        super.onInactive()

        mMediaUrl.removeObserver(this)
    }
}