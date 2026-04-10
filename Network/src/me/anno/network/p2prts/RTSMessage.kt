package me.anno.network.p2prts

import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readNBytes2
import me.anno.io.Streams.writeLE32
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

sealed class RTSMessage(val frameIndex: Int) {

    /**
     * C->S after a frame calculation has finished
     * */
    class Hash(frameIndex: Int, val clientId: Int, val hash: Int) : RTSMessage(frameIndex)

    /**
     * C->S, what happened within the last frame
     * */
    class ClientInput(frameIndex: Int, val clientId: Int, val commands: List<RTSAction>) : RTSMessage(frameIndex)

    /**
     * S->C, which actions were accepted by the server; for each player
     * */
    class ClientInputs(frameIndex: Int, val commandsByPlayers: List<ClientInput>) : RTSMessage(frameIndex)

    /**
     * S->C, ping, "please send me the world state"
     * */
    class WorldRequest(frameIndex: Int) : RTSMessage(frameIndex)

    /**
     * C->S, pong, "here is the world state"
     * */
    class WorldResponse(frameIndex: Int, val payload: ByteArray) : RTSMessage(frameIndex)

    fun write(out: OutputStream) {
        out.writeLE32(frameIndex)
        val type = when (this) {
            is Hash -> 1
            is ClientInput -> 2
            is ClientInputs -> 3
            is WorldRequest -> 4
            is WorldResponse -> 5
        }
        out.write(type)
        when (this) {
            is ClientInput -> write(out, commands)
            is Hash -> out.writeLE32(hash)
            is ClientInputs -> {
                out.writeLE32(commandsByPlayers.size)
                for (command in commandsByPlayers) {
                    out.writeLE32(command.clientId)
                    write(out, command.commands)
                }
            }
            is WorldRequest -> {}
            is WorldResponse -> {
                out.writeLE32(payload.size)
                out.write(payload)
            }
        }
        out.flush()
    }

    private fun write(out: OutputStream, commands: List<RTSAction>) {
        out.writeLE32(commands.size)
        for (command in commands) {
            write(out, command)
        }
    }

    private fun write(out: OutputStream, command: RTSAction) {
        out.writeLE32(command.actionType)
        out.writeLE32(command.payload.size)
        out.write(command.payload)
    }

    companion object {
        fun read(input: InputStream, clientId: Int): RTSMessage {
            val frameIndex = input.readLE32()
            return when (val type = input.read()) {
                1 -> Hash(frameIndex, clientId, input.readLE32())
                2 -> readCommands(input, frameIndex, clientId)
                3 -> {
                    val commandsByPlayers = List(input.readLE32()) {
                        val playerId = input.readLE32()
                        readCommands(input, frameIndex, playerId)
                    }
                    ClientInputs(frameIndex, commandsByPlayers)
                }
                else -> throw IOException("Invalid packet type: $type")
            }
        }

        private fun readCommands(input: InputStream, frameIndex: Int, clientId: Int): ClientInput {
            val commands = List(input.readLE32()) {
                readCommand(input)
            }
            return ClientInput(frameIndex, clientId, commands)
        }

        private fun readCommand(input: InputStream): RTSAction {
            val commandType = input.read()
            val payload = ByteArray(input.readLE32())
            input.readNBytes2(payload.size, payload, true)
            return RTSAction(commandType, payload)
        }
    }
}