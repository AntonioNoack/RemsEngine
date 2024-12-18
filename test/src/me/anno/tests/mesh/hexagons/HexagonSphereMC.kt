package me.anno.tests.mesh.hexagons

import me.anno.bullet.BulletPhysics
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.maths.chunks.spherical.Hexagon
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.systems.Systems
import me.anno.ecs.systems.Updatable
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.PipelineStage
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.clamp
import me.anno.maths.noise.FullNoise
import me.anno.utils.hpc.threadLocal
import me.anno.utils.pooling.JomlPools
import me.anno.utils.pooling.Pools
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Triangles
import org.joml.Quaterniond
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

// create a Minecraft world on a hex sphere :3
// use chunks

fun main() {

    val n = 100
    val (hexagons, _) = createHexSphere(n)
    val world = HexagonSphereMCWorld(HexagonSphere(n, 1))

    val scene = Entity()
    val mesh = createMesh(hexagons, world, null)
    scene.add(MeshComponent(mesh))

    val sky = Skybox()
    sky.spherical = true
    scene.add(sky)

    val physics = BulletPhysics()
    Systems.registerSystem(physics)
    physics.gravity.set(0.0)

    val sun = DirectionalLight()
    sun.shadowMapCascades = 1
    sun.shadowMapResolution = 4096
    sun.autoUpdate = 10
    sun.color.set(5f)
    val sunEntity = Entity("Sun")
    sunEntity.add(sun)
    sunEntity.setScale(2.0)
    sunEntity.rotation = Quaterniond(sky.sunRotation)

    sunEntity.add(object : Component(), Updatable {
        override fun update(instances: Collection<Component>) {
            sky.applyOntoSun(sunEntity, sun, 5f)
        }
    })

    scene.add(sunEntity)

    testSceneWithUI("HexSphere MC", scene) {
        if (false) {
            it.renderView.playMode = PlayMode.PLAYING // remove grid
            it.renderView.enableOrbiting = false
            it.renderView.radius = 0.1
            it.playControls = ControllerOnSphere(it.renderView, sky)
        }
    }
}

// todo change light direction to come from sun

open class ControllerOnSphere(
    rv: RenderView,
    val sky: Skybox?
) : ControlScheme(rv) {

    // todo set and destroy blocks

    val forward = Vector3d(0.0, 0.0, -1.0)
    val right = Vector3d(1.0, 0.0, 0.0)
    val position = rv.orbitCenter
    val up = Vector3d()

    init {
        if (position.length() < 1e-16) {
            position.set(0.0, 1.52, 0.0)
        }// todo else find axes :)
        position.normalize(up)
    }

    override fun rotateCamera(vx: Float, vy: Float, vz: Float) {
        val axis = up
        val s = 1.0.toRadians()
        forward.rotateAxis(vy * s, axis.x, axis.y, axis.z)
        right.rotateAxis(vy * s, axis.x, axis.y, axis.z)
        val dx = vx * s
        // todo clamp angle
        // val currAngle = forward.angleSigned(up, right)
        // val dx2 = clamp(currAngle + dx, 1.0.toRadians(), 179.0.toRadians()) - currAngle
        forward.rotateAxis(dx, right.x, right.y, right.z)
        correctAxes()
    }

    fun correctAxes() {
        val dirY = up
        val er1 = right.dot(dirY)
        right.sub(er1 * dirY.x, er1 * dirY.y, er1 * dirY.z)
        val dirZ = forward
        val er2 = right.dot(dirZ)
        right.sub(er2 * dirZ.x, er2 * dirZ.y, er2 * dirZ.z)
        right.normalize()
    }

    override fun updateViewRotation(jump: Boolean) {
        renderView.orbitRotation.identity()
            .lookAlong(forward, up)
            .invert()
        renderView.updateEditorCameraTransform()
        invalidateDrawing()
    }

    override fun moveCamera(dx: Double, dy: Double, dz: Double) {
        val height = position.length()
        position.add(dx * right.x, dx * right.y, dx * right.z)
        position.sub(dz * forward.x, dz * forward.y, dz * forward.z)
        position.normalize(height + dy)
        onChangePosition()
    }

    fun onChangePosition() {
        val rot = JomlPools.quat4d.borrow().rotationTo(up, position)
        position.normalize(up)
        rot.transform(forward)
        rot.transform(right)
        sky?.worldRotation?.mul(-rot.x.toFloat(), -rot.y.toFloat(), -rot.z.toFloat(), rot.w.toFloat())
        correctAxes()
    }
}

// todo free: one small world, basic MC-like mechanics
// todo cost: unlimited worlds
// todo cost: multiplayer
// todo cost: chemistry dlc, mineral dlc, gun play dlc...