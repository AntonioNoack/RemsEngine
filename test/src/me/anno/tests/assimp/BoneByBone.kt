package me.anno.tests.assimp

import me.anno.animation.LoopingState
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView
import me.anno.engine.EngineBase
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

fun main() {

    OfficialExtensions.initForTests()
    ECSRegistry.init()

    LogManager.enableLogger("AnimatedMeshesLoader")
    val project = OS.documents.getChild("RemsEngine/YandereSim")
    EngineBase.workspace = project
    val animFiles = project.getChild("Characters/anim-files/Walking.fbx")
    // done: BoneByBone != Imported on test meshes, only Imported works
    val animFiles1 = listOf("BoneByBone", "Imported").map {
        animFiles.getChild("animations/mixamo.com/$it.json")
    }
    val scene = Entity()
    // these animations look fine -> check their rotations and translations
    //  -> if they are fine, their retargeted versions shouldn't have any translations either
    // -> solution: we did need scales in our implementation
    for ((i, anim1f) in animFiles1.withIndex()) {
        val instance = PrefabCache[animFiles]!!.createInstance() as Entity
        instance.name = anim1f.nameWithoutExtension
        instance.setPosition(i * 2.0, 0.0, 0.0)
        instance.forAllComponentsInChildren(AnimMeshComponent::class) {
            it.animations = listOf(AnimationState(anim1f, 1f, 0f, 0.1f, LoopingState.PLAY_LOOP))
        }
        scene.add(instance)
    }
    SceneView.testSceneWithUI("Retargeting", scene)
    // done: correct mesh is drawing two skeletons??? -> yes, we had to set values to 0
}