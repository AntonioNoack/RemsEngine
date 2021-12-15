package me.anno.ui.editor.sceneView

import me.anno.config.DefaultStyle
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.GridColors.colorX
import me.anno.engine.ui.render.GridColors.colorY
import me.anno.engine.ui.render.GridColors.colorZ
import me.anno.engine.ui.render.RenderView.Companion.camPosition
import me.anno.engine.ui.render.RenderView.Companion.worldScale
import me.anno.gpu.GFX
import me.anno.gpu.GFX.shaderColor
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.pipeline.M4x3Delta.m4x3delta
import me.anno.io.files.BundledRef
import me.anno.io.files.FileReference
import me.anno.objects.Transform
import org.joml.*

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
        drawMesh(cameraTransform, position, scale, 0, colorX, clickId, mesh)
        drawMesh(cameraTransform, position, scale, 1, colorY, clickId + 1, mesh)
        drawMesh(cameraTransform, position, scale, 2, colorZ, clickId + 2, mesh)
    }

    // todo ui does not need lighting, and we can use pbr rendering

    val local = Matrix4x3d()

    fun drawMesh(
        cameraTransform: Matrix4f,
        position: Vector3d,
        scale: Double,
        axis: Int,
        color: Int,
        clickId: Int,
        mesh: Mesh
    ) {
        GFX.drawnId = clickId
        val material = defaultMaterial
        val shader = (material.shader ?: pbrModelShader).value
        shader.use()
        shader.m4x4("transform", cameraTransform)
        val local = local
        local.identity()
        local.translate(position)
        when (axis) {
            1 -> local.rotateZ(+Math.PI * 0.5)
            2 -> local.rotateY(-Math.PI * 0.5)
        }
        local.scale(scale)
        shader.m4x3delta("localTransform", local, camPosition, worldScale)
        material.defineShader(shader)
        shader.v4("diffuseBase", color or (255 shl 24))
        shaderColor(shader, "tint", color or (255 shl 24))
        shader.v1("hasAnimation", false)
        shader.v1("hasVertexColors", false)
        mesh.draw(shader, 0)
    }

    // avoid unnecessary allocations ;)
    private val tmpDistances = FloatArray(3)
    fun drawGizmo(cameraTransform: Matrix4f, x0: Int, y0: Int, w: Int, h: Int) {

        /**
         * display a 3D gizmo
         * todo beautify a little, take inspiration from Blender maybe ;)
         * */

        val vx = cameraTransform.transformDirection(Transform.xAxis, Vector3f())
        val vy = cameraTransform.transformDirection(Transform.yAxis, Vector3f())
        val vz = cameraTransform.transformDirection(Transform.zAxis, Vector3f())

        val gizmoSize = 50f
        val gizmoPadding = 10f
        val gx = x0 + w - gizmoSize - gizmoPadding
        val gy = y0 + gizmoSize + gizmoPadding

        fun drawCircle(x: Float, y: Float, z: Float, color: Int) {
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

        tmpDistances[0] = vx.z
        tmpDistances[1] = vy.z
        tmpDistances[2] = vz.z

        tmpDistances.sortDescending()

        for (d in tmpDistances) {
            if (d == vx.z) drawCircle(vx.x, vx.y, vx.z, 0xff7777)
            if (d == vy.z) drawCircle(vy.x, vy.y, vy.z, 0x77ff77)
            if (d == vz.z) drawCircle(vz.x, vz.y, vz.z, 0x7777ff)
        }

    }

}