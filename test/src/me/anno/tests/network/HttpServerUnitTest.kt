package me.anno.tests.network

import me.anno.Engine
import me.anno.io.Streams.writeString
import me.anno.io.files.WebRef
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.http.HttpProtocol
import me.anno.tests.network.NetworkTests.nextPort
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * test the sample web server
 * */
class HttpServerUnitTest {

    companion object {
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
        Engine.cancelShutdown()
        server.register(TestProtocol("GET")) // for getting content
        server.register(TestProtocol("HEAD")) // for getting metadata only
        server.start(nextPort(), -1)
    }

    @Test
    fun testOK() {
        val (path, contents) = files.entries.first()
        val file = WebRef("http://localhost:${server.tcpPort}$path")
        assertTrue(file.exists)
        val answer = file.readTextSync()
        assertEquals(contents, answer)
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