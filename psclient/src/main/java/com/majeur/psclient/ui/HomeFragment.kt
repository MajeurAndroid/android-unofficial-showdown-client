package com.majeur.psclient.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
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
import com.majeur.psclient.model.AvailableBattleRoomsInfo
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
import java.util.*

class HomeFragment : BaseFragment(), GlobalMessageObserver.UiCallbacks, View.OnClickListener {

    private val observer get() = service!!.globalMessageObserver

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
    private var onConnectedListener: (() -> Unit)? = null
    private var nextDeinitListener: ((String) -> Unit)? = null

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var activeSnackbar: Snackbar? = null
    private val snackbarCallbacks = object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
        override fun onDismissed(snackbar: Snackbar?, event: Int) {
            activeSnackbar?.removeCallback(this)
            if (activeSnackbar == snackbar) activeSnackbar = null
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        assetLoader = mainActivity.assetLoader
        soundEnabled = Preferences.getBoolPreference(context, "sound")
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
    }

    private fun makeSnackbar(message: String, indefinite: Boolean = false) {
        activeSnackbar = Snackbar.make(binding.root, message, if (indefinite) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG)
                .addCallback(snackbarCallbacks)
        activeSnackbar!!.show()
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
                override fun isCategoryItem(position: Int): Boolean {
                    return getItem(position) is BattleFormat.Category
                }

                override fun getCategoryLabel(position: Int): String {
                    return (getItem(position) as BattleFormat.Category).label
                }

                override fun getItemLabel(position: Int): String {
                    return (getItem(position) as BattleFormat).label
                }
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
                Preferences.setPreference(context, "sound", soundEnabled)
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
        binding.cancelButton.setOnClickListener(this)
        binding.searchButton.setOnClickListener(this)
        binding.userSearchButton.setOnClickListener(this)
        binding.bugReportButton.setOnClickListener(this)

        binding.TODOViewReplayButton.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        if (service?.isConnected != true) return
        when (view) {
            binding.loginButton -> {
                val myUsername = service?.getSharedData<String?>("myusername")?.substring(1)
                if (myUsername?.toLowerCase()?.startsWith("guest") == true) {
                    if (parentFragmentManager.findFragmentByTag(SignInDialog.FRAGMENT_TAG) == null)
                        SignInDialog.newInstance().show(parentFragmentManager, SignInDialog.FRAGMENT_TAG)
                } else {
                    service?.sendGlobalCommand("logout")
                    service?.forgetUserLoginInfos()
                }
            }
            binding.searchButton -> {
                if (observer.isUserGuest) {
                    if (parentFragmentManager.findFragmentByTag(SignInDialog.FRAGMENT_TAG) == null)
                        SignInDialog.newInstance().show(parentFragmentManager, SignInDialog.FRAGMENT_TAG)
                } else {
                    if (battleFragment.battleRunning())
                        makeSnackbar("A battle is already running")
                    else
                        searchForBattle()
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
            binding.bugReportButton -> AlertDialog.Builder(requireActivity())
                    .setTitle("Wait a minute !")
                    .setMessage("If the bug you want to report needs a detailed description to be clearly understood, please consider posting on the Smogon forum thread.\nIf not, you can continue to the form.\nThanks !")
                    .setPositiveButton("Continue") { _: DialogInterface?, _: Int -> openUrl(URL_BUG_REPORT_GFORM, true) }
                    .setNeutralButton("Go to smogon thread") { _: DialogInterface?, _: Int -> openUrl(URL_SMOGON_THREAD, false) }
                    .setNegativeButton("Cancel", null)
                    .show()

//           binding.TODOViewReplayButton
            binding.TODOViewReplayButton -> {
//                service?.replayManager?.startReplayDownload("https://replay.pokemonshowdown.com/smogtours-ou-39893.json");
                service?.replayManager?.downloadAndStartReplay("https://replay.pokemonshowdown.com/gen8randombattle-1154942374.json");
            }
        }
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
            val count = adapter.count
            for (position in 0 until count) {
                if (adapter.getItem(position) == battleFormat) binding.formatsSelector.setSelection(position)
            }
        }
        updateTeamSpinner()
    }

    fun updateTeamSpinner() {
        if (currentBattleFormat == null) return
        val adapter = binding.teamsSelector.adapter as CategoryAdapter
        adapter.clearItems()
        if (currentBattleFormat?.isTeamNeeded == true) {
            val teamGroups = teamsFragment.teams
            var matchingFormatGroupIndex = -1
            var otherFormatGroupIndex = -1
            for (i in teamGroups.indices) {
                val id = teamGroups[i].format.toId()
                if (BattleFormat.FORMAT_OTHER.toId() == id) otherFormatGroupIndex = i
                if (currentBattleFormat!!.toId() == id) matchingFormatGroupIndex = i
            }
            if (matchingFormatGroupIndex != -1) Collections.swap(teamGroups,
                    matchingFormatGroupIndex, 0)
            if (otherFormatGroupIndex != -1) Collections.swap(teamGroups, otherFormatGroupIndex,
                    if (matchingFormatGroupIndex == -1) 0 else 1)
            for (group in teamGroups) {
                adapter.addItem(group)
                adapter.addItems(group.teams)
            }
            binding.teamsSelector.apply {
                isEnabled = true
                setSelection(1)
            }
        } else {
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

    private fun viewReplay() {

    }

    fun requestRoomJoin(roomId: String) {
        val isWaitingForConnection = onConnectedListener != null
        if (isWaitingForConnection) return
        if (service?.isConnected != true) {
            onConnectedListener = {
                onConnectedListener = null
                requestRoomJoin(roomId)
            }
            return
        }
        val isWaitingForRoomToDeinit = nextDeinitListener != null
        if (isWaitingForRoomToDeinit) return

        val isBattle = roomId.startsWith("battle-", ignoreCase = true)
        val isReplay = roomId.startsWith("replay-", ignoreCase = true)

        val currentRoomId = if (isBattle) battleFragment.observedRoomId else chatFragment.observedRoomId
        if (currentRoomId != null) {
            service?.sendRoomCommand(currentRoomId, "leave")
            nextDeinitListener = { deinitRoomId ->
                if (deinitRoomId == currentRoomId) { // Now that previous room is safely leaved, join the new one
                    service?.sendGlobalCommand("join", roomId)
                    nextDeinitListener = null
                }
            }
        } else {
            service?.sendGlobalCommand("join", roomId)
        }
    }

    fun startPrivateChat(user: String) {
        val myUsername = service?.getSharedData<String?>("myusername")?.substring(1)
        if (user == myUsername) {
            makeSnackbar("Cannot talk to yourself")
            return
        }
        if (parentFragmentManager.findFragmentByTag(PrivateChatDialog.FRAGMENT_TAG) != null) return
        val dialog = PrivateChatDialog.newInstance(user)
        dialog.show(parentFragmentManager, PrivateChatDialog.FRAGMENT_TAG)
    }

    fun getPrivateMessages(with: String?): List<String>? {
        return observer.getPrivateMessages(with!!)
    }

    fun challengeSomeone(user: String) {
        val myUsername = service?.getSharedData<String?>("myusername")?.substring(1)
        when {
            user == myUsername -> makeSnackbar("You should try challenging yourself in an other way")
            isChallengingSomeone -> makeSnackbar("You are already challenging someone")
            isSearchingBattle -> makeSnackbar("Cannot challenge someone while searching for battle")
            isAcceptingChallenge -> makeSnackbar("You are already accepting a challenge")
            battleFragment.battleRunning() -> makeSnackbar("You cannot challenge someone while being in a battle")
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
        binding.teamsSelector.isEnabled = enabled
        binding.formatsSelector.isEnabled = enabled
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

    override fun onServiceBound(service: ShowdownService) {
        super.onServiceBound(service)
        service.globalMessageObserver.uiCallbacks = this
        if (!service.isConnected) {
            makeSnackbar("Connecting to Showdown server...", indefinite = true)
            service.connectToServer()
        }
    }

    override fun onServiceWillUnbound(service: ShowdownService) {
        super.onServiceWillUnbound(service)
        service.globalMessageObserver.uiCallbacks = null
    }

    override fun onConnectedToServer() {
        activeSnackbar?.dismiss()
        if (battleFragment.observedRoomId != null) battleFragment.observedRoomId = null
        if (chatFragment.observedRoomId != null) chatFragment.observedRoomId = null
        onConnectedListener?.invoke()
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
        val signInDialog = parentFragmentManager.findFragmentByTag(SignInDialog.FRAGMENT_TAG) as SignInDialog?
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
    }

    override fun onSearchBattlesChanged(searching: List<String>, games: Map<String, String>) {
        isSearchingBattle = searching.isNotEmpty()
        when {
            isSearchingBattle -> setBattleButtonUIState("Searching...", enabled = false, showCancel = true, tintCard = false)
            !isChallengingSomeone -> setBattleButtonUIState("Battle !", enabled = true, showCancel = false, tintCard = false)
        }
        binding.joinContainer.visibility = if (games.isEmpty()) View.GONE else View.VISIBLE
        binding.searchContainer.visibility = if (games.isNotEmpty()) View.GONE else View.VISIBLE

        binding.joinedBattlesContainer.removeAllViews()
        for ((roomId, value) in games) {
            layoutInflater.inflate(R.layout.button_joined_battle, binding.joinedBattlesContainer)
            (binding.joinedBattlesContainer.children.last() as MaterialButton).apply {
                text = value
                tag = roomId
                isEnabled = roomId != battleFragment.observedRoomId
                setOnClickListener { requestRoomJoin(roomId) }
            }
        }
    }

    override fun onUserDetails(id: String, name: String, online: Boolean, group: String,
                               rooms: List<String>, battles: List<String>) {
        val builder = SpannableStringBuilder()
        builder.append("Group: ".italic()).append(group.replace(" ", "␣")).append("\n")
        builder.append("Battles: ".italic())
        if (battles.isNotEmpty()) {
            val stringBuilder = StringBuilder()
            for (battle in battles) stringBuilder.append(battle).append(", ")
            stringBuilder.deleteCharAt(stringBuilder.length - 2)
            builder.append(stringBuilder.toString().small())
        } else {
            builder.append("None".small()).append("\n")
        }
        builder.append("Chatrooms: ".italic())
        if (rooms.isNotEmpty()) {
            val stringBuilder = StringBuilder()
            for (room in rooms) stringBuilder.append(room).append(", ")
            stringBuilder.deleteCharAt(stringBuilder.length - 2)
            builder.append(stringBuilder.toString().small())
        } else {
            builder.append("None".small()).append("\n")
        }
        if (!online) builder.append("(Offline)".color(Color.RED))
        AlertDialog.Builder(requireActivity())
                .setTitle(name)
                .setMessage(builder)
                .setPositiveButton("Challenge") { _: DialogInterface?, _: Int -> challengeSomeone(name) }
                .setNegativeButton("Chat") { _: DialogInterface?, _: Int -> startPrivateChat(name) }
                .show()
    }

    override fun onShowPopup(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
        val view = snackbar.view
        val textView = view.findViewById<TextView?>(com.google.android.material.R.id.snackbar_text)
        textView!!.maxLines = 5
        snackbar.setAction("Ok") { }
        snackbar.show()
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

    override fun onAvailableBattleRoomsChanged(availableRoomsInfo: AvailableBattleRoomsInfo) {}

    override fun onNewPrivateMessage(with: String, message: String) {
        binding.pmsOverview.incrementPmCount(with)
        if (!binding.pmsOverview.isEmpty) binding.pmsContainer.visibility = View.VISIBLE
        val dialog = parentFragmentManager.findFragmentByTag(PrivateChatDialog.FRAGMENT_TAG) as PrivateChatDialog?
        if (dialog?.chatWith == with) dialog.onNewMessage(message) else notifyNewMessageReceived()
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
            var done = true
            for (user in from.keys) if (isAcceptingFrom == user) {
                done = false
                break
            }
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
        }
    }

    override fun onRoomInit(roomId: String, type: String) {
        when (type) {
            "battle" -> {
                if (battleFragment.observedRoomId == null || !battleFragment.battleRunning()) {
                    battleFragment.observedRoomId = roomId
                    mainActivity.showBattleFragment()
                } else {
                    // Most of the time this is an auto joined battle coming from a new search, let's
                    // just leave it silently. If the user wants to join it deliberately, he will
                    // be able to do that from the "you're currently in" menu.
                    this@HomeFragment.service!!.sendRoomCommand(roomId, "leave")
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
                    this@HomeFragment.service!!.sendRoomCommand(roomId, "leave")
                }
            }
        }
    }

    override fun onRoomDeinit(roomId: String) {
        when (roomId) {
            battleFragment.observedRoomId -> battleFragment.observedRoomId = null
            chatFragment.observedRoomId -> chatFragment.observedRoomId = null
        }
        nextDeinitListener?.invoke(roomId)
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