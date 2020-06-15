package com.majeur.psclient.service

class ServerMessage(val roomId: String, data: String) {

    val command: String
    val args: List<String>
    val kwargs: Map<String, String>

    private val SEPARATOR = '|'
    private var argsIterator: Iterator<String>

    var nextArg: String = ""
        private set
        get() = argsIterator.next()

    var nextArgSafe: String? = ""
        private set
        get() = if (hasNextArg) argsIterator.next() else null

    var hasNextArg: Boolean = true
        private set
        get() = argsIterator.hasNext()

    var remainingArgsRaw: String = ""
        private set
        get() {
            val result = mutableListOf<String>()
            while (hasNextArg) result.add(nextArg)
            return result.joinToString("|")
        }

    init {
        if (data == SEPARATOR.toString()) { // "|" type
            command = "break"
            args = emptyList()
            kwargs = emptyMap()
        } else if (data[0] != SEPARATOR || data[1] == SEPARATOR) { // "MESSAGE" and "||MESSAGE" type
            command = "raw"
            val (arguments, kwArguments) = parseArguments(data, true)
            args = arguments
            kwargs = kwArguments
        } else {
            val sepIndex = data.indexOf('|', 1)
            if (sepIndex == -1) {
                command = data.substring(1)
                args = emptyList()
                kwargs = emptyMap()
            } else {
                command = data.substring(1, sepIndex)
                val (arguments, kwArguments) = parseArguments(data.substring(sepIndex + 1),
                        arrayOf("formats", "c", "c:", "tier", "error").contains(command))
                args = arguments
                kwargs = kwArguments
            }
        }
        argsIterator = args.iterator()
    }

    private fun parseArguments(rawArgs: String, escapeKwargs: Boolean) : Pair<List<String>, Map<String, String>> {
        val args = rawArgs.split(SEPARATOR).filter { it.isNotBlank() }
                .filter { escapeKwargs || !(it.startsWith('[') && it.contains(']')) }
        val kwargs = rawArgs.split(SEPARATOR).filter { it.isNotBlank() }
                .filter { !escapeKwargs }
                .associateBy ( { it.substringAfter('[').substringBefore(']') } ,
                        { it.substringAfter(']').trim() })
        return Pair(args, kwargs)
    }

    fun newArgsIteration() {
        argsIterator = args.iterator()
    }
}