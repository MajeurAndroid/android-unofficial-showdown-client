package com.majeur.psclient.ui

import android.content.*
import android.os.Bundle
import android.os.IBinder
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

    val selectedFragmentId get() = binding.bottomNavigation.selectedItemId

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Timber.d("ShowdownService bounded.")
            _service = (iBinder as ShowdownService.Binder).service
            notifyServiceBound()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            // Should never be called as our ShowdownService is running in the same process
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            val fragmentId = menuItem.itemId
            if (fragmentId == selectedFragmentId) return@setOnNavigationItemSelectedListener false
            clearBadge(fragmentId)
            supportFragmentManager.beginTransaction().apply {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                listOf<Fragment>(homeFragment, battleFragment, chatFragment, teamsFragment).forEach {
                    if (it.id == fragmentId) show(it) else hide(it)
                }
                commitAllowingStateLoss()
            }
            true
        }

        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction().apply {
                hide(battleFragment)
                hide(chatFragment)
                hide(teamsFragment)
                commit()
            }

        val showdownServiceIntent = Intent(this, ShowdownService::class.java)
        startService(showdownServiceIntent)
        canUnbindService = bindService(showdownServiceIntent, serviceConnection,
                Context.BIND_AUTO_CREATE)
    }

    override fun onBackPressed() {
        if (binding.bottomNavigation.selectedItemId != R.id.fragment_home) {
            binding.bottomNavigation.selectedItemId = R.id.fragment_home
        } else {
            MaterialAlertDialogBuilder(this)
                    .setTitle("Are you sure you want to quit ?")
                    .setMessage("Connection to Showdown server will be closed.")
                    .setPositiveButton("Yes") { _: DialogInterface?, _: Int -> finish() }
                    .setNegativeButton("No", null)
                    .show()
        }
    }

    fun showBadge(fragmentId: Int) {
        if (selectedFragmentId != fragmentId) {
            val badge = binding.bottomNavigation.getOrCreateBadge(fragmentId)
            badge.backgroundColor = resources.getColor(R.color.secondary)
        }
    }

    fun clearBadge(fragmentId: Int) = binding.bottomNavigation.removeBadge(fragmentId)

    private fun notifyServiceBound() = supportFragmentManager.fragments.forEach {
        (it as? Callbacks)?.onServiceBound(_service!!)
    }

    private fun notifyServiceWillUnbound() = supportFragmentManager.fragments.forEach {
        (it as? Callbacks)?.onServiceWillUnbound(_service!!)
    }

    override fun onDestroy() {
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

    fun showHomeFragment() {
        binding.bottomNavigation.selectedItemId = R.id.fragment_home
    }

    fun showBattleFragment() {
        binding.bottomNavigation.selectedItemId = R.id.fragment_battle
    }

    fun setKeepScreenOn(keep: Boolean) {
        if (keep) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    interface Callbacks {
        fun onServiceBound(service: ShowdownService)
        fun onServiceWillUnbound(service: ShowdownService)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}