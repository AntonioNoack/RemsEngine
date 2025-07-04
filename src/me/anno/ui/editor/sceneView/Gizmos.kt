package me.anno.ui.editor.sceneView

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.engine.raycast.RaycastMesh
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.GridColors.colorX
import me.anno.engine.ui.render.GridColors.colorY
import me.anno.engine.ui.render.GridColors.colorZ
import me.anno.engine.ui.render.RenderState.cameraPosition
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.files.FileReference
import me.anno.maths.Maths.PIf
import me.anno.ui.UIColors
import me.anno.utils.Color.black
import me.anno.utils.OS.res
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

object Gizmos {

    val arrowRef = res.getChild("meshes/arrowX.obj")
    val arrowRef1 = res.getChild("meshes/arrowX1.obj")
    val ringRef = res.getChild("meshes/ringX.obj")
    val scaleRef = res.getChild("meshes/scaleX.obj")

    private fun combineResults(a: Int, b: Int): Int {
        return if (b != 0) b.inv().and(7) else a
    }

    fun drawScaleGizmos(
        pipeline: Pipeline?, cameraTransform: Matrix4f, position: Vector3d, rotation: Quaternionf, scale: Float,
        clickId: Int, chosenId: Int, mouseDirection: Vector3f
    ): Int {
        val a = drawMesh(
            pipeline, cameraTransform, position, rotation, scale,
            clickId, chosenId, mouseDirection, scaleRef
        )
        val b = drawMesh(
            pipeline, cameraTransform, position, rotation, scale * 0.35f,
            clickId + 3, chosenId, mouseDirection, arrowRef1
        )
        return combineResults(a, b)
    }

    fun drawRotateGizmos(
        pipeline: Pipeline?, cameraTransform: Matrix4f, position: Vector3d, scale: Float,
        clickId: Int, chosenId: Int, mouseDirection: Vector3f
    ): Int {
        return drawMesh(pipeline, cameraTransform, position, scale, clickId, chosenId, mouseDirection, ringRef)
    }

    fun drawTranslateGizmos(
        pipeline: Pipeline?, cameraTransform: Matrix4f, position: Vector3d, scale: Float,
        clickId: Int, chosenId: Int, mouseDirection: Vector3f
    ): Int {
        val a = drawMesh(
            pipeline, cameraTransform, position, scale,
            clickId, chosenId, mouseDirection, arrowRef
        )
        val b = drawMesh(
            pipeline, cameraTransform, position, scale * 0.35f,
            clickId + 3, chosenId, mouseDirection, arrowRef1
        )
        return combineResults(a, b)
    }

    fun drawMesh(
        pipeline: Pipeline?, cameraTransform: Matrix4f, position: Vector3d, scale: Float,
        clickId: Int, chosenId: Int, mouseDirection: Vector3f, ref: FileReference
    ): Int {
        val tmp = rotations[4].identity()
        return drawMesh(
            pipeline, cameraTransform, position, tmp, scale,
            clickId, chosenId, mouseDirection, ref
        )
    }

    fun drawMesh(
        pipeline: Pipeline?, cameraTransform: Matrix4f, position: Vector3d, rotation: Quaternionf, scale: Float,
        clickId: Int, chosenId: Int, mouseDirection: Vector3f, ref: FileReference
    ): Int {
        val mesh = MeshCache.getEntry(ref).value as? Mesh ?: return 0
        var result = 0
        val tmp = rotations[3]
        for (i in 0 until 3) {
            rotation.mul(rotations[i], tmp)
            result += drawMesh(
                pipeline, cameraTransform, position, tmp, scale,
                colors[i], clickId + i, chosenId, mesh, mouseDirection
            ).toInt(1 shl i)
        }
        return result
    }

    val local = Matrix4x3()
    val localInv = Matrix4x3()

    val rayPos = Vector3f()
    val rayDir = Vector3f()

