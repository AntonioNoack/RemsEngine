package me.anno.gpu.pipeline

import me.anno.ecs.Transform
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.LightType
import me.anno.utils.structures.lists.SmallestKList
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f

class LightData {

    class Entry(val type: LightType) {

        val values = ArrayList<LightRequest>()
        var index = 0

        fun add(
            light: LightComponent,
            drawMatrix: Matrix4x3d,
            invCamSpaceMatrix: Matrix4x3f
        ) {
            val index = index++
            val list = values
            if (index >= list.size) {
                list.add(LightRequest(light, drawMatrix, invCamSpaceMatrix))
            } else {
                list[index].set(light, drawMatrix, invCamSpaceMatrix)
            }
        }
    }

    val entries = LightType.values().map {
        Entry(it)
    }.toTypedArray()

    fun clear() {
        for (entry in entries) {
            entry.index = 0
        }
    }

    fun add(light: LightComponent, transform: Transform) {
        add(light, transform.getDrawMatrix(), light.invCamSpaceMatrix)
    }

    fun add(light: LightComponent, drawMatrix: Matrix4x3d, invCamSpaceMatrix: Matrix4x3f) {
        this[light].add(light, drawMatrix, invCamSpaceMatrix)
    }

    operator fun get(light: LightComponent) = entries[light.lightType.id]

    fun listOfAll(): List<LightRequest> {
        val dst = ArrayList<LightRequest>(size)
        for (entry in entries) {
            dst.addAll(entry.values.subList(0, entry.index))
        }
        return dst
    }

    fun listOfAll(dst: SmallestKList<LightRequest>): Int {
        for (entry in entries) {
            dst.addAll(entry.values, 0, entry.index)
        }
        return dst.size
    }

    operator fun get(index: Int): LightRequest {
        var remainingIndex = index
        for (entry in entries) {
            if (remainingIndex < entry.index) {
                return entry.values[remainingIndex]
            }
            remainingIndex -= entry.index
        }
        throw IndexOutOfBoundsException()
    }

    inline fun forEachType(callback: (lights: List<LightRequest>, type: LightType, size: Int) -> Unit) {
        for (entry in entries) {
            if (entry.index > 0) {
                callback(entry.values, entry.type, entry.index)
            }
        }
    }

    val size get() = entries.sumOf { it.index }
    fun isNotEmpty() = size > 0
}