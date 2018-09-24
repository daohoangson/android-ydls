package com.daohoangson.ydls.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.view.View

import com.daohoangson.ydls.R
import com.daohoangson.ydls.cast.ExpandedControlsActivity
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage

import java.lang.ref.WeakReference

import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

class MainViewModel(@NonNull application: Application) : AndroidViewModel(application) {

    val mediaUrl: MutableLiveData<String>
    val ydlsUrl: MutableLiveData<String>
    val og: OpenGraphLiveData
    val ogIsLoading: LiveData<Boolean>

    private val mActionStartActivity = ActionLiveData<Class<*>>()
    private val mCanCast = MutableLiveData<Boolean>()
    private var mCastSession: WeakReference<CastSession>? = null

    val actionStartActivity: LiveData<Class<*>>
        get() = mActionStartActivity

    init {
        mediaUrl = PrefLiveData(application, R.string.pref_media_url)
        ydlsUrl = PrefLiveData(application, R.string.pref_ydls_url)

        mCanCast.value = false

        og = OpenGraphLiveData(application, mediaUrl)
        ogIsLoading = Transformations.map<OpenGraphLiveData.Data, Boolean>(og) { d -> d.isLoading }
    }

    fun canCast(): LiveData<Boolean> {
        return mCanCast
    }

    fun play(v: View?) {
        if (v == null) {
            throw RuntimeException("View is missing")
        }

        val castSession = mCastSession!!.get() ?: throw RuntimeException("There is no cast session")

        val remoteMediaClient = castSession.remoteMediaClient
                ?: throw RuntimeException("There is no remote media client")

        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                mActionStartActivity.setActionData(ExpandedControlsActivity::class.java)
                remoteMediaClient.unregisterCallback(this)
            }
        })

        val options = MediaLoadOptions.Builder()
                .setAutoplay(true)
                .build()
        remoteMediaClient.load(buildMediaInfo(), options)
    }

    fun setCastSession(session: CastSession?) {
        mCastSession = WeakReference<CastSession>(session)
        mCanCast.value = session != null
    }

    fun setMediaUrlFromIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        if (Intent.ACTION_SEND != intent.action || "text/plain" != intent.type) {
            return
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (TextUtils.isEmpty(text)) {
            return
        }

        val uri = Uri.parse(text)
        if (TextUtils.isEmpty(uri.scheme) || TextUtils.isEmpty(uri.host) || TextUtils.isEmpty(uri.path)) {
            return
        }

        uri.scheme?.matches("^https?".toRegex()) ?: return

        mediaUrl.value = text
    }

    private fun buildMediaInfo(): MediaInfo {
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)

        val url = mediaUrl.value
        if (url == null || url.isEmpty()) {
            throw RuntimeException("Media URL is missing")
        }

        val uri = Uri.parse(url)
        val og = this.og.value
        if (og != null && og.hasData) {
            movieMetadata.putString(MediaMetadata.KEY_ARTIST, uri.host)
            movieMetadata.putString(MediaMetadata.KEY_TITLE, og.title)
            movieMetadata.addImage(WebImage(Uri.parse(og.imageUrl)))
        } else {
            val pathAndQuery = String.format("%s?%s", uri?.path, uri?.query)
            movieMetadata.putString(MediaMetadata.KEY_ARTIST, pathAndQuery)
            movieMetadata.putString(MediaMetadata.KEY_TITLE, uri.host)
            movieMetadata.addImage(WebImage(Uri.parse("https://via.placeholder.com/512x512?text=$pathAndQuery")))
            movieMetadata.addImage(WebImage(Uri.parse("https://via.placeholder.com/512x512?text=" + uri.host)))
        }

        return MediaInfo.Builder(buildOggUrl(url))
                .setContentType("audio/ogg")
                .setMetadata(movieMetadata)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .build()
    }

    private fun buildOggUrl(mediaUrl: String): String {
        var ydlsUrl = this.ydlsUrl.value
        if (ydlsUrl == null || ydlsUrl.isEmpty()) {
            throw RuntimeException("YDLS URL is missing")
        }

        ydlsUrl = ydlsUrl.replace("/+$".toRegex(), "")

        return "$ydlsUrl/ogg/$mediaUrl"
    }
}
