package me.anno.network.p2prts

interface RTSState {
    fun copy(): RTSState
    fun copyInto(dst: RTSState)
    fun applyInputs(inputs: List<RTSPlayerAction>)
    fun hash(): Int
}