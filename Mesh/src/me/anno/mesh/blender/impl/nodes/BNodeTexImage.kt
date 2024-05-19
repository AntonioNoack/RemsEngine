package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.ConstructorData

class BNodeTexImage(ptr: ConstructorData) : BNodeTexBase(ptr) {

    // iuser=ImageUser(40)@960, color_space=int(4)@1000, projection=int(4)@1004, projection_blend=float(4)@1008,
    // interpolation=int(4)@1012, extension=int(4)@1016

    override fun toString(): String {
        return "NodeTexImage { ${super.toString()} }"
    }
}