package me.anno.network.packets

import me.anno.Time
import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

open class PingPacket(magic: String = "PING") : Packet(magic) {

    override val size = 8
    override val constantSize = true

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        dos.writeLong(Time.nanoTime)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        val localTimeNanos0 = dis.readLong()
        // waste no time, and execute this immediately
        val localTimeNanos = Time.nanoTime
        client.localTimeOffset = localTimeNanos0 - localTimeNanos
    }

    override fun onReceive(server: Server?, client: TCPClient) {
        if (server != null) {// send a response
            client.sendTCP(this)
        }
    }

    override fun onReceiveUDP(server: Server?, client: TCPClient, sendResponse: (packet: Packet) -> Unit) {
        if (server != null) {// send a response
            sendResponse(this)
        }
    }

    override fun toString() = "PingPacket"
}