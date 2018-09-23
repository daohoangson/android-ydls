package com.daohoangson.ydls;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

public class MainData {
    public ObservableBoolean canCast = new ObservableBoolean(false);
    public ObservableField<String> mediaUrl = new ObservableField<>();
    public OpenGraph openGraph = new OpenGraph(this.mediaUrl);
    public ObservableField<String> ydlsUrl = new ObservableField<>();

    private CastSession mCastSession;
    private Context mContext;
    private boolean mMediaUrlSetFromIntent = false;

    MainData(Context context) {
        mContext = context;
    }

    RemoteMediaClient getRemoteMediaClient() {
        if (mCastSession == null) {
            return null;
        }
        return mCastSession.getRemoteMediaClient();
    }

    void lifecycleOnResume() {
        SharedPreferences sharedPref = getSharedPreferences();
        if (!mMediaUrlSetFromIntent) {
            mediaUrl.set(sharedPref.getString(mContext.getString(R.string.pref_media_url), ""));
        }
        ydlsUrl.set(sharedPref.getString(mContext.getString(R.string.pref_ydls_url), ""));
    }

    void lifecycleOnPause() {
        openGraph.cancel();

        SharedPreferences sharedPref = getSharedPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(mContext.getString(R.string.pref_media_url), mediaUrl.get());
        editor.putString(mContext.getString(R.string.pref_ydls_url), ydlsUrl.get());
        editor.apply();
    }

    void setCastSession(CastSession session) {
        mCastSession = session;
        canCast.set(session != null);
    }

    void setMediaUrlFromIntent(Intent intent) {
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
        mMediaUrlSetFromIntent = true;
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(mContext.getString(R.string.pref_file_key), Context.MODE_PRIVATE);
    }
}
