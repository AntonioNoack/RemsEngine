package me.anno.engine.ui.render

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.light.*
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.EditorState
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.input.Input
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.firstOrNull2
import me.anno.utils.structures.lists.Lists.mapFirstNotNull
import org.joml.Vector3d
import kotlin.math.floor
import kotlin.math.min

object DebugRendering {

    fun showShadowMapDebug(view: RenderView) {
        // show the shadow map for debugging purposes
        val light = EditorState.selection
            .mapFirstNotNull { e ->
                if (e is Entity) {
                    e.getComponentsInChildren(LightComponentBase::class)
                        .firstOrNull2 { if (it is LightComponent) it.hasShadow else true }
                } else if (e is LightComponent && e.hasShadow) e else null
            }
        if (light != null) {
            val x = view.x
            val y = view.y
            val w = view.width
            val h = view.height
            val s = min(w, h) / 3
            var texture: ITexture2D? = null
            var isDepth = false
            val flipY = light is PlanarReflection || light is DirectionalLight
            when (light) {
                is LightComponent -> {
                    val tex = light.shadowTextures
                    texture = tex?.depthTexture ?: tex?.getTexture0()
                    isDepth = true
                }
                is EnvironmentMap -> texture = light.texture?.textures?.firstOrNull()
                is PlanarReflection -> texture = light.lastBuffer?.getTexture0()
            }
            // draw the texture
            if (texture is Texture2D && texture.isDestroyed) return
            if (texture is Texture2DArray && texture.isDestroyed) return
            if (texture is CubemapTexture && texture.isDestroyed) return
            when (texture) {
                is CubemapTexture -> {
                    DrawTextures.drawProjection(x, y + h - s, s * 3 / 2, s, texture, true, -1, false, isDepth)
                }
                is Texture2DArray -> {
                    val layer = floor(Time.gameTime % texture.layers).toFloat()
                    if (Input.isShiftDown && light is PlanarReflection) {
                        DrawTextures.drawTextureArray(x, y, w, h, texture, layer, true, 0x33ffffff, null)
                    } else if (isDepth) {
                        DrawTextures.drawDepthTextureArray(x, y + h, s, -s, texture, layer)
                    } else if (flipY) {
                        DrawTextures.drawTextureArray(x, y + h - s, s, s, texture, layer, true, -1, null)
                    } else {
                        DrawTextures.drawTextureArray(x, y + h, s, -s, texture, layer, true, -1, null)
                    }
                    DrawTexts.drawSimpleTextCharByChar(x, y + h - s, 2, "#${layer.toInt()}")
                }
                is ITexture2D -> {
                    if (Input.isShiftDown && light is PlanarReflection) {
                        DrawTextures.drawTexture(x, y, w, h, texture, true, 0x33ffffff, null)
                    } else if (isDepth) {
                        DrawTextures.drawDepthTexture(x, y + h, s, -s, texture)
                    } else if (flipY) {
                        DrawTextures.drawTexture(x, y + h - s, s, s, texture, true, -1, null)
                    } else {
                        DrawTextures.drawTexture(x, y + h, s, -s, texture, true, -1, null)
                    }
                }
            }
        }
    }

    fun showCameraRendering(view: RenderView, x0: Int, y0: Int, x1: Int, y1: Int) {
        val camera = EditorState.selection
            .firstOrNull { it is Camera } ?: EditorState.selection
            .firstNotNullOfOrNull { e -> if (e is Entity) e.getComponentInChildren(Camera::class) else null }
        if (camera is Camera && !Input.isShiftDown) {
            // todo this is incorrect for orthographic cameras, I think
            // calculate size of sub camera
            val w = (x1 - x0 + 1) / 3
            val h = (y1 - y0 + 1) / 3
            val buffer = view.base1Buffer
            val renderer = Renderers.pbrRenderer
            // todo is tone mapping used? adjust parameter for drawScene
            view.prepareDrawScene(w, h, w.toFloat() / h, camera, camera, 0f, false)
            view.drawScene(w, h, renderer, buffer, true, false)
            DrawTextures.drawTexture(x1 - w, y1, w, -h, buffer.getTexture0(), true, -1, null)
            // prepareDrawScene needs to be reset afterwards, because we seem to have a kind-of-bug somewhere
            val camera2 = view.editorCamera
            view.prepareDrawScene(
                view.width,
                view.height,
                view.width.toFloat() / view.height,
                camera2,
                camera2,
                0f,
                false
            )
        }
    }

