package me.anno.tests.maths.geometry

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.geometry.MeshSplitter
import me.anno.sdf.shapes.SDFPlane
import me.anno.ui.UIColors
import me.anno.utils.Color.mixARGB2
import me.anno.utils.Color.white
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector4f

/**
 * split a sphere mesh by an SDF shape;
 *
 * e.g., could be used for cutting vegetables
 * */
fun main() {

    val sdfShape = SDFPlane()

    val mesh = UVSphereModel.createUVSphere(20, 10)
    val pos = Vector4f()
    val seeds = IntArrayList(0)
    val split = MeshSplitter.split(mesh) { v ->
        pos.set(v, 0f)
        sdfShape.computeSDF(pos, seeds)
    }

    val scene = Entity("Scene")
        .add(
            Entity("Top")
                .add(MeshComponent(split[0], Material.diffuse(UIColors.axisXColor)))
                .add(MeshComponent(split[2], Material.diffuse(mixARGB2(UIColors.axisXColor, white, 0.2f))))
                .setPosition(0.0, 0.5, 0.0)
        )
        .add(
            Entity("Bottom")
                .add(MeshComponent(split[1], Material.diffuse(UIColors.axisZColor)))
                .add(MeshComponent(split[3], Material.diffuse(mixARGB2(UIColors.axisZColor, white, 0.2f))))
                .setPosition(0.0, 0.0, 0.0)
        )
    testSceneWithUI("SplitTriangle", scene)
}
