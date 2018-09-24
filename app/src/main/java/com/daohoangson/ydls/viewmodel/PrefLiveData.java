package com.daohoangson.ydls.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.daohoangson.ydls.R;

import androidx.lifecycle.MutableLiveData;

class PrefLiveData extends MutableLiveData<String> implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String key;
    private SharedPreferences p;

    PrefLiveData(Application application, int keyResId) {
        Context c = application.getApplicationContext();
        this.key = c.getString(keyResId);
        this.p = c.getSharedPreferences(c.getString(R.string.pref_file_key), Context.MODE_PRIVATE);
    }

    @Override
    protected void onActive() {
        super.onActive();

        syncValue();
        p.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onInactive() {
        super.onInactive();

        p.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void postValue(String value) {
        syncValue(value, true);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);

        syncValue(value, false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(this.key)) {
            syncValue();
        }
    }

    private void syncValue() {
        String newValue = p.getString(key, "");
        if (!newValue.equals(getValue())) {
            setValue(newValue);
        }
    }

    private void syncValue(String value, boolean apply) {
        SharedPreferences.Editor editor = p.edit();
        editor.putString(key, value);

        if (apply) {
            editor.apply();
        } else {
            editor.commit();
        }
    }
}
