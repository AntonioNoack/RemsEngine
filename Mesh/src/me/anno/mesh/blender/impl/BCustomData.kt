package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("SpellCheckingInspection", "unused")
class BCustomData(ptr: ConstructorData) : BlendData(ptr) {

    val external: List<BlendData>?
        get() = getStructArray("*external")

    // val size = int("totsize") // idk what this is
    val numLayers = i32("totlayer")
    val maxLayer = i32("maxlayer")

    val layers: List<BCustomDataLayer>
        get() = getStructArray("*layers") ?: emptyList()

    val pool get() = getPointer("*pool")

    override fun toString(): String {
        return "BCustomData { layers: $layers, ext: $external, numLayers: $numLayers, maxLayer: $maxLayer, pool: $pool }"
    }
}