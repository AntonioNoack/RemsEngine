package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.control.DCPaintable
import me.anno.engine.ui.control.DraggingControls
import me.anno.gpu.GFXState.currentRenderer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.sdf.SDFComponent
import me.anno.sdf.SDFTransform
import me.anno.sdf.VariableCounter
import me.anno.utils.LOGGER
import org.apache.logging.log4j.LogManager
import kotlin.math.max

open class SDFShape : SDFComponent(), DCPaintable {

    companion object {
        private val LOGGER = LogManager.getLogger(SDFShape::class)
    }

    var dynamicSize = false
        set(value) {
            if (field != value) {
                field = value
                if (!globalDynamic) invalidateShader()
            }
        }

    var materialId = 0

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFShape
        dst.dynamicSize = dynamicSize
        // clone.material = material
        dst.materialId = materialId
    }

    fun smartMinBegin(builder: StringBuilder, dstIndex: Int) {
        builder.append("res").append(dstIndex)
        builder.append("=vec4(((")
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
        builder.append(",uv);\n")
    }

    fun smartMinEnd(
        builder: StringBuilder,
        dstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>,
        trans: SDFTransform
    ) {
        smartMinEnd(builder, uniforms, trans.scaleName, trans.offsetName)
        buildDistanceMapperShader(builder, trans.posIndex, dstIndex, nextVariableId, uniforms, functions, seeds)
        sdfTransPool.destroy(trans)
    }

    override fun paint(self: DraggingControls, color: Material, file: FileReference) {
        val materialId = materialId
        val root = getRoot(SDFComponent::class)
        val oldList = root.sdfMaterials
        if (materialId >= 0 && (materialId < oldList.size + 3)) {
            val newSize = max(materialId + 1, oldList.size)
            val newList = ArrayList<FileReference>(newSize)
            for (i in 0 until newSize) {
                newList.add(oldList.getOrNull(i) ?: InvalidRef)
            }
            newList[materialId] = file
            root.sdfMaterials = newList
            root.prefab?.set(root, "sdfMaterials", newList)
        } else {
            if (materialId >= 0) LOGGER.warn("Material id is unexpectedly large: $materialId > ${oldList.size} + 3")
            else LOGGER.warn("Invalid material id, must be non-negative")
        }
    }
}