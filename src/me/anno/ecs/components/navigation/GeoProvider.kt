package me.anno.ecs.components.navigation

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.utils.types.Matrices.set2
import org.joml.Matrix4x3f
import org.joml.Vector3f
import org.recast4j.recast.ConvexVolume
import org.recast4j.recast.geom.InputGeomProvider
import org.recast4j.recast.geom.TriMesh

class GeoProvider(world: Entity) : InputGeomProvider {

    init {
        world.validateAABBs()
    }

    fun transform(tr: Matrix4x3f, tmp: Vector3f, src: FloatArray, si: Int, dst: FloatArray, di: Int) {
        tmp.set(src[si], src[si + 1], src[si + 2])
        tr.transformPosition(tmp)
        dst[di] = tmp.x
        dst[di + 1] = tmp.y
        dst[di + 2] = tmp.z
    }

    val meshes = ArrayList<TriMesh>()

    init {
        for (it in world.getComponentsInChildren(MeshComponentBase::class)) {
            val mesh = it.getMesh()
            if (mesh != null) {
                val vertices = mesh.positions!!
                val faces = mesh.indices ?: IntArray(vertices.size / 3) { it }
                val vs = FloatArray(vertices.size)
                // apply transform onto mesh
                val tmp = Vector3f()
                val tr = Matrix4x3f()
                    .set2(it.transform!!.globalTransform)
                for (i in vs.indices step 3) {
                    transform(tr, tmp, vertices, i, vs, i)
                }
                meshes.add(TriMesh(vertices, faces))
            }
        }
    }

    override fun meshes() = meshes
    // those are extra
    override fun convexVolumes() = emptyList<ConvexVolume>()

    override val meshBoundsMin: Vector3f
    override val meshBoundsMax: Vector3f

    init {
        val aabb = world.aabb
        meshBoundsMin = Vector3f(aabb.minX.toFloat(), aabb.minY.toFloat(), aabb.minZ.toFloat())
        meshBoundsMax = Vector3f(aabb.maxX.toFloat(), aabb.maxY.toFloat(), aabb.maxZ.toFloat())
    }

}