package com.getadhell.androidapp.fragments;

import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.getadhell.androidapp.R;
import com.getadhell.androidapp.viewmodel.SharedBillingViewModel;

public class OnlyPremiumFragment extends LifecycleFragment {
    private static final String TAG = OnlyPremiumFragment.class.getCanonicalName();
    private Button goPremiumButton;
    private TextView goPremiumTextView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle("Only for Premium");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_only_premium, container, false);
        goPremiumButton = (Button) view.findViewById(R.id.goPremiumButton);
        goPremiumTextView = (TextView) view.findViewById(R.id.goPremiumTextView);

        SharedBillingViewModel sharedBillingViewModel = ViewModelProviders.of(this).get(SharedBillingViewModel.class);
        sharedBillingViewModel.billingModel.isSupportedLiveData.observe(this, aBoolean -> {
            Log.w(TAG, "Is subscription mode supported: " + aBoolean);
            if (aBoolean != null && aBoolean) {
                Log.w(TAG, "Subscription mode supported");
                goPremiumTextView.setText(R.string.only_for_premium);
                goPremiumButton.setEnabled(true);
                sharedBillingViewModel.billingModel.priceLiveData.observe(this, s -> {
                    goPremiumButton.setText(s);
                });

                goPremiumButton.setOnClickListener(v -> {
                    sharedBillingViewModel.startSubscriptionDialog(this.getActivity());
                });
            } else {
                Log.w(TAG, "Subscription mode is not supported");
                goPremiumTextView.setText(R.string.subs_not_supported_text_view);
                goPremiumButton.setText(R.string.billing_not_supported);
                goPremiumButton.setEnabled(false);
            }
        });
        return view;
    }
}
