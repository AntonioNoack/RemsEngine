package me.anno.ecs.components.mesh.grid

import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.maths.Maths.posMod
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.editor.sceneView.Grid.gridCellSize
import org.joml.Matrix4fArrayList
import org.joml.Matrix4x3
import org.joml.Vector2d
import org.joml.Vector2i

// todo when this is selected, allow dropping meshes into the different cells
// todo test this with different floor tiles,
//  maybe a complete Sims-like mesh catalogue,
//  and then build something epic :3

@Docs("This is effectively a sprite layer, a regular 2d grid, where you can add meshes")
class MeshGrid : MeshSpawner(), OnDrawGUI {

    // todo support other grid shapes than just regular rectangular?
    val grid = HashMap<Vector2i, FileReference>()
    var cellSize = Vector2d(1.0)
        set(value) {
            field.set(value)
            invalidateBounds()
        }

    @DebugAction(parameterNames = "x,y,w,h,mesh")
    fun fill(x: Int, y: Int, w: Int, h: Int, mesh: Mesh?) {
        for (x in x until x + w) {
            for (y in y until y + h) {
                set(x, y, mesh)
            }
        }
    }

    @DebugAction(parameterNames = "x,y,mesh")
    fun set(x: Int, y: Int, mesh: Mesh?) {
        val key = Vector2i(x, y)
        if (mesh != null) grid[key] = mesh.ref
        else grid.remove(key)
        invalidateBounds()
    }

    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {
        var i = 0
        for ((pos, mesh) in grid) {
            val mesh = MeshCache[mesh] ?: continue
            val transform = getTransform(i++)
            transform.setLocalPosition(pos.x * cellSize.x, 0.0, pos.y * cellSize.y)
            callback(mesh, null, transform)
        }
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        // draw the grid cells
        val stack = Matrix4fArrayList()
        stack.set(RenderState.cameraMatrix)
        val transform = transform?.getDrawMatrix() ?: Matrix4x3()
        val pos = RenderState.cameraPosition
        stack.translate(
            (posMod(transform.m30 - pos.x, cellSize.x) - cellSize.x * 0.5f).toFloat(),
            (transform.m31 - pos.y).toFloat(),
            (posMod(transform.m32 - pos.z, cellSize.y) - cellSize.y * 0.5f).toFloat()
        )

        // todo why is blending not working???
        GFXState.blendMode.use(BlendMode.DEFAULT) {
            stack.scale(cellSize.x.toFloat() / gridCellSize, 0f, cellSize.y.toFloat() / gridCellSize)
            Grid.drawGrid(stack, 0.1f)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeVector2d("cellSize", cellSize)
        writer.writeObjectList(null, "grid", grid.map { MeshGridEntry(it.key, it.value) })
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "cellSize" -> cellSize.set(value as? Vector2d ?: return)
            "grid" -> {
                if (value !is List<*>) return
                grid.clear()
                for (value in value) {
                    value as? MeshGridEntry ?: continue
                    grid[value.position] = value.mesh
                }
            }
            else -> super.setProperty(name, value)
        }
    }

}