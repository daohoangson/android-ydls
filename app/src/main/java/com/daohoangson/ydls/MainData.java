package com.daohoangson.ydls;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

public class MainData {
    public ObservableBoolean canCast = new ObservableBoolean(false);
    public ObservableField<String> mediaUrl = new ObservableField<>();
    public ObservableBoolean mediaUrlFromIntent = new ObservableBoolean(false);
    public OpenGraph openGraph = new OpenGraph(this.mediaUrl);
    public ObservableField<String> ydlsUrl = new ObservableField<>();

    private CastContext mCastContext;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;

    RemoteMediaClient getRemoteMediaClient() {
        if (mCastSession == null) {
            return null;
        }
        return mCastSession.getRemoteMediaClient();
    }

    void handleTextIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (!Intent.ACTION_SEND.equals(intent.getAction()) || !"text/plain".equals(intent.getType())) {
            return;
        }

        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (TextUtils.isEmpty(text)) {
            return;
        }

        Uri uri = Uri.parse(text);
        if (TextUtils.isEmpty(uri.getScheme()) || TextUtils.isEmpty(uri.getHost()) || TextUtils.isEmpty(uri.getPath())) {
            return;
        }
        if (!uri.getScheme().matches("^https?")) {
            return;
        }

        mediaUrl.set(text);
        mediaUrlFromIntent.set(true);
    }

    void onResume(Context context) {
        SharedPreferences sharedPref = getSharedPreferences(context);
        if (!mediaUrlFromIntent.get()) {
            mediaUrl.set(sharedPref.getString(context.getString(R.string.pref_media_url), ""));
        }
        ydlsUrl.set(sharedPref.getString(context.getString(R.string.pref_ydls_url), ""));

        mCastContext.getSessionManager().addSessionManagerListener(mSessionManagerListener, CastSession.class);
    }

    void onPause(Context context) {
        openGraph.cancel();

        mCastContext.getSessionManager().removeSessionManagerListener(mSessionManagerListener, CastSession.class);

        SharedPreferences sharedPref = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(context.getString(R.string.pref_media_url), mediaUrl.get());
        editor.putString(context.getString(R.string.pref_ydls_url), ydlsUrl.get());
        editor.apply();
    }

    void setup(Context context) {
        setupCastListener();
        mCastContext = CastContext.getSharedInstance(context);
        setCastSession(mCastContext.getSessionManager().getCurrentCastSession());
    }

    private SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(context.getString(R.string.pref_file_key), Context.MODE_PRIVATE);
    }

    private void setCastSession(CastSession session) {
        mCastSession = session;
        canCast.set(session != null);
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
                setCastSession(session);
            }

            private void onApplicationDisconnected() {
                setCastSession(null);
            }
        };
    }
}
