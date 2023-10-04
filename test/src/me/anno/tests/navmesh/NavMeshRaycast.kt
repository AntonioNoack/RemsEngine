package me.anno.tests.navmesh

import me.anno.Engine
import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.recast.NavMesh
import me.anno.recast.NavMeshAgent
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.ECSRegistry
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.CullMode
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.studio.StudioBase
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS.documents
import org.joml.Vector3d
import org.recast4j.detour.*
import org.recast4j.detour.crowd.Crowd
import org.recast4j.detour.crowd.CrowdConfig
import java.util.*
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.max

// walk along path
class AgentController1a(
    meshData: MeshData,
    navMesh: org.recast4j.detour.NavMesh,
    query: NavMeshQuery,
    filter: DefaultQueryFilter,
    random: Random,
    navMesh1: NavMesh,
    crowd: Crowd,
    val flag: Entity,
    mask: Int
) : NavMeshAgent(meshData, navMesh, query, filter, random, navMesh1, crowd, mask) {

    override fun findNextTarget() {
        super.findNextTarget()
        val tr = flag.transform
        tr.globalPosition = tr.globalPosition.set(crowdAgent.targetPos)
        tr.teleportUpdate()
    }

    private var upDownAngle = 0.0
    private val raycastDir = Vector3d(0.0, -1.0, 0.0)

    private var ctr = (Math.random() * 16).toInt()

    override fun onUpdate(): Int {

        if (ctr++ < 16) return 1
        else ctr = 0

        // move agent from src to dst
        val entity = entity!!
        val nextPos = crowdAgent.currentPosition
        val distSq = crowdAgent.actualVelocity.lengthSquared()
        if (!(distSq > 0f && crowdAgent.targetPos.distanceSquared(nextPos) >= 1f)) {
            findNextTarget()
        }
        // project agent onto surface
        val lp = entity.transform.localPosition
        val start = Vector3d(nextPos)
        start.y = lp.y + crowdAgent.params.height * 0.5
        val dist = crowdAgent.params.height.toDouble()
        val world = entity.parentEntity!!
        val hr = Raycast.raycast(
            world, start, raycastDir, 0.0, 0.0,
            dist, Raycast.TRIANGLE_FRONT, mask
        )
        // DebugShapes.debugLines.add(DebugLine(start, Vector3d(raycastDir).mul(dist).add(start), -1))
        val np = hr?.positionWS ?: Vector3d(nextPos)
        val dt = Time.deltaTime
        np.lerp(lp, dtTo01(dt * 3.0))
        upDownAngle = mix(upDownAngle, atan((lp.y - np.y) / max(np.distance(lp), 1e-308)), dtTo01(dt * 3.0))
        entity.rotation = entity.rotation
            .identity()
            .rotateY(atan2(np.x - lp.x, np.z - lp.z))
            .rotateX(upDownAngle)
        entity.position = np
        return 1
    }

}

/**
 * test recast navmesh generation and usage
 * */
fun main() {
    testUI("NavMeshRaycast") {

        StudioBase.instance?.enableVSync = false
        ECSRegistry.init()

        val mask = 1 shl 16
        val world = Entity("World")
        world.add(Skybox())

        val agentMeshRef = documents.getChild("CuteGhost.fbx")
        val agentMesh = MeshCache[agentMeshRef, false]!!
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
        world.add(Entity().apply {
            add(MeshComponent(documents.getChild("NavMeshTest2.obj")).apply {
                collisionMask = mask
            })
            scale = scale.set(1.5)
        })

        val meshData = navMesh1.build() ?: throw IllegalStateException("Failed to build NavMesh")
        navMesh1.data = meshData

        // visualize navmesh
        if (false) world.add(MeshComponent(navMesh1.toMesh(Mesh())!!.apply {
            material = Material().apply {
                cullMode = CullMode.BOTH
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
        val random = Random(1234L)

        val config = CrowdConfig(navMesh1.agentRadius)
        val crowd = Crowd(config, navMesh)

        val flagMesh = documents.getChild("Flag.fbx")
        for (i in 0 until 500) {
            val flag = Entity("Flag")
            flag.scale = Vector3d(flagScale.toDouble())
            flag.add(MeshComponent(flagMesh).apply { isInstanced = true })
            world.add(flag)
            val agent = Entity("Agent")
            agent.add(Entity().apply {
                scale = Vector3d(agentScale.toDouble())
                add(MeshComponent(agentMeshRef).apply { isInstanced = true })
            })
            agent.add(AgentController1a(meshData, navMesh, query, filter, random, navMesh1, crowd, flag, mask))
            world.add(agent)
        }

        world.addComponent(object : Component() {
            override fun onUpdate(): Int {
                crowd.update(Time.deltaTime.toFloat(), null)
                return 1
            }
        })

        testScene(world)

    }
}
