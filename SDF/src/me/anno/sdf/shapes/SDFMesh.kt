package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.TypeValue
import me.anno.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.arrays.IntArrayList
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
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions, seeds)
        functions.add(sdMesh)
        smartMinBegin(builder, dstIndex)
        builder.append("sdBox(pos")
        builder.append(trans.posIndex)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        TODO()
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFMesh
        dst.mesh = mesh
    }

    override val className: String get() = "SDFMesh"

    companion object {
        const val sdMesh = "" +
                ""
    }

}