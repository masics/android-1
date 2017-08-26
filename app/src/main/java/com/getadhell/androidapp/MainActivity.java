package com.getadhell.androidapp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.getadhell.androidapp.blocker.ContentBlocker;
import com.getadhell.androidapp.blocker.ContentBlocker56;
import com.getadhell.androidapp.blocker.ContentBlocker57;
import com.getadhell.androidapp.db.AppDatabase;
import com.getadhell.androidapp.db.entity.BlockUrl;
import com.getadhell.androidapp.db.entity.BlockUrlProvider;
import com.getadhell.androidapp.deviceadmin.DeviceAdminInteractor;
import com.getadhell.androidapp.dialogfragment.AdhellNotSupportedDialogFragment;
import com.getadhell.androidapp.dialogfragment.AdhellTurnOnDialogFragment;
import com.getadhell.androidapp.dialogfragment.NoInternetConnectionDialogFragment;
import com.getadhell.androidapp.fragments.AdhellNotSupportedFragment;
import com.getadhell.androidapp.fragments.AppSupportFragment;
import com.getadhell.androidapp.fragments.BlockerFragment;
import com.getadhell.androidapp.fragments.PackageDisablerFragment;
import com.getadhell.androidapp.service.BlockedDomainService;
import com.getadhell.androidapp.utils.AppWhiteList;
import com.getadhell.androidapp.utils.AppsListDBInitializer;
import com.getadhell.androidapp.utils.BlockUrlUtils;
import com.getadhell.androidapp.utils.DeviceUtils;
import com.roughike.bottombar.BottomBar;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {
    public static final String ADHELL_STANDARD_PACKAGE = "http://getadhell.com/standard-package.txt";
    private static final String TAG = MainActivity.class.getCanonicalName();
    private static FragmentManager fragmentManager;
    private static int tabState = R.id.blockerTab;
    protected DeviceAdminInteractor mAdminInteractor;
    private AdhellNotSupportedDialogFragment adhellNotSupportedDialogFragment;
    private AdhellTurnOnDialogFragment adhellTurnOnDialogFragment;
    private NoInternetConnectionDialogFragment noInternetConnectionDialogFragment;

    @Override
    public void onBackPressed() {
        int count = fragmentManager.getBackStackEntryCount();
        if (count == 0) {
            super.onBackPressed();
        } else {
            fragmentManager.popBackStack();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Answers(), new Crashlytics());
        setContentView(R.layout.activity_main);
        fragmentManager = getSupportFragmentManager();

        adhellNotSupportedDialogFragment = AdhellNotSupportedDialogFragment.newInstance("Some title");
        if (!DeviceUtils.isContentBlockerSupported()) {
            Log.i(TAG, "Device not supported");
            return;
        }

        adhellTurnOnDialogFragment = AdhellTurnOnDialogFragment.newInstance("Adhell Turn On");
        noInternetConnectionDialogFragment = NoInternetConnectionDialogFragment.newInstance("No Internet connection");
        adhellNotSupportedDialogFragment.setCancelable(false);
        adhellTurnOnDialogFragment.setCancelable(false);
        noInternetConnectionDialogFragment.setCancelable(false);


        BottomBar bottomBar = (BottomBar) findViewById(R.id.bottomBar);

        bottomBar.setOnTabSelectListener(tabId -> {
            tabState = tabId;
            for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++)
                fragmentManager.popBackStack();
            changeFragment();
        });

        if (!DeviceUtils.isContentBlockerSupported()) {
            return;
        }
        mAdminInteractor = DeviceAdminInteractor.getInstance();

        AsyncTask.execute(() ->
        {
//        HeartbeatAlarmHelper.scheduleAlarm();
            AppDatabase appDatabase = AppDatabase.getAppDatabase(getApplicationContext());
            if (appDatabase.applicationInfoDao().getAll().size() == 0) {
                AppsListDBInitializer.getInstance().fillPackageDb(getPackageManager());
            }
            // TODO: Make it with database
            // Add to whitelist if not exists
            AppWhiteList appWhiteList = new AppWhiteList();
            appWhiteList.addToWhiteList("com.google.android.music");

            // Check if standard ad provider is exist. if not add
            BlockUrlProvider blockUrlProvider =
                    appDatabase.blockUrlProviderDao().getByUrl(ADHELL_STANDARD_PACKAGE);
            if (blockUrlProvider == null) {
                blockUrlProvider = new BlockUrlProvider();
                blockUrlProvider.url = ADHELL_STANDARD_PACKAGE;
                blockUrlProvider.lastUpdated = new Date();
                blockUrlProvider.deletable = false;
                blockUrlProvider.selected = true;
                long ids[] = appDatabase.blockUrlProviderDao().insertAll(blockUrlProvider);
                blockUrlProvider.id = ids[0];
                List<BlockUrl> blockUrls;
                try {
                    blockUrls = BlockUrlUtils.loadBlockUrls(blockUrlProvider);
                    blockUrlProvider.count = blockUrls.size();
                    Log.d(TAG, "Number of urls to insert: " + blockUrlProvider.count);
                    // Save url provider
                    appDatabase.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);
                    // Save urls from providers
                    appDatabase.blockUrlDao().insertAll(blockUrls);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to download urls", e);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                fragmentManager.popBackStack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!DeviceUtils.isContentBlockerSupported()) {
            Log.i(TAG, "Device not supported");
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragmentContainer, new AdhellNotSupportedFragment());
            fragmentTransaction.commit();
            return;
        }
        showDialog();
        if (!mAdminInteractor.isActiveAdmin() || !mAdminInteractor.isKnoxEnbaled()) {
            Log.d(TAG, "Admin not active");
            return;
        }
        Log.d(TAG, "Everything is okay");
        changeFragment();

        ContentBlocker contentBlocker = DeviceUtils.getContentBlocker();
        if (contentBlocker != null && contentBlocker.isEnabled() && (contentBlocker instanceof ContentBlocker56
                || contentBlocker instanceof ContentBlocker57)) {
            Intent i = new Intent(App.get().getApplicationContext(), BlockedDomainService.class);
            i.putExtra("launchedFrom", "main-activity");
            App.get().getApplicationContext().startService(i);
        }
    }

    private void changeFragment() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment replacing;
        if (tabState == R.id.blockerTab) replacing = new BlockerFragment();
        else if (tabState == R.id.packageDisablerTab) replacing = new PackageDisablerFragment();
        else replacing = new AppSupportFragment();
        fragmentTransaction.replace(R.id.fragmentContainer, replacing);
        fragmentTransaction.commitAllowingStateLoss();
    }


    public void showDialog() {
        if (!(DeviceUtils.isSamsung() && DeviceUtils.isKnoxSupported())) {
            Log.i(TAG, "Device not supported");
            if (!adhellNotSupportedDialogFragment.isVisible()) {
                adhellNotSupportedDialogFragment.show(fragmentManager, "dialog_fragment_adhell_not_supported");
            }
            return;
        }
        if (!mAdminInteractor.isActiveAdmin()) {
            Log.d(TAG, "Admin is not active. Request enabling");
            if (!adhellTurnOnDialogFragment.isVisible()) {
                adhellTurnOnDialogFragment.show(fragmentManager, "dialog_fragment_turn_on_adhell");
            }
            return;
        }

        if (!mAdminInteractor.isKnoxEnbaled()) {
            Log.d(TAG, "Knox disabled");
            Log.d(TAG, "Checking if internet connection exists");
            ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            Log.d(TAG, "Is internet connection exists: " + isConnected);
            if (!isConnected) {
                if (!noInternetConnectionDialogFragment.isVisible()) {
                    noInternetConnectionDialogFragment.show(fragmentManager, "dialog_fragment_no_internet_connection");
                }
            } else {
                if (!adhellTurnOnDialogFragment.isVisible()) {
                    adhellTurnOnDialogFragment.show(fragmentManager, "dialog_fragment_turn_on_adhell");
                }
            }
        }
    }
}
