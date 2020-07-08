package com.majeur.psclient.ui

import android.content.*
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.IBinder
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.majeur.psclient.R
import com.majeur.psclient.databinding.ActivityMainBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.io.GlideHelper
import com.majeur.psclient.service.ShowdownService
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    val glideHelper by lazy { GlideHelper(this) }
    val assetLoader by lazy { AssetLoader(this) }

    private val canUseLandscapeLayout by lazy { resources.getBoolean(R.bool.canUseLandscapeLayout) }
    private val useLandscapeLayout by lazy { resources.getBoolean(R.bool.landscape) }
    private val selectedNavigationItemId
        get() = if (useLandscapeLayout) binding.navigationView!!.checkedItem?.itemId ?: 0
                else binding.bottomNavigation!!.selectedItemId

    private var canUnbindService = false
    private var _service: ShowdownService? = null
    val service get() = if (canUnbindService) _service else null

    private lateinit var binding: ActivityMainBinding

    val homeFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.fragment_home) as HomeFragment
    }

    val battleFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.fragment_battle) as BattleFragment
    }

    val chatFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.fragment_chat) as ChatFragment
    }

    val teamsFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.fragment_teams) as TeamsFragment
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Timber.d("ShowdownService bounded. (MainActivity ${this@MainActivity.hashCode()})")
            _service = (iBinder as ShowdownService.Binder).service
            notifyServiceBound()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            // Should never be called as our ShowdownService is running in the same process
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("(${hashCode()}) Lifecycle: onCreate")
        super.onCreate(savedInstanceState)
        if (!canUseLandscapeLayout) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (useLandscapeLayout) {
            binding.navigationView!!.setNavigationItemSelectedListener(this::onNavigationItemSelected)
        } else {
            binding.bottomNavigation!!.setOnNavigationItemSelectedListener(this::onNavigationItemSelected)
        }

        val selectedId = if (useLandscapeLayout) R.id.fragment_battle else R.id.fragment_home
        showFragment(selectedId, now = true, checkAlreadyShown = false)
        setSelectedNavigationItem(selectedId)

        val showdownServiceIntent = Intent(this, ShowdownService::class.java)
        startService(showdownServiceIntent)
        canUnbindService = bindService(showdownServiceIntent, serviceConnection,
                Context.BIND_AUTO_CREATE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val id = if (useLandscapeLayout && selectedNavigationItemId == R.id.fragment_battle)
            R.id.fragment_home else selectedNavigationItemId
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM_ID, id)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM_ID)) {
            var id = savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM_ID)
            // There is now fragment_home nav item in landscape, make sure we are not trying to show that
            if (useLandscapeLayout && id == R.id.fragment_home) id = R.id.fragment_battle
            setSelectedNavigationItem(id)
        }
    }

    private fun setSelectedNavigationItem(itemId: Int) {
        if (useLandscapeLayout) {
            binding.navigationView!!.setCheckedItem(itemId)
            // NavigationView do not trigger its NavigationItemSelectedListener by default
            onNavigationItemSelected(binding.navigationView!!.menu.findItem(itemId))
        } else {
            binding.bottomNavigation!!.selectedItemId = itemId
        }
    }

    private fun showFragment(fragmentId: Int, now: Boolean = false, checkAlreadyShown: Boolean = true) {
        if (checkAlreadyShown && supportFragmentManager.findFragmentById(fragmentId)?.isHidden == false) return
        supportFragmentManager.beginTransaction().apply {
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            val frags = mutableListOf<Fragment>(homeFragment, battleFragment, chatFragment, teamsFragment)
            if (useLandscapeLayout) frags.remove(homeFragment)
            if (useLandscapeLayout && !checkAlreadyShown) show(homeFragment)
            frags.forEach { fragment ->
                if (fragment.id == fragmentId) show(fragment) else hide(fragment)
            }
            if (now) commitNowAllowingStateLoss() else commitAllowingStateLoss()
        }
    }

    override fun onBackPressed() {
        if (!useLandscapeLayout && selectedNavigationItemId != R.id.fragment_home) {
            setSelectedNavigationItem(R.id.fragment_home)
        } else {
            MaterialAlertDialogBuilder(this)
                    .setTitle("Are you sure you want to quit ?")
                    .setMessage("Connection to Showdown server will be closed.")
                    .setPositiveButton("Yes") { _: DialogInterface?, _: Int -> finish() }
                    .setNegativeButton("No", null)
                    .show()
        }
    }

    private fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        val itemId = menuItem.itemId
        clearBadge(itemId)
        showFragment(itemId)
        return true
    }

    fun showBadge(navigationItemId: Int) {
        if (useLandscapeLayout) return // For now NavigationView cannot display badges natively
        if (selectedNavigationItemId != navigationItemId) {
            val badge = binding.bottomNavigation!!.getOrCreateBadge(navigationItemId)
            badge.backgroundColor = resources.getColor(R.color.secondary)
        }
    }

    fun clearBadge(fragmentId: Int) {
        if (!useLandscapeLayout) binding.bottomNavigation!!.removeBadge(fragmentId)
    }

    private fun notifyServiceBound() = supportFragmentManager.fragments.forEach {
        (it as? Callbacks)?.onServiceBound(_service!!)
    }

    private fun notifyServiceWillUnbound() = supportFragmentManager.fragments.forEach {
        (it as? Callbacks)?.onServiceWillUnbound(_service!!)
    }

    override fun onDestroy() {
        Timber.d("(${hashCode()}) Lifecycle: onDestroy")
        // Let fragments remove their message observers from service before they are destroyed
        if (canUnbindService) {
            if (_service != null) { // We might not have had access to binder yet somehow
                notifyServiceWillUnbound()
                _service = null
            }
            unbindService(serviceConnection)
            canUnbindService = false
        }
        super.onDestroy()
    }

    fun showHomeFragment() = setSelectedNavigationItem(R.id.fragment_home)

    fun showBattleFragment() = setSelectedNavigationItem(R.id.fragment_battle)

    fun setKeepScreenOn(keep: Boolean) {
        if (keep) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    interface Callbacks {
        fun onServiceBound(service: ShowdownService)
        fun onServiceWillUnbound(service: ShowdownService)
    }

    companion object {
        private const val STATE_SELECTED_NAVIGATION_ITEM_ID = "psclient:selectedNavigationItemId"
    }
}