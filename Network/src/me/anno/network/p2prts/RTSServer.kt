package me.anno.network.p2prts

import org.apache.logging.log4j.LogManager
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.concurrent.thread

// todo test this class on a Baldur's Gate-like sample game

class RTSServer(val port: Int) {

    companion object {
        private val LOGGER = LogManager.getLogger(RTSServer::class)
    }

    private val clients = ConcurrentSkipListSet<RTSServerClientThread>()

    // todo we only need 1-2
    private val inputsPerFrame = ConcurrentHashMap<Int, ArrayList<RTSMessage.ClientInput>>()

    // todo we only need 1-2
    private val hashesPerFrame = ConcurrentHashMap<Int, ArrayList<RTSMessage.Hash>>()

    private var nextClientId = 0

    fun start() {
        val serverSocket = ServerSocket(port)
        LOGGER.info("Server started on $port")

        runTickLogic()
        inviteUsers(serverSocket)
    }

    fun runTickLogic() {
        thread(name = "TickLogic") {
            var frameId = 0
            while (true) {
                if (processFrame(frameId)) {
                    frameId++
                } else {
                    Thread.sleep(10)
                }
            }
        }
    }

    fun inviteUsers(serverSocket: ServerSocket) {
        while (true) {
            val socket = serverSocket.accept()
            val clientId = nextClientId++
            val client = RTSServerClientThread(socket, this, clientId)
            clients += client
            client.start()
        }
    }

    fun receiveInput(input: RTSMessage.ClientInput) {
        inputsPerFrame.computeIfAbsent(input.frameIndex, ::ArrayList).add(input)
    }

    fun receiveHash(hash: RTSMessage.Hash) {
        hashesPerFrame.computeIfAbsent(hash.frameIndex, ::ArrayList).add(hash)
    }

    fun processFrame(frame: Int): Boolean {
        val hashes = hashesPerFrame[frame] ?: return false
        val grouped = hashes.groupBy { it.hash }

        val majority = grouped.maxByOrNull { it.value.size }?.key
        if (grouped.size > 1) {
            println("Desync detected at frame $frame")
            // todo request resync from majority clients, prefer self
        }

        // todo kick any clients, which posted no input? or do we just discard theirs? (outdated)

        // Broadcast inputs for frame
        val inputs = inputsPerFrame[frame] ?: emptyList()
        broadcast(RTSMessage.ClientInputs(frame, inputs))
        return true
    }

    fun broadcast(msg: RTSMessage) {
        clients.forEach { it.send(msg) }
    }

    fun disconnect(connection: RTSServerClientThread) {
        clients.remove(connection)
    }
}