package me.anno.network.p2prts

/**
 * whatever the player logic can decide to do
 * */
class RTSPlayerAction(
    val actionType: Int,
    val payload: ByteArray // keep generic (serialized input)
)