package me.anno.tests.game.pacman

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponentInChildren
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.player.LocalPlayer
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView1
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.pipeline.PipelineStage
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths.dtTo01
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.game.pacman.logic.Moveable
import me.anno.tests.game.pacman.logic.PacmanLogic
import me.anno.ui.UIColors.cornFlowerBlue
import me.anno.ui.UIColors.darkOrange
import me.anno.ui.debug.PureTestEngine.Companion.testPureUI
import me.anno.utils.Color.toVecRGBA
import org.joml.Vector2f
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class PacmanControls(
    val game: PacmanLogic, val camEntity: Entity,
    val enemies: Entity, val player: Entity
) : Component() {

    val baseCameraHeight = camEntity.position.y
    fun setPos(moveable: Moveable, entity: Entity, mixDt: Float) {
        val transform = entity.transform
        val pos = transform.localPosition
        val rot = transform.localRotation
        val currPosition = moveable.currPosition
        val prevPosition = moveable.prevPosition
        pos.x = currPosition.x + 0.5
        pos.z = currPosition.y + 0.5
        if (currPosition.distanceSquared(prevPosition) > 1e-5f) {
            val angle = atan2(
                currPosition.x - prevPosition.x,
                currPosition.y - prevPosition.y
            )
            rot.identity().rotateY(angle.toDouble())
        }
        prevPosition.lerp(currPosition, mixDt)
        transform.localPosition = pos
        transform.localRotation = rot
        transform.smoothUpdate()
        entity.invalidateOwnAABB()
    }

    override fun onUpdate(): Int {
        // controls
        game.updateControls()
        // update visuals
        val rv = RenderView.currentInstance
        if (rv != null) {
            val pos = camEntity.transform.localPosition
            pos.y = baseCameraHeight * rv.height.toDouble() / min(rv.width, rv.height)
            camEntity.transform.localPosition = pos
            camEntity.transform.smoothUpdate()
        }
        val mixDt = dtTo01(Time.deltaTime.toFloat() * 5f)
        for (i in game.enemies.indices) {
            val enemy = game.enemies[i]
            val entity = enemies.children[i]
            setPos(enemy, entity, mixDt)
        }
        setPos(game.player, player, mixDt)
        return 1
    }
}

/**
 * renders a pacman game with standard flat UI
 * */
fun spatialPacmanGame(): Entity {

    val collectibleLookup = HashMap<Vector2f, Entity>()
    val scene = Entity("Scene")
    val camEntity = Entity("Camera", scene)
    val game = object : PacmanLogic() {
        override fun onCollect(collectible: Vector2f) {
            collectibleLookup[collectible]!!.removeFromParent()
        }
    }

    val wallHeight = 0.50
    val camera = Camera()
    camEntity.add(camera)
    camEntity.setPosition(
        game.size.x * 0.5, max(game.size.x, game.size.y) * 0.55 + wallHeight,
        game.size.y * 0.5
    ).setRotation(-PI / 2, 0.0, 0.0)

    val walls = Entity("Walls", scene)
    for (wall in game.walls) {
        val wallThickness = 0.03
        val cx = (wall.start.x + wall.end.x) * 0.5
        val cz = (wall.start.y + wall.end.y) * 0.5
        val wallE = Entity(walls)
            .setPosition(cx, wallHeight * 0.5, cz)
            .setScale(
                wallThickness + (wall.end.x - wall.start.x) * 0.5, wallHeight * 0.5,
                wallThickness + (wall.end.y - wall.start.y) * 0.5,
            )
        wallE.add(MeshComponent(flatCube.front))
    }

    for (pos in game.voidPositions) {
        val block = Entity(walls)
            .setPosition(pos.x + 0.5, 0.0, pos.y + 0.5)
            .setScale(0.5)
        block.add(MeshComponent(flatCube.front))
    }

    val floor = Entity("Floor", scene)
        .setPosition(game.size.x * 0.5, -1.0, game.size.y * 0.5)
        .setScale(game.size.x * 0.5, 1.0, game.size.y * 0.5)
    floor.add(MeshComponent(flatCube.front))


    val collectibles = Entity("Gems", scene)
    val collectibleMesh = IcosahedronModel.createIcosphere(3)
    collectibleMesh.materials = listOf(getReference("materials/Golden.json"))
    for (collectible in game.collectables) {
        val entity = Entity(collectibles)
            .setPosition(collectible.x + 0.5, 0.15, collectible.y + 0.5)
            .setScale(0.13)
        entity.add(MeshComponent(collectibleMesh))
        collectibleLookup[collectible] = entity
    }

    val playerMaterial = Material().apply {
        roughnessMinMax.set(0.2f)
        darkOrange.toVecRGBA(diffuseBase)
    }

    val ghostMaterial = Material().apply {
        metallicMinMax.set(1f)
        roughnessMinMax.set(0.2f)
        cornFlowerBlue.toVecRGBA(diffuseBase)
        pipelineStage = PipelineStage.TRANSPARENT
    }

    val enemies = Entity("Enemies", scene)
    val enemyMesh = getReference("res://meshes/CuteGhost.fbx")
    for (enemy in game.enemies) {
        val entity = Entity(enemies)
            .setPosition(enemy.currPosition.x + 0.5, 0.0, enemy.currPosition.y + 0.5)
            .setScale(0.1)
        val comp = MeshComponent(enemyMesh)
        comp.materials = listOf(ghostMaterial.ref)
        entity.add(comp)
    }

    val player = Entity("Player", scene)
        .setPosition(0.0, 0.0, 0.0)
        .setScale(0.1)
    player.add(MeshComponent(enemyMesh).apply {
        materials = listOf(playerMaterial.ref)
    })
    scene.add(PacmanControls(game, camEntity, enemies, player))

    return scene
}

fun main() {
    // todo show lives and score
    disableRenderDoc()
    OfficialExtensions.initForTests()
    val world = spatialPacmanGame()
    val renderView = RenderView1(PlayMode.PLAYING, world, style)
    val sceneView = SceneView(renderView, style)
    val player = LocalPlayer()
    renderView.localPlayer = player
    world.getComponentInChildren(Camera::class)!!.use(player)
    testPureUI("Spatial Pacman", sceneView)
}