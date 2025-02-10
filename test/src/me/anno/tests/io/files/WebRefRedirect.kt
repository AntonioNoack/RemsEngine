package me.anno.tests.io.files

import me.anno.Engine
import me.anno.io.files.Reference.getReference
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.http.HttpProtocol
import me.anno.utils.assertions.assertEquals
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

object WebRefRedirect {
    private val LOGGER = LogManager.getLogger(WebRefRedirect::class)

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testAutomaticRedirect() {
        Engine.cancelShutdown()
        val port = 8081
        val server = Server()
        val content = "Hello World!"
        server.register(object : HttpProtocol("GET") {
            override fun handleRequest(
                server: Server,
                client: TCPClient,
                path: String,
                args: Map<String, String>,
                meta: Map<String, String>,
                data: ByteArray
            ) {
                LOGGER.info("Resolving $path")
                when (path) {
                    "/" -> sendResponse(
                        client, 302, getCodeName(302)!!,
                        mapOf("Location" to "/redirected")
                    )
                    "/redirected" -> sendResponse(client, content)
                    else -> sendResponse(client, 404)
                }
            }
        })
        server.start(port, 0)
        val ref = getReference("http://localhost:$port").readTextSync()
        assertEquals(ref, content)
        server.stop()
    }
}