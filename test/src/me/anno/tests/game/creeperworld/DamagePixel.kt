package me.anno.tests.game.creeperworld

class DamagePixel(
    path: IntArray, color: Int,
    val onHit: () -> Unit
) : Pixel(path, color) {
    override fun onFinish(world: CreeperWorld) {
        onHit()
    }
}