package me.anno.network.http

import me.anno.network.NetworkProtocol
import me.anno.network.Protocol
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.utils.Warning.unused
import me.anno.utils.input.Input.readNBytes2
import java.io.IOException
import kotlin.math.max

abstract class HttpProtocol(val method: String, val maxCapacity: Int = 1_000_000) :
    Protocol(methodToMagic(method), NetworkProtocol.TCP) {

    override fun serverHandshake(server: Server, client: TCPClient, magic: Int): Boolean {
        handleRequest(server, client, magic)
        return false // http clients are not registered
    }

    private fun handleRequest(server: Server, client: TCPClient, magic: Int) {
        unused(magic)
        val ri = client.dis.bufferedReader()
        val header = ri.readLine()!! // 1.1 200 OK
        val si = header.indexOf(' ')
        if (si < 0) throw IOException("Invalid header")
        val path = header.substring(0, si)
        // version = header.substring(si+1)
        val meta = HashMap<String, String>()
        while (true) {
            val line = ri.readLine() ?: break
            if (line.isEmpty()) break // end of header
            val ix = line.indexOf(": ")
            if (ix >= 0) {
                val key = line.substring(0, ix).trim()
                val value = line.substring(ix + 2).trim()
                meta[key] = value
            }
        }
        // read the rest as binary
        val capacity = meta["Content-Length"]?.toIntOrNull() ?: 0
        if (capacity in 0..maxCapacity) {
            val data = client.dis.readNBytes2(capacity, false)
            try {
                handleRequest(server, client, path, meta, data)
            } catch (e: Exception) {
                e.printStackTrace()
                sendResponse(client, 500)
            }
        } else sendResponse(client, 413)
        client.close()
    }

    /**
     * a function, that should call sendResponse()
     * */
    abstract fun handleRequest(
        server: Server,
        client: TCPClient,
        path: String,
        meta: HashMap<String, String>,
        data: ByteArray
    )

    fun sendResponse(client: TCPClient, message: String) {
        sendResponse(
            client, 200, getCodeName(200)!!, mapOf(
                "Server" to "Rem's Engine",
                "Content-Type" to "text/html",
                "Content-Length" to message.length,
                "Connection" to "close"
            )
        )
        client.dos.writeBytes(message)
    }

    fun sendResponse(client: TCPClient, code: Int) {
        sendResponse(
            client, code, getCodeName(code)!!, mapOf(
                "Server" to "Rem's Engine",
                "Content-Length" to "0",
                "Content-Type" to "text/html",
                "Connection" to "close"
            )
        )
    }

    fun sendResponse(client: TCPClient, code: Int, codeName: String, meta: Map<String, Any>, data: ByteArray?) {
        sendResponse(client, code, codeName, meta)
        if (data != null) {
            client.dos.write(data)
        }
    }

    fun sendResponse(client: TCPClient, code: Int, codeName: String, meta: Map<String, Any>) {
        val dos = client.dos
        val ln = "\r\n"
        dos.writeBytes("HTTP/1.1 ")
        if (code in 100 until 1000) {
            // response code without converting it to a string
            dos.writeByte(((code / 100) % 100) + 48)
            dos.writeByte(((code / 10) % 10) + 48)
            dos.writeByte((code % 10) + 48)
        } else {
            // should not happen
            dos.writeBytes(code.toString())
        }
        dos.writeBytes(" ")
        dos.writeBytes(codeName)
        dos.writeBytes(ln)
        for ((key, value) in meta) {
            dos.writeBytes(key)
            dos.writeBytes(": ")
            dos.writeBytes(value.toString())
            dos.writeBytes(ln)
        }
        dos.writeBytes(ln)
    }

    companion object {
        private fun methodToMagic(method: String): String {
            return when (method.length) {
                0 -> "    "
                1 -> "$method   "
                2 -> "$method  "
                3 -> "$method "
                4 -> method
                else -> method.substring(0, 4)
            }
        }

        fun getCodeName(code: Int): String? {
            // from https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
            return when (code) {
                100 -> "Continue"
                101 -> "Switching Protocols"
                102 -> "Processing"
                103 -> "Early Hints"
                200 -> "OK"
                201 -> "Created"
                202 -> "Accepted"
                203 -> "Non-Authoritative Information"
                204 -> "No Content"
                205 -> "Reset Content"
                206 -> "Partial Content"
                207 -> "Multi-Status"
                208 -> "Already Reported"
                226 -> "IM Used"
                300 -> "Multiple Choice"
                301 -> "Moved Permanently"
                302 -> "Found"
                303 -> "See Other"
                304 -> "Not Modified"
                305 -> "Use Proxy" // deprecated
                // 306 -> "unused" // just reserved
                307 -> "Temporary Redirect"
                308 -> "Permanent Redirect"
                400 -> "Bad Request"
                401 -> "Unauthorized"
                402 -> "Payment Required" // experimental
                403 -> "Forbidden"
                404 -> "Not Found"
                405 -> "Method Not Allowed" // e.g. delete may be forbidden
                406 -> "Not Acceptable"
                407 -> "Proxy Authentication Required"
                408 -> "Request Timeout"
                409 -> "Conflict"
                410 -> "Gone"
                411 -> "Length Required"
                412 -> "Precondition Failed"
                413 -> "Payload Too Large"
                414 -> "URI Too Long"
                415 -> "Unsupported Media Type"
                416 -> "Range Not Satisfiable"
                417 -> "Expectation Failed"
                418 -> "I'm a teapot"
                421 -> "Misdirected Request"
                422 -> "Unprocessable Entity" // web dav
                423 -> "Locked" // web dav
                424 -> "Failed Dependency" // web dav
                425 -> "Too Early" // experimental
                426 -> "Upgrade Required"
                428 -> "Precondition Required"
                429 -> "Too Many Requests"
                431 -> "Request Header Fields Too Large"
                451 -> "Unavailable For Legal Reasons"
                500 -> "Internal Server Error"
                501 -> "Not Implemented"
                502 -> "Bad Gateway"
                503 -> "Service Unavailable"
                504 -> "Gateway Timeout"
                505 -> "HTTP Version Not Supported"
                506 -> "Variant Also Negotiates"
                507 -> "Insufficient Storage" // web dav
                508 -> "Loop Detected" // web dav
                510 -> "Not Extended"
                511 -> "Network Authentication Required"
                else -> null
            }
        }
    }

}