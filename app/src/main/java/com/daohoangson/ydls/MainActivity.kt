package com.daohoangson.ydls

import android.content.Intent
import android.os.Bundle
import android.view.Menu

import com.daohoangson.ydls.databinding.ActivityMainBinding
import com.daohoangson.ydls.viewmodel.MainViewModel
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders

class MainActivity : AppCompatActivity() {

    private var mCastContext: CastContext? = null
    private var mSessionManagerListener: SessionManagerListener<CastSession>? = null

    private var viewmodel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupCastListener()
        mCastContext = CastContext.getSharedInstance(this)

        viewmodel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewmodel!!.setMediaUrlFromIntent(intent)
        viewmodel!!.setCastSession(mCastContext!!.sessionManager.currentCastSession)

        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.viewmodel = viewmodel
        binding.setLifecycleOwner(this)

        viewmodel!!.actionStartActivity.observe(this, Observer { aClass ->
            val intent = Intent(this@MainActivity, aClass)
            startActivity(intent)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.cast, menu)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)

        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        viewmodel!!.setMediaUrlFromIntent(intent)
    }

    override fun onPause() {
        super.onPause()

        mCastContext!!.sessionManager.removeSessionManagerListener(mSessionManagerListener, CastSession::class.java)
    }

    override fun onResume() {
        mCastContext!!.sessionManager.addSessionManagerListener(mSessionManagerListener!!, CastSession::class.java)

        super.onResume()
    }

    private fun setupCastListener() {
        mSessionManagerListener = object : SessionManagerListener<CastSession> {

            override fun onSessionEnded(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                onApplicationConnected(session)
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                onApplicationConnected(session)
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarting(session: CastSession) {}

            override fun onSessionEnding(session: CastSession) {}

            override fun onSessionResuming(session: CastSession, sessionId: String) {}

            override fun onSessionSuspended(session: CastSession, reason: Int) {}

            private fun onApplicationConnected(session: CastSession) {
                viewmodel!!.setCastSession(session)
            }

            private fun onApplicationDisconnected() {
                viewmodel!!.setCastSession(null)
            }
        }
    }
}
