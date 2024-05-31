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
import me.anno.engine.ui.render.RenderState.worldScale
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.files.BundledRef
import me.anno.io.files.FileReference
import me.anno.utils.Color.black
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.toInt
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI

object Gizmos {

    val arrowRef = BundledRef("meshes/arrowX.obj")
    val arrowRef1 = BundledRef("meshes/arrowX1.obj")
    val ringRef = BundledRef("meshes/ringX.obj")
    val scaleRef = BundledRef("meshes/scaleX.obj")

    fun drawScaleGizmos(
        pipeline: Pipeline?, cameraTransform: Matrix4f, position: Vector3d, scale: Double,
        clickId: Int, chosenId: Int, mouseDirection: Vector3d
    ): Int {
        val a = drawMesh(pipeline, cameraTransform, position, scale, clickId, chosenId, mouseDirection, scaleRef)
        val b = drawMesh(
            pipeline,
            cameraTransform,
            position,
            scale * 0.35,
            clickId + 3,
            chosenId,
            mouseDirection,
            arrowRef1
        )
        return if (b != 0) b.inv().and(7) else a
    }

    fun drawRotateGizmos(
        pipeline: Pipeline?, cameraTransform: Matrix4f, position: Vector3d, scale: Double,
        clickId: Int, chosenId: Int, mouseDirection: Vector3d
    ): Int {
        return drawMesh(pipeline, cameraTransform, position, scale, clickId, chosenId, mouseDirection, ringRef)
    }

    fun drawTranslateGizmos(
        pipeline: Pipeline?, cameraTransform: Matrix4f, position: Vector3d, scale: Double,
        clickId: Int, chosenId: Int, mouseDirection: Vector3d
    ): Int {
        val a = drawMesh(pipeline, cameraTransform, position, scale, clickId, chosenId, mouseDirection, arrowRef)
        val b = drawMesh(
            pipeline, cameraTransform, position, scale * 0.35,
            clickId + 3, chosenId, mouseDirection, arrowRef1
        )
        return if (b != 0) b.inv().and(7) else a
    }

    fun drawMesh(
        pipeline: Pipeline?, cameraTransform: Matrix4f, position: Vector3d, scale: Double,
        clickId: Int, chosenId: Int, mouseDirection: Vector3d, ref: FileReference
    ): Int {
        val mesh = MeshCache[ref] ?: return 0
        val x = drawMesh(
            pipeline, cameraTransform, position, rotations[0], scale,
            colorX, clickId, chosenId, mesh, mouseDirection
        )
        val y = drawMesh(
            pipeline, cameraTransform, position, rotations[1], scale,
            colorY, clickId + 1, chosenId, mesh, mouseDirection
        )
        val z = drawMesh(
            pipeline, cameraTransform, position, rotations[2], scale,
            colorZ, clickId + 2, chosenId, mesh, mouseDirection
        )
        return x.toInt() + y.toInt(2) + z.toInt(4)
    }

    val local = Matrix4x3d()
    val localInv = Matrix4x3d()

    val rayPos = Vector3f()
    val rayDir = Vector3f()

    val rotations = listOf(
        Quaterniond(),
        Quaterniond().rotateX(+PI * 0.5).rotateY(+PI * 0.5),
        Quaterniond().rotateX(-PI * 0.5).rotateZ(-PI * 0.5)
    )

    fun drawMesh(
        pipeline: Pipeline?, cameraTransform: Matrix4f,
        position: Vector3d, rotation: Quaterniond, scale: Double,
        color: Int, clickId: Int, chosenId: Int, mesh: Mesh, mouseDirection: Vector3d
    ): Boolean = drawMesh(
        pipeline, cameraTransform, position, rotation, scale, defaultMaterial,
        if (clickId == chosenId) -1 else color, mesh, mouseDirection
    )

    fun drawMesh(
        pipeline: Pipeline?, cameraTransform: Matrix4f,
        position: Vector3d, rotation: Quaterniond, scale: Double,
        material: Material, color: Int, mesh: Mesh, mouseDirection: Vector3d
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

        val hit = RaycastMesh.raycastLocalMeshAnyHit(mesh, rayPos, rayDir, Float.POSITIVE_INFINITY, -1)
        drawMesh(pipeline, cameraTransform, localTransform, material, color, mesh)
        return hit
    }

    fun drawMesh(
        pipeline: Pipeline?, cameraTransform: Matrix4f, localTransform: Matrix4x3d?,
        material: Material, color: Int, mesh: Mesh
    ) {
        val shader = (material.shader ?: pbrModelShader).value
        shader.use()
        shader.m4x4("transform", cameraTransform)
        shader.m4x3delta("localTransform", localTransform, cameraPosition, worldScale)
        if (shader["invLocalTransform"] >= 0) {
            val tmp = JomlPools.mat4x3d.borrow()
            if (localTransform != null) localTransform.invert(tmp)
            else tmp.identity()
            val tmp2 = JomlPools.mat4x3f.borrow().set(tmp)
            shader.m4x3("invLocalTransform", tmp2)
        }
        shader.v1f("worldScale", worldScale)
        material.bind(shader)
        shader.v4f("diffuseBase", color)
        shader.v4f("tint", color)
        shader.v1b("hasAnimation", false)
        shader.v1b("hasVertexColors", false)
        mesh.draw(pipeline, shader, 0)
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
                v === oriX -> 0xff7777
                v === oriY -> 0x77ff77
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
                rectSize, rectSize, color or black
            )
        }
    }
}