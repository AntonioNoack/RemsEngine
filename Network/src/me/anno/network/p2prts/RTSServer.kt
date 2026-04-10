package me.anno.network.p2prts

import me.anno.Time
import me.anno.maths.Maths.SECONDS_TO_NANOS
import org.apache.logging.log4j.LogManager
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

// todo test this class on a Baldur's Gate-like sample game

class RTSServer(val port: Int) {

    companion object {
        private val LOGGER = LogManager.getLogger(RTSServer::class)
    }

    private val clients = ConcurrentSkipListSet<RTSServerClientThread>()
    private val nextFrameInputs = ConcurrentSkipListSet<RTSMessage.ClientInput>()
    private val currFrameHashes = ConcurrentHashMap<Int, RTSMessage.Hash>()

    private var nextClientId = 0
    private val frameIndex = AtomicInteger(1)

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
        nextFrameInputs.add(input)
    }

    fun receiveHash(hash: RTSMessage.Hash) {
        if (hash.frameIndex < frameIndex.get()) {
            // outdated package...
            // check it against our known hashes, and respond with it???
            //  we already sent the hash to the client...
            return
        }
        if (hash.frameIndex > frameIndex.get()) {
            // todo invalid package -> disconnect client
            return
        }
        currFrameHashes[hash.clientId] = hash
    }

    private fun waitForHashes() {
        // wait N seconds until all hashes arrive
        val timeoutTime = Time.nanoTime + 10 * SECONDS_TO_NANOS
        while (currFrameHashes.size < clients.size) {
            Thread.sleep(10)
            if (Time.nanoTime > timeoutTime) break
        }
    }

    fun processFrame(frameIndex: Int): Boolean {

        waitForHashes()
        if (currFrameHashes.isEmpty()) return false

        val hashes = HashMap(currFrameHashes)
        val grouped = hashes.entries.groupBy { it.value.hash }

        val majority = grouped.maxBy { it.value.size }
        if (grouped.size > 1) {
            println("Desync detected at frame $frameIndex")
            // request resync from majority clients
            val majorityClients = majority.value.map { it.value.clientId }
            // todo prefer client with lowest ping
            val client = clients.filter { it.clientId in majorityClients }.first()
            client.send(RTSMessage.WorldRequest(frameIndex))
            // todo wait for client response...

            for (entry in hashes) {
                if (entry.value.hash != majority.key) {
                    // todo send state-data-packet
                }
            }
        }

        // todo kick any clients, which posted no input? or do we just discard theirs? (outdated)

        // Broadcast inputs for frame
        val inputs = ArrayList<RTSMessage.ClientInput>()
        nextFrameInputs.removeIf { inputs.add(it); true }
        broadcast(RTSMessage.ClientInputs(frameIndex, inputs))
        return true
    }

    fun broadcast(msg: RTSMessage) {
        clients.forEach { it.send(msg) }
    }

    fun disconnect(connection: RTSServerClientThread) {
        clients.remove(connection)
        currFrameHashes.remove(connection.clientId)
    }
}