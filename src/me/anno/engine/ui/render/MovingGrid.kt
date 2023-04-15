package me.anno.engine.ui.render

import me.anno.config.DefaultConfig.defaultFont
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.ECSShaderLib.simpleShader
import me.anno.engine.ui.render.GridColors.colorX
import me.anno.engine.ui.render.GridColors.colorY
import me.anno.engine.ui.render.GridColors.colorZ
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.LineBuffer
import me.anno.maths.Maths
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11C.GL_LINES
import kotlin.math.*

object MovingGrid {

    fun drawGrid(radius: Double) {
        LineBuffer.finish(RenderState.cameraMatrix)
        GFXState.depthMask.use(false) {
            drawGrid3(radius)
        }
    }

    fun drawGrid3(radius0: Double) {

        val log = log10(radius0)
        val floorLog = floor(log)
        alphas[2] = (log - floorLog).toFloat()
        alphas[1] = 1f
        alphas[0] = 1f - alphas[2]
        val radius1 = Maths.pow(10.0, floorLog + 1)
        val position = RenderView.currentInstance?.position ?: RenderState.cameraPosition

        for (i in 0 until 3) {

            val scale = 10.0.pow(i)
            val radius2 = radius1 * scale

            val dx = round(position.x / radius2) * radius2
            val dz = round(position.z / radius2) * radius2

            init()
                .translate(dx, 0.0, dz)
                .scale(radius2)

            alpha = 0.05f * alphas[i]
            drawMesh(gridMesh)

            alpha *= 2f
            val textSize = radius2 * 0.01
            drawTextMesh(textSize, 1)
            drawTextMesh(textSize, 5)

        }

        // to do replace with one mesh
        drawAxes(radius0)

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

    val alphas = FloatArray(3)
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
        gridMesh.drawMode = GL_LINES

    }

    fun drawTextMesh(
        baseSize: Double,
        factor: Int
    ) {
        val size = baseSize * factor
        val mesh = cachedMeshes.getOrPut(size) {
            val text = "$factor${getSuffix(baseSize)}" // format size
            val font = FontManager.getFont(defaultFont)
            val meshGroup = TextMeshGroup(font, text, 0f, false, debugPieces = false)
            meshGroup.createMesh()
        }
        init()
            .translate(size, 0.0, -size * 0.02)
            .rotateX(-PI * 0.5)
            .scale(size)
        drawMesh(mesh)
    }

    fun init(): Matrix4d {
        val pos = RenderState.cameraPosition
        return transform
            .set(RenderState.cameraMatrix)
            .translate(-pos.x, -pos.y, -pos.z)
            .scale(RenderState.worldScale)
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

    private val cachedMeshes = HashMap<Double, Mesh>()

    private fun drawAxes(scale: Double) {
        val length = 1e3 * scale
        val alpha = 127 shl 24
        LineShapes.drawLine(null, -length, 0.0, 0.0, +length, 0.0, 0.0, colorX or alpha)
        LineShapes.drawLine(null, 0.0, -length, 0.0, 0.0, +length, 0.0, colorY or alpha)
        LineShapes.drawLine(null, 0.0, 0.0, -length, 0.0, 0.0, +length, colorZ or alpha)
    }

}