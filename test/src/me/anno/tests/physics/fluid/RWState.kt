package me.anno.tests.physics.fluid

class RWState<V>(init: (Int) -> V) {
    var read = init(0)
    var write = init(1)
    fun swap() {
        val tmp = read
        read = write
        write = tmp
    }
}