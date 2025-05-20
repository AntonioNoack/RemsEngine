package me.anno.tests.assimp

import me.anno.animation.LoopingState
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.anim.Retargetings
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView
import me.anno.engine.EngineBase
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS

fun main() {
    // todo for testing, find an easier case: one, where the mesh isn't rotated/scaled
    OfficialExtensions.initForTests()
    EngineBase.workspace = OS.documents.getChild("RemsEngine/YandereSim")
    // find two human meshes with different skeletons
    val meshFile = EngineBase.workspace.getChild("Characters/SK_Chr_Asian_Gangster_Male_01.json")
    val animFile = EngineBase.workspace.getChild("Characters/anim-files/Walking-inPlace.fbx")
    val scene = PrefabCache[meshFile]!!.createInstance() as Entity
    val animation = animFile.getChild("animations/mixamo.com/BoneByBone.json")
    lateinit var testedComponent: AnimMeshComponent
    scene.forAllComponentsInChildren(AnimMeshComponent::class) { mesh ->
        mesh.animations = listOf(AnimationState(animation, 1f, 0f, 1f, LoopingState.PLAY_LOOP))
        testedComponent = mesh
    }
    val retargeting = Retargetings.getRetargeting(AnimationCache[animation]!!.skeleton, testedComponent.getMesh()!!.skeleton)!!
    Retargetings.sampleModel = testedComponent
    Retargetings.sampleAnimation = animation
    SceneView.testSceneWithUI("Retargeting", retargeting)
}