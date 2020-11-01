package com.majeur.psclient.ui

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.AdapterView.VISIBLE
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.majeur.psclient.R
import com.majeur.psclient.databinding.DialogBattleMessageBinding
import com.majeur.psclient.databinding.FragmentHomeBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.model.BattleRoomInfo
import com.majeur.psclient.model.ChatRoomInfo
import com.majeur.psclient.model.common.BattleFormat
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.model.common.toId
import com.majeur.psclient.service.ShowdownService
import com.majeur.psclient.service.observer.GlobalMessageObserver
import com.majeur.psclient.util.*
import com.majeur.psclient.widget.CategoryAdapter
import com.majeur.psclient.widget.PrivateMessagesOverviewWidget.OnItemButtonClickListener
import com.majeur.psclient.widget.PrivateMessagesOverviewWidget.OnItemClickListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeFragment : BaseFragment(), GlobalMessageObserver.UiCallbacks, View.OnClickListener {

    private val observer get() = service!!.globalMessageObserver
    private val clipboardManager
        get() = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private lateinit var assetLoader: AssetLoader

    private var currentBattleFormat: BattleFormat? = null
    private var battleFormats: List<BattleFormat.Category>? = null
    private var soundEnabled = false
    private var isSearchingBattle = false
    private var isChallengingSomeone = false
    private var waitingForChallenge = false
    private var challengeTo: String? = null
    private var isAcceptingChallenge = false
    private var isAcceptingFrom: String? = null
    private var onConnectedListeners = mutableMapOf<String, () -> Unit>()
    private var roomDeinitListeners = mutableMapOf<String, (String) -> Unit>()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var activeSnackbar: Snackbar? = null
    private val snackbarCallbacks = object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
        override fun onDismissed(snackbar: Snackbar?, event: Int) {
            snackbar?.removeCallback(this)
            if (activeSnackbar == snackbar) activeSnackbar = null
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        assetLoader = mainActivity.assetLoader
        soundEnabled = Preferences.isBattleSoundEnabled(context)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Timber.d(container.toString())
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        service?.globalMessageObserver?.uiCallbacks = null
    }

    private fun makeSnackbar(message: String, indefinite: Boolean = false, action: Pair<String, () -> Unit>? = null, maxLines: Int = 0) {
        val snackbar = Snackbar.make(binding.root, message, if (indefinite) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG)
                .addCallback(snackbarCallbacks)
        if (action != null) snackbar.setAction(action.first) { action.second.invoke() }
        if (maxLines > 0) {
            val textView = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.maxLines = 5
        }
        activeSnackbar = snackbar
        snackbar.show()
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.background = BackgroundBitmapDrawable(resources, R.drawable.client_bg)
        binding.usersCount.apply {
            text = "-".bold()
            append("\nusers online".small())
        }
        binding.battlesCount.apply {
            text = "-".bold() concat "\nactive battles".small()
        }
        binding.username.apply {
            text = "Connected as\n".small() concat "-".bold()
        }
        binding.loginButton.apply {
            isEnabled = false
            setImageResource(R.drawable.ic_login)
            setOnClickListener(this@HomeFragment)
        }
        binding.formatsSelector.apply {
            adapter = object : CategoryAdapter(context) {
                override fun isCategoryItem(position: Int) = getItem(position) is BattleFormat.Category
                override fun getCategoryLabel(position: Int) = (getItem(position) as BattleFormat.Category).label
                override fun getItemLabel(position: Int) = (getItem(position) as BattleFormat).label
            }
            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
                    val adapter = adapterView.adapter as CategoryAdapter
                    if (!adapter.isEnabled(position)) { // Skip category label
                        binding.formatsSelector.setSelection(position + 1)
                    } else {
                        val format = adapter.getItem(position) as BattleFormat
                        setCurrentBattleFormat(format, true)
                    }
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        }
        binding.teamsSelector.adapter = TeamsAdapter()
        binding.soundButton.apply {
            setImageResource(if (soundEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
            setOnClickListener {
                soundEnabled = !soundEnabled
                setImageResource(if (soundEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
                Preferences.setBattleSoundEnabled(context, soundEnabled)
            }
        }
        binding.pmsOverview.apply {
            onItemClickListener = object : OnItemClickListener {
                override fun onItemClick(with: String) = startPrivateChat(with)
            }
            onItemButtonClickListener = object : OnItemButtonClickListener {
                override fun onChallengeButtonClick(with: String) = challengeSomeone(with)
                override fun onAcceptButtonClick(with: String, format: String) {
                    when {
                        isSearchingBattle -> makeSnackbar("Cannot accept challenge while searching for battle")
                        isChallengingSomeone -> makeSnackbar("You are already challenging someone")
                        else -> {
                            isAcceptingChallenge = true
                            isAcceptingFrom = with
                            setBattleButtonUIState("Accept\n$with's\nchallenge", enabled = true, showCancel = true, tintCard = true)
                            showSearchableFormatsOnly(false)
                            battleFormats?.forEach { category ->
                                category.formats.firstOrNull { format.toId() == it.toId() }?.let {
                                    setCurrentBattleFormat(it, false)
                                }
                            }
                            binding.formatsSelector.isEnabled = false
                            view.post { (view as ScrollView).fullScroll(View.FOCUS_UP) }
                        }
                    }
                }
            }
        }

        binding.news.apply {
            alpha = 0f
            translationY = -dp(42f).toFloat()
            visibility = View.GONE
            setOnClickListener {
                if (!text.isNullOrEmpty() && childFragmentManager.findFragmentByTag(NewsDialog.FRAGMENT_TAG) == null)
                    NewsDialog().show(childFragmentManager, NewsDialog.FRAGMENT_TAG)
            }
        }

        binding.cancelButton.setOnClickListener(this)
        binding.searchButton.setOnClickListener(this)
        binding.userSearchButton.setOnClickListener(this)
        binding.battleSearchButton.setOnClickListener(this)
        binding.replaySearchButton.setOnClickListener(this)
        binding.newsButton.setOnClickListener(this)
        binding.bugReportButton.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        if (service?.isConnected != true) return
        when (view) {
            binding.loginButton -> {
                if (observer.isUserGuest) {
                    promptUserSignIn()
                } else {
                    service?.sendGlobalCommand("logout")
                    service?.forgetUserLoginInfos()
                }
            }
            binding.searchButton -> {
                if (observer.isUserGuest) {
                    promptUserSignIn()
                } else {

                    if (battleFragment.battleRunning) {
                        if (battleFragment.isReplay) {
                            AlertDialog.Builder(requireActivity())
                                    .setMessage("You are already viewing a replay. Do you want to search anyway?")
                                    .setPositiveButton("Yes, search") { _, _ ->
                                        service?.replayManager?.closeReplay()
                                        searchForBattle()
                                    }
                                    .setNegativeButton("No, go back", null)
                                    .show()
                        } else {
                            makeSnackbar("A battle is already running")
                        }
                    } else {
                        searchForBattle()
                    }
                }
            }
            binding.cancelButton -> when {
                isChallengingSomeone -> {
                    if (waitingForChallenge) {
                        service?.sendGlobalCommand("cancelchallenge", challengeTo!!.toId())
                    } else {
                        isChallengingSomeone = false
                        challengeTo = null
                        setBattleButtonUIState("Battle !")
                        showSearchableFormatsOnly(true)
                    }
                }
                isAcceptingChallenge -> {
                    isAcceptingChallenge = false
                    isAcceptingFrom = null
                    setBattleButtonUIState("Battle !")
                    showSearchableFormatsOnly(true)
                    binding.formatsSelector.isEnabled = true
                }
                else -> service?.sendGlobalCommand("cancelsearch")
            }
            binding.userSearchButton -> {
                val dialogBinding = DialogBattleMessageBinding.inflate(layoutInflater)
                dialogBinding.editTextTeamName.hint = "Type a username"
                MaterialAlertDialogBuilder(requireActivity())
                        .setPositiveButton("Find") { _: DialogInterface?, _: Int ->
                            val input = dialogBinding.editTextTeamName.text.toString().replace(USERNAME_REGEX, "")
                            if (input.isNotEmpty()) service?.sendGlobalCommand("cmd userdetails", input)
                        }
                        .setNegativeButton("Cancel", null)
                        .setView(dialogBinding.root)
                        .show()
                dialogBinding.editTextTeamName.requestFocus()
            }
            binding.battleSearchButton -> {
                if (childFragmentManager.findFragmentByTag(SearchBattleDialog.FRAGMENT_TAG) == null)
                    SearchBattleDialog().show(childFragmentManager, SearchBattleDialog.FRAGMENT_TAG)
            }
            binding.replaySearchButton -> {
                if (childFragmentManager.findFragmentByTag(SearchReplayDialog.FRAGMENT_TAG) == null)
                    SearchReplayDialog().show(childFragmentManager, SearchReplayDialog.FRAGMENT_TAG)
            }
            binding.newsButton -> {
                if (childFragmentManager.findFragmentByTag(NewsDialog.FRAGMENT_TAG) == null)
                    NewsDialog().show(childFragmentManager, NewsDialog.FRAGMENT_TAG)
            }
            binding.bugReportButton -> AlertDialog.Builder(requireActivity())
                    .setTitle("Wait a minute !")
                    .setMessage("If the bug you want to report needs a detailed description to be clearly understood, please consider posting on the Smogon forum thread.\nIf not, you can continue to the form.\nThanks !")
                    .setPositiveButton("Continue") { _: DialogInterface?, _: Int -> openUrl(URL_BUG_REPORT_GFORM, true) }
                    .setNeutralButton("Go to smogon thread") { _: DialogInterface?, _: Int -> openUrl(URL_SMOGON_THREAD, false) }
                    .setNegativeButton("Cancel", null)
                    .show()
        }
    }

    private fun promptUserSignIn() {
        if (childFragmentManager.findFragmentByTag(SignInDialog.FRAGMENT_TAG) == null)
            SignInDialog().show(childFragmentManager, SignInDialog.FRAGMENT_TAG)
    }

    private fun openUrl(url: String, useChrome: Boolean) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (useChrome) intent.setPackage("com.android.chrome") // Try to use chrome to autoconnect to GForms
            startActivity(intent)
        } catch (e1: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) // Fallback to default browser
            } catch (e2: ActivityNotFoundException) {
                makeSnackbar("No web browser found.")
            }
        }
    }

    private fun setCurrentBattleFormat(battleFormat: BattleFormat, fromUser: Boolean) {
        currentBattleFormat = battleFormat
        if (!fromUser) {
            val adapter = binding.formatsSelector.adapter as CategoryAdapter
            for (position in 0 until adapter.count) {
                if (adapter.getItem(position) == battleFormat) binding.formatsSelector.setSelection(position)
            }
        }
        updateTeamSpinner()
    }

    fun onTeamsChanged() {
        updateTeamSpinner(maintainSelection = true)
    }

    private fun updateTeamSpinner(maintainSelection: Boolean = false) {
        val battleFormat = currentBattleFormat ?: return
        val adapter = binding.teamsSelector.adapter as CategoryAdapter
        if (currentBattleFormat?.isTeamNeeded == true) {
            val previousSelection = if (maintainSelection) binding.teamsSelector.selectedItem as? Team else null
            adapter.clearItems()
            val matches = arrayOf(battleFormat.toId().removeSuffix("blitz"), BattleFormat.FORMAT_OTHER.toId())
            teamsFragment.teams.filter { matches.contains(it.format.toId()) }.forEach { group ->
                adapter.addItem(group)
                adapter.addItems(group.teams)
            }
            if (adapter.count == 0) adapter.addItem(Team.dummyTeam("You have no teams", false))
            binding.teamsSelector.apply {
                isEnabled = adapter.count > 1
                if (maintainSelection && previousSelection != null) {
                    setSelection(adapter.findItemIndex { (it as? Team)?.uniqueId == previousSelection.uniqueId }
                            .takeIf { it > 0 } ?: if (adapter.count > 1) 1 else 0)
                } else {
                    setSelection(if (adapter.count > 1) 1 else 0) // Skip first group label
                }
            }
        } else {
            adapter.clearItems()
            adapter.addItem(Team.dummyTeam("Random"))
            binding.teamsSelector.apply {
                isEnabled = false
                setSelection(0)
            }
        }
    }

    private fun searchForBattle() {
        if (service?.isConnected != true) return
        if (currentBattleFormat!!.isTeamNeeded) {
            val team = binding.teamsSelector.selectedItem as Team?
            if (team == null) {
                makeSnackbar("You have no team !")
                return
            } else if (team.isEmpty) {
                makeSnackbar("Your team is empty !")
                return
            }
            service?.sendGlobalCommand("utm", team.pack())
        } else {
            service?.sendGlobalCommand("utm", "null")
        }
        when {
            isChallengingSomeone -> {
                service?.sendGlobalCommand("challenge", challengeTo!!.toId(), currentBattleFormat!!.label.toId())
                setBattleButtonUIState("Challenging\n$challengeTo...", enabled = false, showCancel = true, tintCard = true)
            }
            isAcceptingChallenge -> service?.sendGlobalCommand("accept", isAcceptingFrom!!)
            else -> service?.sendGlobalCommand("search", currentBattleFormat!!.label.toId())
        }
    }

    fun startReplay(replayId: String) {
        joinRoom("replay-$replayId")
    }

    fun joinRoom(roomId: String) {
        val isReplay = roomId.startsWith("replay-", ignoreCase = true)
        val isBattle = roomId.startsWith("battle-", ignoreCase = true) || isReplay
        val currentRoomId = if (isBattle) battleFragment.observedRoomId else chatFragment.observedRoomId
        if (currentRoomId != null) {
            if (isBattle) mainActivity.showBattleFragment() else mainActivity.showChatFragment()
            if (currentRoomId == roomId) return
            AlertDialog.Builder(mainActivity).apply {
                setTitle("Warning")
                setMessage("You are already in " concat readableRoomName(currentRoomId) concat ".\nJoining "
                        concat readableRoomName(roomId) concat " will make you leave "
                        concat readableRoomName(currentRoomId) concat ".")
                setPositiveButton("Continue") { _, _ -> requestRoomJoin(roomId) }
                setNegativeButton("Cancel") { _,_ -> }
                show()
            }
        } else {
            requestRoomJoin(roomId)
        }
    }

    private fun readableRoomName(roomId: String) = when {
        roomId.startsWith("replay-", ignoreCase = true) -> "replay " concat "'${roomId.removePrefix("replay-").replace("-", " ")}'".italic()
        roomId.startsWith("battle-", ignoreCase = true) -> "battle " concat "'${roomId.removePrefix("battle-").replace("-", " ")}'".italic()
        else -> "room " concat "'$roomId'".italic()
    }

    private fun requestRoomJoin(roomId: String) {
        val isWaitingForConnection = onConnectedListeners.containsKey(roomId)
        if (isWaitingForConnection) return
        if (service?.isConnected != true) {
            onConnectedListeners[roomId] = {
                onConnectedListeners.remove(roomId)
                requestRoomJoin(roomId)
            }
            return
        }
        val isWaitingForRoomToDeinit = roomDeinitListeners.containsKey(roomId)
        if (isWaitingForRoomToDeinit) return

        val isReplay = roomId.startsWith("replay-", ignoreCase = true)
        val isBattle = roomId.startsWith("battle-", ignoreCase = true) || isReplay
        val currentRoomId = if (isBattle) battleFragment.observedRoomId else chatFragment.observedRoomId
        if (currentRoomId != null) {
            roomDeinitListeners[roomId] = { deinitRoomId ->
                if (deinitRoomId == currentRoomId) { // Now that previous room is safely leaved, join the new one
                    roomDeinitListeners.remove(roomId)
                    if (isReplay)
                        service?.replayManager?.startReplay(roomId)
                    else
                        service?.sendGlobalCommand("join", roomId)
                }
            }
            if (currentRoomId.startsWith("replay-"))
                service?.replayManager?.closeReplay()
            else
                service?.sendRoomCommand(currentRoomId, "leave")
        } else {
            if (isReplay)
                service?.replayManager?.startReplay(roomId)
            else
                service?.sendGlobalCommand("join", roomId)
        }
    }

    fun startPrivateChat(user: String) {
        if (user.toId() == observer.myUsername?.toId()) {
            makeSnackbar("Cannot talk to yourself")
            return
        }
        if (parentFragmentManager.findFragmentByTag(PrivateChatDialog.FRAGMENT_TAG) != null) return
        val dialog = PrivateChatDialog.newInstance(user)
        dialog.show(parentFragmentManager, PrivateChatDialog.FRAGMENT_TAG)
    }

    fun getPrivateMessages(with: String): List<String>? {
        return observer.getPrivateMessages(with)
    }

    fun challengeSomeone(user: String) {
        when {
            user.toId() == observer.myUsername?.toId() -> makeSnackbar("You should try challenging yourself in an other way")
            isChallengingSomeone -> makeSnackbar("You are already challenging someone")
            isSearchingBattle -> makeSnackbar("Cannot challenge someone while searching for battle")
            isAcceptingChallenge -> makeSnackbar("You are already accepting a challenge")
            battleFragment.battleRunning -> makeSnackbar("You cannot challenge someone while being in a battle")
            else -> {
                isChallengingSomeone = true
                waitingForChallenge = false
                challengeTo = user
                setBattleButtonUIState("Challenge\n$user!", showCancel = true, tintCard = true)
                showSearchableFormatsOnly(false)
                mainActivity.showHomeFragment()
                requireView().post { (requireView() as ScrollView).fullScroll(View.FOCUS_UP) }
            }
        }
    }

    private fun setBattleButtonUIState(label: String, enabled: Boolean = true, showCancel: Boolean = false, tintCard: Boolean = false) {
        binding.searchButton.apply {
            text = label
            isEnabled = enabled
        }
        binding.formatsSelector.isEnabled = enabled
        binding.teamsSelector.apply { if (isEnabled && !enabled) isEnabled = false }
        val isCancelShown = binding.cancelButton.isShown
        if (isCancelShown != showCancel) {
            if (showCancel) binding.cancelButton.apply {
                visibility = View.VISIBLE
                alpha = 0f
                animate().alpha(1f).setDuration(250).withEndAction(null).start()
            }
            else binding.cancelButton.animate().alpha(0f).setDuration(250).withEndAction {
                binding.cancelButton.visibility = View.GONE
            }.start()

        }
        binding.searchContainer.setCardBackgroundColor(
                if (tintCard) ColorUtils.blendARGB(
                        ContextCompat.getColor(requireActivity(), R.color.surfaceBackground),
                        ContextCompat.getColor(requireActivity(), R.color.primary),
                        0.25f)
                else ContextCompat.getColor(requireActivity(), R.color.surfaceBackground)
        )
    }

    private fun showSearchableFormatsOnly(yes: Boolean) {
        if (battleFormats == null) return
        val adapter = binding.formatsSelector.adapter as CategoryAdapter
        adapter.clearItems()
        for (category in battleFormats!!) {
            val formats = if (yes) category.searchableBattleFormats else category.formats
            if (formats.isEmpty()) continue
            adapter.addItem(category)
            adapter.addItems(formats)
        }
        binding.formatsSelector.setSelection(1)
    }

    private fun notifyNewMessageReceived() {
        mainActivity.showBadge(id)
    }

    fun showNewsBanner() {
        fragmentScope.launch {
            val latestNews = service?.retrieveLatestNews()?.getJSONObject(0)?.let { NewsDialog.News(it) } ?: return@launch
            binding.news.apply {
                text = latestNews.title.bold() concat " - " concat latestNews.content
                isSelected = true
                visibility = VISIBLE
                animate().alpha(1f).translationY(0f)
            }
        }
    }

    fun hideNewsBanner() {
        binding.news.apply {
            animate().alpha(0f).translationY(-height.toFloat()).withEndAction {
                text = null
                isSelected = false
                visibility = View.GONE
            }.start()
        }
    }

    override fun onServiceBound(service: ShowdownService) {
        super.onServiceBound(service)
        service.globalMessageObserver.uiCallbacks = this
        if (!service.isConnected) {
            makeSnackbar("Connecting to Showdown server...", indefinite = true)
            service.connectToServer()
        }
        if (Preferences.isNewsBannerEnabled(requireContext())) showNewsBanner()
    }

    override fun onServiceWillUnbound(service: ShowdownService) {
        super.onServiceWillUnbound(service)
        service.globalMessageObserver.uiCallbacks = null
    }

    override fun onConnectedToServer() {
        activeSnackbar?.dismiss()
        if (battleFragment.observedRoomId != null) battleFragment.observedRoomId = null
        if (chatFragment.observedRoomId != null) chatFragment.observedRoomId = null
        onConnectedListeners.values.forEach { it.invoke() }
    }

    override fun onUserChanged(userName: String, isGuest: Boolean, avatarId: String) {
        binding.username.apply {
            text = "Connected as\n".small()
            append(userName.truncate(10).bold())
        }
        binding.loginButton.isEnabled = true
        if (isGuest) {
            binding.loginButton.setImageResource(R.drawable.ic_login)
        } else {
            binding.loginButton.setImageResource(R.drawable.ic_logout)
            makeSnackbar("Connected as $userName")
        }
        val signInDialog = childFragmentManager.findFragmentByTag(SignInDialog.FRAGMENT_TAG) as SignInDialog?
        signInDialog?.dismissAllowingStateLoss()
    }

    override fun onUpdateCounts(userCount: Int, battleCount: Int) {
        binding.usersCount.apply {
            text = (if (userCount >= 0) userCount.toString() else "-").bold()
            append("\nusers online".small())
        }
        binding.battlesCount.apply {
            text = (if (battleCount >= 0) battleCount.toString() else "-").bold()
            append("\nactive battles".small())
        }
    }

    override fun onBattleFormatsChanged(battleFormats: List<BattleFormat.Category>) {
        this@HomeFragment.battleFormats = battleFormats
        showSearchableFormatsOnly(true)
        teamsFragment.onBattleFormatsChanged()
        val dialog = childFragmentManager.findFragmentByTag(SearchBattleDialog.FRAGMENT_TAG) as SearchBattleDialog?
        dialog?.onBattleFormatsChanged()
    }

    override fun onSearchBattlesChanged(searching: List<String>, games: Map<String, String>) {
        isSearchingBattle = searching.isNotEmpty()
        when {
            isSearchingBattle -> setBattleButtonUIState("Searching...", enabled = false, showCancel = true, tintCard = false)
            !isChallengingSomeone -> setBattleButtonUIState("Battle !", enabled = true, showCancel = false, tintCard = false)
        }

        binding.joinContainer.visibility = if (games.isEmpty()) View.GONE else View.VISIBLE
        if (! battleFragment.isReplay) {
            // Don't hide the search container if the user is watching a replay
            binding.searchContainer.visibility = if (games.isNotEmpty()) View.GONE else View.VISIBLE
        }

        val previousCount = binding.joinContainer.childCount
        binding.joinedBattlesContainer.removeAllViews()
        for ((roomId, value) in games) {
            layoutInflater.inflate(R.layout.button_joined_battle, binding.joinedBattlesContainer)
            (binding.joinedBattlesContainer.children.last() as MaterialButton).apply {
                text = value
                tag = roomId
                isEnabled = roomId != battleFragment.observedRoomId
                setOnClickListener { joinRoom(roomId) }
            }
        }
        val newCount = binding.joinedBattlesContainer.childCount
        if (newCount > 0 && newCount != previousCount) notifyNewMessageReceived()
    }

    override fun onReplaySaved(replayId: String, url: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Battle replay url", url))
        MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle("Replay saved successfully")
            setMessage("Your replay has been uploaded! Url has been copied to clipboard.")
            setPositiveButton("Share") { _, _ ->
                val intent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(intent, "Share replay"))
            }
            setNegativeButton("Close", null)
            show()

        }
    }

    override fun onUserDetails(id: String, name: String, online: Boolean, group: String,
                               rooms: List<String>, battles: List<String>) {
        val builder = SpannableStringBuilder()
        builder.append("Group: ".italic()).append(group.replace(" ", "␣")).append("\n")
        builder.append("Battles:\n".italic())
        if (battles.isNotEmpty()) {
            battles.forEach { battleId ->
                builder.append(" - ".small())
                builder.append(battleId.removePrefix("battle-").url("https://play.pokemonshowdown.com/$battleId").small())
                builder.append("\n")
            }
        } else {
            builder.append("None".small()).append("\n")
        }
        builder.append("Chatrooms:\n".italic())
        if (rooms.isNotEmpty()) {
            rooms.forEach { roomId ->
                builder.append(" - ".small())
                builder.append(roomId.url("https://play.pokemonshowdown.com/$roomId").small())
                builder.append("\n")
            }
        } else {
            builder.append("None".small()).append("\n")
        }
        if (!online) builder.append("(Offline)".color(Color.RED))
        val dialog = AlertDialog.Builder(requireActivity()).run {
            setTitle(name)
            setMessage(builder)
            if (online) {
                setPositiveButton("Challenge") { _, _ -> challengeSomeone(name) }
                setNegativeButton("Chat") { _, _ -> startPrivateChat(name) }
            } else {
                setNegativeButton("Close") { _, _ -> }
            }
            show()
        }
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onShowPopup(message: String) {
        if (message.length > 120 || message.count { it == '\n' } > 4) { // If there is a lot of text, rather show a dialog
            AlertDialog.Builder(requireContext()).apply {
                setMessage(message)
                setPositiveButton("Ok") { _, _ -> }
                show()
            }
            activeSnackbar?.dismiss()
        } else {
            makeSnackbar(message, indefinite = true, maxLines = 5, action = "Ok" to {})
        }

        if (isChallengingSomeone) { // Resetting pending challenges
            isChallengingSomeone = false
            waitingForChallenge = isChallengingSomeone
            challengeTo = null
            setBattleButtonUIState("Battle !")
            showSearchableFormatsOnly(true)
        } else if (isAcceptingChallenge) {
            isAcceptingChallenge = false
            isAcceptingFrom = null
            setBattleButtonUIState("Battle !", enabled = true, showCancel = false, tintCard = false)
            showSearchableFormatsOnly(true)
            binding.formatsSelector.isEnabled = true
        }

        // Closing pm dialog to see this popup
        val dialog = parentFragmentManager.findFragmentByTag(PrivateChatDialog.FRAGMENT_TAG) as PrivateChatDialog?
        if (dialog != null && dialog.isVisible) dialog.dismiss()
    }

    override fun onAvailableRoomsChanged(officialRooms: List<ChatRoomInfo>, chatRooms: List<ChatRoomInfo>) {
        chatFragment.onAvailableRoomsChanged(officialRooms, chatRooms)
    }

    override fun onAvailableBattleRoomsChanged(battleRooms: List<BattleRoomInfo>) {
        val dialog = childFragmentManager.findFragmentByTag(SearchBattleDialog.FRAGMENT_TAG) as SearchBattleDialog?
        dialog?.onSearchBattleResponse(battleRooms)
    }

    override fun onNewPrivateMessage(with: String, message: String) {
        binding.pmsOverview.incrementPmCount(with)
        if (!binding.pmsOverview.isEmpty) binding.pmsContainer.visibility = View.VISIBLE
        val dialog = parentFragmentManager.findFragmentByTag(PrivateChatDialog.FRAGMENT_TAG) as PrivateChatDialog?
        if (dialog?.chatWith?.toId() == with.toId()) dialog.onNewMessage(message) else notifyNewMessageReceived()
    }

    override fun onChallengesChange(to: String?, format: String?, from: Map<String, String>) {
        binding.pmsOverview.apply {
            updateChallengeTo(to, format)
            updateChallengesFrom(from.keys, from.values)
        }
        if (binding.pmsOverview.isEmpty) {
            binding.pmsContainer.visibility = View.GONE
        } else {
            binding.pmsContainer.visibility = View.VISIBLE
            notifyNewMessageReceived()
        }
        if (isAcceptingChallenge) {
            val done = !from.keys.contains(isAcceptingFrom)
            if (done) {
                isAcceptingChallenge = false
                isAcceptingFrom = null
                setBattleButtonUIState("Battle !")
                showSearchableFormatsOnly(true)
                binding.formatsSelector.isEnabled = true
            }
        }
        if (isChallengingSomeone) {
            if (to != null) {
                waitingForChallenge = true
                setBattleButtonUIState("Waiting for\n$to...", enabled = false, showCancel = true, tintCard = true)
            } else {
                isChallengingSomeone = false
                waitingForChallenge = isChallengingSomeone
                challengeTo = null
                setBattleButtonUIState("Battle !")
                showSearchableFormatsOnly(true)
            }
        } else {
            if (to != null) { // We are challenging someone but we discover that by the server
                challengeTo = to
                isChallengingSomeone = true
                waitingForChallenge = true
                setBattleButtonUIState("Waiting for\n$to...", enabled = false, showCancel = true, tintCard = true)
            }
        }

    }

    override fun onRoomInit(roomId: String, type: String) {
        when (type) {
            "battle" -> {
                if (battleFragment.observedRoomId == null) {
                    battleFragment.observedRoomId = roomId
                    mainActivity.showBattleFragment()
                } else if (!battleFragment.battleRunning && !battleFragment.isReplay) {
                    service?.sendRoomCommand(battleFragment.observedRoomId, "leave")
                    battleFragment.observedRoomId = roomId
                    mainActivity.showBattleFragment()
                } else {
                    // Most of the time this is an auto joined battle coming from a new search, let's
                    // just leave it silently. If the user wants to join it deliberately, he will
                    // be able to do that from the "you're currently in" menu.
                    if (roomId.startsWith("replay-")) service?.replayManager?.closeReplay()
                    else service?.sendRoomCommand(roomId, "leave")
                }
                binding.joinedBattlesContainer.children.forEach { button ->
                    // Disable the corresponding button if battle is already joined
                    button.isEnabled = button.tag as String != battleFragment.observedRoomId
                }
            }
            "chat" -> {
                // lobby init can trigger this two times, make sure to avoid that
                if (chatFragment.observedRoomId == roomId) return

                if (chatFragment.observedRoomId == null) {
                    chatFragment.observedRoomId = roomId
                    mainActivity.showChatFragment()
                } else {
                    service?.sendRoomCommand(roomId, "leave")
                }
            }
        }
    }

    override fun onRoomDeinit(roomId: String) {
        when (roomId) {
            battleFragment.observedRoomId -> battleFragment.observedRoomId = null
            chatFragment.observedRoomId -> chatFragment.observedRoomId = null
        }
        roomDeinitListeners.values.forEach { it.invoke(roomId) }
    }

    override fun onNetworkError() {
        Snackbar.make(requireView(), "Unable to reach Showdown server", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    makeSnackbar("Reconnecting to Showdown server...", indefinite = true)
                    service!!.reconnectToServer()
                }
                .show()
    }

    private inner class TeamsAdapter : CategoryAdapter(context) {

        override fun isCategoryItem(position: Int) = getItem(position) is Team.Group

        override fun getCategoryLabel(position: Int): String? {
            val formatId = (getItem(position) as Team.Group).format
            return BattleFormat.resolveName(battleFormats, formatId)
        }

        internal inner class ViewHolder(view: View) {
            var job: Job? = null
            val labelView: TextView = view.findViewById(R.id.text_view_title)
            val pokemonViews = listOf(
                    R.id.image_view_pokemon1, R.id.image_view_pokemon2, R.id.image_view_pokemon3,
                    R.id.image_view_pokemon4, R.id.image_view_pokemon5, R.id.image_view_pokemon6
            ).map { view.findViewById<ImageView>(it) }
        }

        override fun getItemView(position: Int, convertView: View?, parent: ViewGroup): View {
            val convertView = convertView ?: layoutInflater.inflate(R.layout.dropdown_item_team, parent, false).apply {
                tag = ViewHolder(this)
            }
            val viewHolder = convertView.tag as ViewHolder

            val team = getItem(position) as Team
            viewHolder.labelView.text = team.label
            viewHolder.pokemonViews.forEach { it.setImageDrawable(null) }
            if (team.pokemons.isEmpty()) return convertView

            viewHolder.job?.cancel()
            viewHolder.job = fragmentScope.launch {
                assetLoader.dexIcons(*team.pokemons.map { it.species.toId() }.toTypedArray()).forEachIndexed { index, bitmap ->
                    val drawable = BitmapDrawable(convertView.resources, bitmap)
                    viewHolder.pokemonViews[index].setImageDrawable(drawable)
                }
            }
            return convertView
        }
    }

    companion object {
        private const val URL_BUG_REPORT_GFORM = "https://docs.google.com/forms/d/e/1FAIpQLSfvaHpKtRhN-naHtmaIongBRzjU0rmPXu770tvjseWUNky48Q/viewform?usp=send_form"
        private const val URL_SMOGON_THREAD = "https://www.smogon.com/forums/threads/02-23-alpha06-unofficial-showdown-android-client.3654298/"
        private val USERNAME_REGEX = "[{}:\",|\\[\\]]".toRegex()
    }
}