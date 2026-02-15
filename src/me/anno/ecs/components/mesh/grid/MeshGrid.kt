package me.anno.ecs.components.mesh.grid

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
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
import me.anno.maths.Packing.pack64
import me.anno.maths.chunks.hexagon.HexagonGridMaths
import me.anno.maths.chunks.triangles.TriangleGridMaths
import me.anno.maths.noise.RandomBySeed.getNextSeed
import me.anno.maths.noise.RandomBySeed.getRandomFloat
import me.anno.maths.noise.RandomBySeed.initialMix
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.editor.sceneView.Grid.gridCellSize
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4fArrayList
import org.joml.Matrix4x3
import org.joml.Vector2d
import org.joml.Vector2i
import org.joml.Vector3d
import kotlin.math.round

// todo when this is selected, allow dropping meshes into the different cells
// todo test this with different floor tiles,
//  maybe a complete Sims-like mesh catalogue,
//  and then build something epic :3

@Docs("This is effectively a sprite layer, a regular 2d grid, where you can add meshes")
class MeshGrid : MeshSpawner(), OnDrawGUI, DropPositionAdjuster {

    val grid = HashMap<Vector2i, FileReference>()

    var cellSize = Vector2d(1.0)
        set(value) {
            field.set(value)
            invalidateBounds()
        }

    var seed = 0L
        set(value) {
            field = value
            invalidateBounds()
        }

    var gridShape = GridShape.RECTANGULAR
        set(value) {
            field = value
            invalidateBounds()
        }

    enum class GridShape {
        RECTANGULAR,
        TRIANGULAR,
        HEXAGONAL,
        RANDOMIZED
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

    @DebugAction
    fun convertToMeshComponents() {
        // todo we have to change the prefab, too
        val entity = entity ?: return
        entity.remove(this)

        val tmp = Vector2d()
        for ((key, value) in grid) {
            getPosition(key, tmp)
            Entity(entity)
                .setPosition(tmp.x, 0.0, tmp.y)
                .add(MeshComponent(value))
        }
    }

    fun getPosition(key: Vector2i, dst: Vector2d): Vector2d {
        return getPosition(key.x, key.y, dst)
    }

    fun getPosition(i: Int, j: Int, dst: Vector2d): Vector2d {
        return when (gridShape) {
            GridShape.RECTANGULAR -> dst.set(i.toDouble(), j.toDouble())
            GridShape.TRIANGULAR -> TriangleGridMaths.getCenter(i, j, dst)
            GridShape.HEXAGONAL -> HexagonGridMaths.getCenter(i, j, dst)
            GridShape.RANDOMIZED -> {
                // randomize a little;
                // todo keep distances to neighbors...
                val seed0 = seed xor initialMix(pack64(i, j))
                val rx = getRandomFloat(seed0) - 0.5f
                val ry = getRandomFloat(getNextSeed(seed0)) - 0.5f
                dst.set((i + rx) * cellSize.x, (j + ry) * cellSize.y)
            }
        }.mul(cellSize)
    }

    override fun adjust(position: Vector3d) {
        val transform = transform?.globalTransform
        transform?.transformPositionInverse(position)
        var px = position.x / cellSize.x
        var py = position.z / cellSize.y
        when (gridShape) {
            GridShape.RECTANGULAR -> {
                px = round(px) * cellSize.x
                py = round(py) * cellSize.y
            }
            GridShape.TRIANGULAR -> {
                val tmp = Vector2d(px, py)
                val (i, j) = TriangleGridMaths.getClosestTriangle(tmp, Vector2i())
                TriangleGridMaths.getCenter(i, j, tmp).mul(cellSize)
                px = tmp.x
                py = tmp.y
            }
            GridShape.HEXAGONAL -> {
                val tmp = Vector2d(px, py)
                val (i, j) = HexagonGridMaths.getClosestHexagon(tmp, Vector2i())
                HexagonGridMaths.getCenter(i, j, tmp).mul(cellSize)
                px = tmp.x
                py = tmp.y
            }
            GridShape.RANDOMIZED -> {
                val xi = round(px).toInt()
                val yi = round(py).toInt()
                val tmp = Vector2d()
                getPosition(xi, yi, tmp) // already multiplied
                px = tmp.x
                py = tmp.y
            }
        }
        position.set(px, 0.0, py)
        transform?.transformPosition(position)
    }

    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {
        var i = 0
        val tmp = JomlPools.vec2d.create()
        for ((key, mesh) in grid) {
            val mesh = MeshCache[mesh] ?: continue
            val transform = getTransform(i++)
            getPosition(key, tmp)
            transform.setLocalPosition(tmp.x, 0.0, tmp.y)
            callback(mesh, null, transform)
        }
        JomlPools.vec2d.sub(1)
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        // draw the grid cells
        val stack = Matrix4fArrayList()
        stack.set(RenderState.cameraMatrix)
        val transform = transform?.getDrawMatrix() ?: Matrix4x3()
        val pos = RenderState.cameraPosition
        var x = transform.m30 - pos.x
        val y = transform.m31 - pos.y
        var z = transform.m32 - pos.z
        when (gridShape) {
            GridShape.RECTANGULAR -> {
                x = posMod(x, cellSize.x) - cellSize.x * 0.5f
                z = posMod(z, cellSize.y) - cellSize.y * 0.5f
            }
            GridShape.TRIANGULAR -> {}
            GridShape.HEXAGONAL -> {}
            GridShape.RANDOMIZED -> {}
        }
        stack.translate(x.toFloat(), y.toFloat(), z.toFloat())

        // todo why is blending not working???
        // todo render appropriate grid (triangles, hexagons, random cells)
        GFXState.blendMode.use(BlendMode.DEFAULT) {
            stack.scale(cellSize.x.toFloat() / gridCellSize, 0f, cellSize.y.toFloat() / gridCellSize)
            Grid.drawGrid(stack, 0.1f)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("gridShape", gridShape)
        writer.writeVector2d("cellSize", cellSize)
        writer.writeObjectList(null, "grid", grid.map { MeshGridEntry(it.key, it.value) })
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "cellSize" -> cellSize.set(value as? Vector2d ?: return)
            "gridShape" -> gridShape = value as? GridShape
                ?: GridShape.entries[value as? Int ?: return]
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