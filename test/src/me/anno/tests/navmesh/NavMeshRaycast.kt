package me.anno.tests.navmesh

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Updatable
import me.anno.engine.ECSRegistry
import me.anno.engine.EngineBase
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.CullMode
import me.anno.maths.Maths
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.recast.NavMesh
import me.anno.recast.NavMeshAgent
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.OS.res
import org.joml.Vector3d
import org.recast4j.detour.*
import org.recast4j.detour.crowd.Crowd
import org.recast4j.detour.crowd.CrowdConfig
import java.util.Random
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.max

// walk along path
class AgentController1a(
    val meshEntity: Entity,
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
        val flagTransform = flag.transform
        val crowdAgent = crowdAgent ?: return
        flagTransform.globalPosition = flagTransform.globalPosition.set(crowdAgent.targetPosOrVel)
        flagTransform.teleportUpdate()
    }

    private var upDownAngle = 0.0
    private val raycastDir = Vector3d(0.0, -1.0, 0.0)

    override fun onUpdate() {

        if (crowdAgent == null) init()
        val crowdAgent = crowdAgent ?: return

        // move agent from src to dst
        val entity = entity!!
        val transform = entity.transform
        val nextPos = crowdAgent.currentPosition
        // todo bug in recast: velocity reaches zero for no apparent reason
        val distSq = crowdAgent.actualVelocity.lengthSquared()
        if (distSq == 0f || crowdAgent.targetPosOrVel.distanceSquared(nextPos) < 0.1f) {
            findNextTarget()
        }

        // project agent onto surface
        val lp = transform.localPosition
        val start = Vector3d(nextPos)
        start.y = lp.y + crowdAgent.params.height * 0.5
        val dist = crowdAgent.params.height.toDouble()
        val query = RayQuery(
            start, raycastDir, start + raycastDir * dist, 0.0, 0.0,
            Raycast.TRIANGLE_FRONT, mask, false, emptySet(), RayHit(dist)
        )
        val hr = Raycast.raycast(meshEntity, query)
        // DebugShapes.debugLines.add(DebugLine(start, Vector3d(raycastDir).mul(dist).add(start), -1))
        val np = if (hr) query.result.positionWS else Vector3d(nextPos)
        val dt = Time.deltaTime
        np.mix(lp, dtTo01(dt * 3.0))
        upDownAngle = mix(upDownAngle, atan((lp.y - np.y) / max(np.distance(lp), 1e-308)), dtTo01(dt * 3.0))
        entity.rotation = entity.rotation
            .identity()
            .rotateY(atan2(np.x - lp.x, np.z - lp.z))
            .rotateX(upDownAngle)
        entity.position = np
    }
}

/**
 * test recast navmesh generation and usage
 *
 * todo why is it running soo badly??? -> too many raycasts
 * */
fun main() {
    testUI("NavMeshRaycast") {

        EngineBase.enableVSync = false
        ECSRegistry.init()

        val mask = 1 shl 16
        val world = Entity("World")
        world.add(Skybox())

        val agentMeshRef = res.getChild("meshes/CuteGhost.fbx")
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

        val meshEntity = Entity(world)
            .setScale(1.5)
            .add(MeshComponent(res.getChild("meshes/NavMesh.fbx")).apply {
                collisionMask = mask
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

        val flagMesh = res.getChild("meshes/Flag.fbx")
        for (i in 0 until 500) {
            val flag = Entity("Flag", world)
            flag.scale = Vector3d(flagScale.toDouble())
            flag.add(MeshComponent(flagMesh).apply { isInstanced = true })
            val agent = Entity("Agent", world).add(
                AgentController1a(
                    meshEntity, meshData, navMesh, query, filter,
                    random, navMesh1, crowd, flag, mask
                )
            )
            Entity(agent)
                .setScale(agentScale.toDouble())
                .add(MeshComponent(agentMeshRef).apply { isInstanced = true })
        }

        world.addComponent(object : Component(), Updatable {
            override fun update(instances: Collection<Component>) {
                crowd.update(Time.deltaTime.toFloat(), null)
            }
        })

        testScene(world)

    }
}
