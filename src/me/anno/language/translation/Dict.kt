package me.anno.language.translation

import me.anno.config.DefaultConfig
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.language.Language
import me.anno.studio.StudioBase
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.io.InputStream

object Dict {

    private var values = HashMap<String, String>()

    fun load(input: InputStream, clear: Boolean) {
        val text = String(input.readBytes())
        load(text, clear)
    }

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

    @Suppress("unused")
    fun getLanguageName(input: InputStream): String? {
        val text = String(input.readBytes())
        return getLanguageName(text)
    }

    fun getLanguageName(text: String): String? {
        for (line in text.split('\n')) {
            val startIndex = line.indexOf(':')
            if (startIndex >= 0) {
                val key = line.substring(0, startIndex).trim()
                if ("lang.name".equals(key, true)) {
                    val value = line.substring(startIndex + 1).trim()
                    if (value.isNotEmpty()) {
                        return value
                    }
                }
            }
        }
        return null
    }

    fun getOptions(): List<LanguageOption> {
        val options = ArrayList<LanguageOption>()
        val internalFiles = listOf("en.lang", "es.lang", "fr.lang", "de.lang", "zh.lang")
        for (it in internalFiles) {
            try {
                val data = getReference("res://lang/$it").readTextSync()
                val name = getLanguageName(data)
                if (name?.isNotEmpty() == true) {
                    options += LanguageOption(data, "internal/$it", name)
                }
            } catch (e: IOException) {
                LOGGER.warn("Skipped $it, didn't find it")
            }
        }
        val externalFiles = ConfigBasics.configFolder.getChild("lang").listChildren()
        if (externalFiles != null) for (file in externalFiles) {
            if (!file.isDirectory && file.name.endsWith(".lang")) {
                try {
                    val data = file.readTextSync()
                    val name = getLanguageName(data)
                    if (name?.isNotEmpty() == true) {
                        options += LanguageOption(data, "config/${file.name}", name)
                    }
                } catch (_: IOException) {
                }
            }
        }
        if (options.isEmpty()) {
            options += LanguageOption("", "", "Missing :/")
        }
        return options
    }

    fun getDefaultOption(): LanguageOption {
        val options = getOptions()
        val currentLanguagePath = DefaultConfig["ui.language", "internal/en.lang"]
        return options.firstOrNull { it.path == currentLanguagePath }
            ?: options.firstOrNull { it.path == "internal/en.lang" } ?: options.first()
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
            StudioBase.instance?.language = Language.get(Dict["en-US", "lang.spellcheck"])
            changeListener(option)
        }
        return input
    }

    operator fun get(key: String) = values[key]
    operator fun get(default: String, key: String) = values[key] ?: default

    private val LOGGER = LogManager.getLogger(Dict::class)

}