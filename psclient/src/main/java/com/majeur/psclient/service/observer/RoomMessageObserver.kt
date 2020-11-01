package com.majeur.psclient.service.observer

import android.graphics.Color
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import com.majeur.psclient.service.ServerMessage
import com.majeur.psclient.service.ShowdownService
import com.majeur.psclient.util.*
import com.majeur.psclient.util.html.UsernameSpan

abstract class RoomMessageObserver<C : RoomMessageObserver.UiCallbacks>(service: ShowdownService)
    : AbsMessageObserver<C>(service) {

    var roomJoined = false
        private set

    private var currentUsers = mutableListOf<String>()
    private val _usernameColorCache = mutableMapOf<String, Int>()

    override var observedRoomId: String? = null
        set(value) {
            if (value == null && field != null) {
                roomJoined = false
                currentUsers.clear()
                _usernameColorCache.clear()
                onRoomDeInit()
            }
            field = value
        }

    val users: List<String>
        get() = currentUsers.toList()

    override fun onUiCallbacksAttached() {

    }

    override fun onMessage(message: ServerMessage) {
        message.newArgsIteration()
        when (message.command) {
            "init" -> {
                _usernameColorCache.clear()
                roomJoined = true
                onRoomInit()
            }
            "title" -> onRoomTitleChanged(message.nextArg)
            "users" -> initializeUserList(message)
            "J", "j", "join" -> {
                val username = message.nextArg
                currentUsers.add(username)
                onUpdateUsers(currentUsers.toList())
                if (message.command != "J") printUserRelatedMessage("$username joined")
            }
            "L", "l", "leave" -> {
                val username = message.nextArg
                currentUsers.remove(username)
                onUpdateUsers(currentUsers.toList())
                if (message.command != "L") printUserRelatedMessage("$username left")
            }
            "html" -> { // printMessage("~html messages aren't supported yet~");
            }
            "uhtml" -> { // printMessage("~html messages aren't supported yet~");
            }
            "uhtmlchange" -> { // TODO
            }
            "N", "n", "name" -> if (message.command != "N") handleNameChange(message)
            "c", "chat" -> handleChatMessage(message)
            "c:" -> {
                message.nextArg // Skipping time stamp
                handleChatMessage(message)
            }
            ":" -> { // Time stamp, we aren't using it yet
            }
            "B", "b", "battle" -> {
                val roomId = message.nextArg
                val user1 = message.nextArg
                val user2 = message.nextArg
                if (message.command != "B") printMessage("A battle started between $user1 and $user2 (in room $roomId)")
            }
            "error" -> printErrorMessage(message.nextArg)
            "raw" -> {
                val html = message.remainingArgsRaw
                printHtml(Utils.prepareForHtml(html))
            }
            "deinit" -> {
                roomJoined = false
                currentUsers.clear()
                _usernameColorCache.clear()
                onRoomDeInit()
            }
            "noinit" -> { // TODO
            }
        }
    }

    private fun initializeUserList(args: ServerMessage) {
        val rawUsers = args.nextArg // first element is total user count, skipping it
        currentUsers.clear()
        // first element is total user count, skipping it
        // we substring names from 1 to avoid prefixes
        currentUsers.addAll(rawUsers.split(',').drop(1).map { it.substring(1) })
        onUpdateUsers(currentUsers.toList())
    }

    private fun handleNameChange(args: ServerMessage) {
        val user = args.nextArg
        val oldName = args.nextArg
        printUserRelatedMessage("User $oldName changed its name and is now $user")
    }

    private fun handleChatMessage(args: ServerMessage) {
        val userWithPrefix = args.nextArg
        val user = userWithPrefix.removePrefix(" ")
        var userMessage = args.remainingArgsRaw
        when {
            userMessage.startsWith("/raw") -> { // Todo: support more commands and enhance code.
                val html = userMessage.substringAfter("/raw ")
                printHtml(Utils.prepareForHtml(html))
            }
            userMessage.startsWith("/uhtml") -> {
                val html = userMessage.substringAfter(",") // msg: /uhtml id,
                printHtml(Utils.prepareForHtml(html))
            }
            else -> {
                val announce = userMessage.startsWith("/announce")
                if (announce) userMessage = userMessage.removePrefix("/announce ")

                val spannable = SpannableStringBuilder("$user: ${Utils.specChars(userMessage)}")
                spannable.setSpan(TextTagSpan(getHashColor(userWithPrefix)), 0, user.length + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                spannable.setSpan(UsernameSpan(user), 0, user.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                Utils.applyStylingTags(spannable)
                if (announce) {
                    spannable.setSpan(BackgroundColorSpan(Color.parseColor("#678CB1")),
                            user.length + 2, spannable.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(ForegroundColorSpan(Color.WHITE),
                            user.length + 2, spannable.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }
                printMessage(spannable)
            }
        }
    }

    private fun getHashColor(username: String) = _usernameColorCache.getOrPut(username) { Utils.hashColor(username.toId()) }

    private fun printUserRelatedMessage(message: String) {
        printMessage(message.italic().color(-0xbdbdbe).small())
    }

    protected fun printErrorMessage(message: String) {
        printMessage(message.bold().color(-0x750000))
    }

    protected open fun printMessage(text: CharSequence) = onPrintText(text)

    protected open fun printHtml(html: String) = onPrintHtml(html)

    protected open fun onRoomInit() = uiCallbacks?.onRoomInit()
    protected open fun onRoomTitleChanged(title: String) = uiCallbacks?.onRoomTitleChanged(title)
    protected open fun onUpdateUsers(users: List<String>) = uiCallbacks?.onUpdateUsers(users)
    protected open fun onPrintText(text: CharSequence) = uiCallbacks?.onPrintText(text)
    protected open fun onPrintHtml(html: String) = uiCallbacks?.onPrintHtml(html)
    protected open fun onRoomDeInit() = uiCallbacks?.onRoomDeInit()

    interface UiCallbacks : AbsMessageObserver.UiCallbacks {
        fun onRoomInit()
        fun onRoomTitleChanged(title: String)
        fun onUpdateUsers(users: List<String>)
        fun onPrintText(text: CharSequence)
        fun onPrintHtml(html: String)
        fun onRoomDeInit()
    }
}