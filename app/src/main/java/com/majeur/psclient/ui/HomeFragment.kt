package com.majeur.psclient.ui

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
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.majeur.psclient.R
import com.majeur.psclient.databinding.DialogBattleMessageBinding
import com.majeur.psclient.databinding.FragmentHomeBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.model.AvailableBattleRoomsInfo
import com.majeur.psclient.model.Id
import com.majeur.psclient.model.common.BattleFormat
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.service.GlobalMessageObserver
import com.majeur.psclient.service.ShowdownService
import com.majeur.psclient.util.BackgroundBitmapDrawable
import com.majeur.psclient.util.Preferences
import com.majeur.psclient.util.Utils
import com.majeur.psclient.util.toId
import com.majeur.psclient.widget.CategoryAdapter
import com.majeur.psclient.widget.PrivateMessagesOverviewWidget
import com.majeur.psclient.widget.PrivateMessagesOverviewWidget.OnItemButtonClickListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class HomeFragment : BaseFragment(), View.OnClickListener {

    private lateinit var assetLoader: AssetLoader

    private var currentBattleFormat: BattleFormat? = null
    private var battleFormats: List<BattleFormat.Category>? = null
    private var pendingBattleToJoin: String? = null
    private var soundEnabled = false
    private var isSearchingBattle = false
    private var isChallengingSomeone = false
    private var waitingForChallenge = false
    private var challengeTo: String? = null
    private var isAcceptingChallenge = false
    private var isAcceptingFrom: String? = null

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun makeSnackbar(message: String, indefinite: Boolean = false) {
        Snackbar.make(binding.root, message, if (indefinite) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (view.background == null) view.background = BackgroundBitmapDrawable(resources, R.drawable.client_bg)
        binding.usersCount.apply {
            text = Utils.boldText("-")
            append(Utils.smallText("\nusers online"))
        }
        binding.battlesCount.apply {
            text = Utils.boldText("-")
            append(Utils.smallText("\nactive battles"))
        }
        binding.username.apply {
            text = Utils.smallText("Connected as\n")
            append(Utils.boldText("-"))
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
            setOnItemClickListener { _, with -> startPrivateChat(with) }
            setOnItemButtonClickListener(object : OnItemButtonClickListener {

                override fun onChallengeButtonClick(p: PrivateMessagesOverviewWidget, with: String) = challengeSomeone(with)

                override fun onAcceptButtonClick(p: PrivateMessagesOverviewWidget, with: String, format: String) {
                    when {
                        isSearchingBattle -> makeSnackbar("Cannot accept challenge while searching for battle")
                        isChallengingSomeone -> makeSnackbar("You are already challenging someone")
                        else -> {
                            isAcceptingChallenge = true
                            isAcceptingFrom = with
                            setBattleButtonUIState("Accept\n$with's\nchallenge", enabled = true, showCancel = true, tintCard = true)
                            showSearchableFormatsOnly(false)
                            battleFormats?.forEach { category ->
                                category.battleFormats.firstOrNull { Id.toId(format) == it.id() }?.let {
                                    setCurrentBattleFormat(it, false)
                                }
                            }
                            binding.formatsSelector.isEnabled = false
                            view.post { (view as ScrollView).fullScroll(View.FOCUS_UP) }
                        }
                    }

                }
            })
        }
        binding.cancelButton.setOnClickListener(this)
        binding.searchButton.setOnClickListener(this)
        binding.userSearchButton.setOnClickListener(this)
        binding.bugReportButton.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        if (service?.isConnected != true) return
        when (view) {
            binding.loginButton -> {
                val myUsername = service?.getSharedData<String?>("myusername")?.substring(1)
                if (myUsername?.toLowerCase()?.startsWith("guest") == true) {
                    if (fragmentManager?.findFragmentByTag(SignInDialog.FRAGMENT_TAG) == null)
                        SignInDialog.newInstance().show(fragmentManager!!, SignInDialog.FRAGMENT_TAG)
                } else {
                    service?.sendGlobalCommand("logout")
                    service?.forgetUserLoginInfos()
                }
            }
            binding.searchButton -> {
                if (observer.isUserGuest) {
                    if (requireFragmentManager().findFragmentByTag(SignInDialog.FRAGMENT_TAG) == null)
                        SignInDialog.newInstance().show(requireFragmentManager(), SignInDialog.FRAGMENT_TAG)
                } else {
                    if (battleFragment.battleRunning() == true)
                        makeSnackbar("A battle is already running")
                    else
                        searchForBattle()
                }
            }
            binding.cancelButton -> when {
                isChallengingSomeone -> {
                    if (waitingForChallenge) {
                        service?.sendGlobalCommand("cancelchallenge", Id.toId(challengeTo))
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
                val id = Id.toIdSafe(teamGroups[i].format)
                if (BattleFormat.FORMAT_OTHER.id() == id) otherFormatGroupIndex = i
                if (currentBattleFormat!!.id() == id) matchingFormatGroupIndex = i
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
                service?.sendGlobalCommand("challenge", Id.toId(challengeTo), Id.toId(currentBattleFormat!!.label))
                setBattleButtonUIState(String.format("Challenging\n%s...", challengeTo), enabled = false, showCancel = true, tintCard = true)
            }
            isAcceptingChallenge -> service?.sendGlobalCommand("accept", isAcceptingFrom!!)
            else -> service?.sendGlobalCommand("search", Id.toId(currentBattleFormat!!.label))
        }
    }

    private fun tryJoinBattleRoom(roomId: String) {
        if (service?.isConnected != true) return
        if (battleFragment.observedRoomId == null || !battleFragment.battleRunning()) {
            service?.sendGlobalCommand("join", roomId)
        } else {
            val runningBattleRoomId = battleFragment.observedRoomId
            if (runningBattleRoomId == roomId) {
                mainActivity.showBattleFragment()
                return
            }
            val currentBattleName = runningBattleRoomId?.substring("battle-".length)
            val battleName = roomId.substring("battle-".length)
            AlertDialog.Builder(requireActivity())
                    .setTitle("Do you want to continue ?")
                    .setMessage(String.format("Joining battle '%s' will make you leave (and forfeit) the current battle.", battleName))
                    .setPositiveButton("Continue") { _: DialogInterface?, _: Int ->
                        pendingBattleToJoin = roomId
                        service!!.sendRoomCommand(runningBattleRoomId, "forfeit")
                        service!!.sendRoomCommand(runningBattleRoomId, "leave")
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
        }
    }

    fun startPrivateChat(user: String) {
        val myUsername = service?.getSharedData<String?>("myusername")?.substring(1)
        if (user == myUsername) {
            makeSnackbar("Cannot talk to yourself")
            return
        }
        if (requireFragmentManager().findFragmentByTag(PrivateChatDialog.FRAGMENT_TAG) != null) return
        val dialog = PrivateChatDialog.newInstance(user)
        dialog.show(requireFragmentManager(), PrivateChatDialog.FRAGMENT_TAG)
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
                setBattleButtonUIState(String.format("Challenge\n%s!", user), showCancel = true, tintCard = true)
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
        binding.searchContainer.setBackgroundColor(
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
            val formats = if (yes) category.searchableBattleFormats else category.battleFormats
            if (formats.size == 0) continue
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
        service.registerMessageObserver(observer)
        if (!service.isConnected) {
            makeSnackbar("Connecting to Showdown server...", indefinite = true)
            service.connectToServer()
        }
    }

    override fun onServiceWillUnbound(service: ShowdownService) {
        super.onServiceWillUnbound(service)
        service.unregisterMessageObserver(observer)
    }

    private val observer: GlobalMessageObserver = object : GlobalMessageObserver() {

        override fun onConnectedToServer() {
            if (battleFragment.observedRoomId != null) battleFragment.observedRoomId = null
            if (chatFragment.observedRoomId != null) chatFragment.observedRoomId = null
        }

        override fun onUserChanged(userName: String, isGuest: Boolean, avatarId: String) {
            binding.username.apply {
                text = Utils.smallText("Connected as\n")
                append(Utils.boldText(Utils.truncate(userName, 10)))
            }
            binding.loginButton.isEnabled = true
            if (isGuest) {
                makeSnackbar("Connected as guest !")
                binding.loginButton.setImageResource(R.drawable.ic_login)
            } else {
                makeSnackbar("Connected as $userName")
                binding.loginButton.setImageResource(R.drawable.ic_logout)
            }
            val signInDialog = requireFragmentManager().findFragmentByTag(SignInDialog.FRAGMENT_TAG) as SignInDialog?
            signInDialog?.dismissAllowingStateLoss()
        }

        override fun onUpdateCounts(userCount: Int, battleCount: Int) {
            binding.usersCount.apply {
                text = Utils.boldText(userCount.toString())
                append(Utils.smallText("\nusers online"))
            }
            binding.battlesCount.apply {
                text = Utils.boldText(battleCount.toString())
                append(Utils.smallText("\nactive battles"))
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
            binding.joinedBattlesContainer.visibility = if (games.isEmpty()) View.GONE else View.VISIBLE
            binding.searchContainer.visibility = if (games.isNotEmpty()) View.GONE else View.VISIBLE

            binding.joinedBattlesContainer.removeAllViews()
            for ((roomId, value) in games) {
                layoutInflater.inflate(R.layout.button_joined_battle, binding.joinedBattlesContainer)
                val button: Button? = binding.joinedBattlesContainer.getChildAt(binding.joinedBattlesContainer.childCount - 1) as Button
                button?.text = value
                button?.setOnClickListener { _: View? -> tryJoinBattleRoom(roomId) }
            }
        }

        override fun onUserDetails(id: String, name: String, online: Boolean, group: String,
                                   rooms: List<String>, battles: List<String>) {
            val builder = SpannableStringBuilder()
            builder.append(Utils.italicText("Group: ")).append(group.replace(" ", "␣")).append("\n")
            builder.append(Utils.italicText("Battles: "))
            if (battles.isNotEmpty()) {
                val stringBuilder = StringBuilder()
                for (battle in battles) stringBuilder.append(battle).append(", ")
                stringBuilder.deleteCharAt(stringBuilder.length - 2)
                builder.append(Utils.smallText(stringBuilder.toString()))
            } else {
                builder.append(Utils.smallText("None")).append("\n")
            }
            builder.append(Utils.italicText("Chatrooms: "))
            if (rooms.isNotEmpty()) {
                val stringBuilder = StringBuilder()
                for (room in rooms) stringBuilder.append(room).append(", ")
                stringBuilder.deleteCharAt(stringBuilder.length - 2)
                builder.append(Utils.smallText(stringBuilder.toString()))
            } else {
                builder.append(Utils.smallText("None")).append("\n")
            }
            if (!online) builder.append(Utils.coloredText("(Offline)", Color.RED))
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
            val dialog = requireFragmentManager().findFragmentByTag(PrivateChatDialog.FRAGMENT_TAG) as PrivateChatDialog?
            if (dialog != null && dialog.isVisible) dialog.dismiss()
        }

        override fun onAvailableRoomsChanged(officialRooms: List<RoomInfo>, chatRooms: List<RoomInfo>) {
            chatFragment.onAvailableRoomsChanged(officialRooms, chatRooms)
        }

        override fun onAvailableBattleRoomsChanged(availableRoomsInfo: AvailableBattleRoomsInfo) {}

        override fun onNewPrivateMessage(with: String, message: String) {
            binding.pmsOverview.incrementPmCount(with)
            if (!binding.pmsOverview.isEmpty) binding.pmsContainer.visibility = View.VISIBLE
            val dialog = requireFragmentManager().findFragmentByTag(PrivateChatDialog.FRAGMENT_TAG) as PrivateChatDialog?
            if (dialog?.chatWith == with) dialog.onNewMessage(message) else notifyNewMessageReceived()
        }

        override fun onChallengesChange(to: String?, format: String?, from: Map<String, String>) {
            binding.pmsOverview.apply {
                updateChallengeTo(to, format)
                updateChallengesFrom(from.keys.toTypedArray(), from.values.toTypedArray())
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
                    setBattleButtonUIState(String.format("Waiting for\n%s...", to), enabled = false, showCancel = true, tintCard = true)
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
                }
                "chat" -> {
                    if (chatFragment.observedRoomId == null) chatFragment.observedRoomId = roomId
                }
            }
        }

        override fun onRoomDeinit(roomId: String) {
            if (battleFragment.observedRoomId == roomId) {
                battleFragment.observedRoomId = null
                if (pendingBattleToJoin != null) {
                    this@HomeFragment.service?.sendGlobalCommand("join", pendingBattleToJoin!!)
                    pendingBattleToJoin = null
                }
            }
            if (chatFragment.observedRoomId == roomId) chatFragment.observedRoomId = null
        }

        override fun onNetworkError() {
            Snackbar.make(requireView(), "Unable to reach Showdown server", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") {
                        makeSnackbar("Reconnecting to Showdown server...", indefinite = true)
                        service!!.reconnectToServer()
                    }
                    .show()
        }
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

        override fun getItemView(position: Int, view: View?, parent: ViewGroup?): View? {
            val convertView = view ?: layoutInflater.inflate(R.layout.dropdown_item_team, parent, false).apply {
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