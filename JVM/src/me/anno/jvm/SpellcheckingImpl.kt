package me.anno.jvm

import me.anno.Engine
import me.anno.cache.CacheData
import me.anno.cache.IgnoredException
import me.anno.config.DefaultConfig
import me.anno.engine.EngineBase
import me.anno.fonts.Codepoints.codepoints
import me.anno.installer.Installer
import me.anno.io.Streams.listen
import me.anno.io.files.FileReference
import me.anno.io.json.generic.JsonReader
import me.anno.jvm.utils.BetterProcessBuilder
import me.anno.language.Language
import me.anno.language.spellcheck.Spellchecking
import me.anno.language.spellcheck.Spellchecking.defaultLanguage
import me.anno.language.spellcheck.Suggestion
import me.anno.utils.Color
import me.anno.utils.OS
import me.anno.utils.Sleep
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.titlecase
import org.apache.logging.log4j.LogManager
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

object SpellcheckingImpl {

    private val path = DefaultConfig["spellchecking.path", OS.downloads.getChild("lib/spellchecking")]

    private val language: Language? get() = EngineBase.instance?.language ?: defaultLanguage

    fun check(sentence: CharSequence, allowFirstLowercase: Boolean, async: Boolean): List<Suggestion>? {
        val language = language
        if (language == null || sentence.isBlank2()) return null
        var sentence2 = sentence.trim()
        if (allowFirstLowercase) sentence2 = sentence2.toString().titlecase()
        if (sentence2 == "#quit") return null
        val data = Spellchecking.getDualEntry(sentence2, language, timeout, async) { seq, lang ->
            val answer = CacheData<List<Suggestion>?>(null)
            getValue(seq, lang) { rawSuggestions ->
                answer.value = rawSuggestions
            }
            answer
        } as? CacheData<*> ?: return null

        @Suppress("unchecked_cast")
        val value = data.value as? List<Suggestion> ?: return null
        return if (sentence != sentence2) {
            val offset = sentence
                .withIndex()
                .indexOfFirst { (index, _) -> !sentence.substring(0, index + 1).isBlank2() }
            if (offset > 0) value.map {
                Suggestion(
                    it.start + offset,
                    it.end + offset,
                    it.message,
                    it.shortMessage,
                    it.improvements
                )
            } else value
        } else value
    }

    private fun getValue(sentence: CharSequence, language: Language, callback: (List<Suggestion>) -> Unit) {
        synchronized(this) {
            val queue = queues.getOrPut(language) { start(language) }
            synchronized(queue) { // should be ok ^^
                queue.add(sentence)
                queue.add(callback)
            }
        }
    }

    private val libraries = listOf(
        // todo these break with NoClassDefFoundError: org/languagetool/language/BritishEnglish
        /*Language.entries.filter {
            it.code.startsWith("en")
        } to "enCompact.jar",*/
        /*Language.entries.filter {
            it.code.startsWith("en") ||
                    it.code.startsWith("de") ||
                    it.code.startsWith("es") ||
                    it.code.startsWith("fr")
        } to "de-en-es-frCompact.jar",*/
        Language.entries to "allCompact.jar"
    )

    private val requestedDownloads = HashSet<FileReference>()
    private fun getExecutable(language: Language, callback: (FileReference) -> Unit) {
        LOGGER.info("Requesting executable for language ${language.naming.name}")
        var fileName: String? = null
        for ((languages, fileNameMaybe) in libraries) {
            if (language in languages) {
                val dst = path.getChild(fileNameMaybe)
                if (dst.exists) {// done :)
                    callback(dst)
                    return
                } else if (dst in requestedDownloads) {
                    waitForDownload(dst, callback)
                }
                if (fileName == null) fileName = fileNameMaybe
            }
        }
        val dst = path.getChild(fileName!!)
        if (dst in requestedDownloads) {
            waitForDownload(dst, callback)
        } else {
            requestedDownloads += dst
            download(dst, callback)
        }
    }

