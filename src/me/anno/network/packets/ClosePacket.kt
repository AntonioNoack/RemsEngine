package me.anno.network.packets

import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.utils.LOGGER
import java.io.DataInputStream
import java.io.DataOutputStream

class ClosePacket(var reason: String = "", magic: String = "CLOS") : Packet(magic) {

    override val size: Int = if (reason.any { it.code !in 0 until 128 }) -1 else reason.length

    override fun sendData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        dos.writeUTF(reason)
    }

    override fun receive(server: Server?, client: TCPClient, dis: DataInputStream) {
        reason = dis.readUTF()
        // should be shown in the UI
        client.closingReason = reason
        client.close()
        LOGGER.warn("Connection was closed, because '$reason'")
    }

}