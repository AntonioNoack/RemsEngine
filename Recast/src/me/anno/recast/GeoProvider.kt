package me.anno.recast

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.maths.Maths.hasFlag
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Vector3f
import org.recast4j.recast.ConvexVolume
import org.recast4j.recast.geom.InputGeomProvider
import org.recast4j.recast.geom.TriMesh

class GeoProvider(world: Entity, mask: Int) : InputGeomProvider {

    init {
        world.validateTransform()
        world.getBounds()
    }

    val meshes1 = ArrayList<TriMesh>()
    val bounds = AABBf()

    init {
        world.forAllComponentsInChildren(MeshComponentBase::class) {
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
        if (gt != null && !gt.isIdentity()) {
            val dst = FloatArray(src.size)
            val vec = JomlPools.vec3f.borrow()
            val mat = JomlPools.mat4x3f.borrow().set(gt)
            for (i in dst.indices step 3) {
                vec.set(src[i], src[i + 1], src[i + 2])
                mat.transformPosition(vec)
                dst[i] = vec.x
                dst[i + 1] = vec.y
                dst[i + 2] = vec.z
                bounds.union(vec)
            }
            src = dst
        }
        meshes1.add(TriMesh(src, faces))
    }

    override fun meshes() = meshes1

    // those are extra
    override fun convexVolumes() = emptyList<ConvexVolume>()

    override val meshBoundsMin = Vector3f(bounds.minX, bounds.minY, bounds.minZ)
    override val meshBoundsMax = Vector3f(bounds.maxX, bounds.maxY, bounds.maxZ)
}