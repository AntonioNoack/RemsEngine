package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.ecs.components.mesh.sdf.SDFTransform
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.OpenGL.currentRenderer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer

open class SDFShape : SDFComponent() {

    // todo use raycast for physics & ray tests

    // todo special sdf materials? ...
    var dynamicSize = false
        set(value) {
            if (field != value) {
                field = value
                if(!globalDynamic) invalidateShader()
            }
        }

    // todo mix materials somehow...
    var materialId = 0

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFShape
        clone.dynamicSize = dynamicSize
        // clone.material = material
        clone.materialId = materialId
    }

    fun smartMinBegin(builder: StringBuilder, dstIndex: Int) {
        builder.append("res").append(dstIndex)
        builder.append("=vec2(((")
    }

    fun smartMinEnd(
        builder: StringBuilder,
        uniforms: HashMap<String, TypeValue>,
        scaleName: String?,
        offsetName: String?
    ) {
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
        if (localReliability != 1f) {
            builder.append('*')
            builder.appendUniform(uniforms, GLSLType.V1F) { localReliability }
        }
        builder.append(",")
        builder.appendUniform(uniforms, GLSLType.V1F) {
            val currentRenderer = currentRenderer
            if (currentRenderer == Renderer.idRenderer) clickId.toFloat()
            else materialId.toFloat()
        }
        builder.append(");\n")
    }

    fun smartMinEnd(
        builder: StringBuilder,
        dstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        trans: SDFTransform
    ) {
        smartMinEnd(builder, uniforms, trans.scaleName, trans.offsetName)
        buildDMShader(builder, trans.posIndex, dstIndex, nextVariableId, uniforms, functions)
        sdfTransPool.destroy(trans)
    }

}