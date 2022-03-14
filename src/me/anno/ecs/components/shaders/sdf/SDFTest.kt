package me.anno.ecs.components.shaders.sdf

import me.anno.Engine
import me.anno.ecs.components.shaders.sdf.modifiers.SDFArray
import me.anno.ecs.components.shaders.sdf.shapes.*
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.ui.debug.TestDrawPanel
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.sin
import kotlin.math.tan

fun main() {
    // render test of shapes
    // todo we could try to recreate some basic samples from IQ with our nodes :)

    // this would ideally test our capabilities
    val camRotation = Quaternionf()
    val camPosition = Vector3f(0f, 0f, 5f)
    val camMatrix = Matrix3f()
    val fovDegrees = 90f

    fun createGroup(): SDFGroup {
        val obj0 = SDFBoundingBox()
        obj0.thickness = 0.1f
        obj0.dynamicThickness = true
        obj0.smoothness = 0.1f
        obj0.dynamicSmoothness = true
        val obj1 = SDFBox()
        obj1.smoothness = 0.3f
        obj1.dynamicSmoothness = true
        val obj2 = SDFSphere()
        val obj3 = SDFTorus()
        val obj4 = SDFCylinder()
        obj4.smoothness = 0.1f
        obj4.dynamicSmoothness = true
        val obj5 = SDFHexPrism()
        obj5.smoothness = 0.1f
        obj5.dynamicSmoothness = true
        obj5.dynamicRotation = true
        val group = SDFGroup()
        group.add(obj0)
        group.add(obj1)
        group.add(obj2)
        group.add(obj4)
        group.add(obj3)
        group.add(obj5)
        return group
    }

    val group1 = createGroup()
    group1.position.y += 1f
    group1.dynamicRotation = true
    val group2 = createGroup()
    group2.position.y -= 1f
    group2.dynamicRotation = true
    val megaGroup = SDFGroup()
    megaGroup.add(group1)
    megaGroup.add(group2)
    // megaGroup.add(SDFSphere().apply { radius = 2.5f; position.y += 2f })
    megaGroup.type = SDFGroup.CombinationMode.UNION
    megaGroup.smoothness = 0.5f
    val array = SDFArray()
    array.repetition.set(2.7f, 0f, 2.7f)
    array.repLimit.set(3f)
    megaGroup.add(array)
    val (uniforms, shaderBase) = SDFComposer.createShader(megaGroup)
    println(shaderBase.fragmentSource)
    TestDrawPanel.testDrawing {
        val dt = Engine.deltaTime
        val dt5 = 5f * dt
        camRotation.transformInverse(camPosition)
        if (Input.isKeyDown('w')) camPosition.z -= dt5
        if (Input.isKeyDown('s')) camPosition.z += dt5
        if (Input.isKeyDown('a')) camPosition.x -= dt5
        if (Input.isKeyDown('d')) camPosition.x += dt5
        if (Input.isKeyDown('q')) camPosition.y -= dt5
        if (Input.isKeyDown('e')) camPosition.y += dt5
        camRotation.transform(camPosition)
        GFX.clip(it.x, it.y, it.w, it.h) {
            it.clear()
            val shader = shaderBase.value
            val fovFactor = tan(fovDegrees.toRadians() * 0.5f)
            shader.use()
            shader.v2f("camScale", (fovFactor * it.w) / it.h, fovFactor)
            shader.v3f("camPosition", camPosition)
            shader.v2f("distanceBounds", 0.01f, 1e3f)
            group1.progress = (sin(Engine.gameTimeF.toFloat()) * .5f + .5f) * (group1.children.size - 1f)
            group2.progress = (sin(Engine.gameTimeF.toFloat() + 1.57f) * .5f + .5f) * (group2.children.size - 1f)
            /*group1.rotation.rotateY(Engine.deltaTime)
            group1.rotation.rotateX(2f * Engine.deltaTime)*/
            group2.rotation.rotateY(-dt * 3f)
            for (child in group1.children) {
                if (child is SDFSmoothShape && child !is SDFCylinder) {
                    child.smoothness = sin(Engine.gameTimeF.toFloat()) * .5f + .5f
                }
                if (child is SDFHexPrism) {
                    child.rotation.rotateY(dt)
                }
            }
            camRotation.identity()
                .rotateY(it.mx * 2f)
                .rotateX(it.my * 2f)
            camMatrix.identity().rotate(camRotation)
            shader.m3x3("camMatrix", camMatrix)
            for ((key, value) in uniforms) {
                value.bind(shader, key)
            }
            GFX.flat01.draw(shader)
        }
    }
}