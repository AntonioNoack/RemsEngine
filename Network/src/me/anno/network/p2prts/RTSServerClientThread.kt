package me.anno.network.p2prts

import java.io.IOException
import java.net.Socket

class RTSServerClientThread(
    private val socket: Socket,
    private val server: RTSServer,
    val clientId: Int,
) : Thread() {

    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    override fun run() {
        try {
            while (true) {
                when (val msg = RTSMessage.read(input, clientId)) {
                    is RTSMessage.ClientInput -> server.receiveInput(msg)
                    is RTSMessage.Hash -> server.receiveHash(msg)
                    else -> {}
                }
            }
        } catch (_: IOException) {
            server.disconnect(this)
        }
    }

    fun send(msg: RTSMessage) {
        msg.write(output)
    }
}