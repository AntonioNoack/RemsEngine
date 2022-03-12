package me.anno.network

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket

class UDPSender(val protocol: Protocol, val client: TCPClient) {

    private val socket = DatagramSocket()
    private val address = client.socket.inetAddress

    private val bufferData = ByteArray(NetworkProtocol.UDP.limit)

    private val outBuffer = ResetByteArrayOutputStream(bufferData)
    private val dos = DataOutputStream(outBuffer)

    private val inBuffer = ResetByteArrayInputStream(bufferData)
    private val dis = DataInputStream(inBuffer)

    private val packet0 = DatagramPacket(outBuffer.buffer, outBuffer.size, address, client.udpPort)

    fun send(packet: Packet, receive: Boolean) {
        if (receive) send(packet) {}
        else send(packet, null)
    }

    fun send(packet: Packet, onReceive: ((Packet) -> Unit)?) {
        outBuffer.reset()
        dos.writeInt(protocol.bigEndianMagic)
        dos.writeInt(packet.bigEndianMagic)
        dos.writeInt(client.randomId)
        packet.send(null, client, dos)
        packet0.length = outBuffer.size
        socket.send(packet0)
        if (onReceive != null) {
            socket.receive(packet0)
            inBuffer.size = packet0.offset
            val magic = dis.readInt()
            when (val packet2 = protocol.find(magic)) {
                is Packet -> synchronized(packet2) {
                    packet2.receiveUdp(null, client, dis) {
                        send(it, false)
                    }
                    onReceive(packet2)
                }
                is ThreadLocal<*> -> {
                    val packet2i = packet2.get() as Packet
                    packet2i.receiveUdp(null, client, dis) {
                        send(it, false)
                    }
                    onReceive(packet2i)
                }
            }
        }
    }

    fun close() {
        socket.close()
    }

}