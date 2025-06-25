package me.anno.games.spiderplaguehotel

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.games.spiderplaguehotel.SpiderLogic.Companion.manhattenDistance
import me.anno.games.spiderplaguehotel.SpiderLogic.Companion.simulationMultiplier
import me.anno.games.spiderplaguehotel.SpiderLogic.Companion.speedScale
import me.anno.games.spiderplaguehotel.SpiderLogic.Companion.spiderWorldScale
import me.anno.games.spiderplaguehotel.SpiderLogic.Companion.trapPositions
import me.anno.games.spiderplaguehotel.SpiderLogic.Companion.trapSize
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.pow
import kotlin.random.Random

class SpiderTrap(
    val index: Int, val random: Random,
    val spiders: Entity
) : Component() {

    var duration = 1f
    var progress = duration * fract(index / 5f)

    fun onUpdate() {
        progress += simulationMultiplier * speedScale
        val position = trapPositions[index]
        var teleport = false
        if (progress >= duration) {

            // kill all spiders here
            killSpiders()

            // reset trap
            val minDist = 3f * trapSize
            do {
                position.set(random.nextFloat(), random.nextFloat())
            } while (trapPositions.withIndex().any { (i, pos) ->
                    i != index && position.manhattenDistance(pos) < minDist
                })

            teleport = true
            progress -= duration
        }

        val transform = transform!!
        val y = 10.0 - 15.0 * pow(progress / duration, 10f)
        transform.setLocalPosition(
            (position.x * 2f - 1f) * spiderWorldScale, y,
            (position.y * 2f - 1f) * spiderWorldScale
        )
        // todo regular switching between these two isn't well supported...
        /*if (teleport)*/ transform.teleportUpdate()
        // else transform.smoothUpdate()
        invalidateBounds()
    }

    fun killSpiders() {
        val position = trapPositions[index]
        for (spider in spiders.children) {
            val spiderComp = spider.getComponent(SpiderLogic::class) ?: continue
            if (spiderComp.position.manhattenDistance(position) < trapSize) {
                killSpider(spiderComp)
            }
        }
    }

    fun killSpider(spiderComp: SpiderLogic) {
        // replace brain with other brain, and mutate it a little
        spiderComp.position.set(random.nextFloat(), random.nextFloat())
        val sourceBrain = spiders.children.random().getComponent(SpiderLogic::class)!!.brain
        sourceBrain.copyInto(spiderComp.brain)
        spiderComp.brain.mutate(0.05f, random)
    }
}