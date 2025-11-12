package me.anno.tests.mesh.hexagons

import com.bulletphysics.collision.shapes.SphereShape
import me.anno.bullet.HexagonSpherePhysics
import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.GFX
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.tests.mesh.hexagons.physics.HSPhysicsControls
import me.anno.tests.mesh.hexagons.physics.MCTriangleQuery
import me.anno.ui.UIColors.gold
import me.anno.ui.debug.TestEngine
import me.anno.utils.Color.black
import me.anno.utils.OS.documents
import org.joml.Vector3f
import java.io.IOException

fun testIncompletePentagons() {
    val sphere = HexagonSphere(10, 1)
    val world = HexagonSphereMCWorld(sphere)
    val sc = sphere.findClosestChunk(Vector3f(0f, 0f, 1f)).center
    val hex = sphere.findClosestHexagon(sc)
    val (_, map) = world.generateWorld(hex)
    var pentagonsIncluded = 0
    for (pentagonId in sphere.special0 until sphere.special) {
        if (map[pentagonId] >= 0) pentagonsIncluded++
    }
}


// test player physics on a hexagon sphere
fun main() {

    testIncompletePentagons()

    val n = 100
    val t = 25

    val sphere = HexagonSphere(n, n / t)
    val world = HexagonSphereMCWorld(sphere)
    val len = sphere.len

    val save = world.save
    val file = documents.getChild("hexSphere.bin")
    if (file.exists) try {
        save.read(file)
    } catch (e: IOException) {
        e.printStackTrace()
    }

    // todo using a capsule, the collisions often are neighbor triangles... why???
    val shape = SphereShape(0.25f)
    val triQ = MCTriangleQuery(world)
    val physics = HexagonSpherePhysics(sphere, shape, triQ)

    physics.debugMeshInactiveColor = 0x777777 or black
    physics.debugMeshActiveColor = gold or black

    // add visuals
    val chunks = HashMap<HexagonSphere.Chunk, Mesh>()
    val scene = Entity()
    scene.add(Skybox().apply {
        spherical = true
    })
    for (tri in 0 until sphere.triangles.size) {
        val triEntity = Entity()
        scene.add(triEntity)
        for (si in 0 until sphere.chunkCount) {
            for (sj in 0 until sphere.chunkCount - si) {
                val hexagons = sphere.queryChunk(tri, si, sj)
                val mesh = createMesh(hexagons, world, null)
                val comp = MeshComponent(mesh.ref)
                val entity = Entity()
                entity.add(comp)
                triEntity.add(entity)
                chunks[sphere.chunk(tri, si, sj)] = mesh
            }
        }
    }

    physics.gravity /= sphere.hexagonsPerSide

    TestEngine.testUI3("HexagonSphere Physics") {
        GFX.someWindow.windowStack.firstOrNull()?.drawDirectly = false
        val renderView = object : RenderView(PlayMode.PLAYING, DefaultConfig.style) {
            override fun getWorld() = scene
        }
        val sv = SceneView(renderView, DefaultConfig.style)
        // override controller
        sv.renderView.enableOrbiting = false
        sv.renderView.near = 1e-7f
        val hex0 = sphere.findClosestHexagon(Vector3f(0f, 1f, 0f))
        var yi = triQ.getWorld(hex0).indexOfFirst { block -> block == air }
        if (yi < 0) yi = world.sy
        physics.init(Vector3f(0f, world.h(yi + 1), 0f))
        sv.playControls = HSPhysicsControls(scene, sphere, chunks, sv, triQ, physics, file, save, world, len)
        sv
    }
}