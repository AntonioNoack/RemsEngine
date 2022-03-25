package me.anno.ecs.components.player

import me.anno.network.TCPClient

// save info as local player? idk...
// at least it can be only modified by the server or via a consent mechanism
class RemotePlayer : Player() {
    // would be defined on a server
    var connection: TCPClient? = null

    override fun clone(): RemotePlayer {
        val clone = RemotePlayer()
        copy(clone)
        return clone
    }

    override val className: String = "RemotePlayer"

}