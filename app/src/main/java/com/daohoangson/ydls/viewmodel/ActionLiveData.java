package com.daohoangson.ydls.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

class ActionLiveData<T> extends LiveData<T> {
    void setActionData(T data) {
        setValue(data);
    }

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull final Observer<? super T> observer) {
        if (hasObservers()) {
            throw new RuntimeException("Too many observers");
        }

        super.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(T data) {
                if (data == null) {
                    return;
                }

                observer.onChanged(data);
                setValue(null);
            }
        });
    }
}
