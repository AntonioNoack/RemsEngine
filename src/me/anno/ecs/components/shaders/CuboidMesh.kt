package me.anno.ecs.components.shaders

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.mesh.Shapes
import org.joml.Vector3f

class CuboidMesh : ProceduralMesh() {

    var size = Vector3f(1f)

    override fun generateMesh(mesh: Mesh) {
        val base = Shapes.flatCube.front
        val src = base.positions!!
        var dst = mesh.positions
        if (dst == null || dst.size != src.size) dst = FloatArray(src.size)
        val size = size
        val sx = size.x*0.5f
        val sy = size.y*0.5f
        val sz = size.z*0.5f
        for (i in src.indices step 3) {
            dst[i] = src[i] * sx
            dst[i + 1] = src[i + 1] * sy
            dst[i + 2] = src[i + 2] * sz
        }
        mesh.positions = dst
        mesh.invalidateGeometry()
    }

    override fun clone(): CuboidMesh {
        val clone = CuboidMesh()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CuboidMesh
        clone.size.set(size)
        clone.invalidateMesh()
    }

    override val className get() = "CuboidMesh"

}