package me.anno.tests.maths.geometry

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponentInChildren
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.geometry.MeshSplitter
import me.anno.ui.UIColors
import me.anno.utils.Color.mixARGB2
import me.anno.utils.Color.white
import me.anno.utils.OS.downloads
import org.joml.Planef

/**
 * split an animated mesh by a plane;
 *
 * could be used for cutting zombies into pieces
 * */
fun main() {

    OfficialExtensions.initForTests()
    val plane = Planef(0f, 0f, 1f, 0f)

    val prefab = PrefabCache[downloads.getChild("3d/azeria/scene.gltf/Scene.json")]!!
    val sceneInstance = prefab.getSampleInstance() as Entity
    val srcAnimMesh = sceneInstance.getComponentInChildren(AnimMeshComponent::class)!!

    val mesh = MeshCache[downloads.getChild("3d/azeria/scene.gltf")] as Mesh
    val split = MeshSplitter.split(mesh) { v -> plane.dot(v) }

    fun createMeshComponent(mesh: Mesh, material: Material): AnimMeshComponent {
        val dst = AnimMeshComponent()
        dst.meshFile = mesh.ref
        dst.materials = listOf(material.ref)
        dst.animations = srcAnimMesh.animations
        return dst
    }

    val scene = Entity("Scene")
        .add(
            Entity("Front")
                .add(createMeshComponent(split[0], Material.diffuse(UIColors.axisXColor)))
                .add(createMeshComponent(split[1], Material.diffuse(mixARGB2(UIColors.axisXColor, white, 0.2f))))
                .setPosition(0.0, 0.0, 3.0)
        )
        .add(
            Entity("Back")
                .add(createMeshComponent(split[2], Material.diffuse(UIColors.axisZColor)))
                .add(createMeshComponent(split[3], Material.diffuse(mixARGB2(UIColors.axisZColor, white, 0.2f))))
                .setPosition(0.0, 0.0, -3.0)
        )
    testSceneWithUI("SplitTriangle", scene)
}
