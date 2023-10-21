package me.anno.tests.physics.fluid

class RWState<V>(init: () -> V) {
    var read = init()
    var write = init()
    fun swap() {
        val tmp = read
        read = write
        write = tmp
    }
}