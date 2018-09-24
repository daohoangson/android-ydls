package com.daohoangson.ydls.viewmodel;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import com.daohoangson.ydls.R;
import com.daohoangson.ydls.cast.ExpandedControlsActivity;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

public class MainViewModel extends AndroidViewModel {

    public final MutableLiveData<String> mediaUrl;
    public final MutableLiveData<String> ydlsUrl;
    public final OpenGraphLiveData og;
    public final LiveData<Boolean> ogIsLoading;

    private final ActionLiveData<Class> mActionStartActivity = new ActionLiveData<>();
    private final MutableLiveData<Boolean> mCanCast = new MutableLiveData<>();
    private WeakReference<CastSession> mCastSession;

    public MainViewModel(@NonNull Application application) {
        super(application);

        mediaUrl = new PrefLiveData(application, R.string.pref_media_url);
        ydlsUrl = new PrefLiveData(application, R.string.pref_ydls_url);

        mCanCast.setValue(false);

        og = new OpenGraphLiveData(application, mediaUrl);
        ogIsLoading = Transformations.map(og, new Function<OpenGraphLiveData.Data, Boolean>() {
            @Override
            public Boolean apply(OpenGraphLiveData.Data d) {
                return d.isLoading;
            }
        });
    }

    public LiveData<Boolean> canCast() {
        return mCanCast;
    }

    public LiveData<Class> getActionStartActivity() {
        return mActionStartActivity;
    }

    public void play(View v) {
        if (v == null) {
            throw new RuntimeException("View is missing");
        }

        CastSession castSession = mCastSession.get();
        if (castSession == null) {
            throw new RuntimeException("There is no cast session");
        }

        final RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            throw new RuntimeException("There is no remote media client");
        }

        remoteMediaClient.registerCallback(new RemoteMediaClient.Callback() {
            @Override
            public void onStatusUpdated() {
                mActionStartActivity.setActionData(ExpandedControlsActivity.class);
                remoteMediaClient.unregisterCallback(this);
            }
        });

        MediaLoadOptions options = new MediaLoadOptions.Builder()
                .setAutoplay(true)
                .build();
        remoteMediaClient.load(buildMediaInfo(), options);
    }

    public void setCastSession(CastSession session) {
        mCastSession = new WeakReference<>(session);
        mCanCast.setValue(session != null);
    }

    public void setMediaUrlFromIntent(Intent intent) {
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

        mediaUrl.setValue(text);
    }

    private MediaInfo buildMediaInfo() {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);

        String url = mediaUrl.getValue();
        if (TextUtils.isEmpty(url)) {
            throw new RuntimeException("Media URL is missing");
        }

        Uri uri = Uri.parse(url);
        OpenGraphLiveData.Data og = this.og.getValue();
        if (og.hasData) {
            movieMetadata.putString(MediaMetadata.KEY_ARTIST, uri.getHost());
            movieMetadata.putString(MediaMetadata.KEY_TITLE, og.title);
            movieMetadata.addImage(new WebImage(Uri.parse(og.imageUrl)));
        } else {
            String pathAndQuery = uri.getPath() + "?" + uri.getQuery();
            movieMetadata.putString(MediaMetadata.KEY_ARTIST, pathAndQuery);
            movieMetadata.putString(MediaMetadata.KEY_TITLE, uri.getHost());
            movieMetadata.addImage(new WebImage(Uri.parse("https://via.placeholder.com/512x512?text=" + pathAndQuery)));
            movieMetadata.addImage(new WebImage(Uri.parse("https://via.placeholder.com/512x512?text=" + uri.getHost())));
        }

        return new MediaInfo.Builder(buildOggUrl(url))
                .setContentType("audio/ogg")
                .setMetadata(movieMetadata)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .build();
    }

    private String buildOggUrl(String mediaUrl) {
        String ydlsUrl = this.ydlsUrl.getValue();
        if (TextUtils.isEmpty(ydlsUrl)) {
            throw new RuntimeException("YDLS URL is missing");
        }

        ydlsUrl = ydlsUrl.replaceAll("/+$", "");

        return ydlsUrl + "/ogg/" + mediaUrl;
    }
}
