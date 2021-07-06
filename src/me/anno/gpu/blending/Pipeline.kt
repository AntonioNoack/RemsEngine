package me.anno.gpu.blending

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshRenderer
import org.joml.Matrix4f
import org.joml.Vector3d

// todo idea: the scene rarely changes -> we can reuse it, and just update the uniforms
// and the graph may be deep, however meshes may be only in parts of the tree
class Pipeline {

    // todo pipelines, that we need:
    //  - 3d world,
    //  - 2d ui,
    //  - ...
    // todo every local player needs its own pipeline to avoid too much sorting

    // todo we can sort by material and shaders...
    // todo or by distance...

    val stages = ArrayList<PipelineStage>()
    lateinit var defaultStage: PipelineStage

    fun add(mesh: MeshComponent, transform: Transform) {
        for ((index, material) in mesh.materials.withIndex()) {
            val stage = material.pipelineStage ?: defaultStage
            stage.add(mesh, transform, index)
        }
    }

    // todo collect all buffers + materials, which need to be drawn at a certain stage, and then draw them together
    fun draw(cameraMatrix: Matrix4f, cameraPosition: Vector3d) {
        for (stage in stages) {
            stage.bindDraw(cameraMatrix, cameraPosition)
        }
    }

    fun reset() {
        for (stage in stages) {
            stage.reset()
        }
    }

    fun fill(rootElement: Entity) {
        rootElement.simpleTraversal(false) { entity ->
            val renderer = entity.getComponent<MeshRenderer>()
            if (renderer != null) {
                val meshes = entity.getComponents<MeshComponent>()
                for (mesh in meshes) {
                    add(mesh, entity.transform)
                }
            }
            // todo add AnimRenderer ...
            false
        }
    }

}