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
import me.anno.recast.CrowdUpdateComponent
import me.anno.recast.NavMeshBuilder
import me.anno.recast.NavMeshData
import me.anno.recast.NavMeshDebugComponent
import me.anno.tests.recast.TestAgent
import me.anno.utils.structures.lists.Lists.wrap
import org.joml.Vector3f

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

val clickCollision = 1
val navMeshCollision = 2

fun generateNavMesh(scene: Entity): NavMeshData {
    // scene is needed as a mesh-lookup

    // setup recast
    val builder = NavMeshBuilder()
    builder.agentType.height = 2f
    builder.cellSize = 0.5f
    builder.cellHeight = 2f
    builder.agentType.maxSpeed = 3f
    builder.agentType.radius = 0.35f
    builder.agentType.maxStepHeight = 0.2f
    builder.collisionMask = navMeshCollision
    builder.edgeMaxError = 1f

    return builder.buildData(scene)!!
}

fun main() {
    OfficialExtensions.initForTests()
    // todo create world
    // todo create UI

    val scene = Entity("Scene")
    Entity("Floor", scene)
        .setScale(40f)
        .add(MeshComponent(DefaultAssets.plane, Material.diffuse(0x335533)).apply {
            collisionMask = clickCollision or navMeshCollision
        }).add(SimAction().apply {
            name = "Walk Here"
        }).add(SimAction().apply {
            name = "Sprint Here"
        }).add(SimAction().apply {
            name = "Sit Here"
        })

    val navMeshData = generateNavMesh(scene)
    scene.add(CrowdUpdateComponent(navMeshData))
    scene.add(NavMeshDebugComponent(navMeshData))

    val sims = Entity("Sims", scene)
    val household = Household()
    val names = listOf("Rem", "Ram", "Emilia", "Satou")
    val animatedMeshSrc = getReference("E:/Assets/Mixamo XBot/Female Locomotion Pack.zip")
    for ((i, nameI) in names.withIndex()) {

        Entity(nameI, sims)
            // todo integrate navigation agent into Sim
            .add(Sim().apply { name = nameI; household.sims.add(this) })
            // todo why is TestAgent not moving???
            .add(TestAgent(navMeshData, Vector3f(), Vector3f(0f, 0f, 10f)))
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
