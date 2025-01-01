package com.thomas.zipcracker.utility

import androidx.datastore.core.Serializer
import com.thomas.zipcracker.crypto.CrackingOptions
import com.thomas.zipcracker.metadata.LastPwdMetadata
import com.thomas.zipcracker.ui.Theme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class UserPreferences(
    val uiMode: Theme? = null,
    val lastPwdInfo: LastPwdMetadata? = null,
    val lastOptions: CrackingOptions? = null,
)

class PreferencesSerializer: Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        return try {
            val data = input.readBytes().decodeToString()
            Json.decodeFromString(
                deserializer = UserPreferences.serializer(),
                string = data
            )
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        Json.encodeToString(UserPreferences.serializer(), t)
            .also { output.write(it.toByteArray()) }
    }
}