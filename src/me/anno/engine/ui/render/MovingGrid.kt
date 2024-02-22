package me.anno.engine.ui.render

import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.TextShapes
import me.anno.engine.ui.render.ECSShaderLib.simpleShader
import me.anno.engine.ui.render.GridColors.colorX
import me.anno.engine.ui.render.GridColors.colorY
import me.anno.engine.ui.render.GridColors.colorZ
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.hasFlag
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

object MovingGrid {

    fun drawGrid(mask: Int) {
        if (mask.and(7) == 0) return
        LineBuffer.finish(RenderState.cameraMatrix)
        GFXState.depthMask.use(false) {
            drawGrid3(mask)
        }
    }

    private fun drawGrid3(mask: Int) {

        val pos0 = RenderView.currentInstance?.orbitCenter ?: RenderState.cameraPosition
        val pos1 = RenderView.currentInstance?.cameraPosition ?: pos0

        for (axis in 0 until 3) {
            if (mask.hasFlag(1 shl axis)) {

                val distance = max(abs(pos0[axis]), abs(pos1[axis]))

                val log = log10(distance)
                val floorLog = floor(log)
                val fractLog = (log - floorLog).toFloat()
                val radius1 = Maths.pow(10.0, floorLog + 1)

                // number of levels depends on distance from camera to center
                val numLevels = fractLog + 2f
                for (i in 0..2) {

                    val scale = 10.0.pow(i)
                    val radius2 = radius1 * scale

                    val dx = round(pos0.x / radius2) * radius2
                    val dz = round(pos0.z / radius2) * radius2

                    val alpha0 = 0.05f * when (i) {
                        0 -> 1f - fractLog
                        else -> clamp(numLevels - i)
                    }


                    val transform = init()
                        .translate(dx, 0.0, dz)
                        .scale(radius2)

                    // for XY and YZ, rotate the grid
                    val baseRot = when (axis) {
                        0 -> baseRotX
                        1 -> baseRotY
                        2 -> baseRotZ
                        else -> throw NotImplementedError()
                    }
                    transform.rotate(baseRot)

                    alpha = alpha0
                    drawMesh(gridMesh)

                    alpha *= 2f
                    val textSize = radius2 * 0.01
                    val textRots = when (axis) {
                        0 -> textRotX
                        1 -> textRotY
                        2 -> textRotZ
                        else -> throw NotImplementedError()
                    }
                    for (textRot in textRots) {
                        drawTextMesh(textSize, 1, textRot)
                        drawTextMesh(textSize, 5, textRot)
                    }
                }
            }
        }

        // to do replace with one mesh
        val distance = max(abs(pos1.x), max(abs(pos1.y), abs(pos1.z)))
        drawAxes(distance)
    }

    fun drawMesh(mesh: Mesh) {
        val shader = simpleShader.value
        shader.use()
        val material = defaultMaterial
        material.bind(shader)
        shader.m4x4("transform", transform2.set(transform))
        shader.v4f("diffuseBase", 1f, 1f, 1f, alpha)
        mesh.draw(shader, 0)
        GFX.check()
    }

    val gridMesh = Mesh()

    var alpha = 0f
    val transform = Matrix4d()
    val transform2 = Matrix4f()

    init {

        val numLines = 201 * 2 // +/- 100, y; x and z
        val numPoints = numLines * 2
        var i = 0
        val positions = FloatArray(numPoints * 3)

        val di = 1f
        for (line in -100..+100) {
            val dj = di * 0.01f * line

            positions[i++] = +dj
            positions[i++] = 0f
            positions[i++] = +di
            positions[i++] = +dj
            positions[i++] = 0f
            positions[i++] = -di

            positions[i++] = +di
            positions[i++] = 0f
            positions[i++] = +dj
            positions[i++] = -di
            positions[i++] = 0f
            positions[i++] = +dj
        }

        gridMesh.positions = positions
        gridMesh.drawMode = DrawMode.LINES
    }

    fun drawTextMesh(
        baseSize: Double,
        factor: Int,
        rotation: Quaterniond,
    ) {
        val size = baseSize * factor
        val mesh = cachedMeshes.getOrPut(size) { "$factor${getSuffix(baseSize)}" }
        TextShapes.drawTextMesh(
            mesh, Vector3d(size, size * 0.02, 0.0).rotate(rotation),
            rotation, size, null
        )
    }

    fun init(): Matrix4d {
        val pos = RenderState.cameraPosition
        return transform
            .set(RenderState.cameraMatrix)
            .scale(RenderState.worldScale)
            .translate(-pos.x, -pos.y, -pos.z)
    }

    fun getSuffix(baseSize: Double): String {
        return when (val power = round(log10(baseSize)).toInt()) {
            -12 -> "pm"
            -11 -> "0pm"
            -10 -> "00pm"
            -9 -> "nm"
            -8 -> "0nm"
            -7 -> "00nm"
            -6 -> "µm"
            -5 -> "0µm"
            -4 -> "00µm"
            -3 -> "mm"
            -2 -> "cm"
            -1 -> "0cm"
            0 -> "m"
            1 -> "0m"
            2 -> "00m"
            3 -> "km"
            4 -> "0km"
            5 -> "00km"
            6 -> "Mm"
            7 -> "0Mm"
            8 -> "00Mm"
            100 -> "Googol m"
            else -> "e${power}m"
        }
    }

    private val cachedMeshes = HashMap<Double, String>()
    private val baseRotX = Quaterniond().rotateX(PI * 0.5)
    private val baseRotY = Quaterniond()
    private val baseRotZ = Quaterniond().rotateY(PI * 0.5).rotateX(PI * 0.5)
    private val textRotX = arrayOf(
        Quaterniond(baseRotX).rotateX(-PI * 0.5),
        Quaterniond(baseRotX).rotateY(PI * 0.5).rotateX(-PI * 0.5),
        Quaterniond(baseRotX).rotateY(PI * 1.0).rotateX(-PI * 0.5),
        Quaterniond(baseRotX).rotateY(PI * 1.5).rotateX(-PI * 0.5),
    )
    private val textRotY = arrayOf(
        Quaterniond(baseRotY).rotateX(-PI * 0.5),
        Quaterniond(baseRotY).rotateY(PI * 0.5).rotateX(-PI * 0.5),
        Quaterniond(baseRotY).rotateY(PI * 1.0).rotateX(-PI * 0.5),
        Quaterniond(baseRotY).rotateY(PI * 1.5).rotateX(-PI * 0.5),
    )
    private val textRotZ = arrayOf(
        Quaterniond(baseRotZ).rotateX(-PI * 0.5),
        Quaterniond(baseRotZ).rotateY(PI * 0.5).rotateX(-PI * 0.5),
        Quaterniond(baseRotZ).rotateY(PI * 1.0).rotateX(-PI * 0.5),
        Quaterniond(baseRotZ).rotateY(PI * 1.5).rotateX(-PI * 0.5),
    )

    private fun drawAxes(scale: Double) {
        val length = 1e3 * scale
        val alpha = 127 shl 24
        LineShapes.drawLine(null, -length, 0.0, 0.0, +length, 0.0, 0.0, colorX or alpha)
        LineShapes.drawLine(null, 0.0, -length, 0.0, 0.0, +length, 0.0, colorY or alpha)
        LineShapes.drawLine(null, 0.0, 0.0, -length, 0.0, 0.0, +length, colorZ or alpha)
    }
}