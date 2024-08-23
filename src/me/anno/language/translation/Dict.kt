package me.anno.language.translation

import me.anno.Build
import me.anno.config.DefaultConfig
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.yaml.generic.SimpleYAMLReader
import me.anno.ui.Style
import me.anno.ui.input.EnumInput
import me.anno.utils.OS.res
import me.anno.utils.types.Strings.indexOf2
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.Locale

/**
 * translates UI text into different languages
 * todo somehow allow to register multiple subfolders for the same language, so we can extend translations
 * */
object Dict {

    private val values = HashMap<String, String>()
    private const val EXTENSION = "lang"
    var collectDefaults: HashMap<String, String>? =
        if (Build.isDebug) HashMap() else null

    fun load(text: String, clear: Boolean) {
        if (clear) values.clear()
        val lines = text.split('\n')
        SimpleYAMLReader.read(lines.iterator(), false, values)
    }

    private fun getValue(text: String, prefix: String): String? {
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

    fun getLanguageName(text: String): NameDesc? {
        val name = getValue(text, "lang.name:") ?: return null
        val desc = getValue(text, "lang.enName:") ?: name
        return NameDesc(name, desc, "")
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
            options += load(res.getChild("lang/$fileName")) ?: continue
        }
        val externalFiles = ConfigBasics.configFolder.getChild(EXTENSION).listChildren()
        for (file in externalFiles) {
            if (!file.isDirectory && file.lcExtension == EXTENSION) {
                options += load(file) ?: continue
            }
        }
        if (options.isEmpty()) {
            options += LanguageOption("", InvalidRef, NameDesc("Missing :/"))
        }
        return options
    }

    fun getDefaultOption(): LanguageOption {
        val options = getOptions()
        val userLanguage = Locale.getDefault().language
        val userLanguagePath = res.getChild("$EXTENSION/$userLanguage.$EXTENSION")
        val userLanguageIsSupported = getReference(userLanguagePath).exists
        val defaultLang0 = res.getChild("$EXTENSION/en.$EXTENSION")
        val defaultLang = if (userLanguageIsSupported) userLanguagePath else defaultLang0
        val currentLanguagePath = DefaultConfig["ui.language", defaultLang]
        return options.firstOrNull { it.path == currentLanguagePath }
            ?: options.firstOrNull { it.path == defaultLang0 } ?: options.first()
    }

    fun loadDefault() {
        load(getDefaultOption().data, true)
    }

    @JvmStatic
    fun selectLanguage(style: Style, changeListener: (LanguageOption) -> Unit = {}): EnumInput {
        // two folders, one in the config (lang), and one internally (assets/lang)
        // data, path, name
        val options = getOptions()
        val currentLanguage = getDefaultOption()
        val input = EnumInput(
            NameDesc("Language", "", "ui.input.language.title"), true,
            currentLanguage.nameDesc,
            options.map { it.nameDesc },
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
    operator fun get(default: String, key: String): String {
        collectDefaults?.put(key, default)
        return values[key] ?: default
    }

    /**
     * prints all used translation key-pairs:
     *  - fill in collectDefaults
     *  - execute/play your program/game with all texts
     *  - execute this function
     *  - put it into some auto-translation tool like Google Translate or DeepL,
     *  - clean it up, so all keys are proper, and all colons are correct ASCII
     *  - put it into assets/lang/xy.lang
     *  - enjoy
     * */
    fun printDefaults() {
        val values = collectDefaults
        if (values != null) {
            LOGGER.info("DictDefaults:")
            for ((k, v) in values.toSortedMap()) {
                if (v.isBlank2()) continue // ignore these
                println("$k: ${v.replace("\n", "\\n")}")
            }
        } else {
            LOGGER.warn("DictDefault needs to be initialized first")
        }
    }

    private val LOGGER = LogManager.getLogger(Dict::class)
}