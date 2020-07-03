package com.majeur.psclient.ui.teambuilder

import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.majeur.psclient.R

open class ListFragment : Fragment() {

    private var adapter: RecyclerView.Adapter<*>? = null
    private var isListShown = false

    lateinit var recyclerView: RecyclerView
        private set
    private lateinit var progressView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options_menu_searchable, menu)
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.setOnQueryTextListener(object  : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String?) = this@ListFragment.onQueryTextChange(newText ?: "")

            override fun onQueryTextSubmit(query: String?) = false

        })
    }

    protected open fun onQueryTextChange(query: String) = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = requireContext()
        val root = FrameLayout(context)

        recyclerView = RecyclerView(context)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        recyclerView.setHasFixedSize(true)
        root.addView(recyclerView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        progressView = ProgressBar(context, null, android.R.attr.progressBarStyleLarge)
        root.addView(progressView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER))

        root.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isListShown = true
        if (adapter != null) {
            val a = adapter
            adapter = null
            setAdapter(a!!)
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            setListShown(shown = false, animate = false)
        }
    }



    fun setAdapter(a: RecyclerView.Adapter<*>) {
        val hadAdapter = adapter != null
        adapter = a
        recyclerView.adapter = a
        if (!isListShown && !hadAdapter) {
            // The list was hidden, and previously didn't have an
            // adapter.  It is now time to show it.
            setListShown(true, requireView().windowToken != null)
        }
    }

    fun requireAdapter() = adapter ?:
            throw IllegalStateException("ListFragment $this does not have an Adapter.")

    fun setListShown(shown: Boolean, animate: Boolean = true) {
        if (isListShown == shown) return
        isListShown = shown
        if (shown) {
            if (animate) {
                progressView.startAnimation(AnimationUtils.loadAnimation(
                        requireContext(), android.R.anim.fade_out))
                recyclerView.startAnimation(AnimationUtils.loadAnimation(
                        requireContext(), android.R.anim.fade_in))
            } else {
                progressView.clearAnimation()
                recyclerView.clearAnimation()
            }
            progressView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else {
            if (animate) {
                progressView.startAnimation(AnimationUtils.loadAnimation(
                        requireContext(), android.R.anim.fade_in))
                recyclerView.startAnimation(AnimationUtils.loadAnimation(
                        requireContext(), android.R.anim.fade_out))
            } else {
                progressView.clearAnimation()
                recyclerView.clearAnimation()
            }
            progressView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
    }
}