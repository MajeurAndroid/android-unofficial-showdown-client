package com.majeur.psclient.io

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.util.JsonReader
import android.util.JsonToken
import com.majeur.psclient.R
import com.majeur.psclient.model.battle.Move
import com.majeur.psclient.model.common.Item
import com.majeur.psclient.model.common.Species
import com.majeur.psclient.model.common.Stats
import com.majeur.psclient.model.pokemon.DexPokemon
import com.majeur.psclient.util.toId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStreamReader

class AssetLoader(val context: Context) {

    private val io = Dispatchers.IO

    private val allItemsLoader by lazy {
        AllItemsLoader(context)
    }

    private val allSpeciesLoader by lazy {
        AllSpeciesLoader(context)
    }

    private val dexIconLoader by lazy {
        DexIconLoader(context)
    }

    private val dexPokemonLoader by lazy {
        DexPokemonLoader(context)
    }

    private val itemLoader by lazy {
        ItemLoader(context)
    }

    private val learnsetLoader by lazy {
        LearnsetLoader(context)
    }

    private val moveDetailsLoader by lazy {
        MoveDetailsLoader(context)
    }

    suspend fun allItems(constraint: String) = withContext(io) { allItemsLoader.load(constraint) }

    suspend fun allSpecies(constraint: String) = withContext(io) { allSpeciesLoader.load(constraint) }

    suspend fun dexIcon(species: String) = withContext(io) {
        species.run {
            dexIconLoader.load(if (startsWith("arceus", ignoreCase = true)) "arceus" else this.toId())
        }
    }
    suspend fun dexIcons(vararg species: String) = withContext(io) {
        species.map { if (it.startsWith("arceus", ignoreCase = true)) "arceus" else it.toId() }.run {
            dexIconLoader.load(*toTypedArray())
        }
    }

    suspend fun dexPokemon(species: String) = withContext(io) { dexPokemonLoader.load(species) }

    suspend fun item(itemId: String) = withContext(io) { itemLoader.load(itemId) }

    suspend fun learnset(species: String) = withContext(io) { learnsetLoader.load(species) }

    suspend fun moveDetails(moveName: String) = withContext(io) {
        moveName.run {
            moveDetailsLoader.load(if (startsWith("z-", ignoreCase = true)) substring(2).toId() else toId())
        }
    }
    suspend fun movesDetails(vararg moveIds: String) = withContext(io) {
        moveIds.map { if (it.startsWith("-z", ignoreCase = true)) it.substring(2).toId() else it.toId() }.run {
            moveDetailsLoader.load(*toTypedArray())
        }
    }

    fun dexIconNonSuspend(species: String): Bitmap? {
        return dexIconLoader.load(species) // TODO
    }

    fun dexPokemonNonSuspend(species: String) = dexPokemonLoader.load(species)

    abstract class Loader<T>(
            protected val context: Context,
            private val useCache: Boolean = true) {

        private var cache = mutableMapOf<String, T?>()

        fun load(vararg assetIds: String) = assetIds.map { load(it) }

        fun load(assetId: String): T? {
            return if (useCache) cache.getOrPut(assetId) { compute(assetId) }
            else compute(assetId)
        }

        protected abstract fun compute(assetId: String): T?

        protected val resources: Resources = context.resources

        protected fun jsonReader(resId: Int): JsonReader {
                val inputStream = context.resources.openRawResource(resId)
                return JsonReader(InputStreamReader(inputStream))
            }
    }

    class AllItemsLoader(context: Context) : Loader<List<Item>>(context, useCache = false) {

