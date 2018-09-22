package com.daohoangson.ydls;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;

import com.daohoangson.ydls.databinding.ActivityMainBinding;
import com.google.android.gms.cast.framework.CastButtonFactory;

public class MainActivity extends AppCompatActivity {

    private MainActor mActor = new MainActor(this);
    private MainData mData = new MainData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mData.setup(this);
        mData.handleTextIntent(getIntent());

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

        mData.handleTextIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mData.onPause(this);
    }

    @Override
    protected void onResume() {
        mData.onResume(this);

        super.onResume();
    }
}
