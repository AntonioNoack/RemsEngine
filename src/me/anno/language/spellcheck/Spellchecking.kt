package me.anno.language.spellcheck

import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.installer.Installer
import me.anno.io.json.JsonArray
import me.anno.io.json.JsonObject
import me.anno.io.json.JsonReader
import me.anno.language.Language
import me.anno.language.translation.Dict
import me.anno.studio.rems.RemsStudio.project
import me.anno.utils.Color.hex8
import me.anno.utils.OS
import me.anno.utils.Streams.listen
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.streams.toList

object Spellchecking : CacheSection("Spellchecking") {

    private val path = File(DefaultConfig["spellchecking.path", File(OS.downloads, "lib\\spellchecking").toString()])

    private val language get() = project?.language ?: Language.get(Dict["en-US", "lang.spellcheck"])

    fun check(sentence: String, allowFirstLowercase: Boolean, key: Any): List<Suggestion>? {
        val language = language
        if (language == Language.None || sentence.isBlank()) return null
        var sentence2 = sentence.trim()
        if(allowFirstLowercase) sentence2 = sentence2.capitalize()
        if (sentence2 == "#quit") return null
        val data = getEntry(Pair(sentence2, language), timeout, true) {
            val answer = SuggestionData(null)
            getValue(sentence2, language, key) { answer.value = it }
            answer
        } as? SuggestionData
        return if (sentence != sentence2) {
            data?.value?.run {
                val offset =
                    sentence.withIndex().indexOfFirst { (index, _) -> sentence.substring(0, index + 1).isNotBlank() }
                map {
                    Suggestion(it.start + offset, it.end + offset, it.message, it.shortMessage, it.improvements)
                }
            }
        } else {
            data?.value
        }
    }

    fun getValue(sentence: String, language: Language, key: Any, callback: (List<Suggestion>) -> Unit) {
        synchronized(this) {
            val queue = queues.getOrPut(language) {
                start(language)
            }
            queue[key] = Request(sentence, key, callback)
        }
    }

    private val libraries = listOf(
        // currently not working, why ever...
        //Language.values().filter { it.shortCode == "en" } to "enCompact.jar",
        /*Language.values().filter {
            when (it.shortCode) {
                "en", "de", "es", "fr" -> true
                else -> false
            }
        } to "de-en-es-frCompact.jar",*/
        Language.values().toList() to "allCompact.jar"
    )

    private val requestedDownloads = HashSet<File>()
    private fun getExecutable(language: Language, callback: (File) -> Unit) {
        LOGGER.info("Requesting executable for language ${language.naming.name}")
        var fileName: String? = null
        for ((languages, fileNameMaybe) in libraries) {
            if (language in languages) {
                val dst = File(path, fileNameMaybe)
                if (dst.exists()) {// done :)
                    callback(dst)
                    return
                } else if (dst in requestedDownloads) {
                    waitForDownload(dst, callback)
                }
                if (fileName == null) fileName = fileNameMaybe
            }
        }
        val dst = File(path, fileName!!)
        val answer = if (dst in requestedDownloads) {
            true
        } else {
            requestedDownloads += dst; false
        }
        if (answer) {
            waitForDownload(dst, callback)
        } else {
            download(dst, callback)
        }
    }

    private fun waitForDownload(dst: File, callback: (File) -> Unit) {
        thread {
            loop@ while (!shallStop) {
                if (dst.exists()) {
                    callback(dst)
                    break@loop
                } else Thread.sleep(1)
            }
        }
    }

    private fun download(dst: File, callback: (File) -> Unit) {
        if (dst.exists()) {
            callback(dst)
        } else {
            Installer.download("spelling/${dst.name}", dst) {
                callback(dst)
            }
        }
    }

    fun Int.escapeCodepoint() =
        if (this < 128) "${toChar()}"
        else "\\u${hex8((this shr 8) and 255)}${hex8(this and 255)}"


    /**
     * each instance handles a single language only
     * the first argument of the executable can be used to define the language
     * then it will spellcheck all following lines individually.
     * \n and non-ascii symbols should be escaped with \\n or \Uxxxx
     * */
    fun start(language: Language): ConcurrentHashMap<Any, Request> {
        val queue = ConcurrentHashMap<Any, Request>()
        thread {
            getExecutable(language) { executable ->
                LOGGER.info("Starting process for $language")
                val process = ProcessBuilder("java", "-jar", executable.absolutePath, language.code).start()
                val input = process.inputStream.bufferedReader()
                process.errorStream.listen { msg -> LOGGER.warn(msg) }
                val output = process.outputStream.bufferedWriter()
                LOGGER.info(input.readLine())
                while (!shallStop) {
                    if (queue.isEmpty()) Thread.sleep(1)
                    else {
                        val nextTask: Request
                        synchronized(this) {
                            val key = queue.keys().nextElement()
                            nextTask = queue.remove(key)!!
                        }
                        var lines = nextTask.sentence.replace("\n", "\\n")
                        if (lines.any { it > 127.toChar() }) {
                            lines = lines.codePoints()
                                .toList().joinToString("") { it.escapeCodepoint() }
                        }
                        output.write(lines)
                        output.write('\n'.toInt())
                        output.flush()
                        var suggestionsString = ""
                        while (suggestionsString.isEmpty() && !shallStop) {
                            suggestionsString = input.readLine()
                        }
                        try {
                            val suggestionsJson = JsonReader(suggestionsString.toByteArray().inputStream()).readArray()
                            val suggestionsList = suggestionsJson.map { suggestion ->
                                suggestion as JsonObject
                                val start = suggestion["start"]!!.asText().toInt()
                                val end = suggestion["end"]!!.asText().toInt()
                                val message = suggestion["message"]!!.asText()
                                val shortMessage = suggestion["shortMessage"]!!.asText()
                                val improvements = suggestion["suggestions"] as JsonArray
                                val result =
                                    Suggestion(start, end, message, shortMessage, improvements.map { it as String })
                                result
                            }
                            nextTask.callback(suggestionsList)
                        } catch (e: Exception) {
                            LOGGER.error(suggestionsString)
                            e.printStackTrace()
                        }
                    }
                }
                process.destroy()
            }
        }
        return queue
    }

    private val queues = HashMap<Language, ConcurrentHashMap<Any, Request>>()
    private val LOGGER = LogManager.getLogger(Spellchecking::class)

    private const val timeout = 600_000L // 10 min

    private var shallStop = false
    fun destroy() {
        LOGGER.info("Shutting down")
        shallStop = true
    }

}