package me.anno.engine.ui.render

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.LightComponentBase
import me.anno.ecs.components.light.PlanarReflection
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.EditorState
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.input.Input
import me.anno.utils.structures.lists.Lists.firstOrNull2
import me.anno.utils.structures.lists.Lists.mapFirstNotNull
import org.joml.Vector3d

object DebugRendering {

    fun showShadowMapDebug(view: RenderView) {
        // show the shadow map for debugging purposes
        val light = EditorState.selection
            .mapFirstNotNull { e ->
                if (e is Entity) {
                    e.getComponentsInChildren(LightComponentBase::class)
                        .firstOrNull2 { if (it is LightComponent) it.hasShadow else true }
                } else null
            }
        if (light != null) {
            val texture: ITexture2D? = when (light) {
                is LightComponent -> light.shadowTextures?.firstOrNull()?.depthTexture
                is EnvironmentMap -> light.texture?.textures?.firstOrNull()
                is PlanarReflection -> light.lastBuffer
                else -> null
            }
            // draw the texture
            val x = view.x
            val y = view.y
            val w = view.w
            val h = view.h
            when (texture) {
                is CubemapTexture -> {
                    val s = w / 4
                    DrawTextures.drawProjection(x, y + s, s * 3 / 2, -s, texture, true, -1)
                }
                is ITexture2D -> {
                    if (Input.isShiftDown && light is PlanarReflection) {
                        DrawTextures.drawTexture(x, y + h, w, -h, texture, true, 0x33ffffff, null)
                    } else {
                        val s = w / 3
                        DrawTextures.drawTexture(x, y + s, s, -s, texture, true, -1, null)
                    }
                }
            }
        }
    }

    fun showCameraRendering(view: RenderView, x0: Int, y0: Int, x1: Int, y1: Int) {
        val c0 = EditorState.selection
            .filterIsInstance<Entity>()
            .mapNotNull { e -> e.getComponentInChildren(Camera::class) }
        val c1 = EditorState.selection.filterIsInstance<Camera>()
        val camera = (c1 + c0).firstOrNull()
        if (camera != null && !Input.isShiftDown) {
            // calculate size of sub camera
            val w = (x1 - x0 + 1) / 3
            val h = (y1 - y0 + 1) / 3
            val buffer = view.base1Buffer
            val renderer = Renderers.pbrRenderer
            val useDeferredRendering = false
            view.prepareDrawScene(w, h, w.toFloat() / h, camera, camera, 0f, false)
            view.drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)
            DrawTextures.drawTexture(x1 - w, y1, w, -h, buffer.getTexture0(), true, -1, null)
            // prepareDrawScene needs to be reset afterwards, because we seem to have a kind-of-bug somewhere
            val camera2 = view.editorCamera
            view.prepareDrawScene(view.w, view.h, view.w.toFloat() / view.h, camera2, camera2, 0f, false)
        }
    }

    fun drawDebug(view: RenderView) {
        val worldScale = view.worldScale
        val points = DebugShapes.debugPoints
        val lines = DebugShapes.debugLines
        val rays = DebugShapes.debugRays
        val camPosition = RenderView.camPosition
        for (index in points.indices) {
            val point = points[index]
            // visualize a point
            drawDebugPoint(view, point.position, point.color)
        }
        for (index in lines.indices) {
            val line = lines[index]
            LineBuffer.putRelativeLine(
                line.p0, line.p1,
                camPosition, worldScale,
                line.color
            )
        }
        for (index in rays.indices) {
            val ray = rays[index]
            val pos = ray.start
            val dir = ray.direction
            val color = ray.color
            val length = view.radius * 100.0
            drawDebugPoint(view, pos, color)
            LineBuffer.putRelativeVector(
                pos, dir, length,
                camPosition, worldScale,
                color
            )
        }
        val time = Engine.gameTime
        points.removeIf { it.timeOfDeath < time }
        lines.removeIf { it.timeOfDeath < time }
        rays.removeIf { it.timeOfDeath < time }
    }

    fun drawDebugPoint(view: RenderView, p: Vector3d, color: Int) {
        val worldScale = view.worldScale
        val d = p.distance(RenderView.camPosition) * 0.01
        LineBuffer.putRelativeLine(
            p.x - d, p.y, p.z, p.x + d, p.y, p.z,
            RenderView.camPosition, worldScale, color
        )
        LineBuffer.putRelativeLine(
            p.x, p.y - d, p.z, p.x, p.y + d, p.z,
            RenderView.camPosition, worldScale, color
        )
        LineBuffer.putRelativeLine(
            p.x, p.y, p.z - d, p.x, p.y, p.z + d,
            RenderView.camPosition, worldScale, color
        )
    }

}