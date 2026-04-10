package me.anno.network.p2prts

/**
 * whatever the client logic can decide to do
 * */
class RTSAction(
    val actionType: Int,
    val payload: ByteArray // keep generic (serialized input)
)