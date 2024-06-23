package me.anno.network

import me.anno.ecs.components.player.Player

// save info as local player? idk...
// at least it can be only modified by the server or via a consent mechanism
class RemotePlayer : Player() {
    // would be defined on a server
    var connection: TCPClient? = null
}