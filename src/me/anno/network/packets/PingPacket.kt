package me.anno.network.packets

import me.anno.gpu.GFX
import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

open class PingPacket(magic: String = "PING") : Packet(magic) {

    var localTimeNanos = localTime

    override val size: Int = 8
    override val constantSize: Boolean = true

    override fun send(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.send(server, client, dos)
        dos.writeLong(localTimeNanos)
    }

    override fun receive(server: Server?, client: TCPClient, dis: DataInputStream) {
        super.receive(server, client, dis)
        localTimeNanos = dis.readLong()
        client.localTimeNanos = localTimeNanos - localTime
        if (server != null) {// send a response
            localTimeNanos = localTime
            client.send(server, this)
        }
    }

    companion object {
        val localTime get() = System.nanoTime() - GFX.startTime
    }

}