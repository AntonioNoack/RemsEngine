package me.anno.ecs.components.shaders.sdf

import me.anno.ecs.components.mesh.Material

open class SDFShape : SDFComponent() {

    // todo each shape probably should have a software implementation as well, so we can use sdf shapes for physics and accurate ray tests :)

    // todo special sdf materials? ...
    var material: Material? = null

}