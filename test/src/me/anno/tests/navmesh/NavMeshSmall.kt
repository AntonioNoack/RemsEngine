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
import me.anno.engine.OfficialExtensions
import me.anno.engine.WindowRenderFlags
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.CullMode
import me.anno.recast.CrowdUpdateComponent
import me.anno.recast.NavMeshBuilder
import me.anno.recast.NavMeshDebug.toMesh
import me.anno.recast.NavMeshDebugComponent
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.OS.res
import me.anno.utils.structures.lists.Lists.wrap
import kotlin.math.max

/**
 * test recast navmesh generation and usage
 * */
fun main() {
    OfficialExtensions.initForTests()
    testUI("NavMeshSmall") {

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

        val meshEntity = Entity("Mesh", world)
            .setScale(1.5f)
            .add(MeshComponent(res.getChild("meshes/NavMesh.fbx")).apply {
                collisionMask = mask
            })

        val navMeshData = builder.buildData(world) ?: throw IllegalStateException("Failed to build NavMesh")
        val meshData = navMeshData.meshData
        world.add(NavMeshDebugComponent().apply { data = meshData })

        // visualize navmesh
        world.add(MeshComponent(toMesh(meshData)!!.apply {
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

        val agents = Entity("Agents", world)
        val flagMesh = res.getChild("meshes/Flag.fbx")
        for (i in 0 until 5) {
            val group = Entity("Agent[$i]", agents)
            val flag = Entity("Flag", group)
                .setScale(flagScale)
                .add(MeshComponent(flagMesh))
            val agent = Entity("Agent", group)
                .add(AgentController1a(navMeshData, meshEntity, flag))
            Entity("AgentMesh", agent)
                .setScale(agentScale)
                .add(MeshComponent(agentMeshRef))
        }

        world.addComponent(CrowdUpdateComponent(navMeshData))

        testScene(world)

    }
}
