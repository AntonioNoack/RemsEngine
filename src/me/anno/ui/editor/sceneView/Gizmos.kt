package me.anno.ui.editor.sceneView

import me.anno.config.DefaultStyle
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.GridColors.colorX
import me.anno.engine.ui.render.GridColors.colorY
import me.anno.engine.ui.render.GridColors.colorZ
import me.anno.engine.ui.render.RenderState.cameraPosition
import me.anno.engine.ui.render.RenderState.worldScale
import me.anno.gpu.GFX
import me.anno.gpu.GFX.shaderColor
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.io.files.BundledRef
import me.anno.io.files.FileReference
import org.joml.*
import kotlin.math.PI

object Gizmos {

    // todo show drag/hover of these on the specific gizmo parts

    val arrowRef = BundledRef("mesh/arrowX.obj")
    val ringRef = BundledRef("mesh/ringX.obj")
    val scaleRef = BundledRef("mesh/scaleX.obj")

    fun drawScaleGizmos(cameraTransform: Matrix4f, position: Vector3d, scale: Double, clickId: Int): Int {
        drawMesh(cameraTransform, position, scale, clickId, scaleRef)
        return clickId + 3
    }

    fun drawRotateGizmos(cameraTransform: Matrix4f, position: Vector3d, scale: Double, clickId: Int): Int {
        drawMesh(cameraTransform, position, scale, clickId, ringRef)
        return clickId + 3
    }

    fun drawTranslateGizmos(cameraTransform: Matrix4f, position: Vector3d, scale: Double, clickId: Int): Int {
        drawMesh(cameraTransform, position, scale, clickId, arrowRef)
        return clickId + 3
    }

    fun drawMesh(cameraTransform: Matrix4f, position: Vector3d, scale: Double, clickId: Int, ref: FileReference) {
        val mesh = MeshCache[ref] ?: return
        drawMesh(cameraTransform, position, rotations[0], scale, colorX, clickId, mesh)
        drawMesh(cameraTransform, position, rotations[1], scale, colorY, clickId + 1, mesh)
        drawMesh(cameraTransform, position, rotations[2], scale, colorZ, clickId + 2, mesh)
    }

    // todo ui does not need lighting, and we can use pbr rendering

    val local = Matrix4x3d()

    val rotations = arrayOf(
        Quaterniond(),
        Quaterniond().rotateZ(+PI * 0.5),
        Quaterniond().rotateY(-PI * 0.5)
    )

    fun drawMesh(
        cameraTransform: Matrix4f,
        position: Vector3d, rotation: Quaterniond, scale: Double,
        color: Int, clickId: Int, mesh: Mesh
    ) = drawMesh(cameraTransform, position, rotation, scale, defaultMaterial, color, clickId, mesh)

    fun drawMesh(
        cameraTransform: Matrix4f,
        position: Vector3d, rotation: Quaterniond, scale: Double,
        material: Material, color: Int, clickId: Int, mesh: Mesh
    ) {
        val localTransform = local
        localTransform.identity()
        localTransform.translate(position)
        localTransform.rotate(rotation)
        localTransform.scale(scale)
        drawMesh(cameraTransform, localTransform, material, color, clickId, mesh)
    }

    fun drawMesh(
        cameraTransform: Matrix4f, localTransform: Matrix4x3d,
        material: Material, color: Int, clickId: Int, mesh: Mesh
    ) {
        GFX.drawnId = clickId
        val shader = (material.shader ?: pbrModelShader).value
        shader.use()
        shader.m4x4("transform", cameraTransform)
        shader.m4x3delta("localTransform", localTransform, cameraPosition, worldScale)
        shader.v1f("worldScale", worldScale)
        material.bind(shader)
        shader.v4f("diffuseBase", color)
        shaderColor(shader, "tint", color)
        shader.v1b("hasAnimation", false)
        shader.v1b("hasVertexColors", false)
        mesh.draw(shader, 0)
    }

    // avoid unnecessary allocations ;)
    private val tmp3fs = Array(3) { Vector3f() }
    private val xAxis = Vector3f(1f, 0f, 0f)
    private val yAxis = Vector3f(0f, 1f, 0f)
    private val zAxis = Vector3f(0f, 0f, 1f)

    fun drawGizmo(cameraTransform: Matrix4f, x0: Int, y0: Int, w: Int, h: Int) {

        /**
         * display a 3D gizmo
         * todo beautify a little, take inspiration from Blender maybe ;)
         * */

        cameraTransform.transformDirection(xAxis, tmp3fs[0])
        cameraTransform.transformDirection(yAxis, tmp3fs[1])
        cameraTransform.transformDirection(zAxis, tmp3fs[2])

        val gizmoSize = 50f
        val gizmoPadding = 10f
        val gx = x0 + w - gizmoSize - gizmoPadding
        val gy = y0 + gizmoSize + gizmoPadding

        tmp3fs.sortByDescending { it.z }

        for (v in tmp3fs) {
            val x = v.x
            val y = v.y
            val z = v.z
            val color = when {
                v === tmp3fs[0] -> 0xff7777
                v === tmp3fs[1] -> 0x77ff77
                else -> 0x7777ff
            }
            val lx = gx - x0
            val ly = gy - y0
            Grid.drawLine0W(
                lx, ly, lx + gizmoSize * x, ly - gizmoSize * y,
                w, h, color, 1f
            )
            val rectSize = 7f - z * 3f
            drawRect(
                gx + gizmoSize * x - rectSize * 0.5f,
                gy - gizmoSize * y - rectSize * 0.5f,
                rectSize, rectSize, color or DefaultStyle.black
            )
        }

    }

}