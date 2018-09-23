package com.daohoangson.ydls;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;

import com.daohoangson.ydls.databinding.ActivityMainBinding;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;

public class MainActivity extends AppCompatActivity {

    private CastContext mCastContext;
    private SessionManagerListener<CastSession> mSessionManagerListener;

    private MainActor mActor = new MainActor(this);
    private MainData mData = new MainData(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupCastListener();
        mCastContext = CastContext.getSharedInstance(this);

        mData.setCastSession(mCastContext.getSessionManager().getCurrentCastSession());
        mData.setMediaUrlFromIntent(getIntent());

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setA(mActor);
        binding.setD(mData);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.cast, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mData.setMediaUrlFromIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mCastContext.getSessionManager().removeSessionManagerListener(mSessionManagerListener, CastSession.class);

        mData.lifecycleOnPause();
    }

    @Override
    protected void onResume() {
        mCastContext.getSessionManager().addSessionManagerListener(mSessionManagerListener, CastSession.class);

        mData.lifecycleOnResume();

        super.onResume();
    }

    private void setupCastListener() {
        mSessionManagerListener = new SessionManagerListener<CastSession>() {

            @Override
            public void onSessionEnded(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionResumeFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionStartFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarting(CastSession session) {
            }

            @Override
            public void onSessionEnding(CastSession session) {
            }

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {
            }

            @Override
            public void onSessionSuspended(CastSession session, int reason) {
            }

            private void onApplicationConnected(CastSession session) {
                mData.setCastSession(session);
            }

            private void onApplicationDisconnected() {
                mData.setCastSession(null);
            }
        };
    }
}
