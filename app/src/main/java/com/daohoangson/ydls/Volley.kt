package com.daohoangson.ydls

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader

class Volley constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: Volley? = null

        fun getInstance(context: Context) =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: Volley(context).also {
                        INSTANCE = it
                    }
                }
    }

    val imageLoader: ImageLoader by lazy {
        ImageLoader(requestQueue,
                object : ImageLoader.ImageCache {
                    private val cache = LruCache<String, Bitmap>(20)

                    override fun getBitmap(url: String): Bitmap? {
                        return cache.get(url)
                    }

                    override fun putBitmap(url: String, bitmap: Bitmap) {
                        cache.put(url, bitmap)
                    }
                })
    }

    val requestQueue: RequestQueue by lazy {
        com.android.volley.toolbox.Volley.newRequestQueue(context.applicationContext)
    }
}