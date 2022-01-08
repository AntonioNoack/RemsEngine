package me.anno.network

import me.anno.network.Server.Companion.str32
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UDPClient(address: InetAddress, port: Int, timeoutMillis: Int = 10_000) : Closeable {

    val socket = DatagramSocket()
        .apply { soTimeout = timeoutMillis }

    val buffer = ByteArray(NetworkProtocol.UDP.limit)
    val packet = DatagramPacket(buffer, buffer.size, address, port)
    val bos = ResetByteArrayOutputStream(buffer)
    val dos = DataOutputStream(bos)
    val bis = ResetByteArrayInputStream(buffer)
    val dis = DataInputStream(bis)

    fun send(server: Server?, client: TCPClient, protocol: Protocol, packet1: Packet) {
        synchronized(this) {
            dos.writeInt(protocol.bigEndianMagic)
            dos.writeInt(packet1.bigEndianMagic)
            dos.writeInt(client.randomId)
            packet1.send(server, client, dos)
            dos.flush()
            packet.length = bos.size
            socket.send(packet)
        }
    }

    /**
     * after sending you need to synchronize with this instance, so you read the correct data
     * */
    fun receive(server: Server?, client: TCPClient, packet1: Packet): Packet {
        socket.receive(packet)
        bis.reset()
        bis.skip(4) // skip packet id, assume it's correct
        packet1.receiveData(server, client, dis, packet.length - 4)
        return packet1
    }

    /**
     * after sending you need to synchronize with this instance, so you read the correct data
     * reads the packet from the data
     * */
    fun receive(server: Server?, client: TCPClient, protocol: Protocol): Packet {
        socket.receive(packet)
        bis.reset()
        val packetId = dis.readInt()
        val packet = protocol.packets[packetId] ?: throw RuntimeException("Unknown packet ${str32(packetId)}")
        packet.receiveData(server, client, dis, packet.size - 4)
        return packet
    }

    override fun close() {
        socket.close()
    }

}