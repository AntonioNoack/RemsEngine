package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.ConstructorData

open class BNodeTexBase(ptr: ConstructorData) : BTexMapping(ptr) {

    // val texMapping = inside("tex_mapping") as BTexMapping
    // val colorMapping = inside("color_mapping") as BColorMapping

    override fun toString(): String {
        return "NodeTexBase { ${super.toString()} }"
    }
}