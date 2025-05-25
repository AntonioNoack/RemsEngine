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
import me.anno.engine.ECSRegistry
import me.anno.engine.WindowRenderFlags
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.CullMode
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.recast.CrowdUpdateComponent
import me.anno.recast.NavMeshAgent
import me.anno.recast.NavMeshBuilder
import me.anno.recast.NavMeshData
import me.anno.recast.NavMeshDebug.toMesh
import me.anno.recast.NavMeshDebugComponent
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.OS.res
import me.anno.utils.structures.lists.Lists.wrap
import org.joml.Vector3d
import org.joml.Vector3f
import org.recast4j.detour.*
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.max
import kotlin.random.Random

// walk along path
class AgentController1a(
    data: NavMeshData,
    val meshEntity: Entity,
    val flag: Entity,
) : NavMeshAgent(data) {

    override fun findNextTarget(random: Random) {
        super.findNextTarget(random)
        val flagTransform = flag.transform
        val crowdAgent = crowdAgent ?: return
        flagTransform.setLocalPosition(crowdAgent.targetPosOrVel)
    }

    private var upDownAngle = 0.0
    private val raycastDir = Vector3f(0f, -1f, 0f)
    val random = Random(Time.nanoTime)

    override fun onUpdate() {
        super.onUpdate()
        val crowdAgent = crowdAgent ?: return

        // move agent from src to dst
        val entity = entity!!
        val transform = entity.transform
        val nextPos = crowdAgent.currentPosition
        // todo bug in recast: velocity reaches zero for no apparent reason
        val distSq = crowdAgent.actualVelocity.lengthSquared()
        if (distSq == 0f || crowdAgent.targetPosOrVel.distanceSquared(nextPos) < 0.1f) {
            findNextTarget(random)
        }

        // project agent onto surface
        val lp = transform.localPosition
        val start = Vector3d(nextPos)
        start.y = lp.y + crowdAgent.params.height * 0.5
        val dist = crowdAgent.params.height.toDouble()
        val query = RayQuery(
            start, raycastDir, start + Vector3d(raycastDir) * dist, 0.0, 0.0,
            Raycast.TRIANGLE_FRONT, data.collisionMask, false, emptySet(), RayHit(dist)
        )
        val hr = Raycast.raycast(meshEntity, query)
        // DebugShapes.debugLines.add(DebugLine(start, Vector3d(raycastDir).mul(dist).add(start), -1))
        val np = if (hr) query.result.positionWS else Vector3d(nextPos)
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
 * todo why is it running soo badly??? -> too many raycasts
 * */
fun main() {
    testUI("NavMeshRaycast") {

        WindowRenderFlags.enableVSync = false
        ECSRegistry.init()

        val mask = 1 shl 16
        val world = Entity("World")
        world.add(Skybox())

        val agentMeshRef = res.getChild("meshes/CuteGhost.fbx")
        val agentMesh = MeshCache[agentMeshRef, false] as Mesh
        agentMesh.calculateNormals(true)
        val agentBounds = agentMesh.getBounds()
        val agentScale = 1f
        val flagScale = 1f

        val builder = NavMeshBuilder()
        builder.agentType.height = agentBounds.deltaY * agentScale
        builder.agentType.radius = max(agentBounds.deltaX, agentBounds.deltaZ) * agentScale * 0.5f
        builder.agentType.maxStepHeight = builder.agentType.height * 0.7f
        builder.collisionMask = mask

        val meshEntity = Entity(world)
            .setScale(1.5f)
            .add(MeshComponent(res.getChild("meshes/NavMesh.fbx")).apply {
                collisionMask = mask
            })

        val navMeshData = builder.buildData(world) ?: throw IllegalStateException("Failed to build NavMesh")
        world.add(NavMeshDebugComponent().apply { data = navMeshData.meshData })

        // visualize navmesh
        if (false) world.add(MeshComponent(toMesh(navMeshData.meshData, Mesh())!!.apply {
            materials = Material().apply {
                cullMode = CullMode.BOTH
                diffuseBase.set(0.2f, 1f, 0.2f, 0.5f)
            }.ref.wrap()
            positions!!.apply {
                for (i in indices step 3) {
                    this[i + 1] += 0.03f
                }
            }
        }.ref))

        val flagMesh = res.getChild("meshes/Flag.fbx")
        for (i in 0 until 500) {
            val flag = Entity("Flag", world)
            flag.setScale(flagScale)
            flag.add(MeshComponent(flagMesh).apply { isInstanced = true })
            val agent = Entity("Agent", world)
                .add(AgentController1a(navMeshData, meshEntity, flag))
            Entity(agent)
                .setScale(agentScale)
                .add(MeshComponent(agentMeshRef).apply { isInstanced = true })
        }

        world.addComponent(CrowdUpdateComponent(navMeshData))

        testScene(world)

    }
}
