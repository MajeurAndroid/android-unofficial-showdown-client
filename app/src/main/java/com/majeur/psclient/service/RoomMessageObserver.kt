package com.majeur.psclient.service

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.majeur.psclient.model.Id
import com.majeur.psclient.util.SpannableStringBuilder
import com.majeur.psclient.util.TextTagSpan
import com.majeur.psclient.util.Utils
import com.majeur.psclient.util.html.UsernameSpan

abstract class RoomMessageObserver : AbsMessageObserver() {

    private val TAG = RoomMessageObserver::class.simpleName

    var roomJoined = false
        private set

    private var _currentUsers = mutableListOf<String>()
    private val _usernameColorCache = mutableMapOf<String, Int>()

    override var observedRoomId: String? = null
        set(value) {
            if (value == null && field != null) {
                roomJoined = false
                _currentUsers.clear()
                _usernameColorCache.clear()
                onRoomDeInit()
            }
            field = value
        }

    val users: List<String>
        get() = _currentUsers.toList()

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
                _currentUsers.add(username)
                onUpdateUsers(_currentUsers.toList())
                if (message.command != "J") printUserRelatedMessage("$username joined")
            }
            "L", "l", "leave" -> {
                val username = message.nextArg
                _currentUsers.remove(username)
                onUpdateUsers(_currentUsers.toList())
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
                _currentUsers.clear()
                _usernameColorCache.clear()
                onRoomDeInit()
            }
            "noinit" -> { // TODO
            }
        }
    }

    private fun initializeUserList(args: ServerMessage) {
        val rawUsers = args.nextArg // first element is total user count, skipping it
        _currentUsers.also { li ->
            li.clear()
            // first element is total user count, skipping it
            // we substring names from 1 to avoid prefixes
            li.addAll(rawUsers.split(',').drop(1).map { it.substring(1) })
        }
        onUpdateUsers(_currentUsers.toList())
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

    private fun getHashColor(username: String) = _usernameColorCache.getOrPut(username) { Utils.hashColor(Id.toId(username)) }

    private fun printUserRelatedMessage(message: String) = SpannableString(message).let {
        it.setSpan(StyleSpan(Typeface.ITALIC), 0, message.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        it.setSpan(ForegroundColorSpan(-0xbdbdbe), 0, message.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        it.setSpan(RelativeSizeSpan(0.8f), 0, message.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        printMessage(it)
    }

    protected fun printErrorMessage(message: String) = SpannableString(message).let {
        it.setSpan(StyleSpan(Typeface.BOLD), 0, message.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        it.setSpan(ForegroundColorSpan(Color.RED), 0, message.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        printMessage(it)
    }

    protected open fun printMessage(text: CharSequence) = onPrintText(text)

    protected open fun printHtml(html: String) = onPrintHtml(html)

    protected abstract fun onRoomInit()
    protected abstract fun onRoomTitleChanged(title: String)
    protected abstract fun onUpdateUsers(users: List<String>)
    protected abstract fun onPrintText(text: CharSequence)
    protected abstract fun onPrintHtml(html: String)
    protected abstract fun onRoomDeInit()
}