package me.anno.tests.gfx.nanite

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.EngineBase
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.drawing.DrawTexts
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.gfx.nanite.ComputeShaderMesh.Companion.useTraditionalRendering
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3d
import org.joml.Vector3f

// create a software rasterizer for compute shaders
//  - Unreal Engine devs said it was more efficient for small triangles -> let's do the same to render millions of tiny triangles
// todo -> this doesn't work by just using compute; we need something like dynamic LOD or meshlets, too

fun main() {
    testSoftwareRasterizerOnGPU()
}

fun replaceTerms(name: String): String {
    return name
        .replace("gl_Position", "glPosition")
        .replace("gl_FragCoord", "glFragCoord")
        // they are signed in OpenGL because legacy OpenGL didn't have unsigned types
        .replace("gl_VertexID", "int(glVertexID)")
        .replace("gl_InstanceID", "int(glInstanceID)")
        .replace("gl_FrontFacing", "true") // not yet supported properly
        .replace("dFdx", "zero")
        .replace("dFdy", "zero")
        .replace("discard;", "return;")
}

fun extractMain(source: String): Pair<String, String> {
    val prefix = "void main(){"
    val idx = source.indexOf(prefix)
    val end = source.lastIndexOf('}')
    if (idx < 0 || end < idx) throw IllegalArgumentException("Missing main()")
    return replaceTerms(source.substring(0, idx)) to
            replaceTerms(source.substring(idx + prefix.length, end))
}

fun testSoftwareRasterizerOnGPU() {

    // done first step: create an IMesh
    // done create lots of small triangles for testing

    val mesh = IcosahedronModel.createIcosphere(7)
    if (false) mesh.createUniqueIndices()

    val iMesh = ComputeShaderMesh(mesh)
    val scene = Entity("Scene")
    val s = 5
    for (z in -s..s) {
        for (x in -s..s) {
            val comp = object : MeshComponentBase() {
                override fun getMeshOrNull() = iMesh
            }
            iMesh.component = comp // any instance is fine
            scene.add(
                Entity("Compute/$x//$z")
                    .setPosition(x * 2.0, 0.0, z * 2.0)
                    .add(comp)
            )
        }
    }

    for (i in 0 until 40) {
        val da = (45.0 * i).toRadians()
        val db = (30.0 * (i / 8 - 2)).toRadians()
        Entity(scene)
            .add(MeshComponent(flatCube.linear(Vector3f(), Vector3f(1f)).front))
            .setPosition(Vector3d(0.0, 0.0, 2.0).rotateX(db).rotateY(da))
            .setScale(0.6)
    }

    testSceneWithUI("Compute Rasterizer", scene) {
        it.editControls = object : DraggingControls(it.renderView) {
            override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.draw(x0, y0, x1, y1)
                DrawTexts.drawSimpleTextCharByChar(
                    x1, y1, 2,
                    if (useTraditionalRendering()) "Baseline"
                    else "Software Rasterizer",
                    AxisAlignment.MAX, AxisAlignment.MAX
                )
            }
        }
        EngineBase.enableVSync = false // we want to go fast, so we need to measure performance
    }
}