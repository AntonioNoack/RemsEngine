package me.anno.tests.navmesh

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.navigation.NavMesh
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ECSRegistry
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS.documents
import org.joml.Vector3d
import org.joml.Vector3f
import org.recast4j.detour.*
import org.recast4j.detour.crowd.Crowd
import org.recast4j.detour.crowd.CrowdAgent
import org.recast4j.detour.crowd.CrowdAgentParams
import org.recast4j.detour.crowd.CrowdConfig
import java.util.*
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.max

// walk along path
class AgentController(
    meshData: MeshData,
    navMesh: org.recast4j.detour.NavMesh,
    val query: NavMeshQuery,
    val filter: DefaultQueryFilter,
    val random: Random,
    val navMesh1: NavMesh,
    crowd: Crowd,
    val flag: Entity,
    val mask: Int
) : Component() {

    var currRef: FindRandomPointResult

    val agent1: CrowdAgent

    val speed = 10f

    init {

        val header = meshData.header!!
        val tileRef = navMesh.getTileRefAt(header.x, header.y, header.layer)
        currRef = query.findRandomPointWithinCircle(tileRef, Vector3f(), 200f, filter, random).result!!

        val params = CrowdAgentParams()
        params.radius = navMesh1.agentRadius
        params.height = navMesh1.agentHeight
        params.maxSpeed = speed
        params.maxAcceleration = 10f
        // other params?
        agent1 = crowd.addAgent(currRef.randomPt, params)

    }

    fun findNextTarget() {
        val nextRef = query.findRandomPointWithinCircle(
            currRef.randomRef, agent1.targetPos,
            200f, filter, random
        ).result!!
        agent1.setTarget(nextRef.randomRef, nextRef.randomPt)
        flag.teleportToGlobal(Vector3d(nextRef.randomPt))
    }

    private val raycastDir = Vector3d(0.0, -1.0, 0.0)
    override fun onUpdate(): Int {
        // move agent from src to dst
        val entity = entity!!
        val nextPos = agent1.currentPosition
        val distSq = agent1.actualVelocity.lengthSquared()
        if (!(distSq > 0f && agent1.targetPos.distanceSquared(nextPos) >= 1f)) {
            findNextTarget()
        }
        // project agent onto surface
        val lp = entity.position
        val start = Vector3d(nextPos)
        start.y = lp.y + navMesh1.agentHeight * 0.5
        val dist = navMesh1.agentHeight.toDouble()
        val world = entity.parentEntity!!
        val hr = Raycast.raycast(
            world, start, raycastDir, 0.0, 0.0,
            dist, Raycast.TRIANGLE_FRONT, mask
        )
        // DebugShapes.debugLines.add(DebugLine(start, Vector3d(raycastDir).mul(dist).add(start), -1))
        val np = hr?.positionWS ?: Vector3d(nextPos)
        val dt = Engine.deltaTime
        np.lerp(lp, dtTo01(dt * 3.0))
        upDownAngle = mix(upDownAngle, atan((lp.y - np.y) / max(np.distance(lp), 1e-308)), dtTo01(dt * 3.0))
        entity.rotation = entity.rotation
            .identity()
            .rotateY(atan2(np.x - lp.x, np.z - lp.z))
            .rotateX(upDownAngle)
        entity.position = np
        return 1
    }

    var upDownAngle = 0.0

    override fun clone() = this
    override val className = "AgentController"
}

/**
 * test recast navmesh generation and usage
 * */
fun main() {
    testUI {

        ECSRegistry.init()

        val mask = 1 shl 16
        val world = Entity("World")
        world.add(SkyBox())

        // todo loaded normals for ghost look funky
        val agentMeshRef = documents.getChild("CuteGhost.fbx")
        val agentMesh = MeshCache[agentMeshRef, false]!!
        agentMesh.calculateNormals(true)
        val agentBounds = agentMesh.ensureBounds()

        val navMesh1 = NavMesh()
        navMesh1.agentHeight = agentBounds.deltaY()
        navMesh1.agentRadius = max(agentBounds.deltaX(), agentBounds.deltaZ()) * 0.5f
        navMesh1.agentMaxClimb = navMesh1.agentHeight * 0.7f
        navMesh1.collisionMask = mask
        world.add(navMesh1)
        world.add(Entity().apply {
            add(MeshComponent(documents.getChild("NavMeshTest2.obj")).apply {
                collisionMask = mask
            })
            scale = scale.set(1.5)
        })

        val meshData = navMesh1.build()!!
        navMesh1.data = meshData

        // visualize navmesh
        if (false) world.add(MeshComponent(navMesh1.toMesh(Mesh())!!.apply {
            material = Material().apply {
                isDoubleSided = true
                diffuseBase.set(0.2f, 1f, 0.2f, 0.5f)
            }.ref
            positions!!.apply {
                for (i in indices step 3) {
                    this[i + 1] += 0.03f
                }
            }
        }.ref))

        val navMesh = NavMesh(meshData, navMesh1.maxVerticesPerPoly, 0)

        val query = NavMeshQuery(navMesh)
        val filter = DefaultQueryFilter()
        val random = Random(System.nanoTime())

        val config = CrowdConfig(navMesh1.agentRadius)
        val crowd = Crowd(config, navMesh)

        val flagMesh = documents.getChild("Flag.fbx")
        // todo agents should avoid each other
        for (i in 0 until 250) {
            val flag = Entity("Flag")
            flag.add(MeshComponent(flagMesh).apply { isInstanced = true })
            world.add(flag)
            val agent = Entity("Agent")
            agent.add(MeshComponent(agentMeshRef).apply { isInstanced = true })
            agent.add(AgentController(meshData, navMesh, query, filter, random, navMesh1, crowd, flag, mask))
            world.add(agent)
        }

        world.addComponent(object : Component() {
            override fun clone() = this
            override fun onUpdate(): Int {
                crowd.update(Engine.deltaTime, null)
                return 1
            }
        })

        testScene(world)

    }
}