        @Throws(IOException::class)
        override fun compute(constraint: String): List<Item>? {
            val items = mutableListOf<Item>()
            jsonReader(R.raw.items).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    val itemId = reader.nextName()
                    if (itemId.contains(constraint)) {
                        items.add(parseItem(reader, itemId))
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
            return items
        }

        @Throws(IOException::class)
        private fun parseItem(reader: JsonReader, itemId: String): Item {
            return Item().apply {
                id = itemId
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "name" -> name = reader.nextString()
                        "desc" -> reader.skipValue() // Ignored for now
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
        }
    }

    class AllSpeciesLoader(context: Context) : Loader<List<Species>>(context, useCache = false) {

        @Throws(IOException::class)
        override fun compute(constraint: String): List<Species>? {
            val species = mutableListOf<Species>()
            jsonReader(R.raw.dex).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    val speciesId = reader.nextName()
                    if (speciesId.contains(constraint)) {
                        species.add(parseSpecies(reader, speciesId))
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
            return species
        }

        @Throws(IOException::class)
        private fun parseSpecies(reader: JsonReader, speciesId: String): Species {
            return Species().apply {
                id = speciesId
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "name" -> name = reader.nextString()
                        "forme" -> reader.skipValue() // Maybe prevent item related formes from being available in teambuilder ?
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
        }
    }

    class DexIconLoader(context: Context) : Loader<Bitmap>(context) {

        companion object {
            private const val SHEET_WIDTH = 480
            private const val SHEET_HEIGHT = 3030
            private const val ELEMENT_WIDTH = 40
            private const val ELEMENT_HEIGHT = 30
        }

        private val tempRect = Rect()

        private val bitmapDecoder: BitmapRegionDecoder
            get() {
                val inputStream = resources.openRawResource(R.raw.dex_icon_sheet)
                return BitmapRegionDecoder.newInstance(inputStream, true)
            }

        @Throws(IOException::class)
        override fun compute(species: String): Bitmap? {
            val index = jsonReader(R.raw.dex_icon_indexes).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    if (species.contains(reader.nextName())) {
                        return@use reader.nextInt()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
                return@use 0
            }
            val xDim = SHEET_WIDTH / ELEMENT_WIDTH
            val x = index % xDim
            val y = index / xDim
            tempRect.apply {
                left = x * ELEMENT_WIDTH
                top = y * ELEMENT_HEIGHT
                right = (x + 1) * ELEMENT_WIDTH
                bottom = (y + 1) * ELEMENT_HEIGHT
            }
            return bitmapDecoder.run {
                val bitmap = decodeRegion(tempRect, null)
                recycle()
                bitmap
            }
        }
    }

    class DexPokemonLoader(context: Context) : Loader<DexPokemon>(context) {

        override fun compute(species: String): DexPokemon? {
            return jsonReader(R.raw.dex).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    if (species == reader.nextName()) {
                        return@use parseDexPokemon(reader)
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
                return@use null
            }
        }

        @Throws(IOException::class)
        private fun parseDexPokemon(reader: JsonReader): DexPokemon {
            return DexPokemon().apply {
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "num" -> num = reader.nextInt()
                        "name" -> species = reader.nextString()
                        "types" -> parseTypes(reader).let { firstType = it.first; secondType = it.second }
                        "genderRatio" -> reader.skipValue() // TODO
                        "baseStats" -> baseStats = parseStats(reader)
                        "abilities" -> parseAbilities(reader).let { hiddenAbility = it.first; abilities = it.second }
                        "heightm" -> height = reader.nextDouble().toFloat()
                        "weightkg" -> weight = reader.nextDouble().toFloat()
                        "color" -> color = reader.nextString()
                        "gender" -> gender = reader.nextString()
                        "evos" -> reader.skipValue() // TODO
                        "LC" -> tier = reader.nextString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
        }

        @Throws(IOException::class)
        private fun parseTypes(reader: JsonReader): Pair<String, String?> {
            reader.beginArray()
            val types = reader.run {
                nextString() to if (hasNext()) nextString() else null
            }
            reader.endArray()
            return types
        }

        @Throws(IOException::class)
        private fun parseStats(reader: JsonReader): Stats {
            return Stats().apply {
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "hp" -> hp = reader.nextInt()
                        "atk" -> atk = reader.nextInt()
                        "def" -> def = reader.nextInt()
                        "spa" -> spa = reader.nextInt()
                        "spd" -> spd = reader.nextInt()
                        "spe" -> spe = reader.nextInt()
                    }
                }
                reader.endObject()
            }
        }

        @Throws(IOException::class)
        private fun parseAbilities(reader: JsonReader): Pair<String?, List<String>> {
            var hiddenAbility: String? = null
            val abilities = mutableListOf<String>()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "H" -> hiddenAbility = reader.nextString()
                    else -> abilities.add(reader.nextString())
                }
            }
            reader.endObject()
            return hiddenAbility to abilities
        }
    }

