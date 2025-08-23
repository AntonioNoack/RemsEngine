package me.anno.tests.network

import me.anno.io.files.LastModifiedCache
import me.anno.io.files.Reference.getReference
import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.http.HttpProtocol
import me.anno.utils.Sleep
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

/**
 * a sample web server, implemented on top of the server class;
 * this shows how a webserver could run on the same port as the game server
 * */
fun main() {

    Packet.debugPackets = true
    LogManager.define("Server", Level.DEBUG)

    // WARNING: do not copy this 1:1, as this has a lot of security flaws, e.g., you can access all files on the computer this is hosted on
    val logger = LogManager.getLogger("HttpServerTest")
    val server = Server()
    val folder = getReference("C:/XAMPP/htdocs")
    val publicExtensions = "html,htm,js,json,png,webp,jpg,jpeg,ico".split(',')
    fun extractPathFromURL(url: String): String {
        return url.substring(url.indexOf('/', "https://".length))
    }
    server.register(object : HttpProtocol("GET") {
        override fun handleRequest(
            server: Server,
            client: TCPClient,
            path: String,
            args: Map<String, String>,
            meta: Map<String, String>,
            data: ByteArray
        ) {
            var file = folder.getChild(path)
            if (file.isDirectory && !file.name.startsWith('.')) file = file.getChild("index.html")
            if (!file.exists && "Referer" in meta) {
                // for my phone... awkward
                file = folder.getChild(extractPathFromURL(meta["Referer"]!!)).getChild(path)
                logger.info("Extended path from $path")
            }
            if (file.lcExtension in publicExtensions && file.exists && !file.name.startsWith('.') && !file.isDirectory) {
                LastModifiedCache.invalidate(file)
                sendResponse(
                    client, 200, "OK", mapOf(
                        "Content-Length" to file.length(),
                        "Content-Type" to "text/html",
                        "Connection" to "close"
                    )
                )
                // copy the data
                file.inputStreamSync().use { it.copyTo(client.dos) }
                client.dos.flush()
                logger.info("Sent $file")
            } else {
                logger.warn("$path -> $file was not found, $args, $meta")
                val msg = "<h1>404 - File Not Found!</h1>"
                sendResponse(
                    client, 404, "Not Found", mapOf(
                        "Content-Length" to msg.length,
                        "Content-Type" to "text/html",
                        "Connection" to "close"
                    )
                )
                client.dos.write(msg.toByteArray())
            }
        }
    })

    val tcpPort = 80
    println("Running server on port $tcpPort")
    server.start(tcpPort, -1)

    while (true) {
        Sleep.work(true)
    }
}