package me.anno.tests.network

import me.anno.io.Streams.writeString
import me.anno.io.files.WebRef
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.http.HttpProtocol
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * test the sample web server
 * */
class HttpServerUnitTest {

    companion object {
        private val port = AtomicInteger(4123)
        private val files = mapOf("/sample.html" to "this is the test file")
    }

    private val server = Server()

    private class TestProtocol(method: String) : HttpProtocol(method) {
        override fun handleRequest(
            server: Server,
            client: TCPClient,
            path: String,
            args: Map<String, String>,
            meta: Map<String, String>,
            data: ByteArray
        ) {
            val file = files[path]
            if (file != null) {
                sendResponse(client, 200, "OK", file)
            } else {
                val msg = "<h1>404 - File Not Found!</h1>"
                sendResponse(client, 404, "Not Found", msg)
            }
        }

        private fun sendResponse(client: TCPClient, code: Int, codeName: String, msg: String) {
            sendResponse(
                client, code, codeName, mapOf(
                    "Content-Length" to msg.length,
                    "Content-Type" to "text/html",
                    "Connection" to "close"
                )
            )
            if (method != "HEAD") {
                client.dos.writeString(msg)
            }
            client.dos.flush()
        }
    }

    @BeforeEach
    fun startServer() {
        server.register(TestProtocol("GET")) // for getting content
        server.register(TestProtocol("HEAD")) // for getting metadata only
        server.start(port.getAndIncrement(), -1)
    }

    @Test
    fun testOK() {
        val (sample) = files.toList()
        val file = WebRef("http://localhost:${server.tcpPort}${sample.first}")
        assertTrue(file.exists)
        val answer = file.readTextSync()
        assertEquals(sample.second, answer)
    }

    @Test
    fun testNotFound() {
        val file = WebRef("http://localhost:${server.tcpPort}/404.html")
        assertFalse(file.exists)
    }

    @AfterEach
    fun stopServer() {
        server.stop()
    }
}