    val rotations = listOf(
        Quaternionf(),
        Quaternionf().rotateX(+PIf * 0.5f).rotateY(+PIf * 0.5f),
        Quaternionf().rotateX(-PIf * 0.5f).rotateZ(-PIf * 0.5f),
        Quaternionf(),// tmp1
        Quaternionf() // tmp2
    )

    val colors = intArrayOf(
        colorX,
        colorY,
        colorZ
    )

    fun drawMesh(
        pipeline: Pipeline?, cameraTransform: Matrix4f,
        position: Vector3d, rotation: Quaternionf, scale: Float,
        color: Int, clickId: Int, chosenId: Int, mesh: Mesh, mouseDirection: Vector3f
    ): Boolean = drawMesh(
        pipeline, cameraTransform, position, rotation, scale, defaultMaterial,
        if (clickId == chosenId) -1 else color, mesh, mouseDirection
    )

    fun drawMesh(
        pipeline: Pipeline?, cameraTransform: Matrix4f,
        position: Vector3d, rotation: Quaternionf, scale: Float,
        material: Material, color: Int, mesh: Mesh, mouseDirection: Vector3f
    ): Boolean {

        val localTransform = local
        localTransform.translationRotateScale(
            position.x, position.y, position.z,
            rotation.x, rotation.y, rotation.z, rotation.w,
            scale, scale, scale
        )
        localTransform.invert(localInv)

        rayPos.set(cameraPosition)
        rayDir.set(mouseDirection)

        localInv.transformPosition(rayPos)
        localInv.transformDirection(rayDir)
        rayDir.normalize()

        val hit = RaycastMesh.raycastLocalMesh(
            mesh, rayPos, rayDir, Float.POSITIVE_INFINITY,
            -1, null, false
        )
        drawMesh(pipeline, cameraTransform, localTransform, material, color, mesh)
        return hit.isFinite()
    }

    fun drawMesh(
        pipeline: Pipeline?, cameraTransform: Matrix4f, localTransform: Matrix4x3?,
        material: Material, color: Int, mesh: Mesh
    ) {
        val shader = (material.shader ?: pbrModelShader).value
        shader.use()
        shader.m4x4("transform", cameraTransform)
        shader.m4x3delta("localTransform", localTransform, cameraPosition)
        val invLocalTransformU = shader["invLocalTransform"]
        if (invLocalTransformU >= 0) {
            val tmp = JomlPools.mat4x3m.borrow()
            if (localTransform != null) localTransform.invert(tmp)
            else tmp.identity()
            shader.m4x3(invLocalTransformU, tmp)
        }
        material.bind(shader)
        shader.v4f("diffuseBase", color)
        shader.v4f("tint", color)
        shader.v1b("hasAnimation", false)
        shader.v1b("hasVertexColors", false)
        mesh.draw(pipeline, shader, 0)
    }

    // avoid unnecessary allocations ;)
    private val tmp3fs = createArrayList(3) { Vector3f() }
    private val xAxis = Vector3f(1f, 0f, 0f)
    private val yAxis = Vector3f(0f, 1f, 0f)
    private val zAxis = Vector3f(0f, 0f, 1f)

    // used by Rem's Studio
    fun drawGizmo(cameraTransform: Matrix4f, x0: Int, y0: Int, w: Int, h: Int) {

        /**
         * display a 3D gizmo
         * todo beautify a little, take inspiration from Blender maybe ;)
         * */

        cameraTransform.transformDirection(xAxis, tmp3fs[0])
        cameraTransform.transformDirection(yAxis, tmp3fs[1])
        cameraTransform.transformDirection(zAxis, tmp3fs[2])

        val oriX = tmp3fs[0]
        val oriY = tmp3fs[1]

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
                v === oriX -> UIColors.axisXColor
                v === oriY -> UIColors.axisYColor
                else -> UIColors.axisZColor
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
                rectSize, rectSize, color or black
            )
        }
    }
}