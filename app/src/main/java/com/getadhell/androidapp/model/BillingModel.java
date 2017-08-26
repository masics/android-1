package com.getadhell.androidapp.model;

import android.arch.lifecycle.MutableLiveData;

public class BillingModel {
    public MutableLiveData<Boolean> isSupportedLiveData;
    public MutableLiveData<Boolean> isPremiumLiveData;
    public MutableLiveData<String> priceLiveData;

    public BillingModel() {
        isSupportedLiveData = new MutableLiveData<>();
        isPremiumLiveData = new MutableLiveData<>();
        priceLiveData = new MutableLiveData<>();
        isSupportedLiveData.postValue(false);
        isPremiumLiveData.postValue(false);
        priceLiveData.postValue("");
    }
}
