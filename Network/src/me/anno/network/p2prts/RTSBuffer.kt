package me.anno.network.p2prts

class RTSBuffer(initial: RTSState) {
    var current = initial
    var previous = initial.copy()

    fun beginNextFrame() {
        current.copyInto(previous)
    }

    fun apply(inputs: List<RTSPlayerAction>) {
        current.applyInputs(inputs)
    }
}