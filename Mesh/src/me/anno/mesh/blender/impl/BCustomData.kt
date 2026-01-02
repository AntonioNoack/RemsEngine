package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("SpellCheckingInspection", "unused")
class BCustomData(ptr: ConstructorData) : BlendData(ptr) {

    val external get() = getStructArray("*external")

    // val size = int("totsize") // idk what this is
    val numLayers get() = i32("totlayer")
    val maxLayer get() = i32("maxlayer")

    @Suppress("UNCHECKED_CAST")
    val layers: List<BCustomDataLayer>
        get() = getStructArray("*layers")?.toList() as? List<BCustomDataLayer> ?: emptyList()

    val pool get() = getPointer("*pool")

    override fun toString(): String {
        return "BCustomData { layers: $layers, ext: $external, numLayers: $numLayers, maxLayer: $maxLayer, pool: $pool }"
    }
}