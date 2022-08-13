package me.anno.tests.network

import me.anno.Engine
import me.anno.network.NetworkProtocol
import me.anno.network.Protocol
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.packets.MSGBroadcastPacket
import me.anno.network.packets.POS0Packet
import me.anno.network.packets.PingPacket
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

// 673 packets are sent, then it hangs
fun test2() {
    val port = 4444
    Thread {
        try {
            val serverSocket = ServerSocket(port)
            val clientSocket = serverSocket.accept()
            clientSocket.getInputStream()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }.start()
    val client = Socket("localhost", port)
    val outputStream = client.getOutputStream()
    var packet = 0
    val data = ByteArray(1024 * 4)
    while (true) {
        println(++packet)
        outputStream.write(data)
    }
}

fun test1() {
    // the default buffer length is 1024-2048,
    // so send 512 maybe 10 times to all clients
    // one of which won't read, because it's malicious

    var ctr = 0

    class MSGPacket : MSGBroadcastPacket() {
        override fun showMessage(client: TCPClient) {
            println("msg[${++ctr}] (${client.name}): ${message.length}x")
        }
    }

    val protocol = Protocol(1, NetworkProtocol.TCP)
    protocol.register(PingPacket())
    protocol.register { POS0Packet() }
    protocol.register { MSGPacket() }

    val server = Server()
    server.register(protocol)
    server.start(1025, -1)

    println("started server")

    val local = InetAddress.getByName("localhost")

    // "connect" evil client
    val evilSocket = TCPClient.createSocket(local, server.tcpPort, protocol)
    val evilClient = TCPClient(evilSocket, protocol, "Evil")
    evilClient.dos.writeInt(protocol.bigEndianMagic)
    protocol.clientHandshake(evilSocket, evilClient)
    evilClient.isRunning = true
    thread(name = "Evil-Ping") {
        // keep the connection alive
        try {
            while (!Engine.shutdown && !evilClient.isClosed) {
                evilClient.sendTCP(PingPacket())
                Thread.sleep(10)
            }
        } catch (_: IOException) {
        }
    }

    println("connected evil")

    // connect friendly clients
    val friendlySocket = TCPClient.createSocket(local, server.tcpPort, protocol)
    val friendlyClient = TCPClient(friendlySocket, protocol, "Friendly")
    friendlyClient.startClientSideAsync()

    println("connected friendly")

    val longText = String(CharArray(1024 * 4) { 'a' })
    val packet = MSGPacket().apply { message = longText }
    // broadcast a few very long messages
    for (i in 0 until 1000) {
        friendlyClient.sendTCP(packet)
    }

    println("sent messages")

    Thread.sleep(1000L)

    server.close()

    Engine.requestShutdown()

    println("requested shutdown")

}

fun main() {
    test1()
}