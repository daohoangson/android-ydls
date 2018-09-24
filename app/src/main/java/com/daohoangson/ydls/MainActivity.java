package com.daohoangson.ydls;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.daohoangson.ydls.databinding.ActivityMainBinding;
import com.daohoangson.ydls.viewmodel.MainViewModel;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

public class MainActivity extends AppCompatActivity {

    private CastContext mCastContext;
    private SessionManagerListener<CastSession> mSessionManagerListener;

    private MainViewModel viewmodel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupCastListener();
        mCastContext = CastContext.getSharedInstance(this);

        viewmodel = ViewModelProviders.of(this).get(MainViewModel.class);
        viewmodel.setMediaUrlFromIntent(getIntent());
        viewmodel.setCastSession(mCastContext.getSessionManager().getCurrentCastSession());

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setViewmodel(viewmodel);
        binding.setLifecycleOwner(this);

        viewmodel.getActionStartActivity().observe(this, new Observer<Class>() {
            @Override
            public void onChanged(Class aClass) {
                Intent intent = new Intent(MainActivity.this, aClass);
                startActivity(intent);
            }
        });
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

        viewmodel.setMediaUrlFromIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mCastContext.getSessionManager().removeSessionManagerListener(mSessionManagerListener, CastSession.class);
    }

    @Override
    protected void onResume() {
        mCastContext.getSessionManager().addSessionManagerListener(mSessionManagerListener, CastSession.class);

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
                viewmodel.setCastSession(session);
            }

            private void onApplicationDisconnected() {
                viewmodel.setCastSession(null);
            }
        };
    }
}
