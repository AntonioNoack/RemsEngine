package me.anno.tests.engine

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.documents

fun main() {
    OfficialExtensions.initForTests()
    ECSRegistry.init()
    // normals look broken, maybe scaled incorrectly???
    // transforms were broken -> is now fixed :)
    workspace = documents.getChild("RemsEngine/YandereSim")
    val meshFile = workspace.getChild("Walls/Office/meshes/SM_Bld_Floor_Grass_01.json")
    val meshEntity = Entity(MeshComponent(meshFile)).setScale(6.5)
    val file = meshEntity.ref
    val entityPrefab = PrefabCache[file]!!
    println(entityPrefab.sets)
    val entity = entityPrefab.createInstance() as Entity
    println(entity.scale)
    val mesh = MeshCache[file]!!
    val scene = Entity("Scene")
    entity.name = "Original"
    scene.add(entity)
    scene.add(
        Entity("MeshCache").setPosition(-12.0, 0.0, 0.0)
            .add(MeshComponent(mesh))
    )
    testSceneWithUI("MeshCache broken", scene)
}