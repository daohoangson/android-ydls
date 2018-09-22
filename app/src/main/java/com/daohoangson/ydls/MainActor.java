package com.daohoangson.ydls;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.daohoangson.ydls.cast.ExpandedControlsActivity;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

public class MainActor {
    private Context mContext;

    MainActor(Context context) {
        mContext = context;
    }

    public void openInBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        mContext.startActivity(intent);
    }

    public void play(MainData d) {
        final RemoteMediaClient remoteMediaClient = d.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }

        remoteMediaClient.registerCallback(new RemoteMediaClient.Callback() {
            @Override
            public void onStatusUpdated() {
                Intent intent = new Intent(mContext, ExpandedControlsActivity.class);
                mContext.startActivity(intent);
                remoteMediaClient.unregisterCallback(this);
            }
        });

        MediaLoadOptions options = new MediaLoadOptions.Builder()
                .setAutoplay(true)
                .build();
        remoteMediaClient.load(buildMediaInfo(d), options);
    }

    private MediaInfo buildMediaInfo(MainData d) {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);

        String url = d.mediaUrl.get();
        if (TextUtils.isEmpty(url)) {
            throw new RuntimeException("Media URL is missing");
        }

        Uri uri = Uri.parse(url);
        String pathAndQuery = uri.getPath() + "?" + uri.getQuery();
        movieMetadata.putString(MediaMetadata.KEY_ARTIST, pathAndQuery);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, uri.getHost());
        movieMetadata.addImage(new WebImage(Uri.parse("https://via.placeholder.com/512x512?text=" + pathAndQuery)));
        movieMetadata.addImage(new WebImage(Uri.parse("https://via.placeholder.com/512x512?text=" + uri.getHost())));

        return new MediaInfo.Builder(buildOggUrl(d, url))
                .setContentType("audio/ogg")
                .setMetadata(movieMetadata)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .build();
    }

    private String buildOggUrl(MainData d, String mediaUrl) {
        String ydlsUrl = d.ydlsUrl.get();
        if (TextUtils.isEmpty(ydlsUrl)) {
            throw new RuntimeException("YDLS URL is missing");
        }

        ydlsUrl = ydlsUrl.replaceAll("/+$", "");

        return ydlsUrl + "/ogg/" + mediaUrl;
    }
}
