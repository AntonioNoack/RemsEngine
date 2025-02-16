package me.anno.games.pacman

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
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView1
import me.anno.engine.ui.render.SceneView
import me.anno.games.pacman.logic.Moveable
import me.anno.games.pacman.logic.PacmanLogic
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.pipeline.PipelineStage
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.dtTo01
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.UIColors.cornFlowerBlue
import me.anno.ui.UIColors.darkOrange
import me.anno.ui.debug.PureTestEngine.Companion.testPureUI
import me.anno.utils.OS.res
import org.joml.Vector2f
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class PacmanControls(
    val game: PacmanLogic, val camEntity: Entity,
    val enemies: Entity, val player: Entity
) : Component(), OnUpdate {

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
            rot.rotationY(angle)
        }
        prevPosition.mix(currPosition, mixDt)
        transform.localPosition = pos
        transform.localRotation = rot
        entity.invalidateOwnAABB()
    }

    val baseCameraHeight = camEntity.position.y
    override fun onUpdate() {
        // controls
        game.updateControls()
        // update visuals
        val rv = RenderView.currentInstance
        if (rv != null) {
            val transform = camEntity.transform
            val pos = transform.localPosition
            pos.y = baseCameraHeight * rv.height.toDouble() / min(rv.width, rv.height)
            transform.localPosition = pos
        }
        val mixDt = dtTo01(Time.deltaTime.toFloat() * 5f)
        for (i in game.enemies.indices) {
            val enemy = game.enemies[i]
            val entity = enemies.children[i]
            setPos(enemy, entity, mixDt)
        }
        setPos(game.player, player, mixDt)
    }
}

/**
 * renders a pacman game with standard flat UI
 * */
fun spatialPacmanGame(): Entity {

    val collectibleLookup = HashMap<Vector2f, Entity>()
    val scene = Entity("Scene")
    val game = object : PacmanLogic() {
        override fun onCollect(collectible: Vector2f) {
            collectibleLookup[collectible]!!.removeFromParent()
        }
    }

    val wallHeight = 0.50f
    val camEntity = Entity("Camera", scene)
        .setPosition(game.size.x * 0.5, max(game.size.x, game.size.y) * 0.55 + wallHeight, game.size.y * 0.5)
        .setRotation(-PIf / 2f, 0f, 0f)
        .add(Camera())

    val walls = Entity("Walls", scene)
    for ((start, end) in game.walls) {
        val wallThickness = 0.03f
        val cx = (start.x + end.x) * 0.5
        val cz = (start.y + end.y) * 0.5
        Entity("Wall[$start-$end]", walls)
            .setPosition(cx, wallHeight * 0.5, cz)
            .setScale(
                wallThickness + (end.x - start.x) * 0.5f, wallHeight * 0.5f,
                wallThickness + (end.y - start.y) * 0.5f,
            )
            .add(MeshComponent(flatCube.front))
    }

    for (pos in game.voidPositions) {
        Entity("Void[$pos]", walls)
            .setPosition(pos.x + 0.5, 0.0, pos.y + 0.5)
            .setScale(0.5f)
            .add(MeshComponent(flatCube.front))
    }

    Entity("Floor", scene)
        .setPosition(game.size.x * 0.5, -1.0, game.size.y * 0.5)
        .setScale(game.size.x * 0.5f, 1f, game.size.y * 0.5f)
        .add(MeshComponent(flatCube.front))

    val collectibles = Entity("Gems", scene)
    val collectibleMesh = IcosahedronModel.createIcosphere(3)
    collectibleMesh.materials = listOf(getReference("materials/Golden.json"))
    for (pos in game.collectables) {
        collectibleLookup[pos] = Entity(collectibles)
            .setPosition(pos.x + 0.5, 0.15, pos.y + 0.5)
            .setScale(0.13f)
            .add(MeshComponent(collectibleMesh))
    }

    val playerMaterial = Material.diffuse(darkOrange).apply {
        roughnessMinMax.set(0.2f)
    }

    val ghostMaterial = Material.diffuse(cornFlowerBlue).apply {
        metallicMinMax.set(1f)
        roughnessMinMax.set(0.2f)
        pipelineStage = PipelineStage.TRANSPARENT
    }

    val enemies = Entity("Enemies", scene)
    val enemyMesh = res.getChild("meshes/CuteGhost.fbx")
    for (enemy in game.enemies) {
        Entity(enemies)
            .setPosition(enemy.currPosition.x + 0.5, 0.0, enemy.currPosition.y + 0.5)
            .setScale(0.1f)
            .add(MeshComponent(enemyMesh, ghostMaterial))
    }

    val player = Entity("Player", scene)
        .setPosition(0.0, 0.0, 0.0)
        .setScale(0.1f)
        .add(MeshComponent(enemyMesh, playerMaterial))

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