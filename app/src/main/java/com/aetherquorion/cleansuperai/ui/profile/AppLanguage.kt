package com.aetherquorion.cleansuperai.ui.profile

import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.aetherquorion.cleansuperai.R
import java.util.Locale

data class AppLanguage(
    val tag: String?,
    val locale: Locale?,
) {
    val isFollowSystem: Boolean
        get() = tag == null

    fun title(): String {
        if (isFollowSystem) return ""
        return locale
            ?.getDisplayName(locale)
            ?.replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase(locale ?: Locale.getDefault())
                } else {
                    char.toString()
                }
            }
            .orEmpty()
    }

    fun subtitle(displayLocale: Locale): String? {
        if (isFollowSystem) return null
        val languageLocale = locale ?: return null
        val localizedName = languageLocale
            .getDisplayName(displayLocale)
            .replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase(displayLocale)
                } else {
                    char.toString()
                }
            }
        return localizedName.takeUnless {
            it.equals(title(), ignoreCase = true)
        }
    }

    companion object {
        val supported = listOf(
            AppLanguage(tag = null, locale = null),
            AppLanguage(tag = "en", locale = Locale.forLanguageTag("en")),
            AppLanguage(tag = "zh-CN", locale = Locale.forLanguageTag("zh-CN")),
            AppLanguage(tag = "fr", locale = Locale.forLanguageTag("fr")),
            AppLanguage(tag = "de", locale = Locale.forLanguageTag("de")),
            AppLanguage(tag = "it", locale = Locale.forLanguageTag("it")),
            AppLanguage(tag = "es", locale = Locale.forLanguageTag("es")),
            AppLanguage(tag = "sv", locale = Locale.forLanguageTag("sv")),
            AppLanguage(tag = "ru", locale = Locale.forLanguageTag("ru")),
            AppLanguage(tag = "pt", locale = Locale.forLanguageTag("pt")),
            AppLanguage(tag = "ro", locale = Locale.forLanguageTag("ro")),
            AppLanguage(tag = "tr", locale = Locale.forLanguageTag("tr")),
            AppLanguage(tag = "nl", locale = Locale.forLanguageTag("nl")),
            AppLanguage(tag = "id", locale = Locale.forLanguageTag("id")),
            AppLanguage(tag = "pl", locale = Locale.forLanguageTag("pl")),
            AppLanguage(tag = "vi", locale = Locale.forLanguageTag("vi")),
            AppLanguage(tag = "ja", locale = Locale.forLanguageTag("ja")),
            AppLanguage(tag = "ko", locale = Locale.forLanguageTag("ko")),
            AppLanguage(tag = "th", locale = Locale.forLanguageTag("th")),
            AppLanguage(tag = "he", locale = Locale.forLanguageTag("he")),
            AppLanguage(tag = "ar", locale = Locale.forLanguageTag("ar")),
        )

        fun selectedTag(): String? {
            val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return tags.substringBefore(',').ifBlank { null }
        }

        fun selected(): AppLanguage {
            val selectedTag = selectedTag()
            return supported.firstOrNull { it.tag == selectedTag } ?: supported.first()
        }

        fun effectiveLocale(): Locale {
            val appLocales = AppCompatDelegate.getApplicationLocales()
            appLocales[0]?.let { return it }
            return systemLocale()
        }

        fun systemLocale(): Locale {
            val configuration = Resources.getSystem().configuration
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.locales[0] ?: Locale.getDefault()
            } else {
                @Suppress("DEPRECATION")
                configuration.locale ?: Locale.getDefault()
            }
        }

        fun currentSelectionSummary(context: Context): String {
            return selected().let { selectedLanguage ->
                if (selectedLanguage.isFollowSystem) {
                    context.getString(
                        R.string.language_follow_system_subtitle_format,
                        displayName(systemLocale()),
                    )
                } else {
                    displayName(selectedLanguage.locale ?: effectiveLocale())
                }
            }
        }

        fun displayName(locale: Locale): String {
            return locale
                .getDisplayName(locale)
                .replaceFirstChar { char ->
                    if (char.isLowerCase()) {
                        char.titlecase(locale)
                    } else {
                        char.toString()
                    }
                }
        }
    }
}
