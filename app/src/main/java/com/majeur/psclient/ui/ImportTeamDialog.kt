package com.majeur.psclient.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.majeur.psclient.R
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.model.common.BattleFormat
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.ui.teambuilder.EditTeamActivity
import com.majeur.psclient.util.Callback
import com.majeur.psclient.util.ShowdownTeamParser
import com.majeur.psclient.util.ShowdownTeamParser.DexPokemonFactory
import com.majeur.psclient.util.SimpleTextWatcher
import com.majeur.psclient.widget.SwitchLayout
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.Serializable


class ImportTeamDialog : DialogFragment() {

    private lateinit var clipboardManager: ClipboardManager
    private lateinit var assetLoader: AssetLoader

    private var importType = 0
    private var mTeambuilderStub: View? = null
    private var mSwitchLayout: SwitchLayout? = null
    private var mProgressBar: ProgressBar? = null
    private var mEditText: EditText? = null
    private var mImportButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipboardManager = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assetLoader = (activity as MainActivity).assetLoader
        importType = -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_import_team, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.teambuilder_button).setOnClickListener {
            val intent = Intent(context, EditTeamActivity::class.java)
            val battleFormats = (requireActivity() as MainActivity).service!!
                    .getSharedData<List<BattleFormat.Category>>("formats")!!
            intent.putExtra(EditTeamActivity.INTENT_EXTRA_FORMATS, battleFormats as Serializable)
            (requireActivity() as MainActivity).teamsFragment.startActivityForResult(intent, EditTeamActivity.INTENT_REQUEST_CODE)
            dismiss()
        }
        mTeambuilderStub = view.findViewById(R.id.teamBuilderStub)
        mSwitchLayout = view.findViewById(R.id.switch_layout)
        mProgressBar = view.findViewById(R.id.progress_bar)
        mEditText = view.findViewById(R.id.edit_text_import)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group)
        radioGroup.check(R.id.radio_import_pastebin)
        if (!clipboardManager.hasPrimaryClip()) radioGroup.findViewById<View>(R.id.radio_import_clipboard).isEnabled = false
        mImportButton = view.findViewById(R.id.button_)
        mImportButton?.setOnClickListener {
            if (importType == -1) {
                val radioId = radioGroup.checkedRadioButtonId
                moveToSecondStage(radioId)
            } else {
                moveToThirdStage()
            }
        }
    }

    private fun moveToSecondStage(checkedRadioId: Int) {
        mProgressBar!!.visibility = View.INVISIBLE
        mTeambuilderStub!!.visibility = View.GONE
        when (checkedRadioId) {
            R.id.radio_import_pastebin -> {
                mImportButton!!.isEnabled = false
                mEditText!!.hint = "Enter Pastebin URL or 8 characters key"
                mEditText!!.addTextChangedListener(object : SimpleTextWatcher() {
                    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                        mImportButton!!.isEnabled = charSequence.length == 8
                    }
                })
                mEditText!!.maxLines = 2
                mEditText!!.inputType = InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                importType = IMPORT_TYPE_PASTEBIN
            }
            R.id.radio_import_clipboard -> {
                mImportButton!!.isEnabled = false
                mEditText!!.addTextChangedListener(object : SimpleTextWatcher() {
                    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                        mImportButton!!.isEnabled = charSequence.length > 0
                    }
                })
                var hasClipBoardData = false
                if (clipboardManager!!.hasPrimaryClip()) {
                    val clipData = clipboardManager!!.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val item = clipData.getItemAt(0)
                        if (item.text != null) {
                            mEditText!!.setText(item.text)
                            mEditText!!.setSelection(item.text.length)
                            hasClipBoardData = true
                        }
                    }
                }
                if (!hasClipBoardData) {
                    mEditText!!.hint = "No text found in clipboard..."
                }
                importType = IMPORT_TYPE_RAW_TEXT
            }
            R.id.radio_import_manually -> {
                mEditText!!.hint = "Type team here, good luck with that !"
                importType = IMPORT_TYPE_RAW_TEXT
            }
        }
        mSwitchLayout!!.smoothSwitchTo(1)
    }

    private fun moveToThirdStage() {
        mProgressBar!!.visibility = View.VISIBLE
        mImportButton!!.isEnabled = false
        mEditText!!.isEnabled = false
        var text = mEditText!!.text.toString()
        val factory = DexPokemonFactory { TODO("Load Dex pokedex here") }
        when (importType) {
            IMPORT_TYPE_PASTEBIN -> {
                text = text.substring(text.length - 8)
                makePastebinRequest(text, Callback<String?> { s ->
                    if (s == null) {
                        val msg = "Something went wrong when trying to reach Pastebin.com. Check your internet connection."
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        handleParseResult(null, false)
                    } else {
                        ShowdownTeamParser.parseTeams(s, factory, Callback<List<Team>?> { teams ->
                            handleParseResult(teams, true) })
                    }
                })
            }
            IMPORT_TYPE_RAW_TEXT -> ShowdownTeamParser.parseTeams(text, factory, Callback<List<Team>?> { teams ->
                handleParseResult(teams, true) })
        }
    }

    private fun handleParseResult(teams: List<Team>?, toast: Boolean) {
        val assert1 = teams != null && teams.isNotEmpty()
        var assert2 = false
        if (assert1) {
            for (team in teams!!) if (team.pokemons.size != 0) assert2 = true
        }
        if (assert1 && assert2) {
            val teamsFragment = targetFragment as TeamsFragment?
            teamsFragment!!.onTeamsImported(teams!!)
            dismiss()
        } else {
            val msg = "Something went wrong when importing your team, make sure the team is well formatted."
            if (toast) Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            mProgressBar!!.visibility = View.INVISIBLE
            mImportButton!!.text = "Import"
            mImportButton!!.isEnabled = true
            mEditText!!.isEnabled = true
        }
    }

    private fun makePastebinRequest(pasteKey: String, callback: Callback<String?>) {
        val showdownService = (activity as MainActivity?)!!.service
                ?: //TODO
                return
        val url = HttpUrl.Builder()
                .scheme("https")
                .host("pastebin.com")
                .addPathSegment("raw")
                .addPathSegment(pasteKey)
                .build()
        val request = Request.Builder()
                .url(url)
                .build()
        showdownService.okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                // Check if our activity is still alive
                if (activity == null) return
                // Prevents from reading Pastebin.com 404 error web page
                val rawText = if (response.code() == 200) response.body()!!.string() else null
                activity!!.runOnUiThread { callback.callback(rawText) }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                activity!!.runOnUiThread { callback.callback(null) }
            }
        })
    }

    companion object {
        fun newInstance(teamsFragment: TeamsFragment?): ImportTeamDialog {
            val dialog = ImportTeamDialog()
            dialog.setTargetFragment(teamsFragment, 0)
            return dialog
        }

        private const val IMPORT_TYPE_PASTEBIN = 0
        private const val IMPORT_TYPE_RAW_TEXT = 1

        public const val FRAGMENT_TAG = "import-team-dialog"
    }
}