package me.anno.tests.maths.geometry

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.TransformMesh.transform
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.geometry.MeshSplitter
import me.anno.ui.UIColors
import me.anno.utils.Color.mixARGB2
import me.anno.utils.Color.white
import org.joml.Matrix4x3m

/**
 * split a sphere mesh by y=0;
 *
 * e.g., could be used for cutting vegetables
 * */
fun main() {

    val mesh = UVSphereModel.createUVSphere(20, 10)
    mesh.transform(Matrix4x3m().rotateX(1f))
    val split = MeshSplitter.split(mesh) { v -> v.y }

    val scene = Entity("Scene")
        .add(
            Entity("Top")
                .add(MeshComponent(split[0], Material.diffuse(UIColors.axisXColor)))
                .add(MeshComponent(split[1], Material.diffuse(mixARGB2(UIColors.axisXColor, white, 0.2f))))
                .setPosition(0.0, 0.5, 0.0)
        )
        .add(
            Entity("Bottom")
                .add(MeshComponent(split[2], Material.diffuse(UIColors.axisZColor)))
                .add(MeshComponent(split[3], Material.diffuse(mixARGB2(UIColors.axisZColor, white, 0.2f))))
                .setPosition(0.0, 0.0, 0.0)
        )
    testSceneWithUI("SplitTriangle", scene)
}