    class ItemLoader(context: Context) : Loader<Item>(context) {

        override fun compute(itemId: String): Item? {
            return jsonReader(R.raw.items).use {reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    if (itemId == reader.nextName()) {
                        return@use parseItem(reader)
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
                return@use null
            }
        }

        @Throws(IOException::class)
        private fun parseItem(reader: JsonReader): Item {
            return Item().apply {
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "name" -> name = reader.nextString()
                        "id" -> id = reader.nextString()
                        "desc" -> description = reader.nextString()
                        "spritenum" -> spriteId = reader.nextInt()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
        }
    }

    class LearnsetLoader(context: Context) : Loader<List<String>>(context) {

        override fun compute(species: String): List<String>? {
            val family = jsonReader(R.raw.dex).use { reader ->
                return@use parseFamily(reader, species)
            }
            val learnset = mutableListOf<String>()
            jsonReader(R.raw.learnsets).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    if (family.contains(reader.nextName())) {
                        learnset.addAll(parseLearnset(reader))
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
            return learnset.sorted()
        }

        private fun parseFamily(reader: JsonReader, species: String): List<String> {
            val family = mutableListOf<String>()
            reader.beginObject()
            while (reader.hasNext()) {
                val speciesId = reader.nextName()
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "evos" -> if (parseEvos(reader).contains(species)) family.add(speciesId)
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
            reader.endObject()
            family.add(species)
            return family
        }

        @Throws(IOException::class)
        private fun parseEvos(reader: JsonReader): List<String> {
            return mutableListOf<String>().apply {
                reader.beginArray()
                while (reader.hasNext()) add(reader.nextString())
                reader.endArray()
            }
        }

        @Throws(IOException::class)
        private fun parseLearnset(reader: JsonReader): List<String> {
            return mutableListOf<String>().apply {
                reader.beginArray()
                while (reader.hasNext()) add(reader.nextString())
                reader.endArray()
            }
        }

    }

    class MoveDetailsLoader(context: Context) : Loader<Move.Details>(context) {

        override fun compute(moveId: String): Move.Details? {
            return jsonReader(R.raw.moves).use {reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    if (moveId == reader.nextName()) {
                        return@use parseMoveDetails(reader)
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
                return@use null
            }
        }

        @Throws(IOException::class)
        private fun parseMoveDetails(reader: JsonReader): Move.Details? {
            return Move.Details().apply {
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "accuracy" -> accuracy = if (reader.peek() == JsonToken.BOOLEAN) if (reader.nextBoolean()) -1 else 0 else reader.nextInt()
                        "basePower" -> basePower = reader.nextInt()
                        "category" -> category = reader.nextString()
                        "desc" -> desc = reader.nextString()
                        "shortDesc" -> reader.nextString().let { desc = desc ?: it }
                        "type" -> type = reader.nextString()
                        "priority" -> priority = reader.nextInt()
                        "name" -> this.name = reader.nextString()
                        "pp" -> pp = reader.nextInt()
                        "zMovePower" -> zPower = reader.nextInt()
                        "target" -> target = Move.Target.parse(reader.nextString())
                        "zMoveEffect" -> zEffect = reader.nextString()
                        "gmaxPower" -> maxPower = reader.nextInt()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
        }
    }
}