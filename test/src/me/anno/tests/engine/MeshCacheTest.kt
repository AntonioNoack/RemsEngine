package me.anno.tests.engine

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.utils.OS.documents

fun main() {
    // transforms were broken -> is now fixed :)
    workspace = documents.getChild("RemsEngine/YandereSim")
    val file = workspace.getChild("Class Room Pair.json")
    val entity = PrefabCache[file]!!.createInstance() as Entity
    val mesh = MeshCache[file]!!
    val scene = Entity("Scene")
    scene.add(entity.setPosition(0.0, 0.0, 0.0))
    scene.add(
        Entity().setPosition(-10.0, 0.0, 0.0)
            .add(MeshComponent(mesh))
    )
    testSceneWithUI("MeshCache broken", scene)
}