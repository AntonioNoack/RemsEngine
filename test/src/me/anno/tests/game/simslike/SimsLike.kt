package me.anno.tests.game.simslike

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference

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
//  - relations
//  - inventory
//  - items
//  - pick stuff up, place it down
//  - grid world
//  - rooms
//  - walls
//  - carpets
//  - pictures
// ...

// click onto things, and get a menu to select the possible actions

fun main() {
    OfficialExtensions.initForTests()
    // todo create world
    // todo create UI
    val scene = Entity("Scene")
    val floor = Entity("Floor", scene)
    floor.setScale(100.0)
    floor.add(MeshComponent(PlaneModel.createPlane()))
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
    for ((i, nameI) in names.withIndex()) {
        Entity(nameI, sims)
            .add(Sim().apply { name = nameI; household.sims.add(this) })
            .add(MeshComponent(getReference("res://meshes/CuteGhost.fbx"))) // best human model ever ^^
            .setPosition((i - (names.size - 1) * 0.5) * 5.0, 0.0, 0.0)
    }

    testSceneWithUI("SimsLike", scene) {
        val controls = SimsControls(scene, household, it.renderView)
        it.editControls = controls.playControls
    }
}