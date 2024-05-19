package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("SpellCheckingInspection", "unused", "UNCHECKED_CAST")
class BCustomData(ptr: ConstructorData) : BlendData(ptr) {

    val external get() = getStructArray("*external")
    // val size = int("totsize") // idk what this is
    val numLayers = int("totlayer")
    val maxLayer = int("maxlayer")
    val layers get() = getStructArray("*layers")?.toList() as? List<BCustomDataLayer> ?: emptyList()

    override fun toString(): String {
        return "BCustomData { layers: $layers, ext: $external }"
    }
}