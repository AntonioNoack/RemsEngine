package me.anno.recast

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.TransformMesh.transformPositions
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.hasFlag
import org.joml.AABBf
import org.recast4j.recast.ConvexVolume
import org.recast4j.recast.geom.InputGeomProvider
import org.recast4j.recast.geom.TriMesh

class GeoProvider(world: Entity, mask: Int) : InputGeomProvider {

    val meshes1 = ArrayList<TriMesh>()

    override val bounds = AABBf()

    init {
        world.validateTransform()
        world.forAllComponentsInChildren(MeshComponentBase::class) {
            if (it.collisionMask.hasFlag(mask)) {
                val mesh = it.getMesh()
                if (mesh is Mesh) addMesh(mesh, it)
            }
        }
    }

    private fun addMesh(mesh: Mesh, it: MeshComponentBase) {
        var src = mesh.positions ?: return
        val faces = mesh.indices ?: IntArray(src.size / 3) { it }
        // apply transform onto mesh
        val gt = it.transform?.globalTransform
        if (gt != null && !gt.isIdentity()) {
            src = transformPositions(gt, src.copyOf(), 3)
            val vec = JomlPools.vec3f.borrow()
            for (i in 0 until (src.size - 2) / 3) {
                bounds.union(vec.set(src, i * 3))
            }
        } else bounds.union(mesh.getBounds())
        meshes1.add(TriMesh(src, faces))
    }

    // those are extra
    override fun convexVolumes() = emptyList<ConvexVolume>()
    override fun meshes() = meshes1
}