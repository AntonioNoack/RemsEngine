package me.anno.gpu.pipeline

import me.anno.ecs.Transform
import me.anno.ecs.components.light.*
import me.anno.utils.structures.lists.SmallestKList

class LightData {

    val dirs = ArrayList<LightRequest<DirectionalLight>>()
    val spots = ArrayList<LightRequest<SpotLight>>()
    val points = ArrayList<LightRequest<PointLight>>()

    var dirIndex = 0
    var spotIndex = 0
    var pointsIndex = 0

    fun clear() {
        dirIndex = 0
        spotIndex = 0
        pointsIndex = 0
    }

    private fun <V : LightComponent> add(
        list: MutableList<LightRequest<V>>,
        index: Int, light: V, transform: Transform
    ) {
        if (index >= list.size) {
            list.add(LightRequest(light, transform))
        } else {
            list[index].set(light, transform)
        }
    }

    fun add(light: LightComponent, transform: Transform) {
        when (light) {
            is DirectionalLight -> add(dirs, dirIndex++, light, transform)
            is PointLight -> add(points, pointsIndex++, light, transform)
            is SpotLight -> add(spots, spotIndex++, light, transform)
        }
    }

    fun listOfAll(): List<LightRequest<*>> {
        return dirs.subList(0, dirIndex) +
                spots.subList(0, spotIndex) +
                points.subList(0, pointsIndex)
    }

    fun listOfAll(dst: SmallestKList<LightRequest<*>>): Int {
        dst.addAll(dirs, 0, dirIndex)
        dst.addAll(spots, 0, spotIndex)
        dst.addAll(points, 0, pointsIndex)
        return dst.size
    }

    operator fun get(index: Int): LightRequest<*> {
        return when {
            index < dirIndex -> dirs[index]
            index < dirIndex + spotIndex -> spots[index - dirIndex]
            else -> points[index - (dirIndex + spotIndex)]
        }
    }

    inline fun forEachType(run: (lights: List<LightRequest<*>>, type: LightType, size: Int) -> Unit) {
        if (dirIndex > 0) run(dirs, LightType.DIRECTIONAL, dirIndex)
        if (spotIndex > 0) run(spots, LightType.SPOT, spotIndex)
        if (pointsIndex > 0) run(points, LightType.POINT, pointsIndex)
    }

    val size get() = dirIndex + spotIndex + pointsIndex
    fun isNotEmpty() = size > 0

}