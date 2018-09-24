package com.daohoangson.ydls.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;
import com.daohoangson.ydls.R;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class OpenGraphLiveData extends LiveData<OpenGraphLiveData.Data> implements Observer<String> {

    private static final String TAG = "OpenGraph";

    private final Handler mHandler;
    private final RequestQueue mQueue;
    private final LiveData<String> mSourceMediaUrl;

    OpenGraphLiveData(Application application, LiveData<String> mediaUrl) {
        mHandler = new Handler(Looper.getMainLooper());

        mQueue = Volley.newRequestQueue(application.getApplicationContext());

        // https://github.com/google/volley/issues/51
        HttpURLConnection.setFollowRedirects(true);

        mSourceMediaUrl = mediaUrl;

        setValue(new Data(null));
    }

    @Override
    protected void onActive() {
        super.onActive();

        mSourceMediaUrl.observeForever(this);
    }

    @Override
    public void onChanged(final String url) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                parseUrl(url);
            }
        }, 1000);
    }

    @Override
    protected void onInactive() {
        super.onInactive();

        cancelQueue();
        mSourceMediaUrl.removeObserver(this);
    }

    private void cancelQueue() {
        mQueue.cancelAll(TAG);
    }

    private void parseUrl(String url) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException();
        }

        if (!url.equals(mSourceMediaUrl.getValue())) {
            // url is outdated (because of the delayed processing)
            return;
        }

        Data currentData = getValue();
        if (currentData != null) {
            if (url.equals(currentData.mediaUrl)) {
                // url is already being processed
                return;
            }

            if (currentData.isLoading) {
                cancelQueue();
            }
        }

        Data loadingData = new Data(url);
        loadingData.isLoading = true;
        postValue(loadingData);

        OgRequest req = new OgRequest(url);
        mQueue.add(req.setTag(TAG));
    }

    public static class Data {
        boolean hasData = false;
        boolean isLoading = false;
        final String mediaUrl;
        String title = "";
        String imageUrl = "";

        Data(String mediaUrl) {
            this.mediaUrl = mediaUrl;
        }
    }

    class OgRequest extends Request<Data> {
        private final String url;

        OgRequest(final String url) {
            super(
                    Method.GET,
                    url,
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            postValue(new Data(url));
                        }
                    }
            );

            this.url = url;
        }

        @Override
        protected void deliverResponse(Data d) {
            postValue(d);
        }

        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();

            headers.put("User-Agent", "libcurl");

            return headers;
        }

        @Override
        @SuppressWarnings("DefaultCharset")
        protected Response<Data> parseNetworkResponse(NetworkResponse response) {
            String html;
            try {
                html = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            } catch (UnsupportedEncodingException e) {
                // Since minSdkVersion = 8, we can't call
                // new String(response.data, Charset.defaultCharset())
                // So suppress the warning instead.
                html = new String(response.data);
            }

            Data d = new Data(url);
            d.hasData = true;

            Document doc = Jsoup.parse(html);
            Elements titleTags = doc.getElementsByTag("title");
            for (Element titleTag : titleTags) {
                d.title = titleTag.text();
            }

            Elements metaTags = doc.getElementsByTag("meta");
            for (Element metaTag : metaTags) {
                String content = metaTag.attr("content");
                if (TextUtils.isEmpty(content)) {
                    continue;
                }
                String name = metaTag.attr("name");
                if (TextUtils.isEmpty(name)) {
                    name = metaTag.attr("property");
                }

                switch (name) {
                    case "og:image":
                        d.imageUrl = content;
                        break;
                    case "og:title":
                        d.title = content;
                        break;
                }
            }

            return Response.success(d, HttpHeaderParser.parseCacheHeaders(response));
        }
    }

    @BindingAdapter({"ogImageUrl"})
    public static void loadOgImageUrl(ImageView v, OpenGraphLiveData.Data d) {
        if (TextUtils.isEmpty(d.imageUrl)) {
            v.setImageDrawable(ContextCompat.getDrawable(v.getContext(), R.drawable.og_image_default));
            return;
        }

        Picasso.get().load(d.imageUrl)
                .placeholder(R.drawable.og_image_default)
                .into(v);
    }

    @BindingAdapter({"ogTitle"})
    public static void loadOgTitle(TextView v, OpenGraphLiveData.Data d) {
        v.setText(d.title);
    }
}
