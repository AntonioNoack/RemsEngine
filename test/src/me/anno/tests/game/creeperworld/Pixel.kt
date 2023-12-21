package me.anno.tests.game.creeperworld

open class Pixel(
    val path: IntArray,
    val color: Int
) {
    var progress: Int = 0
    fun isFinished() = progress >= path.lastIndex
    open fun update(world: CreeperWorld) {
        progress++
    }
    open fun onFinish(world: CreeperWorld) {
    }
}