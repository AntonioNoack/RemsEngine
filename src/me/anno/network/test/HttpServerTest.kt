package me.anno.network.test

import me.anno.cache.instances.LastModifiedCache
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.http.HttpProtocol
import org.apache.logging.log4j.LogManager
import kotlin.math.log

/**
 * a sample web server, implemented on top of the server class;
 * this shows how a webserver could run on the same port as the game server
 * */
fun main() {
    // WARNING: do not copy this 1:1, as this has a lot of security flaws, e.g. you can access all files on the computer this is hosted on
    val logger = LogManager.getLogger("HttpServerTest")
    val server = Server()
    val folder = getReference("C:/XAMPP/htdocs")
    val privateExtensions = setOf("php")
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
            var file = getReference(folder, path)
            if (file.isDirectory && !file.name.startsWith('.')) file = file.getChild("index.html")
            if (!file.exists && "Referer" in meta) {
                // for my phone... awkward
                file = getReference(getReference(folder, extractPathFromURL(meta["Referer"]!!)), path)
                logger.info("Extended path from $path")
            }
            if (file.exists && !file.name.startsWith('.') && !file.isDirectory && file.lcExtension !in privateExtensions) {
                LastModifiedCache.invalidate(file)
                sendResponse(
                    client, 200, "OK", mapOf(
                        "Content-Length" to file.length(),
                        "Content-Type" to "text/html",
                        "Connection" to "close"
                    )
                )
                // copy the data
                file.inputStream().use { it.copyTo(client.dos) }
                client.dos.flush()
                logger.info("Sent $file")
            } else {
                logger.warn("$path -> $file was not found, $args, $meta")
                sendResponse(client, 404)
            }
        }
    })
    server.start(80, -1)
}