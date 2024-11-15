package me.anno.games.creeperworld

import me.anno.image.Image
import me.anno.maths.Maths
import me.anno.maths.paths.PathFinding
import me.anno.utils.Color
import me.anno.utils.Color.a
import org.joml.Vector2i
import kotlin.math.min

abstract class Agent(val image: Image) {

    val position = Vector2i()

    // todo fire logic and such...
    abstract fun update(world: CreeperWorld)

    val completeState = (0 until image.width * image.height).count {
        image.getRGB(it).a() > 0
    }
    var loadingState = completeState

    fun isVisibleAt(x: Int, y: Int): Boolean {
        val lx = x - position.x
        val ly = y - position.y
        return if (lx in 0 until image.width && ly in 0 until image.height) {
            image.getRGB(lx, ly).a() != 0
        } else false
    }

    fun moveTo(world: CreeperWorld, newPos: Vector2i): Boolean {

        // todo reroute pixels instead?
        if (loadingState < completeState) return false

        // check if pixels can find path
        // dissolve into pixels
        val oldPos = position
        // first check if all new positions are valid (non-rock)
        val rockTypes = world.rockTypes
        val w = world.w
        for (dy in 0 until image.height) {
            for (dx in 0 until image.width) {
                val color = image.getRGB(dx, dy)
                if (color.a() == 0) continue
                if (rockTypes[newPos.x + dx + w * (newPos.y + dy)] != 0) {
                    return false
                }
            }
        }

        fun spawnParticle(dx: Int, dy: Int): Boolean {
            val color = image.getRGB(dx, dy)
            if (color.a() == 0) return false
            val path = findPixelPath(
                world,
                rockTypes, // todo we need an array of is-solid instead, kind of...
                Vector2i(oldPos.x + dx, oldPos.y + dy),
                Vector2i(newPos.x + dx, newPos.y + dy)
            )
            return if (path != null) {
                world.add(AgentPixel(path,  color, this))
                loadingState--
                false
            } else true
        }

        // decide iteration order based on delta-position for faster movement
        fun spawnParticlesX(dy: Int): Boolean {
            if (oldPos.x > newPos.x) {
                for (dx in 0 until image.width) {
                    if (spawnParticle(dx, dy)) return true
                }
            } else {
                for (dx in image.width - 1 downTo 0) {
                    if (spawnParticle(dx, dy)) return true
                }
            }
            return false
        }
        if (oldPos.y > newPos.y) {
            for (dy in 0 until image.height) {
                if (spawnParticlesX(dy)) return true
            }
        } else {
            for (dy in image.height - 1 downTo 0) {
                if (spawnParticlesX(dy)) return true
            }
        }

        // spawn into new location
        oldPos.set(newPos)
        // wait for pixels to appear...
        return true
    }

    fun render(world: CreeperWorld, dst: IntArray) {
        val x0 = position.x
        val y0 = position.y
        val image = image
        // if not healthy, make it transparent
        val alpha = loadingState * 128 / completeState + 128
        val w = world.w
        val h = world.h
        for (y in Maths.max(y0, 0) until min(y0 + image.height, h)) {
            for (x in Maths.max(x0, 0) until min(x0 + image.width, w)) {
                val color = image.getRGB(x - x0, y - y0)
                dst[x + y * w] = Color.mixARGB(dst[x + y * w], color, (color.a() * alpha) ushr 8)
            }
        }
    }

    fun findPixelPath(
        world: CreeperWorld,
        rockTypes: IntArray,
        start: Vector2i,
        end: Vector2i,
    ): IntArray? {
        val path = PathFinding.aStar(
            start, end, start.gridDistance(end).toDouble(),
            Int.MAX_VALUE.toDouble(), 64,
            includeStart = true, includeEnd = true,
        ) { node, callback ->
            val w = world.w
            val h = world.h
            // query all neighbors
            fun call(dx: Int, dy: Int) {
                val x = node.x + dx
                val y = node.y + dy
                val i = x + y * w
                if (rockTypes[i] == 0) {
                    val to = Vector2i(x, y)
                    callback(to, 1.0, to.gridDistance(end).toDouble())
                }
            }
            if (node.x > 0) call(-1, 0)
            if (node.x < w - 1) call(+1, 0)
            if (node.y > 0) call(0, -1)
            if (node.y < h - 1) call(0, +1)
        } ?: return null
        return IntArray(path.size) { index ->
            val node = path[index]
            node.x + node.y * world.w
        }
    }
}
