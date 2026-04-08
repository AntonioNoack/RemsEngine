package me.anno.network.p2prts

import me.anno.io.Streams.readLE32
import java.net.Socket

class RTSClient(
    private val socket: Socket,
    private val server: RTSServer
) : Thread() {

    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    // todo verify magic/handshake

    val clientId = input.readLE32()

    override fun run() {
        while (true) {
            when (val msg = RTSMessage.read(input, clientId)) {
                is RTSMessage.ClientInput -> server.receiveInput(msg)
                is RTSMessage.Hash -> server.receiveHash(msg)
                else -> {}
            }
        }
    }

    fun send(msg: RTSMessage) {
        msg.write(output)
    }
}