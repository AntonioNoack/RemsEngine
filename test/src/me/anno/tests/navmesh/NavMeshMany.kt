package me.anno.tests.navmesh

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.WindowRenderFlags
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.recast.NavMesh
import me.anno.recast.NavMeshAgent
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.OS.res
import me.anno.utils.assertions.assertNotNull
import org.joml.Vector3d
import org.recast4j.detour.*
import org.recast4j.detour.crowd.Crowd
import org.recast4j.detour.crowd.CrowdConfig
import java.util.Random
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.max

// walk along path
class AgentController1b(
    meshData: MeshData,
    navMesh: org.recast4j.detour.NavMesh,
    query: NavMeshQuery,
    filter: DefaultQueryFilter,
    random: Random,
    navMesh1: NavMesh,
    crowd: Crowd,
    val flag: Entity,
    mask: Int
) : NavMeshAgent(
    meshData, navMesh, query, filter, random,
    navMesh1, crowd, mask, 10f, 10f
), OnUpdate {

    override fun findNextTarget() {
        super.findNextTarget()
        val crowdAgent = crowdAgent ?: return
        flag.teleportToGlobal(Vector3d(crowdAgent.targetPosOrVel))
    }

    private var upDownAngle = 0.0

    val np = Vector3d()

    override fun onUpdate() {

        if (crowdAgent == null) init()

        val crowdAgent = crowdAgent ?: return
        // move agent from src to dst
        val entity = entity!!
        val nextPos = crowdAgent.currentPosition
        val distSq = crowdAgent.actualVelocity.lengthSquared()
        if (distSq == 0f || crowdAgent.targetPosOrVel.distanceSquared(nextPos) < 1f) {
            findNextTarget()
        }
        // project agent onto surface
        np.set(nextPos)
        val lp = entity.position
        val dt = Time.deltaTime
        np.mix(lp, dtTo01(dt * 3.0))
        upDownAngle = mix(upDownAngle, atan((lp.y - np.y) / max(np.distance(lp), 1e-308)), dtTo01(dt * 3.0))
        entity.rotation = entity.rotation
            .identity()
            .rotateY(atan2(np.x - lp.x, np.z - lp.z).toFloat())
            .rotateX(upDownAngle.toFloat())
        entity.position = np
    }
}

/**
 * test recast navmesh generation and usage
 *
 * todo bug: everything is at 0, why??
 * */
fun main() {
    OfficialExtensions.initForTests()
    testUI("NavMeshMany") {

        WindowRenderFlags.enableVSync = false

        val mask = 1 shl 16
        val world = Entity("World")

        val agentMeshRef = res.getChild("meshes/CuteGhost.fbx")
        val agentMesh = assertNotNull(MeshCache[agentMeshRef])
        agentMesh.calculateNormals(true)
        val agentBounds = agentMesh.getBounds()
        val agentScale = 1f
        val flagScale = 1f

        val navMesh1 = NavMesh()
        navMesh1.agentHeight = agentBounds.deltaY * agentScale
        navMesh1.agentRadius = max(agentBounds.deltaX, agentBounds.deltaZ) * agentScale * 0.5f
        navMesh1.agentMaxClimb = navMesh1.agentHeight * 0.7f
        navMesh1.collisionMask = mask
        world.add(navMesh1)
        val navMeshSrc = res.getChild("meshes/NavMesh.fbx")
        assertNotNull(MeshCache[navMeshSrc])
        world.add(Entity().apply {
            add(MeshComponent(navMeshSrc).apply {
                collisionMask = mask
            })
            setScale(2.5f)
        })

        val meshData = navMesh1.build() ?: throw IllegalStateException("Failed to build NavMesh")
        navMesh1.data = meshData

        val navMesh = NavMesh(meshData, navMesh1.maxVerticesPerPoly, 0)

        val query = NavMeshQuery(navMesh)
        val filter = DefaultQueryFilter()
        val random = Random(1234L)

        val config = CrowdConfig(navMesh1.agentRadius)
        val crowd = Crowd(config, navMesh)

        val flagMesh = res.getChild("meshes/Flag.fbx")
        assertNotNull(MeshCache[flagMesh])

        for (i in 0 until 2500) {
            val flag = Entity("Flag", world)
            flag.setScale(flagScale)
            flag.add(MeshComponent(flagMesh).apply { isInstanced = true })
            val agent = Entity("Agent", world)
            agent.add(Entity().apply {
                setScale(agentScale)
                add(MeshComponent(agentMeshRef).apply { isInstanced = true })
            })
            agent.add(
                AgentController1b(
                    meshData, navMesh, query, filter,
                    random, navMesh1, crowd, flag, mask
                )
            )
        }

        world.addComponent(object : Component(), OnUpdate {
            override fun onUpdate() {
                crowd.update(Time.deltaTime.toFloat(), null)
            }
        })

        testScene(world)

    }
}
