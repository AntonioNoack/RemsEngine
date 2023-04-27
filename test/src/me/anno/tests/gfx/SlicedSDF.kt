package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComposer
import me.anno.ecs.components.mesh.sdf.SDFGroup
import me.anno.ecs.components.mesh.sdf.shapes.SDFSphere
import me.anno.ecs.components.mesh.sdf.shapes.SDFTorus
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.shader.BaseShader
import me.anno.utils.OS.documents
import me.anno.utils.types.Floats.toRadians

fun main() {

    ECSRegistry.init()

    // todo render an SDF shape:
    //  - dust, fog, modelled by SDF
    // - sorted back to front
    // - alpha blending
    class FogSDFController : SDFGroup() {

        // todo make base mesh a stack of slices, that rotates towards the camera
        // todo compare to background depth
        // todo sample light at currently traced range
        init {
            material.pipelineStage = 1
        }

        override fun createShader(): Pair<Map<String, TypeValue>, BaseShader> {
            // todo modify shader to our needs :3
            return SDFComposer.createECSShader(this)
        }
    }

    val scene = Entity()
    scene.add(SkyBox())
    scene.add(Entity("Floor").apply {
        position = position.set(0.0, -1.0, 0.0)
        scale = scale.set(20.0)
        val mesh = MeshComponent(documents.getChild("plane.obj"))
        mesh.materials = listOf(Material().apply {
            diffuseBase.set(0.05f, 0.05f, 0.05f, 1f)
            cullMode = CullMode.BOTH
        }.ref)
        addChild(mesh)
    })
    scene.add(MeshComponent(documents.getChild("monkey.obj")))
    scene.add(FogSDFController().apply {
        name = "SDFs"
        addChild(SDFSphere().apply {
            position.set(4f, 0f, 0f)
        })
        addChild(SDFTorus().apply {
            position.set(-4f, 0f, 0f)
        })
    })
    scene.add(Entity("Directional").apply {
        rotation = rotation
            .rotateY((-50.0).toRadians())
            .rotateX((-45.0).toRadians())
        scale = scale.set(10.0)
        addChild(DirectionalLight().apply {
            shadowMapCascades = 1
            color = color.set(20f)
        })
        addChild(AmbientLight())
    })
    scene.add(Entity("Point").apply {
        position = position.set(4.0, 2.0, 0.0)
        scale = scale.set(20.0)
        addChild(PointLight().apply {
            color.set(5f, 20f, 5f)
            shadowMapCascades = 1
            shadowMapResolution = 256
        })
    })
    scene.add(Entity("Spot").apply {
        position = position.set(-5.0, 2.5, 0.6)
        rotation = rotation
            .rotateY((-68.0).toRadians())
            .rotateX((-72.0).toRadians())
        scale = scale.set(30.0)
        addChild(SpotLight().apply {
            color.set(20f, 5f, 5f)
            near = 0.01
            shadowMapCascades = 1
            shadowMapResolution = 256
            coneAngle = 0.57

        })
    })
    testSceneWithUI(scene)
}