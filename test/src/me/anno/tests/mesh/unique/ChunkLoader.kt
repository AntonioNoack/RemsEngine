package me.anno.tests.mesh.unique

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.maths.patterns.SpiralPattern.spiral2d
import me.anno.mesh.vox.model.VoxelModel
import me.anno.tests.utils.TestWorld
import me.anno.utils.hpc.ProcessingQueue
import org.joml.AABBf
import org.joml.Vector3i

class ChunkLoader(val chunkRenderer: ChunkRenderer, val world: TestWorld) : Component(), OnUpdate {

    val csx = world.sizeX
    val csy = world.sizeY
    val csz = world.sizeZ

    val worker = ProcessingQueue("chunks")

    // load world in spiral pattern
    val loadingRadius = 10
    val spiralPattern = spiral2d(loadingRadius + 5, 0, true).toList()
    val loadingPattern = spiralPattern.filter { it.length() < loadingRadius - 0.5f }
    val unloadingPattern = spiralPattern.filter { it.length() > loadingRadius + 1.5f }

    val loadedChunks = HashSet<Vector3i>()

    fun generateChunk(chunkId: Vector3i) {

        val x0 = chunkId.x * csx
        val y0 = chunkId.y * csy
        val z0 = chunkId.z * csz

        val model = object : VoxelModel(csx, csy, csz) {
            override fun getBlock(x: Int, y: Int, z: Int): Int {
                return world.getElementAt(x0 + x, y0 + y, z0 + z).toInt()
            }
        }
        model.center0()
        val mesh = model.createMesh(TestWorld.palette, null, { x, y, z ->
            world.getElementAt(x0 + x, y0 + y, z0 + z).toInt() != 0
        })

        val data = chunkRenderer.getData(chunkId, mesh)
        if (data != null) {
            val bounds = mesh.getBounds()
            bounds.translate(x0.toFloat(), y0.toFloat(), z0.toFloat())
            addGPUTask("ChunkUpload", 1) { // change back to GPU thread
                chunkRenderer.set(chunkId, MeshEntry(mesh, bounds, data))
            }
        }
    }

    fun AABBf.translate(dx: Float, dy: Float, dz: Float) {
        minX += dx
        minY += dy
        minZ += dz
        maxX += dx
        maxY += dy
        maxZ += dz
    }

    var budget = 5
    fun loadChunks(center: Vector3i) {
        var budget = budget - worker.remaining
        for (idx in loadingPattern) {
            val vec = Vector3i(idx).add(center)
            if (loadedChunks.add(vec)) {
                worker += { generateChunk(vec) }
                if (budget-- <= 0) break
            }
        }
    }

    fun unloadChunks(center: Vector3i) {
        for (idx in unloadingPattern) {
            val vec = Vector3i(idx).add(center)
            if (loadedChunks.remove(vec)) {
                chunkRenderer.remove(vec, true)
            }
        }
    }

    fun getPlayerChunkId(): Vector3i {
        val delta = Vector3i()
        val ci = RenderView.currentInstance
        if (ci != null) {
            val pos = ci.orbitCenter // around where the camera orbits
            delta.set((pos.x / csx).toInt(), 0, (pos.z / csz).toInt())
        }
        return delta
    }

    override fun onUpdate() {
        // load next mesh
        if (worker.remaining < budget) {
            val chunkId = getPlayerChunkId()
            loadChunks(chunkId)
            unloadChunks(chunkId)
        }
    }
}