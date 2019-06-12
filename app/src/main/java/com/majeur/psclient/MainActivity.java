package com.majeur.psclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.view.MenuItem;

import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.util.Utils;
import com.majeur.psclient.widget.SwitchLayout;

public class MainActivity extends AppCompatActivity {

    private Intent mShowdownServiceIntent;
    private ShowdownService mShowdownService;
    boolean mCanUnbindService = false;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            ShowdownService.Binder binder = (ShowdownService.Binder) iBinder;
            mShowdownService = binder.getService();
            notifyServiceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // Should never be called as our ShowdownService is running in the same process
        }
    };

    private DexIconLoader mDexIconLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setStaticScreenDensity(getResources());
        mDexIconLoader = new DexIconLoader(this);
        setContentView(R.layout.activity_main);

        final SwitchLayout switchLayout = findViewById(R.id.fragment_container);
        BottomNavigationView navigationView = findViewById(R.id.bottom_navigation);
        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_home:
                        switchLayout.smoothSwitchTo(0);
                        return true;
                    case R.id.action_battle:
                        switchLayout.smoothSwitchTo(1);
                        return true;
                    case R.id.action_chat:
                        switchLayout.smoothSwitchTo(2);
                        return true;
                    case R.id.action_teams:
                        switchLayout.smoothSwitchTo(3);
                        return true;
                    default:
                        return false;
                }
            }
        });

        mShowdownServiceIntent = new Intent(this, ShowdownService.class);
        startService(mShowdownServiceIntent);
    }

    public DexIconLoader getDexIconLoader() {
        return mDexIconLoader;
    }

    public ShowdownService getShowdownService() {
        return mCanUnbindService ? mShowdownService : null;
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
                ((Callbacks) fragment).onShowdownServiceBound(mShowdownService);
        }
    }

    private void notifyServiceUnBound() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof Callbacks)
                ((Callbacks) fragment).onShowdownServiceUnBound();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (bindService(mShowdownServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE))
            mCanUnbindService = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCanUnbindService) {
            unbindService(mServiceConnection);
            notifyServiceUnBound();
            mShowdownService = null;
            mCanUnbindService = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations())
            stopService(mShowdownServiceIntent);
    }

    public void showBattleFragmentView() {
        BottomNavigationView navigationView = findViewById(R.id.bottom_navigation);
        navigationView.setSelectedItemId(R.id.action_battle);
    }

    public interface Callbacks {
        public void onShowdownServiceBound(ShowdownService showdownService);

        public void onShowdownServiceUnBound();
    }
}
