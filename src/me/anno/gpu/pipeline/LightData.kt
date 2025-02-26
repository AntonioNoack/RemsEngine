package me.anno.gpu.pipeline

import me.anno.ecs.Transform
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.LightType
import me.anno.utils.assertions.assertFail
import me.anno.utils.structures.lists.SmallestKList
import org.joml.Matrix4x3
import org.joml.Matrix4x3f

class LightData {

    class Entry(val type: LightType) {

        val values = ArrayList<LightRequest>()
        var index = 0

        fun add(
            light: LightComponent,
            drawMatrix: Matrix4x3,
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

    val entries = LightType.entries.map {
        Entry(it)
    }

    fun clear() {
        for (ei in entries.indices) {
            val entry = entries[ei]
            entry.index = 0
        }
    }

    fun add(light: LightComponent, transform: Transform) {
        add(light, transform.getDrawMatrix(), light.invCamSpaceMatrix)
    }

    fun add(light: LightComponent, drawMatrix: Matrix4x3, invCamSpaceMatrix: Matrix4x3f) {
        this[light].add(light, drawMatrix, invCamSpaceMatrix)
    }

    operator fun get(light: LightComponent): Entry = entries[light.lightType.id]

    fun listOfAll(dst: SmallestKList<LightRequest>): Int {
        for (ei in entries.indices) {
            val entry = entries[ei]
            dst.addAll(entry.values, 0, entry.index)
        }
        return dst.size
    }

    operator fun get(index: Long): LightRequest {
        var remainingIndex = index
        for (ei in entries.indices) {
            val entry = entries[ei]
            if (remainingIndex < entry.index) {
                return entry.values[remainingIndex.toInt()]
            }
            remainingIndex -= entry.index
        }
        assertFail("Index not found")
    }

    inline fun forEachType(callback: (lights: List<LightRequest>, size: Int, type: LightType) -> Unit) {
        for (ei in entries.indices) {
            val entry = entries[ei]
            if (entry.index <= 0) continue
            callback(entry.values, entry.index, entry.type)
        }
    }

    val size: Long get() = entries.sumOf { it.index.toLong() }
}