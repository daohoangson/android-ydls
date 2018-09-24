package com.daohoangson.ydls.viewmodel

import androidx.annotation.NonNull
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

internal class ActionLiveData<T> : LiveData<T>() {
    fun setActionData(data: T) {
        value = data
    }

    override fun observe(@NonNull owner: LifecycleOwner, @NonNull observer: Observer<in T>) {
        if (hasObservers()) {
            throw RuntimeException("Too many observers")
        }

        super.observe(owner, Observer { data ->
            if (data == null) {
                return@Observer
            }

            observer.onChanged(data)
            value = null
        })
    }
}
