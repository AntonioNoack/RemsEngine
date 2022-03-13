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
 * todo watch dogs? we only can ask for changes every x seconds
 * */
class WebRef(url: String, args: Map<Any?, Any?>) :
    FileReference(formatAccessURL(url, args)) {

    var valueTimeout = 30_000L

    // doesn't really exist, or does it?
    override val isDirectory: Boolean = false

    // todo we can answer that, when we got a response code, e.g. 404
    override val exists: Boolean
        get() = TODO("Not yet implemented")

    // todo can we ever answer that?
    override val lastModified: Long = 0L
    override val lastAccessed: Long = 0L

    fun toURL() = URL(absolutePath)

    override fun toUri(): URI {
        return URI(absolutePath)
    }

    override fun getChild(name: String): FileReference {
        val splitIndex = absolutePath.indexOf('?')
        val basePath = if (splitIndex < 0) absolutePath else absolutePath.substring(0, splitIndex)
        return WebRef("$basePath/$name", emptyMap())
    }

    override fun inputStream(): InputStream {
        return toURL().openStream()
    }

    override fun outputStream(): OutputStream {
        // in the future, we might use the Apache HTTP API
        // a simple helper function,
        // may be incomplete for your purposes, e.g.
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
        val data = webCache.getEntry(absolutePath, valueTimeout, false) {
            CacheData(getFileSize(toURL()))
        } as? CacheData<*>
        return data?.value as? Long ?: -1L
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

        /** returns -1 if the length is unknown */
        private fun getFileSize(url: URL): Long {
            var conn: URLConnection? = null
            return try {
                conn = url.openConnection()
                if (conn is HttpURLConnection) {
                    conn.requestMethod = "HEAD"
                }
                conn.getInputStream()
                conn.contentLengthLong
            } catch (e: IOException) {
                throw RuntimeException(e)
            } finally {
                if (conn is HttpURLConnection) {
                    conn.disconnect()
                }
            }
        }

        val webCache = CacheSection("Web")

        fun formatAccessURL(url: String, args: Map<Any?, Any?>): String {
            if ('#' in url) return formatAccessURL(url.substring(0, url.indexOf('#')), args)
            return url + (if ('?' in url) "&" else "?") + args.entries.joinToString("&") { entry ->
                val (key, value) = entry
                val key2 = encodeURIComponent(key.toString())
                val val2 = encodeURIComponent(value.toString())
                "$key2=$val2"
            }
        }

        // https://stackoverflow.com/a/10032289/4979303
        /** used for the encodeURIComponent function  */
        private val allowedChars = BitSet(256)

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
                    val b = intToUTF8Bytes(cp)
                    for (j in b.indices) {
                        res.append('%')
                        res.append(hex4(b[j].toInt().shr(4)))
                        res.append(hex4(b[j].toInt()))
                    }
                }
            }
            return res.toString()
        }

        private fun intToUTF8Bytes(cp: Int): ByteArray {
            return try {
                String(intArrayOf(cp), 0, 1).toByteArray(Charsets.UTF_8)
            } catch (e: UnsupportedEncodingException) {
                byteArrayOf(cp.toByte())
            }
        }
    }

}