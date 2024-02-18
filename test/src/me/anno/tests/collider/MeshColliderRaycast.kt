package me.anno.tests.collider

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.extensions.ExtensionLoader
import me.anno.utils.OS.documents

fun main() {

    OfficialExtensions.register()
    ExtensionLoader.load()

    val scene = Entity("Monkey")
    val mesh = documents.getChild("redMonkey.glb")
    scene.add(MeshCollider(mesh))
    val meshComp = MeshComponent(mesh)
    meshComp.collisionMask = 0 // shouldn't be used for ray tests
    scene.add(meshComp)
    testSceneWithUI("Mesh Collider", scene) {
        it.renderer.renderMode = RenderMode.RAY_TEST
    }
}