package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Let's define the Color tokens for our 10 beautiful themes
private val DarkThemePurple = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFDDD6FE),
    secondary = Color(0xFFF472B6),
    onSecondary = Color(0xFF500724),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF064E3B),
    background = Color(0xFF0A0813),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF15122E),
    onSurface = Color(0xFFE5E7EB)
)

private val LightThemePurple = lightColorScheme(
    primary = Color(0xFF6D28D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF5B21B6),
    secondary = Color(0xFFDB2777),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF059669),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFF5F6F8),
    onSurface = Color(0xFF1F2937)
)

// Theme 1: Emerald Wealth
private val LightThemeEmerald = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFF5F6F8),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B)
)
private val DarkThemeEmerald = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF060D0A),
    surface = Color(0xFF10251E),
    onBackground = Color(0xFFF0FDF4),
    onSurface = Color(0xFFE6F4EA)
)

// Theme 2: Midas Gold (Amber)
private val LightThemeGold = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF92400E),
    secondary = Color(0xFF059669),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFF5F6F8),
    onBackground = Color(0xFF291A07),
    onSurface = Color(0xFF3B2314)
)
private val DarkThemeGold = darkColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = Color(0xFF10B981),
    background = Color(0xFF0D0904),
    surface = Color(0xFF22170B),
    onBackground = Color(0xFFFFFBEB),
    onSurface = Color(0xFFFDE68A)
)

// Theme 3: Midnight Oceanic (Teal/Blue)
private val LightThemeOcean = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0D9488),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFF5F6F8),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B)
)
private val DarkThemeOcean = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0369A1),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF2DD4BF),
    background = Color(0xFF040910),
    surface = Color(0xFF0C1929),
    onBackground = Color(0xFFF0F9FF),
    onSurface = Color(0xFFE0F2FE)
)

// Theme 4: Sunset Amber (Coral/Orange)
private val LightThemeSunset = lightColorScheme(
    primary = Color(0xFFEA580C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF9A3412),
    secondary = Color(0xFF7C3AED),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFF5F6F8),
    onBackground = Color(0xFF1C0A00),
    onSurface = Color(0xFF2D1500)
)
private val DarkThemeSunset = darkColorScheme(
    primary = Color(0xFFF97316),
    onPrimary = Color(0xFF431407),
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = Color(0xFFA78BFA),
    background = Color(0xFF0D0502),
    surface = Color(0xFF1E0F0A),
    onBackground = Color(0xFFFFF8F6),
    onSurface = Color(0xFFFFEDD5)
)

// Theme 5: Rose Garden (Pink)
private val LightThemeRose = lightColorScheme(
    primary = Color(0xFFD01B6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF9D174D),
    secondary = Color(0xFF0891B2),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFF5F6F8),
    onBackground = Color(0xFF22030E),
    onSurface = Color(0xFF35041B)
)
private val DarkThemeRose = darkColorScheme(
    primary = Color(0xFFF472B6),
    onPrimary = Color(0xFF500724),
    primaryContainer = Color(0xFF9D174D),
    onPrimaryContainer = Color(0xFFFCE7F3),
    secondary = Color(0xFF22D3EE),
    background = Color(0xFF0D0307),
    surface = Color(0xFF1F0B13),
    onBackground = Color(0xFFFFF5F8),
    onSurface = Color(0xFFFCE7F3)
)

// Theme 6: Cyberpunk Neon
private val DarkThemeCyber = darkColorScheme(
    primary = Color(0xFF06B6D4), // Neon Cyan
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF082F49),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFFEC4899), // Neon Pink
    background = Color(0xFF030108),
    surface = Color(0xFF0B001F),
    onBackground = Color(0xFFA5F3FC),
    onSurface = Color(0xFFFCE7F3)
)
private val LightThemeCyber = lightColorScheme(
    primary = Color(0xFF0EA5E9), // Clean Light Cyan
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFFD946EF), // Fuchsia / Neon Pink
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFF5F6F8),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B)
)

// Theme 7: Forest Sage (Olive/Sage)
private val LightThemeSage = lightColorScheme(
    primary = Color(0xFF4F5E43),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECEFE8),
    onPrimaryContainer = Color(0xFF2E3D2A),
    secondary = Color(0xFF8B5E3C),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFF5F6F8),
    onBackground = Color(0xFF1A1F16),
    onSurface = Color(0xFF232B1F)
)
private val DarkThemeSage = darkColorScheme(
    primary = Color(0xFFA8BFA0),
    onPrimary = Color(0xFF2E3D2A),
    primaryContainer = Color(0xFF2E3D2A),
    onPrimaryContainer = Color(0xFFECEFE8),
    secondary = Color(0xFFD4A373),
    background = Color(0xFF0A0C09),
    surface = Color(0xFF171E14),
    onBackground = Color(0xFFEFF3EE),
    onSurface = Color(0xFFECEFE8)
)

// Theme 8: Crimson Ruby
private val LightThemeRuby = lightColorScheme(
    primary = Color(0xFFDC2626),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEE2E2),
    onPrimaryContainer = Color(0xFF991B1B),
    secondary = Color(0xFF1E3A8A),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFF5F6F8),
    onBackground = Color(0xFF1C0202),
    onSurface = Color(0xFF2F0808)
)
private val DarkThemeRuby = darkColorScheme(
    primary = Color(0xFFEF4444),
    onPrimary = Color(0xFF450A0A),
    primaryContainer = Color(0xFF991B1B),
    onPrimaryContainer = Color(0xFFFEE2E2),
    secondary = Color(0xFF3B82F6),
    background = Color(0xFF0F0303),
    surface = Color(0xFF230808),
    onBackground = Color(0xFFFFF6F6),
    onSurface = Color(0xFFFEE2E2)
)

// Theme 9: Lavender Chill
private val LightThemeLavender = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF5F3FF),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFF06B6D4),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFF5F6F8),
    onBackground = Color(0xFF130A24),
    onSurface = Color(0xFF1F103A)
)
private val DarkThemeLavender = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF22D3EE),
    background = Color(0xFF05020D),
    surface = Color(0xFF160E2A),
    onBackground = Color(0xFFFAF9FF),
    onSurface = Color(0xFFEDE9FE)
)

@Composable
fun MyApplicationTheme(
    themeIndex: Int = 0,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Select the correct light & dark scheme based on 10 themes
    val colorScheme = when (themeIndex) {
        0 -> if (darkTheme) DarkThemePurple else LightThemePurple
        1 -> if (darkTheme) DarkThemeEmerald else LightThemeEmerald
        2 -> if (darkTheme) DarkThemeGold else LightThemeGold
        3 -> if (darkTheme) DarkThemeOcean else LightThemeOcean
        4 -> if (darkTheme) DarkThemeSunset else LightThemeSunset
        5 -> if (darkTheme) DarkThemeRose else LightThemeRose
        6 -> if (darkTheme) DarkThemeCyber else LightThemeCyber
        7 -> if (darkTheme) DarkThemeSage else LightThemeSage
        8 -> if (darkTheme) DarkThemeRuby else LightThemeRuby
        9 -> if (darkTheme) DarkThemeLavender else LightThemeLavender
        else -> if (darkTheme) DarkThemePurple else LightThemePurple
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
