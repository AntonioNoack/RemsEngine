package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.shaders.sdf.SDFComponent
import me.anno.ecs.prefab.PrefabSaveable

open class SDFShape : SDFComponent() {

    // todo each shape probably should have a software implementation as well, so we can use sdf shapes for physics and accurate ray tests :)

    // todo special sdf materials? ...
    var dynamicSize = false
    var material: Material? = null

    // todo mix materials somehow...
    var materialIndex = 0

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFShape
        clone.dynamicSize = dynamicSize
        clone.material = material
        clone.materialIndex = materialIndex
    }

    fun smartMinBegin(builder: StringBuilder, dstName: String) {
        builder.append(dstName)
        builder.append("=vec2((")
    }

    fun smartMinEnd(builder: StringBuilder, scaleName: String?) {
        builder.append(")")
        if (scaleName != null) {
            builder.append('*')
            builder.append(scaleName)
        }
        builder.append(",")
        builder.append(materialIndex)
        builder.append(".0);\n")
    }

}