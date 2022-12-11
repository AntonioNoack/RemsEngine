package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import org.joml.AABBf
import org.joml.Vector4f

// todo groups: bounding spheres / volumes

// todo sdf mesh like https://www.iquilezles.org/www/articles/sdfbounding/sdfbounding.htm
// todo with bvh
open class SDFMesh : SDFSmoothShape() {

    var mesh: FileReference = InvalidRef
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    fun loadMesh() = MeshCache[mesh]

    override fun calculateBaseBounds(dst: AABBf) {
        val mesh = loadMesh()
        if (mesh != null) {
            dst.set(mesh.aabb)
        } else {
            dst.clear()
        }
    }

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        functions.add(sdMesh)
        smartMinBegin(builder, dstIndex)
        builder.append("sdBox(pos")
        builder.append(trans.posIndex)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        TODO()
    }

    override fun clone(): SDFMesh {
        val clone = SDFMesh()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFMesh
        clone.mesh = mesh
    }

    override val className get() = "SDFMesh"

    companion object {
        const val sdMesh = "" +
                ""
    }

}