package com.majeur.psclient.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.majeur.psclient.R
import com.majeur.psclient.databinding.DialogImportTeamBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.util.SmogonTeamBuilder
import com.majeur.psclient.util.SmogonTeamParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Request

class ImportTeamDialog : BottomSheetDialogFragment() {

    private val fragmentScope = BaseFragment.FragmentScope()

    private lateinit var clipboardManager: ClipboardManager
    private lateinit var assetLoader: AssetLoader

    private var _binding: DialogImportTeamBinding? = null
    private val binding get() = _binding!!

    private val teamFragment
        get() = parentFragment as TeamsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assetLoader = (activity as MainActivity).assetLoader
        lifecycle.addObserver(fragmentScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(fragmentScope)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogImportTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.error.text = ""
            when (checkedId) {
                R.id.clipboard_radio -> binding.urlInput.visibility = View.GONE
                R.id.pastebin_radio -> binding.urlInput.visibility = View.VISIBLE
            }
        }
        binding.importButton.setOnClickListener {
            when (binding.radioGroup.checkedRadioButtonId) {
                R.id.clipboard_radio -> importFromClipboard()
                R.id.pastebin_radio -> importFromPastebin()
            }
        }
        binding.exportButton.setOnClickListener {
            binding.exportButton.isEnabled = false
            fragmentScope.launch {
                val teams = teamFragment.teams.flatMap { group -> group.teams }
                val result = SmogonTeamBuilder.buildTeams(assetLoader, teams)
                clipboardManager.setPrimaryClip(ClipData.newPlainText("Exported Teams", result))
                dismiss()
                teamFragment.makeSnackbar("${teams.size} team(s) copied to clipboard")
            }
        }
    }

    private fun makeSnackbar(msg: String) {
        binding.error.text = msg
    }

    private fun importFromClipboard() {
        val clip = clipboardManager.primaryClip
        if (clip == null || !clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || clip.itemCount == 0) {
            makeSnackbar("There is nothing that looks like a Pokemon in clipboard")
            return
        }
        val rawTeam = clip.getItemAt(0).text.toString()
        handleRawTeamData(rawTeam)
    }


    private fun importFromPastebin() {
        val pasteKey = binding.urlInput.text.toString().removeSuffix("/").takeLast(8)
        if (pasteKey.isBlank()) {
            makeSnackbar("Url field is empty")
            return
        }
        val showdownService = teamFragment.mainActivity.service
        if (showdownService == null) {
            makeSnackbar("Cannot retrieve showdown service")
            return
        }
        fragmentScope.launch {
            binding.importButton.isEnabled = false
            val url = HttpUrl.Builder()
                    .scheme("https")
                    .host("pastebin.com")
                    .addPathSegment("raw")
                    .addPathSegment(pasteKey)
                    .build()
            val request = Request.Builder()
                    .url(url)
                    .build()
            val rawTeam = withContext(Dispatchers.IO) {
                val response = showdownService.okHttpClient.newCall(request).execute()
                response.body()?.string() ?: ""
            }
            if (rawTeam.isBlank()) {
                makeSnackbar("Response error, check your url or internet connection.")
                binding.importButton.isEnabled = true
                return@launch
            }
            handleRawTeamData(rawTeam)
        }

    }

    private fun handleRawTeamData(data: String) {
        fragmentScope.launch {
            val teams = SmogonTeamParser.parseTeams(data, assetLoader)
            if (teams.isEmpty()) {
                makeSnackbar("Import resulted in empty team")
            } else {
                teamFragment.onTeamsImported(teams)
                dismiss()
            }
        }
    }

    companion object {
        const val FRAGMENT_TAG = "import-team-dialog"
    }
}