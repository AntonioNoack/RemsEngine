package me.anno.tests.game.creeperworld

import me.anno.image.Image
import me.anno.maths.Maths
import me.anno.utils.Color
import me.anno.utils.Color.a
import org.joml.Vector2i
import kotlin.math.min

abstract class Agent(val image: Image) {

    val position = Vector2i()

    // todo fire logic and such...
    abstract fun update()

    val completeState = (0 until image.width * image.height).count {
        image.getRGB(it).a() > 0
    }
    var loadingState = completeState

    fun moveTo(world: World, newPos: Vector2i): Boolean {
        // check if pixels can find path
        // dissolve into pixels
        val oldPos = position
        // todo first check if all new positions are valid (non-rock)
        for (dy in 0 until image.height) {
            for (dx in 0 until image.width) {
                val color = image.getRGB(dx, dy)
                if (color.a() == 0) continue
                val path = findPixelPath(
                    world.rockTypes, // todo we need an array of is-solid instead, kind of...
                    Vector2i(oldPos.x + dx, oldPos.y + dy),
                    Vector2i(newPos.x + dx, newPos.y + dy)
                )
                if (path != null) {
                    world.pixels.add(Pixel(path, 0, color, this))
                    loadingState--
                } else return false
            }
        }
        // spawn into new location
        oldPos.set(newPos)
        // wait for pixels to appear...
        return true
    }

    fun render(dst: IntArray) {
        // todo if not healthy, make it transparent
        val x0 = position.x
        val y0 = position.y
        val image = image
        for (y in Maths.max(y0, 0) until min(y0 + image.height, h)) {
            for (x in Maths.max(x0, 0) until min(x0 + image.width, w)) {
                val color = image.getRGB(x - x0, y - y0)
                dst[x + y * w] = Color.mixARGB(dst[x + y * w], color, color.a())
            }
        }
    }
}
