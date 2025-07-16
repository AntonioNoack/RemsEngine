package me.anno.tests.network

import me.anno.Engine
import me.anno.network.NetworkProtocol
import me.anno.network.Protocol
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.packets.MSGBroadcastPacket
import me.anno.network.packets.PingPacket
import me.anno.utils.Threads
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

    class MSGPacket : MSGBroadcastPacket() {
        override val canDropPacket: Boolean get() = false
        override fun onReceive(server: Server?, client: TCPClient) {
            super.onReceive(server, client)
            println("received $message on ${if (server != null) "server" else "client"}")
        }
        override fun showMessage(client: TCPClient) {}
    }

    val protocol = Protocol(1, NetworkProtocol.TCP)
    protocol.register(PingPacket())
    protocol.register { MSGPacket() }

    val server = Server()
    server.register(protocol)
    server.start(1025, -1)

    println("started server")

    val local = InetAddress.getByName("localhost")

    // "connect" evil client
    if (false) {
        val evilClient = TCPClient(local, server.tcpPort, protocol, "Evil")
        evilClient.dos.writeInt(protocol.bigEndianMagic)
        protocol.clientHandshake(evilClient.socket, evilClient)
        evilClient.isRunning = true
        Threads.runWorkerThread("Evil-Ping") {
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
    }

    // connect friendly client
    val friendlyClient = TCPClient(local, server.tcpPort, protocol, "Friendly")
    friendlyClient.startClientSideAsync()

    println("connected friendly")

    // broadcast a few messages
    for (i in 0 until 10000) {
        val packet = MSGPacket()
        packet.message = "[$i]"
        friendlyClient.sendTCP(packet, 50L)
    }

    println("sent messages")

    Thread.sleep(10000L)

    println("closing server & engine")

    server.close()

    Engine.requestShutdown()

    println("requested shutdown")

}

fun main() {
    test1()
}