package com.majeur.psclient.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.majeur.psclient.R
import com.majeur.psclient.service.ShowdownService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext

open class BaseFragment : Fragment(), MainActivity.Callbacks {

    private var _service: ShowdownService? = null

    protected val service get() = _service
    protected val fragmentScope = FragmentScope()

    protected val homeFragment by lazy {
        requireFragmentManager().findFragmentById(R.id.fragment_home) as HomeFragment
    }

    protected val battleFragment by lazy {
        requireFragmentManager().findFragmentById(R.id.fragment_battle) as BattleFragment
    }

    protected val chatFragment by lazy {
        requireFragmentManager().findFragmentById(R.id.fragment_chat) as ChatFragment
    }

    protected val teamsFragment by lazy {
        requireFragmentManager().findFragmentById(R.id.fragment_teams) as TeamsFragment
    }

    protected val mainActivity: MainActivity
        get() = requireActivity() as MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(fragmentScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(fragmentScope)
    }

    override fun onServiceBound(service: ShowdownService) {
        _service = service
    }

    override fun onServiceWillUnbound(service: ShowdownService) {
        _service = null
    }

    class FragmentScope : CoroutineScope, LifecycleObserver {

        private val supervisorJob = SupervisorJob()

        override val coroutineContext: CoroutineContext
            get() = supervisorJob + Dispatchers.Main

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun destroy() = coroutineContext.cancelChildren()
    }
}