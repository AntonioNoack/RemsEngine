package me.anno.ecs.components.shaders.sdf

import org.joml.Vector3d

class SDFArray : SDFModifier() {

    // m.x > 0 ? mod(pos.x, m.x) : pos.x for all xyz
    var repetition = Vector3d()

}