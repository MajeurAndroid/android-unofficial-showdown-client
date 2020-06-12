package com.majeur.psclient.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.majeur.psclient.service.ShowdownService
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

open class BaseFragment : Fragment(), MainActivity.Callbacks {

    private var _service: ShowdownService? = null

    protected val service get() = _service
    protected val fragmentScope = MainScope()

    protected val activity: MainActivity
        get() = requireActivity() as MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(fragmentScope)
    }

    override fun onServiceBound(service: ShowdownService) {
        _service = service
    }

    override fun onServiceWillUnbound(service: ShowdownService) {
        _service = null
    }

    private fun loadData() = fragmentScope.launch {

        val result = withContext(Dispatchers.IO) {
            // your blocking call
        }
    }

    inner class MainScope : CoroutineScope, LifecycleObserver {

        private val job = SupervisorJob()

        override val coroutineContext: CoroutineContext
            get() = job + Dispatchers.Main

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun destroy() = coroutineContext.cancelChildren()
    }
}