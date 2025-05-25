package me.anno.games.simslike

import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.wrap

// todo
//  - playable characters
//    - idle animation
//    - walking animation
//    - sitting and eating animation
//  - action queue
//   - add actions
//   - process actions
//      - first goto,
//      - then execute
//   - cancel actions
//  - needs
//  - controls
//  - actions satisfy needs
//  - NPC characters
//  - NPC AI?
//  - relations, ideally async
//  - inventory
//  - items
//  - pick stuff up, place it down
//  - grid world
//  - rooms
//  - walls
//  - carpets
//  - pictures
// ...

// todo https://www.youtube.com/watch?v=qw-wdlkw0Os
//  - make it challenging:
//  - lawsuits, death,
//  - less choice,
//  - harder to manage needs,
//  - real sickness
//  - unwanted pregnancy,
//  - job loss,
//  - raising rent,
//  - basic things breaking, which make whole house unusable
//  - floods,
//  - tsunamis
//  - snow storms/blizzards,
//  - tornados, shark-nados

// click onto things, and get a menu to select the possible actions

fun main() {
    OfficialExtensions.initForTests()
    // todo create world
    // todo create UI
    val scene = Entity("Scene")
    val floor = Entity("Floor", scene)
    floor.setScale(40f)
    floor.add(MeshComponent(DefaultAssets.plane, Material.diffuse(0x335533)))
    floor.add(SimAction().apply {
        name = "Walk Here"
    })
    floor.add(SimAction().apply {
        name = "Sprint Here"
    })
    floor.add(SimAction().apply {
        name = "Sit Here"
    })

    val sims = Entity("Sims", scene)
    val household = Household()
    val names = listOf("Rem", "Ram", "Emilia", "Satou")
    val animatedMeshSrc = getReference("E:/Assets/Mixamo XBot/Female Locomotion Pack.zip")
    for ((i, nameI) in names.withIndex()) {
        Entity(nameI, sims)
            .add(Sim().apply { name = nameI; household.sims.add(this) })
            .add(AnimMeshComponent().apply {
                meshFile = animatedMeshSrc.getChild("X Bot.fbx")
                animations = listOf(AnimationState(animatedMeshSrc.getChild("idle.fbx")))
                materials = Material.diffuse(0xFFC8AA).ref.wrap()
            })
            .setPosition((i - (names.size - 1) * 0.5) * 5.0, 0.0, 0.0)
    }

    testSceneWithUI("SimsLike", scene) {
        val controls = SimsControls(scene, household, it.renderView)
        controls.playControls.rotationTargetDegrees.set(-30.0, 40.0, 0.0)
        it.editControls = controls.playControls
    }
}
