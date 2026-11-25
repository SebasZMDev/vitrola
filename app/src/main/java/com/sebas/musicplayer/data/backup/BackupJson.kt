package com.sebas.musicplayer.data.backup

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Convierte el respaldo a JSON legible y viceversa. */
object BackupJson {

    fun encode(data: BackupData): String = JSONObject().apply {
        put("app", "Vitrola")
        put("version", BackupData.VERSION)
        put("exportedAt", data.exportedAt)
        put("playlists", JSONArray().apply {
            data.playlists.forEach { p ->
                put(JSONObject().put("name", p.name).put("songs", JSONArray().apply { p.songs.forEach { put(key(it)) } }))
            }
        })
        put("favorites", JSONArray().apply {
            data.favorites.forEach { put(key(it.key).put("addedAt", it.addedAt)) }
        })
        put("stats", JSONArray().apply {
            data.stats.forEach { put(key(it.key).put("playCount", it.playCount).put("lastPlayedAt", it.lastPlayedAt)) }
        })
        put("overrides", JSONArray().apply {
            data.overrides.forEach { o ->
                put(key(o.key).apply {
                    o.title?.let { put("newTitle", it) }
                    o.artist?.let { put("newArtist", it) }
                    o.album?.let { put("newAlbum", it) }
                    o.genre?.let { put("newGenre", it) }
                })
            }
        })
        put("gains", JSONArray().apply {
            data.gains.forEach { put(key(it.key).put("gainDb", it.gainDb.toDouble())) }
        })
        // Toda la biblioteca: title/artist/durationSec = originales del archivo (la llave); current* = como se ve hoy.
        put("library", JSONArray().apply {
            data.library.forEach { s ->
                put(key(s.key).put("currentTitle", s.title).put("currentArtist", s.artist).put("currentAlbum", s.album).apply {
                    s.genre?.let { put("currentGenre", it) }
                })
            }
        })
    }.toString(2)

    /** @throws IllegalArgumentException si el texto no es un respaldo de Vitrola válido. */
    fun decode(text: String): BackupData {
        try {
            val root = JSONObject(text)
            require(root.optString("app") == "Vitrola") { "No es un respaldo de Vitrola" }
            val version = root.optInt("version", 0)
            require(version in 1..BackupData.VERSION) { "Versión de respaldo no soportada ($version)" }
            return BackupData(
                exportedAt = root.optLong("exportedAt"),
                playlists = root.optJSONArray("playlists").objects().map { p ->
                    BackupPlaylist(p.getString("name"), p.optJSONArray("songs").objects().map(::readKey))
                },
                favorites = root.optJSONArray("favorites").objects().map { BackupFavorite(readKey(it), it.optLong("addedAt")) },
                stats = root.optJSONArray("stats").objects().map {
                    BackupStat(readKey(it), it.optInt("playCount", 1), it.optLong("lastPlayedAt"))
                },
                overrides = root.optJSONArray("overrides").objects().map {
                    BackupOverride(
                        readKey(it), it.optStringOrNull("newTitle"), it.optStringOrNull("newArtist"),
it.optStringOrNull("newAlbum"),
                        it.optStringOrNull("newGenre"),
                    )
                },
                gains = root.optJSONArray("gains").objects().map { BackupGain(readKey(it), it.optDouble("gainDb", 0.0).toFloat()) },
            )
        } catch (e: JSONException) {
            throw IllegalArgumentException("El archivo no es un respaldo válido", e)
        }
    }

    private fun key(k: SongKey) = JSONObject().put("title", k.title).put("artist", k.artist).put("durationSec", k.durationSec)

    private fun readKey(o: JSONObject) = SongKey(o.getString("title"), o.optString("artist"), o.optInt("durationSec"))

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optStringOrNull(name: String): String? = if (has(name) && !isNull(name)) getString(name) else null
}
