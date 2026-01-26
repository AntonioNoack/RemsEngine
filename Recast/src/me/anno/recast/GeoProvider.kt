package me.anno.recast

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshIndices.flattenedTriangleIndices
import me.anno.ecs.components.mesh.TransformMesh.transformPositions
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.hasFlag
import org.joml.AABBf
import org.recast4j.recast.ConvexVolume
import org.recast4j.recast.geom.InputGeomProvider
import org.recast4j.recast.geom.TriMesh

class GeoProvider(world: Entity, mask: Int) : InputGeomProvider {

    override val meshes = ArrayList<TriMesh>()
    override val bounds = AABBf()

    // those are extra, so not needed here
    override val convexVolumes get() = emptyList<ConvexVolume>()

    init {
        world.validateTransform()
        world.forAllComponentsInChildren(MeshComponentBase::class) { comp ->
            if (comp.collisionMask.hasFlag(mask)) {
                val mesh = comp.getMesh()
                if (mesh is Mesh) addMesh(mesh, comp)
            }
        }
    }

    private fun addMesh(mesh: Mesh, it: MeshComponentBase) {
        var src = mesh.positions ?: return
        val faces = mesh.flattenedTriangleIndices()
        src = transformAndGetBounds(src, mesh, it)
        meshes.add(TriMesh(src, faces))
    }

    private fun transformAndGetBounds(src: FloatArray, mesh: Mesh, it: Component): FloatArray {
        // apply transform onto mesh
        var src = src
        val gt = it.transform?.globalTransform
        if (gt != null && !gt.isIdentity()) {
            src = transformPositions(gt, src.copyOf(), 3)
            val vec = JomlPools.vec3f.borrow()
            forLoopSafely(src.size, 3) { i ->
                bounds.union(vec.set(src, i))
            }
        } else bounds.union(mesh.getBounds())
        return src
    }

}