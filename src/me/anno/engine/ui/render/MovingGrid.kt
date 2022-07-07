package me.anno.engine.ui.render

import me.anno.config.DefaultConfig.defaultFont
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.GridColors.colorX
import me.anno.engine.ui.render.GridColors.colorY
import me.anno.engine.ui.render.GridColors.colorZ
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.pipeline.M4x3Delta.mul4x3delta
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.maths.Maths
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import kotlin.math.*

object MovingGrid {

    fun drawGrid(radius: Double) {
        LineBuffer.finish(RenderState.cameraMatrix)
        OpenGL.blendMode.use(BlendMode.ADD) {
            if (RenderView.currentInstance?.renderMode != RenderMode.DEPTH) {
                // don't write depth, we want to stack it
                OpenGL.depthMask.use(false) {
                    drawGrid3(radius)
                }
            } else {
                drawGrid3(radius)
            }
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

            transform.identity()
                .translate(dx, 0.0, dz)
                .scale(radius2)

            alpha = 0.05f * alphas[i]
            if (alpha > 1f / 255f) {
                drawMesh(gridMesh)

                alpha *= 2f
                val textSize = radius2 * 0.01
                drawTextMesh(textSize, 1)
                drawTextMesh(textSize, 5)
            }

        }

        // to do replace with one mesh
        drawAxes(radius0)

    }

    fun drawMesh(mesh: Mesh) {
        val shader = ShaderLib.shader3D.value
        shader.use()
        GFXx2D.disableAdvancedGraphicalFeatures(shader)
        camera.set(RenderState.cameraMatrix)
            .mul4x3delta(transform, RenderState.cameraPosition, RenderState.worldScale)
        shader.m4x4("transform", camera)
        shader.v3f("offset", 0f)
        shader.v1i("drawMode", GFX.drawMode.id)
        shader.v3f("finalNormal", 1f, 0f, 0f)
        shader.v3f("finalEmissive", 0f, 0f, 0f)
        GFX.shaderColor(shader, "tint", alpha, alpha, alpha, 1f)
        whiteTexture.bind(0)
        mesh.draw(shader, 0)
        GFX.check()
    }

    val gridMesh = Mesh()

    val alphas = FloatArray(3)
    var alpha = 0f
    val transform = Matrix4x3d()
    val camera = Matrix4f()

    init {

        val numLines = 201 * 2 // +/- 100, y; x and z
        val numPoints = numLines * 2
        var i = 0
        val positions = FloatArray(numPoints * 3)
        val indices = IntArray(numPoints * 3)

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

        var j = 0
        var k = 0
        for (line in 0 until numLines) {
            indices[j++] = k++
            indices[j++] = k
            indices[j++] = k++
        }

        gridMesh.positions = positions
        gridMesh.indices = indices

    }

    fun drawTextMesh(
        baseSize: Double,
        factor: Int
    ) {
        val size = baseSize * factor
        val mesh = texts2.getOrPut(size) {
            val text = "$factor${getSuffix(baseSize)}" // format size
            val font = FontManager.getFont(defaultFont).font
            val meshGroup = TextMeshGroup(font, text, 0f, false, debugPieces = false)
            meshGroup.createMesh()
        }
        transform
            .identity()
            .translate(size, 0.0, -size * 0.02)
            .rotateX(-PI * 0.5)
            .scale(size)
        drawMesh(mesh)
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

    val texts = HashMap<Double, TextMeshGroup>()
    val texts2 = HashMap<Double, Mesh>()

    private fun drawAxes(scale: Double) {
        val length = 1e3 * scale
        val alpha = 127 shl 24
        LineShapes.drawLine(null, -length, 0.0, 0.0, +length, 0.0, 0.0, colorX or alpha)
        LineShapes.drawLine(null, 0.0, -length, 0.0, 0.0, +length, 0.0, colorY or alpha)
        LineShapes.drawLine(null, 0.0, 0.0, -length, 0.0, 0.0, +length, colorZ or alpha)
    }

}