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
    val datagramPacket = DatagramPacket(buffer, buffer.size, address, port)
    val bos = ResetByteArrayOutputStream(buffer)
    val dos = DataOutputStream(bos)
    val bis = ResetByteArrayInputStream(buffer)
    val dis = DataInputStream(bis)

    fun send(server: Server?, client: TCPClient, protocol: Protocol, packet: Packet) {
        if (protocol.find(packet.bigEndianMagic) == null) throw UnregisteredPacketException(packet)
        synchronized(this) {
            dos.writeInt(protocol.bigEndianMagic)
            dos.writeInt(packet.bigEndianMagic)
            dos.writeInt(client.randomId)
            packet.send(server, client, dos)
            dos.flush()
            datagramPacket.length = bos.size
            socket.send(datagramPacket)
        }
    }

    /**
     * after sending you need to synchronize with this instance, so you read the correct data
     * */
    fun receive(server: Server?, client: TCPClient, packet1: Packet): Packet {
        socket.receive(datagramPacket)
        bis.reset()
        bis.skip(4) // skip packet id, assume it's correct
        packet1.readData(server, client, dis, datagramPacket.length - 4)
        return packet1
    }

    /**
     * reads the packet from the data
     * */
    fun receive(server: Server?, client: TCPClient, protocol: Protocol, callback: (Packet) -> Unit) {
        socket.receive(datagramPacket)
        bis.reset()
        val packetId = dis.readInt()
        when (val packet = protocol.find(packetId)) {
            is Packet -> synchronized(packet) {
                packet.readData(server, client, dis, packet.size - 4)
                callback(packet)
            }
            is ThreadLocal<*> -> {
                val packet2 = packet.get() as Packet
                packet2.readData(server, client, dis, packet2.size - 4)
                callback(packet2)
            }
            else -> throw RuntimeException("Unknown packet ${str32(packetId)}")
        }
    }

    override fun close() {
        socket.close()
    }

}