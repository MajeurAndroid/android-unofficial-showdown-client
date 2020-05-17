package com.majeur.psclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.service.ShowdownService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ShowdownService mService;
    boolean mCanUnbindService = false;

    private BottomNavigationView mNavigationView;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "ShowdownService bounded.");
            ShowdownService.Binder binder = (ShowdownService.Binder) iBinder;
            mService = binder.getService();
            notifyServiceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // Should never be called as our ShowdownService is running in the same process
        }
    };

    private DexIconLoader mDexIconLoader;
    private GlideHelper mGlideHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDexIconLoader = new DexIconLoader(this);
        mGlideHelper = new GlideHelper(this);
        setContentView(R.layout.activity_main);

        mNavigationView = findViewById(R.id.bottom_navigation);
        mNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            final int fragmentId = menuItem.getItemId();
            if (fragmentId == mNavigationView.getSelectedItemId()) return false;
            clearBadge(fragmentId);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            for (Fragment fragment : new Fragment[]{getHomeFragment(), getBattleFragment(),
                    getChatFragment(), getTeamsFragment()}) {
                if (fragment.getId() == fragmentId)
                   transaction.show(fragment);
                else
                    transaction.hide(fragment);
            }
            transaction.commitAllowingStateLoss();
            return true;
        });

        Intent showdownServiceIntent = new Intent(this, ShowdownService.class);
        startService(showdownServiceIntent);

        getSupportFragmentManager().beginTransaction()
                .hide(getBattleFragment())
                .hide(getChatFragment())
                .hide(getTeamsFragment())
                .commit();

        mCanUnbindService = bindService(showdownServiceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onBackPressed() {
        if (mNavigationView.getSelectedItemId() != R.id.fragment_home)
            mNavigationView.setSelectedItemId(R.id.fragment_home);
        else
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Are you sure you want to quit ?")
                    .setMessage("Connection to Showdown server will be closed.")
                    .setPositiveButton("Yes", (dialogInterface, i) -> finish())
                    .setNegativeButton("No", null)
                    .show();
    }

    public int getSelectedFragmentId() {
        return mNavigationView.getSelectedItemId();
    }

    public void showBadge(int fragmentId) {
        BadgeDrawable badge = mNavigationView.getOrCreateBadge(fragmentId);
        badge.setBackgroundColor(getResources().getColor(R.color.secondary));
    }

    public void clearBadge(int fragmentId) {
        mNavigationView.removeBadge(fragmentId);
    }

    public DexIconLoader getDexIconLoader() {
        return mDexIconLoader;
    }

    public GlideHelper getGlideHelper() {
        return mGlideHelper;
    }

    public ShowdownService getService() {
        return mCanUnbindService ? mService : null;
    }

    public HomeFragment getHomeFragment() {
        return (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_home);
    }

    public BattleFragment getBattleFragment() {
        return (BattleFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_battle);
    }

    public ChatFragment getChatFragment() {
        return (ChatFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_chat);
    }

    public TeamsFragment getTeamsFragment() {
        return (TeamsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_teams);
    }

    private void notifyServiceBound() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof Callbacks)
                ((Callbacks) fragment).onServiceBound(mService);
        }
    }

    private void notifyServiceWillUnbound() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof Callbacks)
                ((Callbacks) fragment).onServiceWillUnbound(mService);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCanUnbindService) {
            if (mService != null) // We might not have had access to binder yet.
                notifyServiceWillUnbound();
            unbindService(mServiceConnection);
            mService = null;
            mCanUnbindService = false;
        }
    }

    public void showHomeFragment() {
        mNavigationView.setSelectedItemId(R.id.fragment_home);
    }

    public void showBattleFragment() {
        mNavigationView.setSelectedItemId(R.id.fragment_battle);
    }

    public void setKeepScreenOn(boolean keep) {
        if (keep)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public interface Callbacks {
        void onServiceBound(ShowdownService service);
        void onServiceWillUnbound(ShowdownService service);
    }
}
