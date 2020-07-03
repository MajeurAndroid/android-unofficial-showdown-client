package com.majeur.psclient.ui.teambuilder

import android.content.Context
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
import com.majeur.psclient.databinding.ListItemMoveBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.model.common.Type
import com.majeur.psclient.ui.BaseFragment
import com.majeur.psclient.util.CategoryDrawable
import com.majeur.psclient.util.Utils
import com.majeur.psclient.util.italic
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MovesFragment : ListFragment(), ListFragment.OnItemClickListener {

    private val fragmentScope = BaseFragment.FragmentScope()
    private lateinit var assetLoader: AssetLoader
    private lateinit var species: String
    private var slot = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        assetLoader = (context as TeamBuilderActivity).assetLoader
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        species = requireArguments().getString(ARG_SPECIES)!!
        slot = requireArguments().getInt(ARG_SLOT)
        lifecycle.addObserver(fragmentScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(fragmentScope)
    }

    override fun onQueryTextChange(query: String): Boolean {
        (requireAdapter() as Adapter).filter(query)
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentScope.launch {
            val moves = assetLoader.learnset(species)
            val adapterItems = listOf("None") + moves.orEmpty()
            val textHighlightColor = Utils.alphaColor(ContextCompat.getColor(requireContext(), R.color.secondary), 0.45f)
            setAdapter(Adapter(adapterItems, this@MovesFragment, textHighlightColor))
        }
    }

    override fun onItemClick(itemView: View, holder: RecyclerView.ViewHolder, position: Int) {
        val moveName = (holder as Adapter.ViewHolder).binding.nameView.text.toString()
        if (moveName.isBlank()) return // Wait for the full name to be loaded by our AssetLoader
        val bundle = bundleOf(
                RESULT_MOVE to moveName,
                RESULT_SLOT to slot
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
                val binding: ListItemMoveBinding,
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
            return ViewHolder(ListItemMoveBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val moveId = getItem(position)
            val isNoneItem = moveId == "None"

            holder.binding.apply {
                //root.animate().cancel()
                //root.alpha = 0f
                nameView.text = ""
                detailsView.text = ""
                typeView.setImageDrawable(null)
                categoryView.setImageDrawable(null)
            }

            holder.job?.cancel()
            if (isNoneItem) {
                holder.binding.apply {
                    nameView.text = moveId.italic()
                    //root.alpha = 1f
                }
                return
            }
            holder.job = fragmentScope.launch {
                val details = assetLoader.moveDetails(moveId) ?: return@launch
                holder.binding.apply {
                    nameView.setText(details.name, TextView.BufferType.SPANNABLE)
                    highlightMatch(nameView)
                    detailsView.text = buildDetailsText(details.pp, details.basePower, details.accuracy)
                    detailsView.append("\n")
                    detailsView.append(details.desc?.italic() ?: "No description".italic())
                    typeView.setImageResource(Type.getResId(details.type))
                    categoryView.setImageDrawable(CategoryDrawable(details.category))
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

        private fun buildDetailsText(pp: Int, bp: Int, acc: Int): CharSequence {
            return "PP: " + (if (pp >= 0) pp else "–") + ", BP: " + (if (bp > 0) bp else "–") + ", AC: " + if (acc > 0) acc else "–"
        }

    }

    companion object {

        const val ARG_SPECIES = "arg-species"
        const val ARG_SLOT = "arg-slot"

        const val RESULT_KEY = "request-result-move"
        const val RESULT_MOVE = "result-move"
        const val RESULT_SLOT = "result-slot"

    }

}