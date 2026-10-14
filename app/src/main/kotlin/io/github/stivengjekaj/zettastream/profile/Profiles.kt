package io.github.stivengjekaj.zettastream.profile

import io.github.stivengjekaj.zettastream.storage.SafeFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** One person who uses the app. [avatar] is the link of a picture, or null for the first letter of the name. */
@Serializable
data class Profile(val id: String, val name: String, val avatar: String? = null)

@Serializable
data class ProfilesData(val profiles: List<Profile> = emptyList(), val last: String? = null)

/**
 * Keeps the profiles in `profiles.json`. Each profile has its own folder for its library
 * and its settings. The sources are the same for all profiles.
 */
class ProfileStore(private val root: File) {
    private val file = File(root, "profiles.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val writer = SafeFile(file)
    private val state = MutableStateFlow(read())
    val profiles: StateFlow<ProfilesData> = state.asStateFlow()

    fun folder(id: String): File = File(root, "profiles/$id")

    suspend fun add(name: String, avatar: String?): Profile {
        val profile = Profile(UUID.randomUUID().toString().take(8), clean(name), avatar)
        update { it.copy(profiles = it.profiles + profile) }
        return profile
    }

    suspend fun edit(id: String, name: String, avatar: String?) =
        update { d -> d.copy(profiles = d.profiles.map { if (it.id == id) it.copy(name = clean(name), avatar = avatar) else it }) }

    /** Removes a profile and its folder. The last profile stays, so the app always has one. */
    suspend fun delete(id: String): Boolean {
        if (state.value.profiles.size <= 1 || state.value.profiles.none { it.id == id }) return false
        update { d -> d.copy(profiles = d.profiles.filterNot { it.id == id }, last = d.last.takeIf { it != id }) }
        folder(id).deleteRecursively()
        return true
    }

    suspend fun select(id: String) = update { it.copy(last = id) }

    private fun clean(name: String) = name.trim().take(MAX_NAME).ifEmpty { "Profile" }

    private suspend fun update(change: (ProfilesData) -> ProfilesData) {
        val next = change(state.value)
        state.value = next
        writer.write(json.encodeToString(ProfilesData.serializer(), next))
    }

    private fun read(): ProfilesData = runCatching { json.decodeFromString(ProfilesData.serializer(), file.readText()) }.getOrNull()
        ?: firstStart()

    /**
     * On the first start with profiles, the library and the settings of the older version
     * move into the folder of a first profile, so nothing is lost.
     */
    private fun firstStart(): ProfilesData {
        val first = Profile(UUID.randomUUID().toString().take(8), FIRST_NAME)
        val dir = folder(first.id).apply { mkdirs() }
        for (name in listOf("library.json", "settings.json")) {
            val old = File(root, name)
            if (old.exists()) old.renameTo(File(dir, name))
        }
        val data = ProfilesData(listOf(first), first.id)
        file.writeText(json.encodeToString(ProfilesData.serializer(), data))
        return data
    }

    companion object {
        const val FIRST_NAME = "Main"
        const val MAX_NAME = 20
        const val MAX_PROFILES = 5
    }
}
