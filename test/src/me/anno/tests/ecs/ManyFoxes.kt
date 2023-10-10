package me.anno.tests.ecs

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimRenderer
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.min
import me.anno.utils.OS.downloads
import org.joml.Matrix4x3d
import org.joml.Vector3f
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

class FoxSpeedController : Component() {

    var localTime = 0.0
    var speed = 1.0
        set(value) {
            if (field != value) {
                field = value
                updateSpeed()
            }
        }

    fun updateSpeed() {
        val speed = speed.toFloat()
        entity?.forAllComponentsInChildren(AnimRenderer::class) {
            it.animations.first().speed = speed
        }
    }

    override fun onUpdate(): Int {
        localTime += speed * Time.deltaTime
        return 1
    }

    override val className: String
        get() = "FoxSpeedController"
}

/**
 * Extract only visual elements
 * todo this could be part of our standard functions 😄
 * */
fun optimizeEntity(entity: Entity): Entity {

    entity.validateTransform()

    val clone = Entity()
    val transformToEntity = HashMap<Matrix4x3d, Entity>()
    fun getTransformedChild(matrix: Matrix4x3d): Entity {
        return if (matrix.isIdentity()) clone
        else transformToEntity.getOrPut(matrix) {
            val child = Entity(clone)
            child.transform.setLocal(matrix)
            child
        }
    }

    fun addTransformedChild(component: Component) {
        val matrix = component.entity!!.transform.globalTransform
        getTransformedChild(matrix).add(component.clone() as Component)
    }

    entity.forAllComponentsInChildren(Renderable::class) {
        addTransformedChild(it as Component)
    }

    return clone
}

/**
 * implement https://bevyengine.org/examples/Stress%20Tests/many-foxes/
 * circles of alternating-direction running foxes in a circle
 *
 * debug and improve performance, 40 fps is a little low
 * -> there was too many unused bone entities, now it's running at 130 fps
 * */
fun main() {

    ECSRegistry.initMeshes()
    registerCustomClass(FoxSpeedController())

    val scene = Entity()
    val foxPrefab = PrefabCache[downloads.getChild("3d/azeria/scene.gltf")]!!
    val fox = optimizeEntity(foxPrefab.createInstance() as Entity)

    val controller = FoxSpeedController()
    scene.add(controller)

    val foxCount = 1000
    var remainingFoxes = foxCount
    val random = Random()
    var maxRadius = 1.0
    for (i in 0 until foxCount) {
        val dir = i.and(1) * 2 - 1
        val radius = i * 2.0 + 1.0
        val count = min(round(radius * 3.0).toInt(), remainingFoxes)
        val ring = Entity("Ring $i", scene)
        ring.add(object : Component() {
            override fun onUpdate(): Int {
                val progress = dir * controller.localTime / radius
                entity!!.setRotation(0.0, progress, 0.0)
                return 1
            }
        })
        for (j in 0 until count) {
            val entity = fox.clone() as Entity
            val angle = j * TAU / count
            entity.setPosition(cos(angle) * radius, 0.08, -sin(angle) * radius)
            entity.setRotation(0.0, if (dir < 0) angle else angle + PI, 0.0)
            entity.setScale(0.01)
            val randomProgress = random.nextFloat() * 100f
            entity.forAllComponentsInChildren(AnimRenderer::class) {
                for (state in it.animations) {
                    val duration = AnimationCache[state.source]!!.duration
                    state.progress = randomProgress * duration
                }
                it.isInstanced = true
            }
            ring.add(entity)
        }
        remainingFoxes -= count
        maxRadius = radius
        if (remainingFoxes == 0) break
    }

    // add floor
    val floorSize = maxRadius.toFloat() * 1.1f
    val floorMesh = PlaneModel.createPlane(2, 2, Vector3f(), Vector3f(floorSize, 0f, 0f), Vector3f(0f, 0f, floorSize))
    scene.add(MeshComponent(floorMesh))

    // run scene
    testSceneWithUI("🦊🦊🦊 Many Foxes! 🦊🦊🦊", scene)
}