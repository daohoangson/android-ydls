package com.daohoangson.ydls.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.widget.TextView

import com.android.volley.toolbox.HttpHeaderParser
import com.daohoangson.ydls.R

import org.jsoup.Jsoup

import java.net.HttpURLConnection
import java.util.HashMap

import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.android.volley.*
import com.android.volley.toolbox.NetworkImageView
import com.daohoangson.ydls.Volley

class OpenGraphLiveData internal constructor(application: Application, private val mSourceMediaUrl: LiveData<String>) : LiveData<OpenGraphLiveData.Data>(), Observer<String> {

    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val mQueue: RequestQueue = Volley.getInstance(application.applicationContext).requestQueue

    init {
        // https://github.com/google/volley/issues/51
        HttpURLConnection.setFollowRedirects(true)

        value = Data(null)
    }

    override fun onActive() {
        super.onActive()

        mSourceMediaUrl.observeForever(this)
    }

    override fun onChanged(url: String) {
        mHandler.postDelayed({ parseUrl(url) }, 1000)
    }

    override fun onInactive() {
        super.onInactive()

        cancelQueue()
        mSourceMediaUrl.removeObserver(this)
    }

    private fun cancelQueue() {
        mQueue.cancelAll(TAG)
    }

    private fun parseUrl(url: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalThreadStateException()
        }

        if (url != mSourceMediaUrl.value) {
            // url is outdated (because of the delayed processing)
            return
        }

        val currentData = value
        if (currentData != null) {
            if (url == currentData.mediaUrl) {
                // url is already being processed
                return
            }

            if (currentData.isLoading) {
                cancelQueue()
            }
        }

        val loadingData = Data(url)
        loadingData.isLoading = true
        postValue(loadingData)

        val req = OgRequest(url)
        mQueue.add(req.setTag(TAG))
    }

    class Data internal constructor(internal val mediaUrl: String?) {
        internal var hasData = false
        internal var isLoading = false
        internal var title = ""
        internal var imageUrl = ""
    }

    internal inner class OgRequest(url: String) : Request<Data>(Request.Method.GET, url, Response.ErrorListener { postValue(Data(url)) }) {

        override fun deliverResponse(d: Data) {
            postValue(d)
        }

        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()

            headers["User-Agent"] = "libcurl"

            return headers
        }

        override fun parseNetworkResponse(response: NetworkResponse): Response<Data> {
            val html = String(response.data)
            val d = Data(url)
            d.hasData = true

            val doc = Jsoup.parse(html)
            val titleTags = doc.getElementsByTag("title")
            for (titleTag in titleTags) {
                d.title = titleTag.text()
            }

            val metaTags = doc.getElementsByTag("meta")
            for (metaTag in metaTags) {
                val content = metaTag.attr("content")
                if (TextUtils.isEmpty(content)) {
                    continue
                }
                var name = metaTag.attr("name")
                if (TextUtils.isEmpty(name)) {
                    name = metaTag.attr("property")
                }

                when (name) {
                    "og:image" -> d.imageUrl = content
                    "og:title" -> d.title = content
                }
            }

            return Response.success(d, HttpHeaderParser.parseCacheHeaders(response)
                    ?: buildDefaultCacheEntry(response))
        }

        private fun buildDefaultCacheEntry(response: NetworkResponse): Cache.Entry {
            val entry = Cache.Entry()

            entry.data = response.data
            entry.etag = url
            entry.responseHeaders = response.headers
            entry.allResponseHeaders = response.allHeaders

            val now = System.currentTimeMillis()
            entry.softTtl = now + 600000
            entry.ttl = entry.softTtl
            entry.serverDate = now
            entry.lastModified = now

            return entry
        }
    }

    companion object {
        private const val TAG = "OpenGraph"

        @BindingAdapter("ogImageUrl")
        @JvmStatic
        fun setOpenGraphDataImageUrl(v: NetworkImageView, d: OpenGraphLiveData.Data) {
            if (!d.hasData || TextUtils.isEmpty(d.imageUrl)) {
                v.setImageDrawable(ContextCompat.getDrawable(v.context, R.drawable.og_image_default))
                return
            }

            v.setImageUrl(d.imageUrl, Volley.getInstance(v.context).imageLoader)
        }

        @BindingAdapter("ogTitle")
        @JvmStatic
        fun setOpenGraphDataTitle(v: TextView, d: OpenGraphLiveData.Data) {
            v.text = d.title
        }
    }
}
