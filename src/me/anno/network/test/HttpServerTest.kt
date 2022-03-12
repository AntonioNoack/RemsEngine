package me.anno.network.test

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.http.HttpProtocol
import me.anno.utils.LOGGER

fun main() {
    // a sample web server, implemented on top of the server class
    val server = Server()
    val folder = getReference("C:/XAMPP/htdocs")
    server.register(object : HttpProtocol("GET") {
        override fun handleRequest(
            server: Server,
            client: TCPClient,
            path: String,
            meta: HashMap<String, String>,
            data: ByteArray
        ) {
            var file = getReference(folder, path)
            if (file.isDirectory) file = file.getChild("index.html")
            if (file.exists && !file.isDirectory) {
                sendResponse(
                    client, 200, "OK", mapOf(
                        "Content-Length" to file.length(),
                        "Content-Type" to "text/html",
                        "Connection" to "close"
                    )
                )
                // copy the data
                file.inputStream().use { it.copyTo(client.dos) }
            } else {
                LOGGER.warn("$path -> $file was not found")
                sendResponse(client, 404)
            }
        }
    })
    server.start(80, -1)
}