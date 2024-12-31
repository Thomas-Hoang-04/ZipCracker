package com.thomas.zipcracker.processor

import androidx.datastore.core.Serializer
import com.thomas.zipcracker.component.CrackingOptions
import com.thomas.zipcracker.ui.Theme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class UserSettings(
    val uiMode: Theme? = null,
    val lastPwdInfo: LastPwdMetadata? = null,
    val lastOptions: CrackingOptions? = null,
)

class UserSettingsSerializer: Serializer<UserSettings> {
    override val defaultValue: UserSettings = UserSettings()

    override suspend fun readFrom(input: InputStream): UserSettings {
        return try {
            val data = input.readBytes().decodeToString()
            Json.decodeFromString(
                deserializer = UserSettings.serializer(),
                string = data
            )
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        Json.encodeToString(UserSettings.serializer(), t)
            .also { output.write(it.toByteArray()) }
    }
}