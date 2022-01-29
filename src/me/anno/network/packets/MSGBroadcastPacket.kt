package me.anno.network.packets

import me.anno.network.Server
import me.anno.network.TCPClient

abstract class MSGBroadcastPacket(magic: String = "MSG1") : MSG0Packet(magic) {

    override fun onReceive(server: Server?, client: TCPClient) {
        if (server != null) {
            server.broadcast(this)
        } else {
            showMessage(client)
        }
    }

    abstract fun showMessage(client: TCPClient)

}