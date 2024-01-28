package me.anno.engine.ui.render

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.EntityQuery.getComponents
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.LightComponentBase
import me.anno.ecs.components.light.PlanarReflection
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.EditorState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.TriangleBuffer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredRenderer
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.graph.render.Texture
import me.anno.input.Input
import me.anno.maths.Maths
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.firstOrNull2
import me.anno.utils.structures.lists.Lists.mapFirstNotNull
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3d
import org.joml.Vector4f
import kotlin.math.floor
import kotlin.math.min

/**
 * Helpers to draw debug information onto RenderView
 * */
object DebugRendering {

    fun showShadowMapDebug(view: RenderView) {
        // show the shadow map for debugging purposes
        val light = EditorState.selection
            .mapFirstNotNull { e ->
                if (e is Entity) {
                    e.getComponents(LightComponentBase::class)
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
            if (texture != null && texture.isCreated()) {
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
                    else -> {
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
    }

    fun showCameraRendering(view: RenderView, x0: Int, y0: Int, x1: Int, y1: Int) {
        val camera = EditorState.selection
            .firstOrNull { it is Camera } ?: EditorState.selection
            .firstNotNullOfOrNull { e -> if (e is Entity) e.getComponent(Camera::class) else null }
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
            // prepareDrawScene needs to be reset afterward, because we seem to have a kind-of-bug somewhere
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
        drawDebugTriangles(view)
        TriangleBuffer.finish(view.cameraMatrix)
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
            LineBuffer.putRelativeLine(line.p0, line.p1, view.cameraPosition, view.worldScale, line.color)
        }
    }

    private fun drawDebugAABBs(view: RenderView) {
        val aabbs = DebugShapes.debugAABBs
        for (i in aabbs.indices) {
            val aabb = aabbs[i]
            DrawAABB.drawAABB(aabb.bounds, aabb.color, view.cameraPosition, view.worldScale)
        }
    }

    private fun drawDebugTriangles(view: RenderView) {
        val triangles = DebugShapes.debugTriangles
        for (i in triangles.indices) {
            val tri = triangles[i]
            TriangleBuffer.putRelativeTriangle(tri.p0, tri.p1, tri.p2, view.cameraPosition, view.worldScale, tri.color)
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
        val betterBlending = GFXState.currentBuffer.getTargetType(0) == TargetType.UInt8x4 &&
                GFXState.currentBuffer.samples == 1 && DrawTexts.canUseComputeShader()
        if (betterBlending) {
            val pbb = DrawTexts.pushBetterBlending(true)
            drawDebugTexts2(view, camPosition, worldScale, v, x0, y0, sx, sy)
            DrawTexts.popBetterBlending(pbb)
        } else {
            GFXState.blendMode.use(me.anno.gpu.blending.BlendMode.DEFAULT) {
                drawDebugTexts2(view, camPosition, worldScale, v, x0, y0, sx, sy)
            }
        }
    }

    private fun drawDebugTexts2(
        view: RenderView, camPosition: Vector3d, worldScale: Double, v: Vector4f,
        x0: Float, y0: Float, sx: Float, sy: Float
    ) {
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
                    text.color, text.color.withAlpha(0),
                    AxisAlignment.CENTER, AxisAlignment.CENTER
                )
            }
        }
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

    fun drawLightCount(
        view: RenderView, x: Int, y: Int, w: Int, h: Int,
        renderer: Renderer, buffer: IFramebuffer, lightBuffer: IFramebuffer, deferred: DeferredSettings
    ) {
        // draw scene for depth
        view.drawScene(
            w, h,
            renderer, buffer,
            changeSize = true,
            hdr = false // doesn't matter
        )
        view.pipeline.lightStage.visualizeLightCount = true

        val tex = Texture.texture(buffer, deferred, DeferredLayerType.DEPTH)
        view.drawSceneLights(buffer, tex.tex as Texture2D, tex.mask!!, lightBuffer)
        view.drawGizmos(lightBuffer, true)

        // todo special shader to better differentiate the values than black-white
        // (1 is extremely dark, nearly black)
        DrawTextures.drawTexture(
            x, y + h - 1, w, -h,
            lightBuffer.getTexture0(), true,
            -1, null, true // lights are bright -> dim them down
        )
        view.pipeline.lightStage.visualizeLightCount = false
    }

    fun drawAllBuffers(
        view: RenderView, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int,
        renderer: Renderer, buffer: IFramebuffer, lightBuffer: IFramebuffer, deferred: DeferredSettings
    ) {

        val layers = deferred.storageLayers
        val size = layers.size + 1 + GFX.supportsDepthTextures.toInt() /* 1 for light, 1 for depth */
        val (rows, cols) = view.findRowsCols(size)

        view.drawScene(w / cols, h / rows, renderer, buffer, changeSize = true, hdr = false)
        view.drawGizmos(buffer, true)

        val tex = Texture.texture(buffer, deferred, DeferredLayerType.DEPTH)
        view.drawSceneLights(buffer, tex.tex as Texture2D, tex.mask!!, lightBuffer)

        val pbb = DrawTexts.pushBetterBlending(true)
        for (index in 0 until size) {

            // rows x N field
            val col = index % cols
            val x02 = x0 + (x1 - x0) * (col + 0) / cols
            val x12 = x0 + (x1 - x0) * (col + 1) / cols
            val row = index / cols
            val y02 = y0 + (y1 - y0) * (row + 0) / rows
            val y12 = y0 + (y1 - y0) * (row + 1) / rows

            var color = white
            var applyToneMapping = false
            val texture = when (index) {
                size - (1 + GFX.supportsDepthTextures.toInt()) -> {
                    // draw the light buffer as the last stripe
                    color = (0x22 * 0x10101) or Color.black
                    applyToneMapping = true
                    lightBuffer.getTexture0()
                }
                size - 1 -> buffer.depthTexture ?: missingTexture
                else -> buffer.getTextureI(index)
            }

            // y flipped, because it would be incorrect otherwise
            if (index == size - 1 && texture != missingTexture) {
                DrawTextures.drawDepthTexture(x02, y02, x12 - x02, y12 - y02, texture)
            } else {
                DrawTextures.drawTexture(
                    x02, y12, x12 - x02, y02 - y12, texture,
                    true, color, null, applyToneMapping
                )
            }

            val f = 0.8f
            // draw alpha on right/bottom side
            if (index < layers.size) GFX.clip2(
                if (y12 - y02 > x12 - x02) x02 else Maths.mix(x02, x12, f),
                if (y12 - y02 > x12 - x02) Maths.mix(y02, y12, f) else y02,
                x12, y12
            ) { DrawTextures.drawTextureAlpha(x02, y12, x12 - x02, y02 - y12, texture) }
            // draw title
            DrawTexts.drawSimpleTextCharByChar(x02, y02, 2, texture.name)
        }
        DrawTexts.popBetterBlending(pbb)
    }

    fun drawAllLayers(
        view: RenderView, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int,
        renderer: Renderer, buffer: IFramebuffer, light: IFramebuffer, deferred: DeferredSettings
    ) {

        val size = deferred.layerTypes.size + 1 + GFX.supportsDepthTextures.toInt() /* 1 for light, 1 for depth */
        val (rows, cols) = view.findRowsCols(size)

        view.drawScene(w / cols, h / rows, renderer, buffer, changeSize = true, hdr = false)
        view.drawGizmos(buffer, true)

        val depthTex = Texture.texture(buffer, deferred, DeferredLayerType.DEPTH)
        view.drawSceneLights(buffer, depthTex.tex as Texture2D, depthTex.mask!!, light)

        // instead of drawing the raw buffers, draw the actual layers (color,roughness,metallic,...)

        val tw = w / cols
        val th = h / rows
        val tmp = FBStack["tmp-layers", tw, th, 4, false, 1, DepthBufferType.INTERNAL]
        val settings = DeferredRenderer.deferredSettings!!

        val pbb = DrawTexts.pushBetterBlending(true)
        for (index in 0 until size) {

            // rows x N field
            val col = index % cols
            val x02 = x0 + (x1 - x0) * (col + 0) / cols
            val x12 = x0 + (x1 - x0) * (col + 1) / cols
            val row = index / cols
            val y02 = y0 + (y1 - y0) * (row + 0) / rows
            val y12 = y0 + (y1 - y0) * (row + 1) / rows

            val name: String
            val texture: ITexture2D
            var applyTonemapping = false
            var color = white
            when (index) {
                size - (1 + GFX.supportsDepthTextures.toInt()) -> {
                    texture = light.getTexture0()
                    val exposure = 0x22 // same as RenderMode.LIGHT_SUM.ToneMappingNode.Inputs[Exposure]
                    color = (exposure * 0x10101).withAlpha(255)
                    applyTonemapping = true
                    name = "Light"
                }
                size - 1 -> {
                    texture = buffer.depthTexture ?: missingTexture
                    name = "Depth"
                }
                else -> {
                    // draw the light buffer as the last stripe
                    val layer = deferred.layerTypes[index]
                    name = layer.name
                    GFXState.useFrame(tmp) {
                        val shader = Renderers.attributeEffects[layer to settings]!!
                        shader.use()
                        DepthTransforms.bindDepthToPosition(shader)
                        settings.findTexture(buffer, layer)!!.bindTrulyNearest(0)
                        SimpleBuffer.flat01.draw(shader)
                    }
                    texture = tmp.getTexture0()
                }
            }

            // y flipped, because it would be incorrect otherwise
            if (index == size - 1 && texture != missingTexture) {
                DrawTextures.drawDepthTexture(x02, y02, tw, th, texture)
            } else {
                DrawTextures.drawTexture(
                    x02, y12, tw, -th, texture, true, color,
                    null, applyTonemapping
                )
            }
            DrawTexts.drawSimpleTextCharByChar(
                (x02 + x12) / 2, (y02 + y12) / 2, 2,
                name, AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        }
        DrawTexts.popBetterBlending(pbb)
    }
}