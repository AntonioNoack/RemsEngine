package me.anno.tests.utils

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Updatable
import me.anno.engine.OfficialExtensions
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.input.Key
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.mesh.Shapes.flatCube
import me.anno.recast.NavMesh
import me.anno.recast.NavMeshAgent
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.recast4j.detour.DefaultQueryFilter
import org.recast4j.detour.NavMeshQuery
import org.recast4j.detour.crowd.Crowd
import org.recast4j.detour.crowd.CrowdConfig
import java.util.Random
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.min

/**
 * eight-legged spider, which walks
 *   - terrain
 *   - path finding on it
 *   - legs using IK
 *   - find new targets in groups of 4 legs
 *   - move body with terrain
 * */
fun main() {

    OfficialExtensions.initForTests()
    val scene = Entity("Scene")
    val terrain = Entity("Terrain", scene)
    terrain.add(MeshComponent(getReference("res://meshes/NavMesh.fbx")))
    terrain.setScale(2.5)

    val navMesh1 = NavMesh()
    scene.add(navMesh1)

    val sunE = Entity("Sun", scene).setScale(100.0)
    val sun = DirectionalLight()
    sun.shadowMapCascades = 1
    sun.autoUpdate = 2
    sun.color.set(2.5f, 1.7f, 1.3f)
    sunE.add(sun)

    val sky = Skybox()
    sky.applyOntoSun(sunE, sun, 20f)
    scene.add(sky)

    navMesh1.cellSize = 2f
    navMesh1.cellHeight = 5f
    navMesh1.agentHeight = 1f
    navMesh1.agentRadius = 1.5f
    navMesh1.agentMaxClimb = 10f
    navMesh1.collisionMask = 1

    val meshData = navMesh1.build()!!
    navMesh1.data = meshData
    val navMesh = org.recast4j.detour.NavMesh(meshData, navMesh1.maxVerticesPerPoly, 0)

    if (false) scene.add(MeshComponent(navMesh1.toMesh(Mesh())!!.apply {
        makeLineMesh(true)
        material = Material().apply {
            cullMode = CullMode.BOTH
            diffuseBase.set(0.2f, 1f, 0.2f, 0.5f)
        }.ref
        positions!!.apply {
            for (i in indices step 3) {
                this[i + 1] += 0.03f
            }
        }
    }.ref)).apply {
        collisionBits = 0
    }

    val query = NavMeshQuery(navMesh)
    val filter = DefaultQueryFilter()

    val config = CrowdConfig(navMesh1.agentRadius)
    val crowd = Crowd(config, navMesh)

    val baseY = 40.0
    val spider = Entity("Spider", scene)
    spider.setPosition(0.0, baseY, 0.0)

    val xs = listOf(-1, +1)
    val black1 = Material.diffuse(0x070707)
    val white = Material.diffuse(0xffffff)
    val gray = Material.diffuse(0x333333)
    val gray2 = Material.diffuse(0x171717)

    fun add(target: Entity, offset: Vector3f, scale: Vector3f, material: Material) {
        target.add(MeshComponent(flatCube.linear(offset, scale).front, material))
    }

    add(spider, Vector3f(0f, 0.2f, -0.1f), Vector3f(1.0f, 0.6f, 1.5f), gray)
    add(spider, Vector3f(0f, 0.2f, +2.8f), Vector3f(1.4f, 1.0f, 1.4f), gray)

    for (x in xs) {
        // teeth
        add(spider, Vector3f(x * 0.4f, -0.4f, -1.68f), Vector3f(0.12f, 0.5f, 0.12f), white)
        // eyes
        add(spider, Vector3f(x * 0.3f, +0.6f, -1.68f), Vector3f(0.08f), black1)
    }

    val legDimensions = listOf(
        Vector3f(1f, 0.20f, 0.20f),
        Vector3f(1f, 0.18f, 0.18f),
        Vector3f(1f, 0.16f, 0.16f),
        Vector3f(0.4f, 0.12f, 0.12f),
    )

    // create 8 legs
    val upperLegMesh = flatCube.linear(Vector3f(-0.9f, 0f, 0f), legDimensions[0]).front
    val middleLegMesh = flatCube.linear(Vector3f(-0.9f, 0f, 0f), legDimensions[1]).front
    val lowerLegMesh = flatCube.linear(Vector3f(-0.9f, 0f, 0f), legDimensions[2]).front
    val footMesh = flatCube.linear(Vector3f(-0.3f, 0f, 0f), legDimensions[3]).front

    fun calc(alpha: Double, delta: Vector3d, c: Double): Pair<Double, Double> {
        val len = delta.length()
        val gamma = 2.0 * asin(min(len / (2 * c), 1.0))
        val a2 = alpha + PI / 2
        val x1 = (PI - gamma) / 2
        val z1 = atan2(delta.x, delta.y)
        val beta = PI - x1 - z1 - a2
        return beta to (PI - gamma)
    }

    // know the spider's transform in the future to plan steps
    val stepFrequency = 3.0
    val futureTransform = Matrix4x3d()
    val predictionDeltaTime = 0.5 / stepFrequency

    val agent = object : NavMeshAgent(
        meshData, navMesh, query, filter, Random(1234),
        navMesh1, crowd, 1, 10f, 300f
    ), Updatable {

        val velocity = Vector3d(1.0, 0.0, 0.0)
        val lastPos = Vector3d()
        val angleDictator = Vector3d(0.0, 1.0, 0.0)

        override fun update(instances: Collection<Component>) {

            crowd.update(Time.deltaTime.toFloat(), null)

            // move spider along path to target
            // find proper position
            val start = Vector3d(crowdAgent.currentPosition).add(0.0, 10.0, 0.0)
            val query0 =
                RayQuery(start, Vector3d(0.0, -1.0, 0.0), 40.0, Raycast.TRIANGLE_FRONT, -1, false, setOf(spider))
            val hit = Raycast.raycastClosestHit(
                scene, query0
            )

            spider.position = (if (hit) query0.result.positionWS else Vector3d(crowdAgent.currentPosition))
                .add(0.0, 1.0, 0.0)

            if (hit) {
                val newAngle = query0.result.shadingNormalWS.normalize()
                angleDictator.lerp(newAngle, dtTo01(5.0 * Time.deltaTime))
            }
            if (velocity.lengthSquared() < 1e-16) velocity.set(1.0, 0.0, 0.0)
            if (angleDictator.lengthSquared() < 1e-16) angleDictator.set(0.0, 1.0, 0.0)

            val rotY = -atan2(velocity.z, velocity.x) - PI / 2
            val rotX = atan2(velocity.y, length(velocity.x, velocity.z))
            val tmp = Vector3d(angleDictator).rotateY(-rotY)
            val rotZ = -atan2(tmp.x, angleDictator.y)

            val currPos = spider.position
            if (lastPos.distanceSquared(currPos) > 1e-8) {
                val velocity1 = Vector3d(currPos).sub(lastPos).div(Time.deltaTime)
                velocity.lerp(Vector3d(velocity1), dtTo01(5.0 * Time.deltaTime))
                lastPos.set(currPos)
            }

            spider.setRotation(rotX, rotY, rotZ)
            futureTransform.identity()
                .setTranslation(spider.position + velocity * predictionDeltaTime)
                .rotateZ(rotZ)
                .rotateX(rotX)
                .rotateY(rotY)
        }
    }
    spider.add(agent)

    // animate body with legs
    for (x in xs) {
        for (z in 0 until 4) {

            val zf = z - 1.5
            val upperLeg = Entity(spider)
            upperLeg.add(MeshComponent(upperLegMesh, gray2))
            upperLeg.setPosition(x * 0.9, 0.0, zf * 0.5)
            val middleLeg = Entity(upperLeg)
            middleLeg.add(MeshComponent(middleLegMesh, gray2))
            middleLeg.setPosition(-1.8, 0.0, 0.0)
            val lowerLeg = Entity(middleLeg)
            lowerLeg.add(MeshComponent(lowerLegMesh, gray2))
            lowerLeg.setPosition(-1.8, 0.0, 0.0)
            val foot = Entity(lowerLeg)
            foot.add(MeshComponent(footMesh, black1))
            foot.setPosition(-1.8, 0.0, 0.0)

            class Leg : Component(), OnUpdate {

                val zero = Vector3d(x * 3.0, -0.5, zf)
                val target = Vector3d()
                val lastTarget = Vector3d()

                var timeAccu = if ((z + (x + 1) / 2).hasFlag(1)) 1.0 else 1.5
                val angles = Vector4d()
                var isWalking = false

                override fun onUpdate() {

                    val step = Time.deltaTime * stepFrequency
                    timeAccu += step
                    if (timeAccu > 1.0) {
                        timeAccu -= 1.0
                        // find new target to step on
                        // use raycast to find floor's y
                        futureTransform.transformPosition(zero, target)
                        val up = 5.0
                        val len = 10.0
                        val query1 = RayQuery(
                            Vector3d(0.0, up, 0.0).add(target), Vector3d(0.0, -1.0, 0.0), len,
                            -1, -1, false, setOf(spider)
                        )
                        if (Raycast.raycastClosestHit(scene, query1)) {
                            val footThickness = legDimensions.last().y
                            target.y += up - query1.result.distance - footThickness
                        }
                        isWalking = lastTarget.distanceSquared(target) > 1e-2 * sq(step)
                        lastTarget.set(target)
                    }

                    val dtMix = dtTo01(5.0 * step)

                    // convert target into "spiderBody" space
                    val toLocal = spider.transform.globalTransform.invert(Matrix4x3d())
                    val localTarget = toLocal.transformPosition(target, Vector3d())
                    // add stepping up at the start of each step
                    if (isWalking) localTarget.y += max(1.0 - 10.0 * timeAccu, 0.0)

                    val upperLegPos = toLocal.transformPosition(upperLeg.transform.globalPosition, Vector3d())
                    val distance0 = length(upperLegPos.x - localTarget.x, upperLegPos.z - localTarget.z)
                    val distance = min(distance0 / (3 * 1.8), 1.0)
                    val downwards = (upperLegPos.y - localTarget.y) / distance0 * .5
                    val alpha = -clamp(
                        mix(
                            mix(1.57, -0.2, downwards), // close
                            mix(0.7, -0.5, downwards), // far away
                            distance
                        ), -1.0, PI / 2
                    )
                    val rotY = -x * atan2(
                        upperLeg.position.z - localTarget.z,
                        -x * (upperLeg.position.x - localTarget.x)
                    )
                    val rotY1 = clamp(-rotY, -1.2, 1.2) + (if (x > 0) PI else 0.0)
                    angles.x = mix(angles.x, alpha, dtMix)
                    angles.y = mix(angles.y, rotY1, dtMix)
                    upperLeg.setRotation(0.0, angles.y, angles.x)
                    upperLeg.validateTransform()
                    middleLeg.validateTransform()
                    val middleLegPos = toLocal.transformPosition(middleLeg.transform.globalPosition, Vector3d())
                    val delta = Vector3d(middleLegPos).sub(localTarget).rotateY(rotY) // z-component is 0
                    delta.x *= -x
                    val (beta, gamma) = calc(alpha, delta, 1.8)
                    val beta1 = max(beta, 0.0)
                    val gamma1 = max(gamma, 0.0)
                    angles.z = mix(angles.z, beta1, dtMix)
                    angles.w = mix(angles.w, gamma1, dtMix)
                    middleLeg.setRotation(0.0, 0.0, angles.z)
                    lowerLeg.setRotation(0.0, 0.0, angles.w)
                    foot.setRotation(0.0, 0.0, -(angles.x + angles.z + angles.w))
                }
            }

            upperLeg.add(Leg())
        }
    }

    testSceneWithUI("Spider IK", scene) {
        it.editControls = object : DraggingControls(it.renderView) {
            override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
                if (button == Key.BUTTON_LEFT) {
                    val ci = it.renderView
                    val query0 = RayQuery(
                        ci.cameraPosition, ci.getMouseRayDirection(), 1e3,
                        -1, -1, false, setOf(spider)
                    )
                    if (Raycast.raycastClosestHit(scene, query0)) {
                        val pt0 = Vector3f(query0.result.positionWS)
                        val poly = query.findNearestPoly(pt0, Vector3f(1e3f), filter).result
                        val pos = poly?.nearestPos
                        if (poly != null && pos != null) {
                            agent.crowdAgent.setTarget(poly.nearestRef, pos)
                        }
                    }
                }
            }
        }
    }
}