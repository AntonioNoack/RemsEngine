package me.anno.network.p2prts

interface RTSState {
    fun copy(): RTSState
    fun copyInto(dst: RTSState)
    fun applyInputs(inputs: List<RTSAction>)
    fun hash(): Int
}