package me.anno.tests.navmesh

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.light.sky.Skybox
import me.anno.engine.ECSRegistry
import me.anno.engine.EngineBase
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.CullMode
import me.anno.recast.NavMesh
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.OS.documents
import org.joml.Vector3d
import org.recast4j.detour.*
import org.recast4j.detour.crowd.Crowd
import org.recast4j.detour.crowd.CrowdConfig
import java.util.Random
import kotlin.math.max

/**
 * test recast navmesh generation and usage
 * */
fun main() {
    testUI("NavMeshSmall") {

        EngineBase.enableVSync = false
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
        world.add(MeshComponent(navMesh1.toMesh(Mesh())!!.apply {
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
        for (i in 0 until 5) {
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
