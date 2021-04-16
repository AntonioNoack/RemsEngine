package me.anno.language.translation

import me.anno.config.DefaultConfig
import me.anno.io.config.ConfigBasics
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.utils.io.ResourceHelper
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException
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
        text.split('\n').forEach {
            val startIndex = it.indexOf(':')
            if (startIndex >= 0) {
                val key = it.substring(0, startIndex).trim()
                val value = it.substring(startIndex + 1).trim()
                if (value.isNotEmpty()) {
                    values[key] = value
                }
            }
        }
    }

    fun getLanguageName(input: InputStream): String? {
        val text = String(input.readBytes())
        return getLanguageName(text)
    }

    fun getLanguageName(text: String): String? {
        text.split('\n').forEach {
            val startIndex = it.indexOf(':')
            if (startIndex >= 0) {
                val key = it.substring(0, startIndex).trim()
                if ("lang.name".equals(key, true)) {
                    val value = it.substring(startIndex + 1).trim()
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
        val internalFiles = listOf("en.lang", "de.lang")
        internalFiles.forEach {
            try {
                val data = String(ResourceHelper.loadResource("lang/$it").readBytes())
                val name = getLanguageName(data)
                if (name?.isNotEmpty() == true) {
                    options += LanguageOption(data, "internal/$it", name)
                }
            } catch (e: FileNotFoundException) {
                LOGGER.warn("Skipped $it, didn't find it")
            }
        }
        val externalFiles = File(ConfigBasics.configFolder.file, "lang").listFiles()
        externalFiles?.forEach { file ->
            if (!file.isDirectory && file.name.endsWith(".lang")) {
                try {
                    val data = file.readText()
                    val name = getLanguageName(data)
                    if (name?.isNotEmpty() == true) {
                        options += LanguageOption(data, "config/${file.name}", name)
                    }
                } catch (e: IOException) {
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

    fun selectLanguages(style: Style, changeListener: () -> Unit = {}): EnumInput {
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
            load(option.data, true)
            changeListener()
        }
        return input
    }

    operator fun get(key: String) = values[key]
    operator fun get(default: String, key: String) = values[key] ?: default

    private val LOGGER = LogManager.getLogger(Dict::class)

}