    private fun waitForDownload(dst: FileReference, callback: (FileReference) -> Unit) {
        Sleep.waitUntil(true, { dst.exists }, { callback(dst) })
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

    private fun escapeCodepoint(v: Int): String =
        if (v < 128) "${v.toChar()}"
        else "\\u${Color.hex8((v shr 8) and 255)}${Color.hex8(v and 255)}"

    /**
     * each instance handles a single language only
     * the first argument of the executable can be used to define the language
     * then it will spellcheck all following lines individually.
     * \n and non-ascii symbols should be escaped with \\n or \Uxxxx
     * */
    private fun start(language: Language): Queue<Any> {
        val queue: Queue<Any> = ConcurrentLinkedQueue()
        thread(name = "Spellchecking ${language.code}") {
            if (!OS.isAndroid) {
                getExecutable(language) { executable ->
                    val process = createProcess(executable, language)
                    runProcess(process, language, queue)
                    process.destroy()
                }
            } else {
                // we cannot execute jar files -> have to have the library bundled
                try {
                    val clazz = javaClass.classLoader.loadClass("me.anno.language.spellcheck.BundledSpellcheck")
                    val method = clazz.getMethod("runInstance", Language::class.java, Queue::class.java)
                    method.invoke(null, language, queue)
                } catch (e: ClassNotFoundException) {
                    LOGGER.warn(e)
                }
            }
        }
        return queue
    }

    private fun createProcess(executable: FileReference, language: Language): Process {
        LOGGER.info("Starting process for $language")
        val builder = BetterProcessBuilder("java", 3, true)
        builder += "-jar"
        builder += executable.absolutePath
        builder += language.code
        return builder.start()
    }

    private fun runProcess(process: Process, language: Language, queue: Queue<Any>) {
        val input = process.inputStream
        val reader = input.bufferedReader()
        process.errorStream.listen("Spellchecking-Listener ${language.code}") { msg -> LOGGER.warn(msg) }
        val writer = process.outputStream.bufferedWriter()
        val startMessage = reader.readLine()
        if (startMessage != null) LOGGER.info(startMessage)
        try {
            while (!Engine.shutdown) {
                if (queue.isEmpty()) Sleep.sleepShortly(true)
                else processRequest(queue, reader, writer)
            }
        } catch (_: IgnoredException) {
        }
    }

    private fun processRequest(queue: Queue<Any>, reader: BufferedReader, writer: BufferedWriter) {

        val sentence = queue.poll() as CharSequence

        @Suppress("unchecked_cast")
        val callback = queue.poll() as ((List<Suggestion>) -> Unit)
        writer.write(formatSentence(sentence))
        writer.write('\n'.code)
        writer.flush()
        var suggestionsString = ""
        while ((suggestionsString.isEmpty() || !suggestionsString.startsWith("[")) && !Engine.shutdown) {
            suggestionsString = reader.readLine() ?: break
            // a random, awkward case, when "Program" or "Transform" is requested in German
            if (suggestionsString.startsWith("[COMPOUND")) {
                suggestionsString = "[" + (reader.readLine() ?: break)
            }
        }
        try {
            callback(translateSuggestions(suggestionsString))
        } catch (e: Exception) {
            LOGGER.error(suggestionsString)
            e.printStackTrace()
        }
    }

    private fun formatSentence(sentence: CharSequence): String {
        var lines = sentence
            .toString()
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
        val limitChar = 127.toChar()
        if (lines.any { it > limitChar }) {
            lines = lines.codepoints().joinToString("") { escapeCodepoint(it) }
        }
        return lines
    }

    private fun translateSuggestions(suggestionsString: String): List<Suggestion> {
        val suggestionsJson = JsonReader(suggestionsString).readArray()
        return suggestionsJson.map { suggestion ->
            suggestion as HashMap<*, *>
            val start = AnyToInt.getInt(suggestion["start"], 0)
            val end = AnyToInt.getInt(suggestion["end"], 0)
            val message = suggestion["message"].toString()
            val shortMessage = suggestion["shortMessage"].toString()
            val improvements = suggestion["suggestions"] as ArrayList<*>
            val result = Suggestion(
                start, end, message, shortMessage,
                improvements.map { it as String }
            )
            result
        }
    }

    private val queues = HashMap<Language, Queue<Any>>()
    private val LOGGER = LogManager.getLogger(Spellchecking::class)

    private const val timeout = 600_000L // 10 min
}