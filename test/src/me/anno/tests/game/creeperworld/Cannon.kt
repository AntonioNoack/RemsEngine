package me.anno.tests.game.creeperworld

import me.anno.image.Image
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.roundDiv
import me.anno.maths.Maths.sq
import me.anno.utils.Color.black
import me.anno.utils.structures.lists.Lists.cross
import org.joml.Vector2i.Companion.length
import kotlin.math.*

class Cannon(
    image: Image,
    val fluid: FluidFramebuffer,
    val pattern: AttackPattern = AttackPattern()
) : Agent(image) {

    class Target1(val dx: Int, val dy: Int, val i0: Int, val i1: Int, val rating: Float)

    class AttackPattern(
        val minRadius: Int = 5,
        val maxRadius: Int = 50
    ) {

        val range = (-maxRadius..maxRadius).toList()
        val isBlockSize = (maxRadius * PI).toInt()
        val isBlocked = BooleanArray(isBlockSize)
        val order = range.cross(range)
            .filter { abs(it.first) + abs(it.second) in minRadius..maxRadius }
            .map {
                val angle = atan2(it.second.toDouble(), it.first.toDouble()) + PI
                val distance = length(it.first, it.second)
                val di = max(1, ceil(isBlockSize / distance).toInt())
                val idx0 = min((angle * isBlockSize / TAU).toInt(), isBlockSize - di)
                Target1(
                    it.first, it.second,
                    idx0, idx0 + di,
                    (1.0 / distance).toFloat()
                )
            }
            .sortedByDescending { it.rating }
    }

    var ammunition = Int.MAX_VALUE // todo make this finite, and use resources to fill it back up
    var timeout = 0

    override fun update(world: CreeperWorld) {
        if (loadingState == completeState && timeout-- <= 0) {
            if (ammunition > 0) {
                ammunition--
                shoot(world)
            }
            timeout = 10
        }
    }

    fun shoot(world: CreeperWorld) {
        // todo shoot something
        // todo find where to shoot: as close as possible, but still not null
        //  -> highest score, or good enough
        //  - must be in line of sight
        val cx = position.x + image.width / 2
        val cy = position.y + image.height / 2
        val isBlocked = pattern.isBlocked
        isBlocked.fill(false)

        val rockType = world.rockTypes

        var bestScore = 0f
        var bestI = 0
        val level = fluid.level.read
        val w = world.w
        val h = world.h
        targets@ for (target in pattern.order) {
            // if is oob, cancel
            val x = cx + target.dx
            val y = cy + target.dy
            if (x in 0 until w && y in 0 until h) {
                // if cannot see that block, cancel
                for (ii in target.i0 until target.i1) {
                    if (isBlocked[ii]) continue@targets
                }
                val i = x + y * w
                if (rockType[i] != 0) {
                    // if is blocked, mark as such
                    isBlocked.fill(true, target.i0, target.i1)
                } else {
                    // register candidate
                    val li = level[i]
                    val score = li * target.rating
                    if (score > bestScore) {
                        bestScore = score
                        bestI = i
                    }
                    // if candidate is good enough, cancel
                    if (li >= 0.7f) {
                        break@targets
                    }
                }
            }
        }
        if (bestScore > 0) {
            // fire a rocket at the target... pixels???
            val tx = bestI % w
            val ty = bestI / w
            val len = max(
                abs(tx - cx),
                abs(ty - cy)
            )
            val path = IntArray(len) {
                val x = cx + roundDiv((tx - cx) * it, len)
                val y = cy + roundDiv((ty - cy) * it, len)
                x + y * w
            }
            world.add(DamagePixel(path, 0xff9900 or black) {
                // do some damage to the fluid
                val dr = 5
                val invDr = 1f / sq(dr + 0.5f)
                val level1 = fluid.level.read
                val vx = fluid.impulseX.read
                val vy = fluid.impulseY.read
                for (dy in -dr..dr) {
                    for (dx in -dr..dr) {
                        val x = tx + dx
                        val y = ty + dy
                        if (x in 0 until w && y in 0 until h) {
                            val i = x + y * w
                            val scale = (dx * dx + dy * dy) * invDr
                            if (scale < 1f) {
                                // damage fluid
                                level1[i] *= scale
                                vx[i] *= scale
                                vy[i] *= scale
                                val l = level1[i]
                                if (l > 0f) {
                                    // add impulse away from hit
                                    vx[i] += (1f - scale) * sign(dx.toFloat()) * l
                                    vy[i] += (1f - scale) * sign(dy.toFloat()) * l
                                }
                            }
                        }
                    }
                }
                // todo add explosion pixels
            })
        }
    }
}