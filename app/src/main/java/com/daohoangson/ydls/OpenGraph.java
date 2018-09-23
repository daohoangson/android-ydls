package com.daohoangson.ydls;

import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.StringRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class OpenGraph {
    private static final String TAG = "OpenGraph";

    public ObservableBoolean hasData = new ObservableBoolean(false);
    public ObservableBoolean isLoading = new ObservableBoolean(false);
    public ObservableField<String> image = new ObservableField<>();
    public ObservableField<String> title = new ObservableField<>();

    private String mCurrentMediaUrl;
    private Handler mHandler;
    private RequestQueue mQueue;
    private ObservableField<String> mSourceMediaUrl;

    OpenGraph(final ObservableField<String> sourceMediaUrl) {
        mHandler = new Handler(Looper.getMainLooper());

        BasicNetwork network = new BasicNetwork(new HurlStack());
        mQueue = new RequestQueue(new NoCache(), network);
        mQueue.start();

        // https://github.com/google/volley/issues/51
        HttpURLConnection.setFollowRedirects(true);

        mSourceMediaUrl = sourceMediaUrl;
        sourceMediaUrl.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                final String url = sourceMediaUrl.get();

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        parseUrl(url);
                    }
                }, 1000);
            }
        });
    }

    void cancel() {
        mQueue.cancelAll(TAG);
    }

    private void parseUrl(String url) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException();
        }

        if (!url.equals(mSourceMediaUrl.get())) {
            // url is outdated (because of the delayed processing)
            return;
        }

        if (url.equals(mCurrentMediaUrl)) {
            // url is already being processed
            return;
        }

        if (isLoading.get()) {
            cancel();
        }

        hasData.set(false);
        isLoading.set(true);
        mCurrentMediaUrl = url;

        image.set("");
        title.set("");

        StringRequest req = new StringRequest(
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String image = "";
                        String title = "";

                        Document doc = Jsoup.parse(response);

                        Elements titleTags = doc.getElementsByTag("title");
                        for (Element titleTag : titleTags) {
                            title = titleTag.text();
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
                                    image = content;
                                    break;
                                case "og:title":
                                    title = content;
                                    break;
                            }
                        }

                        updateData(true, image, title);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateData(false, null, null);
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();

                headers.put("User-Agent", "libcurl");

                return headers;
            }
        };

        mQueue.add(req.setTag(TAG));
    }

    private void updateData(boolean hasData, String image, String title) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException();
        }

        this.hasData.set(hasData);
        isLoading.set(false);

        if (hasData) {
            this.image.set(image);
            this.title.set(title);
        }
    }
}
