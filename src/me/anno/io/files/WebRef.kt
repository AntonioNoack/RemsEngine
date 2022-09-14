package me.anno.io.files

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.utils.Color.hex4
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.util.*


/**
 * http/https resource
 * todo get/put/post
 *
 * todo if is redirect, automatically redirect?
 * todo watch dogs? we only can ask for changes every x seconds
 * */
class WebRef(url: String, args: Map<Any?, Any?>) :
    FileReference(formatAccessURL(url, args)) {

    var valueTimeout = 30_000L

    // doesn't really exist, or does it?
    override val isDirectory: Boolean = false

    // we can answer that, when we got a response code, e.g., 404
    override val exists: Boolean
        get() = getHeaders(toURL(), valueTimeout, false) != null

    // todo parse date
    override val lastModified: Long
        get() = getHeaders(toURL(), valueTimeout, false)
            ?.get("Last-Modified")
            ?.first()?.toLongOrNull() ?: 0L

    val responseCode: Int // HTTP/1.1 200 OK
        get() = getHeaders(toURL(), valueTimeout, false)
            ?.get(null)?.first()?.run {
                val i0 = indexOf(' ') + 1
                val i1 = indexOf(' ', i0 + 1)
                substring(i0, i1).toIntOrNull()
            } ?: 404

    override val lastAccessed: Long = 0L

    fun toURL() = URL(absolutePath)
    override fun toLocalPath(workspace: FileReference?) = absolutePath
    override fun toUri() = URI(absolutePath)

    override fun getChild(name: String): FileReference {
        val splitIndex = absolutePath.indexOf('?')
        val basePath = if (splitIndex < 0) absolutePath else absolutePath.substring(0, splitIndex)
        return WebRef("$basePath/$name", emptyMap())
    }

    override fun inputStream(lengthLimit: Long, callback: (it: InputStream?, exc: Exception?) -> Unit) {
        callback(toURL().openStream(), null)
    }

    override fun inputStreamSync(): InputStream {
        return toURL().openStream()
    }

    override fun outputStream(append: Boolean): OutputStream {
        if (append) throw IOException("Appending isn't supported (yet?)")
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

    override fun length(): Long {
        val headers = getHeaders(toURL(), valueTimeout, false) ?: return -1
        return headers["content-length"]?.first()?.toLongOrNull() ?: -1L
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

    override fun getParent(): FileReference? {
        var endIndex = absolutePath.indexOf('?')
        if (endIndex < 0) endIndex = absolutePath.length
        if (absolutePath[endIndex - 1] == '/') endIndex--
        endIndex = absolutePath.lastIndexOf('/', endIndex)
        if (endIndex < 0) return null // we're already at the top level
        return WebRef(absolutePath.substring(0, endIndex), emptyMap())
    }

    companion object {

        val webCache = CacheSection("Web")

        fun getHeaders(url: URL, timeout: Long, async: Boolean): Map<String?, List<String>>? {
            val data = webCache.getEntry(url, timeout, async) {
                var conn: URLConnection? = null
                val data = try {
                    conn = url.openConnection()
                    if (conn is HttpURLConnection) {
                        conn.requestMethod = "HEAD"
                    }
                    conn.headerFields
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

        fun formatAccessURL(url: String, args: Map<Any?, Any?>): String {
            if ('#' in url) return formatAccessURL(url.substring(0, url.indexOf('#')), args)
            return if (args.isEmpty()) url
            else url + (if ('?' in url) "&" else "?") + args.entries.joinToString("&") { entry ->
                val (key, value) = entry
                val key2 = encodeURIComponent(key.toString())
                val val2 = encodeURIComponent(value.toString())
                "$key2=$val2"
            }
        }

        // https://stackoverflow.com/a/10032289/4979303
        /** used for the encodeURIComponent function  */
        private val allowedChars = BitSet(128)

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
            if (input == null || input.all { allowedChars.get(it.code) })
                return input
            val res = StringBuilder(input.length * 2)
            for (cp in input.codePoints()) {
                if (allowedChars.get(cp)) {
                    res.append(cp.toChar())
                } else {
                    try {
                        val b = String(intArrayOf(cp), 0, 1).toByteArray(Charsets.UTF_8)
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