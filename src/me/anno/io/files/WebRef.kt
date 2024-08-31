package me.anno.io.files

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.fonts.Codepoints.codepoints
import me.anno.io.Streams.readText
import me.anno.io.VoidOutputStream
import me.anno.utils.Color.hex4
import me.anno.utils.async.Callback
import me.anno.utils.structures.arrays.BooleanArrayList
import me.anno.utils.structures.lists.Lists.wrap
import me.anno.utils.types.Ints.toIntOrDefault
import me.anno.utils.types.Ints.toLongOrDefault
import me.anno.utils.types.Strings.indexOf2
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.concurrent.thread
import kotlin.math.min


/**
 * http/https resource
 * on the web, these may be the only resources available except for InnerFiles
 *
 * get/put/post? -> this is just reading, so get is fine unless we want outputStream()
 * todo define request headers like user-agent
 *
 * if is redirect, automatically redirect? yes -> and it works by my testing :)
 * watch dogs? we only can ask for changes every x seconds
 * */
open class WebRef(url: String, args: Map<Any?, Any?> = emptyMap()) :
    FileReference(formatAccessURL(url, args)) {

    var valueTimeout = 30_000L

    val arguments: Map<String, String>
    val path: String
    val hashbang: String?

    init {
        val (p, a, h) = parse(absolutePath)
        path = p
        arguments = a
        hashbang = h
    }

    // doesn't really exist, or does it?
    override val isDirectory: Boolean = false

    // we can answer that, when we got a response code, e.g., 404
    override val exists: Boolean
        get() {
            val headers = headers
            val responseCode = headers?.get(RESPONSE_CODE_KEY)?.firstOrNull()?.toIntOrNull()
            return headers != null &&
                    (responseCode == null || responseCode in 200 until 400)
        }

    override val lastModified: Long
        get() = headers?.get("Last-Modified").toString().parseLastModified()

    val responseCode: Int // HTTP/1.1 200 OK
        get() = headers?.get(null)?.first().run {
            if (this == null) 404
            else {
                val i0 = indexOf(' ') + 1
                val i1 = indexOf(' ', i0 + 1)
                substring(i0, i1).toIntOrDefault(400)
            }
        }

    override val lastAccessed: Long get() = 0L
    override val creationTime: Long get() = 0L

    fun toURL() = URL(absolutePath)
    override fun toLocalPath(workspace: FileReference) = absolutePath

    override fun getChildImpl(name: String): FileReference {
        val splitIndex = absolutePath.indexOf('?')
        val basePath = if (splitIndex < 0) absolutePath else absolutePath.substring(0, splitIndex)
        return WebRef("$basePath/$name", emptyMap())
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        thread(name = "WebRef") {
            callback.ok(inputStreamSync())
        }
    }

    override fun inputStreamSync(): InputStream {
        val connection = toURL().openConnection() as HttpURLConnection
        if (connection.responseCode == 200) {
            return connection.inputStream
        } else {
            LOGGER.warn(connection.errorStream.readText())
            throw IOException("$absolutePath failed")
        }
    }

    override fun outputStream(append: Boolean): OutputStream {
        if (append) {
            LOGGER.warn("Appending isn't supported (yet?)")
            return VoidOutputStream
        }
        // in the future, we might use the Apache HTTP API
        // a simple helper function,
        // may be incomplete for your purposes, e.g.,
        // it doesn't set the content type, or other parameters
        // better tutorial:
        // https://stackoverflow.com/a/35013372/4979303
        val http = toURL().openConnection() as HttpURLConnection
        http.requestMethod = "POST"
        http.doOutput = true
        http.connect()
        return http.outputStream
    }

    val headers get() = getHeaders(toURL(), valueTimeout, false)

    override fun length(): Long {
        val headers = headers ?: return -1
        return headers["content-length"]?.first().toLongOrDefault(-1L)
    }

    override fun delete(): Boolean {
        throw IOException("Operation WebRef.delete() is not supported")
    }

    override fun renameTo(newName: FileReference): Boolean {
        throw IOException("Operation WebRef.renameTo() is not supported")
    }

    override fun mkdirs(): Boolean {
        throw IOException("Operation WebRef.mkdirs() is not supported")
    }

    override fun getParent(): FileReference {
        var endIndex = absolutePath.indexOf('?')
        if (endIndex < 0) endIndex = absolutePath.length
        if (absolutePath[endIndex - 1] == '/') endIndex--
        endIndex = absolutePath.lastIndexOf('/', endIndex)
        if (endIndex < 0) return InvalidRef // we're already at the top level
        return WebRef(absolutePath.substring(0, endIndex), emptyMap())
    }

    companion object {

        private val LOGGER = LogManager.getLogger(WebRef::class)
        private const val RESPONSE_CODE_KEY = "ResponseCode"
        val webCache = CacheSection("Web")

        private fun String.parseLastModified(): Long {
            // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified
            // <day-name>, <day> <month> <year> <hour>:<minute>:<second> GMT
            try {
                val zonedDateTime = ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME)
                return zonedDateTime.toInstant().toEpochMilli()
            } catch (e: DateTimeParseException) {
                return 0
            }
        }

        private fun getHeaders(url: URL, timeout: Long, async: Boolean): Map<String?, List<String>>? {
            val data = webCache.getEntry(url, timeout, async) {
                var conn: URLConnection? = null
                val data: Map<String?, List<String>> = try {
                    conn = url.openConnection()
                    if (conn is HttpURLConnection) {
                        conn.requestMethod = "HEAD"
                    }
                    val responseCode = (conn as? HttpURLConnection)?.responseCode?.toString().wrap()
                    conn.headerFields + mapOf(RESPONSE_CODE_KEY to responseCode)
                } catch (e: IOException) {
                    throw RuntimeException(e)
                } finally {
                    if (conn is HttpURLConnection) {
                        conn.disconnect()
                    }
                }
                CacheData(data)
            } as? CacheData<*>
            @Suppress("unchecked_cast")
            return data?.value as? Map<String?, List<String>>
        }

        private fun formatAccessURL(url: String, args: Map<Any?, Any?>): String {
            if (args.isEmpty()) return url
            val hi = url.indexOf('#')
            return if (hi >= 0) "${formatAccessURL(url.substring(0, hi), args)}#${url.substring(hi + 1)}"
            else url + (if ('?' in url) "&" else "?") + args.entries.joinToString("&") { entry ->
                val (key, value) = entry
                val key2 = encodeURIComponent(key.toString())
                val val2 = encodeURIComponent(value.toString())
                "$key2=$val2"
            }
        }

        private fun parse(url: String): Triple<String, Map<String, String>, String?> {
            val qi = url.indexOf2('?')
            val hi = url.indexOf2('#')
            if (qi == hi) return Triple(url, emptyMap(), null)
            if (hi < qi) return Triple(url.substring(0, hi), emptyMap(), url.substring(hi + 1))
            // extract all arguments :)
            val args = HashMap<String, String>(url.count { it == '&' } * 3 / 2 + 2)
            var i = qi
            while (i < hi) {
                val ni = min(min(url.indexOf2('&', i + 1), url.indexOf2('?', i + 1)), hi)
                // i .. ni
                val eq = min(url.indexOf2('=', i + 1), ni)
                val key = url.substring(i + 1, eq)
                val value = if (eq + 1 <= ni) url.substring(eq + 1, ni) else ""
                args[key] = value
                i = ni
            }
            val hashbang = if (hi < url.length) url.substring(hi + 1) else null
            return Triple(url.substring(0, qi), args, hashbang)
        }

        // https://stackoverflow.com/a/10032289/4979303
        /** used for the encodeURIComponent function  */
        private val allowedChars = BooleanArrayList(128)

        init {
            for (i in 'a'..'z') allowedChars.set(i.code)
            for (i in 'A'..'Z') allowedChars.set(i.code)
            for (i in '0'..'9') allowedChars.set(i.code)
            for (i in "'()*!-_.~") allowedChars.set(i.code)
        }

        /**
         * Escapes all characters except the following: alphanumeric, - _ . ! ~ * ' ( )
         * @param input A component of a URI
         * @return the escaped URI component
         */
        fun encodeURIComponent(input: String?): String? {
            if (input == null || input.all { allowedChars[it.code] })
                return input
            val res = StringBuilder(input.length * 2)
            for (cp in input.codepoints()) {
                if (allowedChars[cp]) {
                    res.append(cp.toChar())
                } else {
                    try {
                        val b = cp.joinChars().toString().encodeToByteArray()
                        for (j in b.indices) {
                            res.append('%')
                            res.append(hex4(b[j].toInt().shr(4)))
                            res.append(hex4(b[j].toInt()))
                        }
                    } catch (e: UnsupportedEncodingException) {
                        res.append('%')
                        res.append(hex4(cp.shr(4)))
                        res.append(hex4(cp))
                    }
                }
            }
            return res.toString()
        }
    }
}