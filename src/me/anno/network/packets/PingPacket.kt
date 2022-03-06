package me.anno.network.packets

import me.anno.Engine
import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

open class PingPacket(magic: String = "PING") : Packet(magic) {

    var localTimeNanos = Engine.nanoTime

    override val size: Int = 8
    override val constantSize: Boolean = true

    override fun sendData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        dos.writeLong(localTimeNanos)
    }

    override fun receiveData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        localTimeNanos = dis.readLong()
        val localTimeNanos = Engine.nanoTime
        client.localTimeOffset = this.localTimeNanos - localTimeNanos
        if (server != null) {// send a response
            this.localTimeNanos = localTimeNanos
            client.send(server, this)
        }
    }

}