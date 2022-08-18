package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.Raycast
import me.anno.engine.raycast.Raycast.TRIANGLES
import me.anno.gpu.pipeline.InstancedStack
import me.anno.io.serialization.NotSerializedProperty
import me.anno.utils.structures.arrays.ExpandingFloatArray
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d

@Docs("Displays many meshes at once without Entities; can be used for particle systems and such")
abstract class MeshSpawner : CollidingComponent() {

    @NotSerializedProperty
    val transforms = ArrayList<Transform>(32)

    fun getTransform(i: Int): Transform {
        val entity = entity
        for (j in transforms.size..i) {
            transforms.add(Transform(entity))
        }
        return transforms[i]
    }

    fun ensureTransforms(count: Int) {
        val entity = entity
        for (i in transforms.size until count) {
            transforms.add(Transform(entity))
        }
    }

    override fun hasRaycastType(typeMask: Int) = (typeMask and TRIANGLES) != 0

    override fun raycast(
        entity: Entity, start: Vector3d, direction: Vector3d, end: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double,
        typeMask: Int, includeDisabled: Boolean, result: RayHit
    ): Boolean {
        var hit = false
        forEachMesh { mesh, _, transform ->
            if (Raycast.raycastTriangleMesh(
                    transform, mesh, start, direction, end, radiusAtOrigin,
                    radiusPerUnit, result, typeMask
                )
            ) {
                result.mesh = mesh
                result.component = this
                hit = true
            }
        }
        return hit
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        return true
    }

    /**
     * iterates over each mesh, which is actively visible; caller shall call transform.validate() if he needs the transform
     * */
    abstract fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit)

    /**
     * iterates over each mesh group, which is actively visible; caller shall call transform.validate();
     * if this is implemented, return true; and forEachMesh just will be a fallback
     * */
    open fun forEachMeshGroup(run: (Mesh, Material?) -> InstancedStack) = false

    /**
     * iterates over each mesh group, which is actively visible; caller shall call transform.validate();
     * if this is implemented, return true; and forEachMeshGroup just will be a fallback;
     *
     * each element must be added as position (x,y,z), scale (1d), rotation (x,y,z,w)
     * */
    open fun forEachMeshGroupPSR(run: (Mesh, Material?) -> ExpandingFloatArray) = false

    /**
     * iterates over each mesh group, which is actively visible; caller shall call transform.validate();
     * if this is implemented, return true; and forEachMeshGroup just will be a fallback;
     *
     * each element must be added as position (x,y,z)
     * */
    open fun forEachMeshGroupP(run: (Mesh, Material?) -> ExpandingFloatArray) = false

}