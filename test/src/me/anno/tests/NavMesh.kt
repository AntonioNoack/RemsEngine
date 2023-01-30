package me.anno.tests

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.navigation.NavMesh
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.io.ISaveable
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
import kotlin.math.atan2
import kotlin.math.max

/**
 * test recast navmesh generation and usage
 * */
fun main() {
    testUI {

        ECSRegistry.init()

        val mask = 1 shl 16
        val world = Entity("World")
        val agentMeshRef = documents.getChild("CuteGhost.fbx")
        val agentMesh = MeshCache[agentMeshRef, false]!!
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
            scale = scale.set(2.0)
        })

        val meshData = navMesh1.build()!!
        navMesh1.data = meshData

        // visualize navmesh
        world.add(MeshComponent(navMesh1.toMesh(Mesh())!!.apply {
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

        val agent = Entity("Agent")
        agent.add(MeshComponent(agentMeshRef))
        world.add(agent)

        val navMesh = NavMesh(meshData, navMesh1.maxVerticesPerPoly, 0)

        val query = NavMeshQuery(navMesh)
        val filter = DefaultQueryFilter()
        val random = Random(System.nanoTime())

        val config = CrowdConfig(navMesh1.agentRadius)
        val crowd = Crowd(config, navMesh)


        val flag = Entity("Flag")
        flag.add(MeshComponent(documents.getChild("Flag.fbx")))
        world.add(flag)

        // walk along path
        class AgentController : Component() {

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

            override fun onUpdate(): Int {
                // move agent from src to dst
                val dt = Engine.deltaTime
                crowd.update(dt, null)
                val entity = entity!!
                val lastPos = entity.position
                val lastX = lastPos.x
                val lastZ = lastPos.z
                val nextPos = agent1.currentPosition
                val distSq = lastPos.distanceSquared(nextPos.x.toDouble(), nextPos.y.toDouble(), nextPos.z.toDouble())
                if (distSq > 0f && agent1.targetPos.distanceSquared(nextPos) >= 1f) {
                    entity.rotation = entity.rotation
                        .identity()
                        .rotateY(atan2(nextPos.x - lastX, nextPos.z - lastZ))
                } else {
                    findNextTarget()
                }
                entity.position = entity.position.set(nextPos)
                return 1
            }

            override fun clone() = this
            override val className = "AgentController"
        }

        ISaveable.registerCustomClass(AgentController())
        agent.add(AgentController())

        testScene(world)

    }

}
