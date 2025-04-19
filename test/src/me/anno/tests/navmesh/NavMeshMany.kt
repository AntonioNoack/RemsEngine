package me.anno.tests.navmesh

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.OfficialExtensions
import me.anno.engine.WindowRenderFlags
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.recast.CrowdUpdateComponent
import me.anno.recast.NavMeshAgent
import me.anno.recast.NavMeshBuilder
import me.anno.recast.NavMeshData
import me.anno.recast.NavMeshDebugComponent
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.OS.res
import me.anno.utils.assertions.assertNotNull
import org.joml.Vector3d
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.max
import kotlin.random.Random

// walk along path
class AgentController1b(
    data: NavMeshData,
    val flag: Entity,
) : NavMeshAgent(data) {

    val random = Random(Time.nanoTime)

    override fun findNextTarget(random: Random) {
        super.findNextTarget(random)
        val crowdAgent = crowdAgent ?: return
        flag.teleportToGlobal(Vector3d(crowdAgent.targetPosOrVel))
    }

    private var upDownAngle = 0.0

    val np = Vector3d()

    override fun onUpdate() {
        super.onUpdate()
        val crowdAgent = crowdAgent ?: return
        // move agent from src to dst
        val entity = entity!!
        val nextPos = crowdAgent.currentPosition
        val distSq = crowdAgent.actualVelocity.lengthSquared()
        if (distSq == 0f || crowdAgent.targetPosOrVel.distanceSquared(nextPos) < 1f) {
            findNextTarget(random)
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

        val builder = NavMeshBuilder()
        builder.agentType.height = agentBounds.deltaY * agentScale
        builder.agentType.radius = max(agentBounds.deltaX, agentBounds.deltaZ) * agentScale * 0.5f
        builder.agentType.maxStepHeight = builder.agentType.height * 0.7f
        builder.collisionMask = mask

        val navMeshSrc = res.getChild("meshes/NavMesh.fbx")
        assertNotNull(MeshCache[navMeshSrc])
        world.add(Entity().apply {
            add(MeshComponent(navMeshSrc).apply {
                collisionMask = mask
            })
            setScale(2.5f)
        })

        val navMeshData = builder.buildData(world) ?: throw IllegalStateException("Failed to build NavMesh")
        world.add(NavMeshDebugComponent().apply { data = navMeshData.meshData })

        val flagMesh = res.getChild("meshes/Flag.fbx")
        assertNotNull(MeshCache[flagMesh])

        for (i in 0 until 2500) {
            val flag = Entity("Flag", world)
            flag.setScale(flagScale)
            flag.add(MeshComponent(flagMesh).apply { isInstanced = true })
            Entity("Agent", world)
                .add(AgentController1b(navMeshData, flag))
                .add(Entity().apply {
                    setScale(agentScale)
                    add(MeshComponent(agentMeshRef).apply { isInstanced = true })
                })
        }

        world.addComponent(CrowdUpdateComponent(navMeshData))

        testScene(world)

    }
}
