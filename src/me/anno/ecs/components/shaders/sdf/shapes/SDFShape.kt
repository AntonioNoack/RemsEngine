package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComponent
import me.anno.ecs.components.shaders.sdf.SDFTransform
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr

open class SDFShape : SDFComponent() {

    // todo use raycast for physics & ray tests

    // todo special sdf materials? ...
    var dynamicSize = false
    var material: Material? = null

    // todo mix materials somehow...
    var materialId = 0f

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFShape
        clone.dynamicSize = dynamicSize
        clone.material = material
        clone.materialId = materialId
    }

    fun smartMinBegin(builder: StringBuilder, dstName: String) {
        builder.append(dstName)
        builder.append("=vec2(((")
    }

    fun smartMinEnd(builder: StringBuilder, scaleName: String?, offsetName: String?) {
        builder.append(")")
        if (offsetName != null) {
            builder.append("+")
            builder.append(offsetName)
        }
        builder.append(")")
        if (scaleName != null) {
            builder.append('*')
            builder.append(scaleName)
        }
        builder.append(",")
        builder.append(materialId)
        builder.append(");\n")
    }

    fun smartMinEnd(
        builder: StringBuilder,
        dstName: String,
        nextVariableId: Ptr<Int>,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        trans: SDFTransform
    ) {
        smartMinEnd(builder, trans.scaleName, trans.offsetName)
        buildDMShader(builder, trans.posIndex, dstName, nextVariableId, uniforms, functions)
        sdfTransPool.destroy(trans)
    }

}