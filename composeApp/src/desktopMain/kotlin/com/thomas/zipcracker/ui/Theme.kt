package com.thomas.zipcracker.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.github.tkuenneth.nativeparameterstoreaccess.Dconf
import com.github.tkuenneth.nativeparameterstoreaccess.Dconf.HAS_DCONF
import com.github.tkuenneth.nativeparameterstoreaccess.MacOSDefaults
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess.IS_MACOS
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess.IS_WINDOWS
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM,
}

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimaryColor,
    secondary = IndicatorColor,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DialogColor,
    onSecondary = Color.LightGray,
    onSecondaryContainer = Color.Gray,
    error = ErrorColor,
)

private val LightColorScheme: ColorScheme = darkColorScheme(
    primary = LightPrimaryColor,
    secondary = IndicatorColor,
    background = Color.White,
    onBackground = Color.DarkGray,
    surface = Color.White,
    onSecondaryContainer = Color.Gray,
    onSecondary = Color.DarkGray,
    error = Color.Red
)

fun isDarkThemeActive(): Boolean = when {
    IS_WINDOWS -> {
        WindowsRegistry.getWindowsRegistryEntry(
            "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "AppsUseLightTheme"
        ) == 0x0
    }
    IS_MACOS -> {
        MacOSDefaults.getDefaultsEntry("AppleInterfaceStyle") == "Dark"
    }
    HAS_DCONF -> {
        val result = Dconf.getDconfEntry("/org/gnome/desktop/interface/gtk-theme")
        result.lowercase().contains("dark")
    }
    else -> false
}

@Composable
fun ZipCrackerTheme(
    darkTheme: Boolean = isDarkThemeActive(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}