package com.majeur.psclient.ui.teambuilder

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.majeur.psclient.databinding.DialogEditStatBinding
import com.majeur.psclient.util.RangeNumberTextWatcher
import com.majeur.psclient.util.SimpleTextWatcher


class EditStatDialog : DialogFragment(), SeekBar.OnSeekBarChangeListener {

    private var _binding: DialogEditStatBinding? = null
    private val binding get() = _binding!!

    private lateinit var statName: String
    private var level = 0
    private var base = 0
    private var ev = 0
    private var iv = 0
    private var natureModifier = 0f
    private var evSum = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        statName = args.getString(ARG_STAT_NAME)!!
        level = args.getInt(ARG_LEVEL)
        base = args.getInt(ARG_STAT_BASE)
        ev = args.getInt(ARG_STAT_EV)
        iv = args.getInt(ARG_STAT_IV)
        evSum = args.getInt(ARG_EVSUM)
        natureModifier = args.getFloat(ARG_NATURE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogEditStatBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            title.text = statName
            ivSlider.apply {
                setOnSeekBarChangeListener(this@EditStatDialog)
                progress = iv
            }
            ivValue.setText(iv.toString())
            evSlider.apply {
                setOnSeekBarChangeListener(this@EditStatDialog)
                progress = ev
            }
            evValue.setText(ev.toString())
            evValue.addTextChangedListener(RangeNumberTextWatcher(0, 252))
            evValue.addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(editable: Editable) {
                    val value = editable.toString().toIntOrNull()
                    if (value != null) evSlider.progress = value
                }
            })
            okButton.setOnClickListener {
                val fragment = targetFragment as EditPokemonFragment
                fragment.onStatModified(statName, ev, iv)
                dismiss()
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
        if (seekBar == binding.evSlider) {
            val currentVal = binding.evValue.text.toString().toIntOrNull()
            if (currentVal == null || currentVal != progress)
                binding.evValue.setText(progress.toString())
            ev = progress
        } else {
            binding.ivValue.text = progress.toString()
            iv = progress
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}

    companion object {

        const val FRAGMENT_TAG = "edit-stat-dialog"

        private const val ARG_STAT_NAME = "arg-stat-name"
        private const val ARG_STAT_BASE = "arg-stat-base"
        private const val ARG_STAT_EV = "arg-stat-ev"
        private const val ARG_STAT_IV = "arg-stat-iv"
        private const val ARG_LEVEL = "arg-level"
        private const val ARG_NATURE = "arg-nature"
        private const val ARG_EVSUM = "arg-evsum"

        fun newInstance(name: String?, base: Int, ev: Int,
                        iv: Int, level: Int, nature: Float, evsum: Int): EditStatDialog {
            val args = Bundle()
            args.putString(ARG_STAT_NAME, name)
            args.putInt(ARG_STAT_BASE, base)
            args.putInt(ARG_STAT_EV, ev)
            args.putInt(ARG_STAT_IV, iv)
            args.putInt(ARG_LEVEL, level)
            args.putFloat(ARG_NATURE, nature)
            val fragment = EditStatDialog()
            fragment.arguments = args
            return fragment
        }
    }
}