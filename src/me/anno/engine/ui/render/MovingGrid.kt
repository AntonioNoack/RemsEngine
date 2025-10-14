package me.anno.engine.ui.render

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.engine.ui.TextShapes
import me.anno.engine.ui.render.ECSShaderLib.simpleShader
import me.anno.engine.ui.render.GridColors.colorX
import me.anno.engine.ui.render.GridColors.colorY
import me.anno.engine.ui.render.GridColors.colorZ
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.MinMax.max
import me.anno.utils.Color
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Quaterniond
import speiger.primitivecollections.LongToObjectHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

object MovingGrid {

    fun drawGrid(pipeline: Pipeline, mask: Int) {
        if (mask.and(7) == 0) return
        LineBuffer.finish(RenderState.cameraMatrix)
        GFXState.depthMask.use(false) {
            drawGrid3(pipeline, mask)
        }
    }

    private fun drawGrid3(pipeline: Pipeline, mask: Int) {

        val pos0 = RenderView.currentInstance?.orbitCenter ?: RenderState.cameraPosition
        val distance0 = (RenderView.currentInstance?.radius ?: 1f).toDouble()

        val shader = simpleShader.value
        for (axis in 0 until 3) {
            if (mask.hasFlag(1 shl axis)) {

                val distance = max(distance0, abs(pos0[axis]))
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
                        else -> baseRotZ
                    }
                    transform.rotate(baseRot)

                    drawMesh(pipeline, gridMesh, shader, Color.white.withAlpha(alpha0))

                    val textColor = Color.white.withAlpha(alpha0 * 2f)
                    val textSize = radius2 * 0.01
                    val textRots = when (axis) {
                        0 -> textRotX
                        1 -> textRotY
                        2 -> textRotZ
                        else -> throw NotImplementedError()
                    }

                    for (textRot in textRots) {
                        drawTextMesh(pipeline, textSize, 1, textRot, textColor)
                        drawTextMesh(pipeline, textSize, 5, textRot, textColor)
                    }
                }
            }
        }

        init().scale(distance0)
        drawMesh(pipeline, axesMesh, shader, -1)
    }

    fun drawMesh(pipeline: Pipeline, mesh: Mesh, color: Int) {
        val shader = simpleShader.value
        drawMesh(pipeline, mesh, shader, color)
    }

    fun drawMesh(pipeline: Pipeline, mesh: Mesh, shader: Shader, color: Int) {
        shader.use()
        val material = defaultMaterial
        material.bind(shader)
        shader.m4x4("transform", transform4f.set(transform4d))
        shader.v4f("diffuseBase", color)
        shader.v1i("hasVertexColors", mesh.hasVertexColors)
        mesh.draw(pipeline, shader, 0)
        GFX.check()
    }

    val gridMesh = Mesh()

    val transform4d = Matrix4d()
    val transform4f = Matrix4f()

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
        pipeline: Pipeline,
        baseSize: Double, factor: Int,
        rotation: Quaterniond, color: Int
    ) {
        val size = baseSize * factor
        val mesh = cachedMeshes.getOrPut(size.toRawBits()) {
            "$factor${getSuffix(baseSize)}"
        }
        val tmpPos = JomlPools.vec3d.create()
            .set(size, size * 0.02, 0.0).rotate(rotation)
        TextShapes.drawTextMesh(pipeline, mesh, tmpPos, rotation, size * 0.2, null, color)
        JomlPools.vec3d.sub(1)
    }

    fun init(): Matrix4d {
        val pos = RenderState.cameraPosition
        return transform4d
            .set(RenderState.cameraMatrix)
            .translate(-pos.x, -pos.y, -pos.z)
    }

    private val suffixes = ("" +
            "pm,0pm,00pm," +
            "nm,0nm,00nm," +
            "µm,0µm,00µm," +
            "mm,cm,0cm," +
            "m,0m,00m," +
            "km,0km,00km," +
            "Mm,0Mm,00Mm," +
            "Gm,0Gm,00Gm," +
            "Tm,0Tm,00Tm," +
            "Pm," +
            "Ly,0Ly,00Ly," +
            "kLy,0kLy,00kLy," +
            "MLy,0MLy,00MLy," +
            "GLy,0GLy,00GLy," +
            "TLy,0TLy,00TLy," +
            "PLy,0PLy,00PLy," +
            "ELy,0ELy,00ELy").split(',')

    fun getSuffix(baseSize: Double): String {
        val power = round(log10(baseSize)).toInt()
        val idx = power + 12
        return suffixes.getOrNull(idx) ?: "e${power}m"
    }

    private val cachedMeshes = LongToObjectHashMap<String>()
    private val baseRotX = Quaterniond().rotateX(PI * 0.5)
    private val baseRotY = Quaterniond()
    private val baseRotZ = Quaterniond().rotateY(PI * 0.5).rotateX(PI * 0.5)
    private val textRotX = listOf(
        Quaterniond(baseRotX).rotateX(-PI * 0.5),
        Quaterniond(baseRotX).rotateY(PI * 0.5).rotateX(-PI * 0.5),
        Quaterniond(baseRotX).rotateY(PI * 1.0).rotateX(-PI * 0.5),
        Quaterniond(baseRotX).rotateY(PI * 1.5).rotateX(-PI * 0.5),
    )
    private val textRotY = listOf(
        Quaterniond(baseRotY).rotateX(-PI * 0.5),
        Quaterniond(baseRotY).rotateY(PI * 0.5).rotateX(-PI * 0.5),
        Quaterniond(baseRotY).rotateY(PI * 1.0).rotateX(-PI * 0.5),
        Quaterniond(baseRotY).rotateY(PI * 1.5).rotateX(-PI * 0.5),
    )
    private val textRotZ = listOf(
        Quaterniond(baseRotZ).rotateX(-PI * 0.5),
        Quaterniond(baseRotZ).rotateY(PI * 0.5).rotateX(-PI * 0.5),
        Quaterniond(baseRotZ).rotateY(PI * 1.0).rotateX(-PI * 0.5),
        Quaterniond(baseRotZ).rotateY(PI * 1.5).rotateX(-PI * 0.5),
    )

    val axesMesh = createAxesMesh()

    private fun createAxesMesh(): Mesh {
        val alpha = 180
        val numLines = 4 * 3 + 4 + 2 + 2 + 3
        val numVertices = numLines * 2
        val positions = FloatArray(numVertices * 3)
        val colors = IntArray(numVertices)
        var i = 0
        var j = 0
        var color = colorX.withAlpha(alpha)

        fun line(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float) {
            positions[i++] = x0
            positions[i++] = y0
            positions[i++] = z0
            colors[j++] = color
            positions[i++] = x1
            positions[i++] = y1
            positions[i++] = z1
            colors[j++] = color
        }

        val l = 1000f
        line(-l, 0f, 0f, +l, 0f, 0f)

        val len1 = 0.3f
        val len2 = len1 * 0.9f
        val len3 = len1 * 0.05f
        line(len2, -len3, 0f, len1, 0f, 0f)
        line(len2, +len3, 0f, len1, 0f, 0f)
        line(len2, 0f, -len3, len1, 0f, 0f)
        line(len2, 0f, +len3, len1, 0f, 0f)

        // X letter
        val x1 = len3 * 2f
        val x2 = len3 * 3.5f
        val y1 = +len3 * 1.2f
        val y2 = -len3 * 1.2f
        line(len1, y2, x1, len1, y1, x2)
        line(len1, y1, x1, len1, y2, x2)


        color = colorY.withAlpha(alpha)
        line(0f, -l, 0f, 0f, +l, 0f)
        line(-len3, len2, 0f, 0f, len1, 0f)
        line(+len3, len2, 0f, 0f, len1, 0f)
        line(0f, len2, -len3, 0f, len1, 0f)
        line(0f, len2, +len3, 0f, len1, 0f)

        // Y letter ([x1,x2], [l+y1,l+y2])
        val z1 = len3 * 1.25f / 1.41f
        val z2 = len3 * 2.75f / 1.41f
        val zm = (z1 + z2) * 0.5f
        line(z1, y2 + len1, z2, z2, y1 + len1, z1)
        line(z1, y1 + len1, z2, zm, 0f + len1, zm)


        color = colorZ.withAlpha(alpha)
        line(0f, 0f, -l, 0f, 0f, +l)
        line(-len3, 0f, len2, 0f, 0f, len1)
        line(+len3, 0f, len2, 0f, 0f, len1)
        line(0f, -len3, len2, 0f, 0f, len1)
        line(0f, +len3, len2, 0f, 0f, len1)

        // Z letter
        line(x1, y1, len1, x2, y1, len1)
        line(x1, y2, len1, x2, y2, len1)
        line(x1, y2, len1, x2, y1, len1)

        val mesh = Mesh()
        mesh.positions = positions
        mesh.color0 = colors
        mesh.drawMode = DrawMode.LINES
        return mesh
    }
}