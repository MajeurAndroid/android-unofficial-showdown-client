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
import com.majeur.psclient.util.smogon.SmogonTeamBuilder
import com.majeur.psclient.util.smogon.SmogonTeamParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

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
                R.id.clipboard_radio -> hideAllUrlInputTextViews()
                R.id.pastebin_radio -> showPastebinTextViewHideOthers()
                R.id.pokepaste_radio -> showPokepasteTextViewHideOthers()
            }
        }
        binding.importButton.setOnClickListener {
            when (binding.radioGroup.checkedRadioButtonId) {
                R.id.clipboard_radio -> importFromClipboard()
                R.id.pastebin_radio -> importFromPastebin()
                R.id.pokepaste_radio -> importFromPokepaste()
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

    private fun hideAllUrlInputTextViews() {
        binding.pastebinUrlInput.visibility = View.INVISIBLE
        binding.pokepasteUrlInput.visibility = View.INVISIBLE
    }

    private fun showPokepasteTextViewHideOthers() {
        binding.pastebinUrlInput.visibility = View.INVISIBLE
        binding.pokepasteUrlInput.visibility = View.VISIBLE
    }

    private fun showPastebinTextViewHideOthers() {
        binding.pastebinUrlInput.visibility = View.VISIBLE
        binding.pokepasteUrlInput.visibility = View.INVISIBLE
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
        val rawPaste = binding.pastebinUrlInput.text.toString()
        if (!isValidPastebinUrl(rawPaste)) {
            makeSnackbar("Not a valid Pastebin URL")
            return
        }
        val rawPastebinKey = rawPaste.substringAfter(PASTEBIN_URL_HOST).removePrefix("/").substringBefore("/")
        // Builds a URL like https://pastebin.com/raw/F5zLJLAn
        val url = HttpUrl.Builder()
                .scheme("https")
                .host("pastebin.com")
                .addPathSegment("raw")
                .addPathSegment(rawPastebinKey)
                .build()
        launchRawTeamDownloadAndImport(url)
    }

    private fun importFromPokepaste() {
        val rawPaste = binding.pokepasteUrlInput.text.toString()
        if (!isValidPokepasteUrl(rawPaste)) {
            makeSnackbar("Not a valid Pokepaste URL")
            return
        }
        val rawPokepasteKey = rawPaste.substringAfter(POKEPASTE_URL_HOST).removePrefix("/").substringBefore("/")
        // Builds a URL like https://pokepast.es/0123456789abcdef/raw
        val url = HttpUrl.Builder()
                .scheme("https")
                .host("pokepast.es")
                .addPathSegment(rawPokepasteKey)
                .addPathSegment("raw")
                .build()
        launchRawTeamDownloadAndImport(url)
    }

    private fun isValidPokepasteUrl(url: String): Boolean {
        return POKEPASTE_URL_REGEX.matches(url)
    }

    private fun isValidPastebinUrl(url: String): Boolean {
        return PASTEBIN_URL_REGEX.matches(url)
    }

    private fun launchRawTeamDownloadAndImport(url: HttpUrl) {
        val showdownService = teamFragment.mainActivity.service
        if (showdownService == null) {
            makeSnackbar("Cannot retrieve showdown service")
            return
        }

        fragmentScope.launch {
            binding.importButton.isEnabled = false
            val request = Request.Builder()
                    .url(url)
                    .build()
            val rawTeam = withContext(Dispatchers.IO) {
                val response = try {
                    showdownService.okHttpClient.newCall(request).execute()
                } catch (e: IOException) {
                    Timber.e(e)
                    null
                }
                response?.body()?.string() ?: ""
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

        private const val PASTEBIN_URL_HOST = "pastebin.com"
        private const val POKEPASTE_URL_HOST = "pokepast.es"
        private val PASTEBIN_URL_REGEX = """https?://pastebin\.com/[a-zA-Z0-9]{8}/?""".toRegex()
        private val POKEPASTE_URL_REGEX = """https?://pokepast\.es/[a-z0-9]{16}/?""".toRegex()
    }
}