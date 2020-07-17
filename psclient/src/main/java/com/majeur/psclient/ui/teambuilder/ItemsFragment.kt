package com.majeur.psclient.ui.teambuilder

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.majeur.psclient.R
import com.majeur.psclient.databinding.ListItemItemBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.ui.BaseFragment
import com.majeur.psclient.util.Utils
import com.majeur.psclient.util.dp
import com.majeur.psclient.util.italic
import com.majeur.psclient.util.recyclerview.OnItemClickListener
import com.majeur.psclient.util.toId
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ItemsFragment : ListFragment(), OnItemClickListener {

    private val fragmentScope = BaseFragment.FragmentScope()
    private lateinit var assetLoader: AssetLoader

    override fun onAttach(context: Context) {
        super.onAttach(context)
        assetLoader = (context as TeamBuilderActivity).assetLoader
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(fragmentScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(fragmentScope)
    }

    override fun onQueryTextChange(query: String): Boolean {
        (requireAdapter() as ItemsFragment.Adapter).filter(query)
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentScope.launch {
            val items = assetLoader.allItems("")
            val adapterItems = listOf("None") + items.orEmpty()
            val textHighlightColor = Utils.alphaColor(ContextCompat.getColor(requireContext(), R.color.secondary), 0.45f)
            setAdapter(Adapter(adapterItems, this@ItemsFragment, textHighlightColor))
        }
    }

    override fun onItemClick(itemView: View, holder: RecyclerView.ViewHolder, position: Int) {
        val itemName = (holder as ItemsFragment.Adapter.ViewHolder).binding.nameView.text.toString()
        if (itemName.isBlank()) return // Wait for the full name to be loaded by our AssetLoader
        val bundle = bundleOf(
                RESULT_ITEM to itemName
        )
        setFragmentResult(RESULT_KEY, bundle)
        findNavController().navigateUp()
    }

    inner class Adapter(
            private val baseList: List<String>,
            private val itemClickListener: OnItemClickListener,
            private val highlightColor: Int
    ) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        private var adapterList = baseList
        private var filteringConstraint = ""

        inner class ViewHolder(
                val binding: ListItemItemBinding,
                var job: Job? = null
        ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

            init {
                binding.root.setOnClickListener(this)
            }

            override fun onClick(view: View?) {
                itemClickListener.onItemClick(binding.root, this, layoutPosition)
            }

        }

        fun filter(constraint: String) {
            filteringConstraint = constraint
            adapterList = baseList.filter { it.replace(" ", "").contains(constraint, true) }
            notifyDataSetChanged()
        }

        fun getItem(position: Int) = adapterList[position]

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ListItemItemBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val itemName = getItem(position)
            val isNoneItem = itemName == "None"

            holder.binding.apply {
                //root.animate().cancel()
                //root.alpha = 0f
                nameView.text = ""
                nameView.setCompoundDrawables(null, null, null, null)
                nameView.compoundDrawablePadding = nameView.dp(4f)
                detailsView.text = ""
            }

            holder.job?.cancel()
            if (isNoneItem) {
                holder.binding.apply {
                    nameView.text = itemName.italic()
                    //root.alpha = 1f
                }
                holder.job = null
                return
            }
            holder.job = fragmentScope.launch {
                val item = assetLoader.item(itemName.toId()) ?: return@launch
                val icon = assetLoader.itemIcon(item.spriteId)
                holder.binding.apply {
                    nameView.setText(item.name, TextView.BufferType.SPANNABLE)
                    highlightMatch(nameView)
                    detailsView.text = item.description?.italic() ?: "No description".italic()
                    val drawable = BitmapDrawable(resources, icon)
                    val size = nameView.dp(24f)
                    drawable.setBounds(0, 0, size, size)
                    nameView.setCompoundDrawables(drawable, null, null, null)
                    //root.animate().alpha(1f).setDuration(100L).start()
                }
            }
        }

        override fun getItemCount() = adapterList.size

        private fun highlightMatch(textView: TextView) {
            val constraint = filteringConstraint
            var text = textView.text.toString().toLowerCase()
            val spaceIndex = text.indexOf(' ')
            text = text.replace(" ", "")
            if (!text.contains(constraint)) return
            var startIndex = text.indexOf(constraint)
            if (spaceIndex in 1..startIndex) startIndex++
            var endIndex = startIndex + constraint.length
            if (spaceIndex > 0 && startIndex < spaceIndex && endIndex > spaceIndex) endIndex++
            val spannable = textView.text as Spannable
            spannable.setSpan(BackgroundColorSpan(highlightColor), startIndex, endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    companion object {
        const val RESULT_KEY = "request-result-item"
        const val RESULT_ITEM = "request-result-item"
    }

}