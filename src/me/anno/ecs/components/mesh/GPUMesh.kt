package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3i

/**
 * don't use yet!
 * */
class GPUMesh : MeshComponentBase() {

    // todo create GPU-created meshes
    //  - shader 2d/3d, size constant or given by script
    //  (xi,yi,zi) -> List<Triple<Transform,(Helper)Mesh,Material>> for more flexibility
    //  - first shader is reduction shader, number of meshes -> index, where to place them
    //  - then a shader, which adds them,

    var spawnSize = Vector3i()
    var numMaterials = 0

    override fun getMesh(): Mesh? {
        TODO("Not yet implemented")
    }

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
        // todo update index-mapping-buffer
        return super.fill(pipeline, entity, clickId)
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        return true
    }

    companion object {

        // todo move to tests
        @JvmStatic
        fun main(args: Array<String>) {
            val scene = Entity()
            scene.add(Skybox())
            scene.add(GPUMesh())
            testSceneWithUI("GPUMesh", scene)
        }
    }

}