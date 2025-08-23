package me.anno.network

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class UDPSender(val protocol: Protocol, val client: TCPClient) {

    private val socket = run {
        val channel = DatagramChannel.open()
        channel.configureBlocking(true)
        channel
    }

    private val targetAddress = InetSocketAddress(
        (client.socket.remoteAddress as InetSocketAddress).address,
        client.udpPort
    )

    private val array = ByteArray(NetworkProtocol.UDP.limit)
    private val buffer = ByteBuffer.wrap(array)

    private val outBuffer = ResetByteArrayOutputStream(array)
    private val dos = DataOutputStream(outBuffer)

    private val inBuffer = ResetByteArrayInputStream(array)
    private val dis = DataInputStream(inBuffer)

    fun send(packet: Packet, receive: Boolean) {
        if (receive) send(packet) {}
        else send(packet, null)
    }

    fun send(packet: Packet, onReceive: ((Packet) -> Unit)?) {
        outBuffer.reset()
        dos.writeInt(protocol.bigEndianMagic)
        dos.writeInt(packet.bigEndianMagic)
        dos.writeInt(client.randomId)
        packet.send(client.server, client, dos)
        dos.flush()

        buffer.position(0).limit(outBuffer.size)
        socket.send(buffer, targetAddress)

        if (onReceive != null) {

            buffer.position(0).limit(buffer.capacity())
            socket.receive(buffer)

            inBuffer.size = buffer.position()
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