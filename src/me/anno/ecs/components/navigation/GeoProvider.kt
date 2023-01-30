package me.anno.ecs.components.navigation

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.maths.Maths.hasFlag
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Matrices.set2
import org.joml.Matrix4x3f
import org.joml.Vector3f
import org.recast4j.recast.ConvexVolume
import org.recast4j.recast.geom.InputGeomProvider
import org.recast4j.recast.geom.TriMesh

class GeoProvider(world: Entity, mask: Int) : InputGeomProvider {

    init {
        world.validateTransform()
        world.validateAABBs()
    }

    val meshes = ArrayList<TriMesh>()

    init {
        for (it in world.getComponentsInChildren(MeshComponentBase::class)) {
            if (it.collisionMask.hasFlag(mask)) {
                val mesh = it.getMesh()
                if (mesh != null) addMesh(mesh, it)
            }
        }
    }

    fun addMesh(mesh: Mesh, it: MeshComponentBase) {
        var src = mesh.positions ?: return
        val faces = mesh.indices ?: IntArray(src.size / 3) { it }
        // apply transform onto mesh
        val gt = it.transform?.globalTransform
        if (gt != null && gt.properties().and(Matrix4x3f.PROPERTY_IDENTITY) == 0) {
            val dst = FloatArray(src.size)
            val vec = JomlPools.vec3f.borrow()
            val mat = JomlPools.mat4x3f.borrow().set2(gt)
            for (i in dst.indices step 3) {
                vec.set(src[i], src[i + 1], src[i + 2])
                mat.transformPosition(vec)
                dst[i] = vec.x
                dst[i + 1] = vec.y
                dst[i + 2] = vec.z
            }
            src = dst
        }
        meshes.add(TriMesh(src, faces))
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