    fun drawDebugShapes(view: RenderView) {
        drawDebugPoints(view)
        drawDebugLines(view)
        drawDebugRays(view)
        drawDebugAABBs(view)
        LineBuffer.finish(view.cameraMatrix)
        drawDebugTexts(view)
    }

    private fun drawDebugPoints(view: RenderView) {
        val points = DebugShapes.debugPoints
        for (i in points.indices) {
            val point = points[i]
            drawDebugPoint(view, point.position, point.color)
        }
    }

    private fun drawDebugLines(view: RenderView) {
        val lines = DebugShapes.debugLines
        for (i in lines.indices) {
            val line = lines[i]
            LineBuffer.putRelativeLine(
                line.p0, line.p1,
                view.cameraPosition, view.worldScale,
                line.color
            )
        }
    }

    private fun drawDebugAABBs(view: RenderView) {
        val aabbs = DebugShapes.debugAABBs
        for (i in aabbs.indices) {
            val aabb = aabbs[i]
            DrawAABB.drawAABB(aabb.bounds, aabb.color, view.cameraPosition, view.worldScale)
        }
    }

    private fun drawDebugRays(view: RenderView) {
        val rays = DebugShapes.debugRays
        for (i in rays.indices) {
            val ray = rays[i]
            val pos = ray.start
            val dir = ray.direction
            val color = ray.color
            val length = view.radius * 100.0
            drawDebugPoint(view, pos, color)
            LineBuffer.putRelativeVector(
                pos, dir, length,
                view.cameraPosition, view.worldScale,
                color
            )
        }
    }

    private fun drawDebugTexts(view: RenderView) {
        val worldScale = view.worldScale
        val camPosition = view.cameraPosition
        val v = JomlPools.vec4f.borrow()
        // transform is only correct, if we use a temporary framebuffer!
        val sx = +view.width * 0.5f
        val sy = -view.height * 0.5f
        val x0 = +sx
        val y0 = -sy
        val betterBlending = GFXState.currentBuffer.getTargetType(0) == TargetType.UByteTarget4
        val pbb = DrawTexts.pushBetterBlending(betterBlending)
        val texts = DebugShapes.debugTexts
        val cameraMatrix = view.cameraMatrix
        for (index in texts.indices) {
            val text = texts[index]
            val pos = text.position
            val px = (pos.x - camPosition.x) * worldScale
            val py = (pos.y - camPosition.y) * worldScale
            val pz = (pos.z - camPosition.z) * worldScale
            cameraMatrix.transform(v.set(px, py, pz, 1.0))
            if (v.w > 0f && v.x in -v.w..v.w && v.y in -v.w..v.w) {
                val vx = v.x * sx / v.w + x0
                val vy = v.y * sy / v.w + y0
                DrawTexts.drawSimpleTextCharByChar(
                    vx.toInt(), vy.toInt(), 0, text.text,
                    text.color, 0, AxisAlignment.CENTER, AxisAlignment.CENTER
                )
            }
        }
        DrawTexts.popBetterBlending(pbb)
    }

    fun drawDebugPoint(view: RenderView, p: Vector3d, color: Int) {
        val camPosition = view.cameraPosition
        val worldScale = view.worldScale
        val d = p.distance(view.cameraPosition) * 0.01
        LineBuffer.putRelativeLine(
            p.x - d, p.y, p.z, p.x + d, p.y, p.z,
            camPosition, worldScale, color
        )
        LineBuffer.putRelativeLine(
            p.x, p.y - d, p.z, p.x, p.y + d, p.z,
            camPosition, worldScale, color
        )
        LineBuffer.putRelativeLine(
            p.x, p.y, p.z - d, p.x, p.y, p.z + d,
            camPosition, worldScale, color
        )
    }
}