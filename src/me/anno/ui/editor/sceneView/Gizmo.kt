package me.anno.ui.editor.sceneView

import me.anno.config.DefaultStyle
import me.anno.gpu.GFX
import me.anno.gpu.GFXx2D.drawRect
import me.anno.objects.Transform
import org.joml.Matrix4f
import org.joml.Vector3f

object Gizmo {

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
            Grid.drawLine01(
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