package com.majeur.psclient.io

import android.content.Context
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.util.toId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class TeamsStore(context: Context) {

    private val jsonFile = File(context.filesDir, FILE_NAME)

    suspend fun get(): List<Team.Group> = withContext(Dispatchers.IO) {
        try {
            getInternal()
        } catch (e: Exception) {
            if (e is JSONException) Timber.e(e)
            emptyList<Team.Group>()
        }
    }

    @Throws(IOException::class, JSONException::class)
    private fun getInternal(): List<Team.Group> {
        return readJsonFromFile().run {
            val groups = mutableListOf<Team.Group>()
            (0 until length()).map { getJSONObject(it) }.forEach { groupJson ->
                val format = groupJson.getString(JSON_KEY_FORMAT)
                val group = Team.Group(format)
                val teamsJson = groupJson.getJSONArray(JSON_KEY_TEAMS)
                (0 until teamsJson.length()).map { teamsJson.getJSONObject(it) }.forEach {
                    val label = it.optString(JSON_KEY_TEAM_LABEL)
                    val data = it.getString(JSON_KEY_TEAM_DATA)
                    Team.unpack(label, format, data)?.let { team -> group.teams.add(team) }
                }
                groups.add(group)
            }
            groups
        }
    }

    suspend fun store(groups: List<Team.Group>): Boolean = withContext(Dispatchers.IO) {
        try {
            makeJson(groups).run { writeJsonToFile(this) }
            true
        } catch (e: Exception) {
            if (e is JSONException || e is IOException) Timber.e(e)
            false
        }
    }

    @Throws(JSONException::class)
    private fun makeJson(groups: List<Team.Group>): JSONArray {
        val jsonArray = JSONArray()
        val formats = groups.map { it.format.toId() }.toSet()
        formats.forEach { formatId ->
            val teams = groups.filter { it.format.toId() == formatId }.flatMap { it.teams }
            if (teams.isEmpty()) return@forEach
            JSONObject().apply {
                put(JSON_KEY_FORMAT, formatId)
                put(JSON_KEY_TEAMS, JSONArray().apply {
                    teams.forEach { team ->
                        put(JSONObject().apply {
                            put(JSON_KEY_TEAM_LABEL, team.label)
                            put(JSON_KEY_TEAM_DATA, team.pack())
                        })
                    }
                })
            }.also { jsonArray.put(it) }
        }
        return jsonArray
    }

    @Throws(JSONException::class, FileNotFoundException::class)
    private fun readJsonFromFile() = JSONArray(jsonFile.readText())

    private fun writeJsonToFile(json: JSONArray) = jsonFile.writeText(json.toString())

    companion object {
        private const val FILE_NAME = "user_teams.json"
        private const val JSON_KEY_FORMAT = "label"
        private const val JSON_KEY_TEAMS = "teams"
        private const val JSON_KEY_TEAM_LABEL = "label"
        private const val JSON_KEY_TEAM_DATA = "data"
    }

}
