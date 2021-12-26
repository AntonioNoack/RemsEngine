package me.anno.language.spellcheck

import me.anno.Engine.shutdown
import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.installer.Installer
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.json.JsonArray
import me.anno.io.json.JsonObject
import me.anno.io.json.JsonReader
import me.anno.language.Language
import me.anno.language.translation.Dict
import me.anno.studio.rems.RemsStudio.project
import me.anno.utils.Color.hex8
import me.anno.utils.OS
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep.sleepABit10
import me.anno.utils.Sleep.sleepShortly
import me.anno.utils.hpc.Threads.threadWithName
import me.anno.utils.io.Streams.listen
import me.anno.utils.process.BetterProcessBuilder
import me.anno.utils.strings.StringHelper.titlecase
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.streams.toList

object Spellchecking : CacheSection("Spellchecking") {

    private val path = DefaultConfig["spellchecking.path", getReference(OS.downloads, "lib\\spellchecking")]

    private val language get() = project?.language ?: Language.get(Dict["en-US", "lang.spellcheck"])

    fun check(sentence: String, allowFirstLowercase: Boolean, key: Any): List<Suggestion>? {
        val language = language
        if (language == Language.None || sentence.isBlank2()) return null
        var sentence2 = sentence.trim()
        if (allowFirstLowercase) sentence2 = sentence2.titlecase()
        if (sentence2 == "#quit") return null
        val data = getEntry(Pair(sentence2, language), timeout, true) {
            val answer = SuggestionData(null)
            getValue(sentence2, language, key) { answer.value = it }
            answer
        } as? SuggestionData
        return if (sentence != sentence2) {
            data?.value?.run {
                val offset =
                    sentence.withIndex().indexOfFirst { (index, _) -> !sentence.substring(0, index + 1).isBlank2() }
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

    private val requestedDownloads = HashSet<FileReference>()
    private fun getExecutable(language: Language, callback: (FileReference) -> Unit) {
        LOGGER.info("Requesting executable for language ${language.naming.name}")
        var fileName: String? = null
        for ((languages, fileNameMaybe) in libraries) {
            if (language in languages) {
                val dst = getReference(path, fileNameMaybe)
                if (dst.exists) {// done :)
                    callback(dst)
                    return
                } else if (dst in requestedDownloads) {
                    waitForDownload(dst, callback)
                }
                if (fileName == null) fileName = fileNameMaybe
            }
        }
        val dst = getReference(path, fileName!!)
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

    private fun waitForDownload(dst: FileReference, callback: (FileReference) -> Unit) {
        threadWithName("Spellchecking::waitForDownload") {
            loop@ while (!shutdown) {
                if (dst.exists) {
                    callback(dst)
                    break@loop
                } else sleepABit10(true)
            }
        }
    }

    private fun download(dst: FileReference, callback: (FileReference) -> Unit) {
        if (dst.exists) {
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
        threadWithName("Spellchecking ${language.code}") {
            if (!OS.isAndroid) {
                getExecutable(language) { executable ->
                    LOGGER.info("Starting process for $language")
                    val builder = BetterProcessBuilder("java", 3, true)
                    builder += "-jar"
                    builder += executable.absolutePath
                    builder += language.code
                    val process = builder.start()
                    val input = process.inputStream.bufferedReader()
                    process.errorStream.listen("Spellchecking-Listener ${language.code}") { msg -> LOGGER.warn(msg) }
                    val output = process.outputStream.bufferedWriter()
                    LOGGER.info(input.readLine())
                    try {
                        while (!shutdown) {
                            if (queue.isEmpty()) sleepShortly(true)
                            else {
                                val nextTask: Request
                                synchronized(this) {
                                    val key = queue.keys().nextElement()
                                    nextTask = queue.remove(key)!!
                                }
                                var lines = nextTask.sentence
                                    .replace("\\", "\\\\")
                                    .replace("\n", "\\n")
                                val limitChar = 127.toChar()
                                if (lines.any { it > limitChar }) {
                                    lines = lines.codePoints()
                                        .toList().joinToString("") { it.escapeCodepoint() }
                                }
                                output.write(lines)
                                output.write('\n'.code)
                                output.flush()
                                var suggestionsString = ""
                                while ((suggestionsString.isEmpty() || !suggestionsString.startsWith("[")) && !shutdown) {
                                    suggestionsString = input.readLine()
                                    // a random, awkward case, when "Program" or "Transform" is requested in German
                                    if (suggestionsString.startsWith("[COMPOUND")) {
                                        suggestionsString = "[" + input.readLine()
                                    }
                                }
                                try {
                                    val suggestionsJson = JsonReader(suggestionsString).readArray()
                                    val suggestionsList = suggestionsJson.map { suggestion ->
                                        suggestion as JsonObject
                                        val start = suggestion["start"]!!.asText().toInt()
                                        val end = suggestion["end"]!!.asText().toInt()
                                        val message = suggestion["message"]!!.asText()
                                        val shortMessage = suggestion["shortMessage"]!!.asText()
                                        val improvements = suggestion["suggestions"] as JsonArray
                                        val result =
                                            Suggestion(
                                                start,
                                                end,
                                                message,
                                                shortMessage,
                                                improvements.map { it as String })
                                        result
                                    }
                                    nextTask.callback(suggestionsList)
                                } catch (e: Exception) {
                                    OS.desktop
                                        .getChild("${System.currentTimeMillis()}.txt")
                                        .writeText("$lines\n$suggestionsString")
                                    LOGGER.error(suggestionsString)
                                    e.printStackTrace()
                                }
                            }
                        }
                    } catch (ignored: ShutdownException) {
                    }
                    process.destroy()
                }
            } else {
                // we cannot execute jar files -> have to have the library bundled
                val clazz = javaClass.classLoader.loadClass("me.anno.language.spellcheck.BundledSpellcheck")
                val method = clazz.getMethod("runInstance", Language::class.java, ConcurrentHashMap::class.java)
                method.invoke(null, language, queue)
            }
        }
        return queue
    }

    private val queues = HashMap<Language, ConcurrentHashMap<Any, Request>>()
    private val LOGGER = LogManager.getLogger(Spellchecking::class)

    private const val timeout = 600_000L // 10 min

}