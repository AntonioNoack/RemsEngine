package me.anno.tests.gfx

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.tests.utils.dr
import org.joml.AABBf

// todo create a software rasterizer for compute shaders
//  - Unreal Engine devs said it was more efficient for small triangles -> let's do the same to render millions of tiny triangles
fun main() {

    // done first step: create an IMesh
    // done create lots of small triangles for testing

    val mesh = IcosahedronModel.createIcosphere(5)
    lateinit var component: Component
    val iMesh = object : IMesh {

        override val numPrimitives: Long
            get() = mesh.numPrimitives

        override fun ensureBuffer() {
            mesh.ensureBuffer()
        }

        override fun getBounds(): AABBf {
            return mesh.getBounds()
        }

        override fun draw(shader: Shader, materialIndex: Int, drawLines: Boolean) {
            mesh.draw(shader, materialIndex, drawLines)
            // todo implement this
        }

        override fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: Buffer, drawLines: Boolean) {
            mesh.drawInstanced(shader, materialIndex, instanceData, drawLines)
            // todo implement this
        }

        override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
            val material = Material.defaultMaterial
            pipeline.findStage(material)
                .add(component, this, entity, material, 0)
            return clickId + 1
        }
    }
    val comp = object : MeshComponentBase() {
        override fun getMeshOrNull() = iMesh
    }
    component = comp

    val scene = Entity("Scene")
    scene.add(comp)
    testSceneWithUI("Compute Rasterizer", scene)
}