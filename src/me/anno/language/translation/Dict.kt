package me.anno.language.translation

import me.anno.config.DefaultConfig
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.ui.Style
import me.anno.ui.input.EnumInput
import me.anno.utils.types.Strings.indexOf2
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.Locale

/**
 * translates UI text into different languages
 * todo somehow allow to register multiple subfolders for the same language, so we can extend translations
 * */
object Dict {

    private val values = HashMap<String, String>()
    private const val ext = "lang"

    fun load(text: String, clear: Boolean) {
        if (clear) values.clear()
        for (line in text.split('\n')) {
            val startIndex = line.indexOf(':')
            if (startIndex >= 0) {
                val key = line.substring(0, startIndex).trim()
                val value = line.substring(startIndex + 1).trim()
                if (value.isNotEmpty()) {
                    values[key] = value
                }
            }
        }
    }

    fun getLanguageName(text: String): String? {
        val prefix = "lang.name:"
        val startIndex = text.indexOf(prefix)
        if (startIndex >= 0) {
            val valueStartIndex = startIndex + prefix.length
            val endOfLineIndex = text.indexOf2('\n', valueStartIndex)
            val value = text.substring(valueStartIndex, endOfLineIndex).trim()
            if (value.isNotEmpty()) {
                return value
            }
        }
        return null
    }

    private fun load(file: FileReference): LanguageOption? {
        return try {
            val data = file.readTextSync()
            val name = getLanguageName(data)
            if (name != null) {
                LanguageOption(data, file, name)
            } else null
        } catch (e: IOException) {
            LOGGER.warn("Skipped $file, didn't find it")
            null
        }
    }

    fun getOptions(): List<LanguageOption> {
        val options = ArrayList<LanguageOption>()
        val internalFiles = listOf(
            "en.lang", "es.lang", "de.lang", "fr.lang", "it.lang",
            "zh.lang", "ja.lang",
            "el.lang",
            "uk.lang", "ru.lang"
        )
        for (fileName in internalFiles) {
            options += load(getReference("res://lang/$fileName")) ?: continue
        }
        val externalFiles = ConfigBasics.configFolder.getChild(ext).listChildren()
        for (file in externalFiles) {
            if (!file.isDirectory && file.lcExtension == ext) {
                options += load(file) ?: continue
            }
        }
        if (options.isEmpty()) {
            options += LanguageOption("", InvalidRef, "Missing :/")
        }
        return options
    }

    fun getDefaultOption(): LanguageOption {
        val options = getOptions()
        val userLanguage = Locale.getDefault().language
        val userLanguagePath = getReference("res://$ext/$userLanguage.$ext")
        val userLanguageIsSupported = getReference(userLanguagePath).exists
        val defaultLang0 = getReference("res://$ext/en.$ext")
        val defaultLang = if (userLanguageIsSupported) userLanguagePath else defaultLang0
        val currentLanguagePath = DefaultConfig["ui.language", defaultLang]
        return options.firstOrNull { it.path == currentLanguagePath }
            ?: options.firstOrNull { it.path == defaultLang0 } ?: options.first()
    }

    fun loadDefault() {
        load(getDefaultOption().data, true)
    }

    fun selectLanguage(style: Style, changeListener: (LanguageOption) -> Unit = {}): EnumInput {
        // two folders, one in the config (lang), and one internally (assets/lang)
        // data, path, name
        val options = getOptions()
        val currentLanguage = getDefaultOption()
        val input = EnumInput(
            Dict["Language", "ui.input.language.title"],
            true,
            currentLanguage.name,
            options.map { NameDesc(it.name) },
            style
        )
        input.setChangeListener { _, index, _ ->
            val option = options[index]
            DefaultConfig["ui.language"] = option.path
            // load all translations
            load(option.data, true)
            // update spellchecking language
            changeListener(option)
        }
        return input
    }

    operator fun get(key: String) = values[key]
    operator fun get(default: String, key: String) = values[key] ?: default

    private val LOGGER = LogManager.getLogger(Dict::class)
}