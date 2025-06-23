package me.anno.engine.ui.render

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.EntityQuery.getComponents
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.LightComponentBase
import me.anno.ecs.components.light.PlanarReflection
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.ControlSettings
import me.anno.engine.ui.render.Renderers.tonemapKt
import me.anno.engine.ui.render.RowColLayout.findGoodTileLayout
import me.anno.gpu.Clipping
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.GLNames
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.buffer.TriangleBuffer
import me.anno.gpu.debug.DebugGPUStorage.isDepthFormat
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredRenderer
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawRectangles.drawBorder
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.query.GPUClockNanos
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.effects.framegen.FrameGenInitNode
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.ui.Panel
import me.anno.ui.UIColors
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.black
import me.anno.utils.Color.r
import me.anno.utils.Color.toRGB
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import me.anno.utils.hpc.WorkSplitter
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.firstOrNull2
import me.anno.utils.structures.lists.Lists.mapFirstNotNull
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.toIntOr
import me.anno.utils.types.NumberFormatter.formatFloat
import me.anno.utils.types.NumberFormatter.formatIntTriplets
import me.anno.utils.types.Vectors.toLinear
import me.anno.utils.types.Vectors.toSRGB
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector4f
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/**
 * Helpers to draw debug information onto RenderView
 * */
object DebugRendering {
    fun findInspectedLight(): LightComponentBase? {
        return EditorState.selection
            .mapFirstNotNull { e ->
                if (e is Entity) {
                    e.getComponents(LightComponentBase::class)
                        .firstOrNull2 { if (it is LightComponent) it.hasShadow else true }
                } else if (e is LightComponent && e.hasShadow) e else null
            }
    }

    fun showShadowMapDebug(view: RenderView) {
        // show the shadow map for debugging purposes
        val light = findInspectedLight() ?: return

        val x = view.x
        val y = view.y
        val w = view.width
        val h = view.height
        val s = min(w, h) / 3
        var texture: ITexture2D? = null
        var isDepth = false
        when (light) {
            is LightComponent -> {
                val tex = light.shadowTextures
                texture = tex?.depthTexture ?: tex?.getTexture0()
                isDepth = true
            }
            is EnvironmentMap -> texture = light.texture?.textures?.firstOrNull()
            is PlanarReflection -> texture = light.framebuffer?.getTexture0()
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
                        DrawTextures.drawTextureArray(
                            x, y, w, h, texture, layer, true,
                            white.withAlpha(0.7f), null, true
                        )
                    } else if (isDepth) {
                        DrawTextures.drawDepthTextureArray(x, y + h - s, s, s, texture, layer)
                    } else {
                        DrawTextures.drawTextureArray(x, y + h - s, s, s, texture, layer, true, -1, null)
                    }
                    DrawTexts.drawSimpleTextCharByChar(x, y + h - s, 2, "#${layer.toInt()}")
                }
                else -> {
                    if (Input.isShiftDown && light is PlanarReflection) {
                        DrawTextures.drawTexture(
                            x, y, w, h, texture, true,
                            white.withAlpha(0.7f), null, true
                        )
                    } else if (isDepth) {
                        DrawTextures.drawDepthTexture(x, y + h, s, -s, texture)
                    } else {
                        DrawTextures.drawTexture(x, y + h - s, s, s, texture, true, -1, null)
                    }
                }
            }
        }
    }

    val drawSceneTimer = GPUClockNanos()
    val drawGizmoTimer = GPUClockNanos()
    val drawLightsTimer = GPUClockNanos()
    val drawFinalTimer = GPUClockNanos()

    fun findInspectedCamera(): Camera? {
        val camera = EditorState.selection
            .firstOrNull { it is Camera } ?: EditorState.selection
            .firstNotNullOfOrNull { e -> if (e is Entity) e.getComponent(Camera::class) else null }
        return camera as? Camera
    }

    fun showCameraRendering(rv: RenderView, x0: Int, y0: Int, x1: Int, y1: Int) {
        val camera = findInspectedCamera() ?: return
        // save near,far
        val near = rv.near
        val far = rv.far
        // calculate size of sub camera
        val w = (x1 - x0 + 1) / 3
        val h = (y1 - y0 + 1) / 3
        val buffer = rv.buffers.base1Buffer
        val renderer = Renderers.pbrRenderer
        timeRendering("DrawScene", drawSceneTimer) {
            rv.prepareDrawScene(w, h, w.toFloat() / h, camera, update = false, fillPipeline = true)
            rv.drawScene(w, h, renderer, buffer, changeSize = true, hdr = true, sky = true)
        }
        // restore near,far
        rv.near = near
        rv.far = far
        DrawTextures.drawTexture(
            x1 - w, y1 - h, w, h,
            buffer.getTexture0(), true
        )
    }

    fun showTimeRecords(rv: RenderView) {
        GFXState.drawCall("ShowTimeRecords") {
            val textBatch = DrawTexts.startSimpleBatch()
            val records = GFXState.timeRecords
            var total = 0L
            for (i in records.indices) {
                val record = records[i]
                drawTime(rv, i, record.name, record.deltaNanos, record.divisor)
                total += record.deltaNanos
            }
            drawTime(rv, records.size, "Total", total, 1)
            val maySkip = rv.renderMode.renderGraph?.nodes?.any2 { it is FrameGenInitNode } == true
            if (!(maySkip && !FrameGenInitNode.isLastFrame())) {
                records.clear()
            }
            DrawTexts.finishSimpleBatch(textBatch)
        }
    }

    private fun drawTime(rv: Panel, i: Int, name: String, time: Long, divisor: Int) {
        val y = rv.y + i * monospaceFont.sizeInt
        if (divisor > 1) debugBuilder.append(divisor).append("x ")
        debugBuilder.append(name).append(": ")
            .formatFloat(time / (1e6 * divisor), 3, false)
            .append(" ms")
        DrawTexts.drawSimpleTextCharByChar(
            rv.x + rv.width, y, 1, debugBuilder,
            AxisAlignment.MAX, AxisAlignment.MIN
        )
        debugBuilder.clear()
    }

    fun drawDebugShapes(view: RenderView, cameraMatrix: Matrix4f) {
        drawDebugPoints(view)
        drawDebugLines(view)
        drawDebugArrows(view)
        drawDebugRays(view)
        drawDebugAABBs(view)
        LineBuffer.finish(cameraMatrix)
        drawDebugTriangles(view)
        TriangleBuffer.finish(cameraMatrix)
        GFXState.depthMode.use(view.depthMode.alwaysMode) {
            GFXState.depthMask.use(false) {
                drawDebugTexts(view)
            }
        }
    }

    private fun drawDebugPoints(view: RenderView) {
        val points = DebugShapes.debugPoints
        for (i in points.indices) {
            val point = points[i]
            drawDebugPoint(view, point.position, point.color)
        }
    }

    private fun drawDebugArrows(view: RenderView) {
        val arrows = DebugShapes.debugArrows
        val dirX = JomlPools.vec3d.create()
        val dirY = JomlPools.vec3d.create()
        val dirZ = JomlPools.vec3d.create()
        for (i in arrows.indices) {
            val arrow = arrows[i]
            LineBuffer.putRelativeLine(arrow.from, arrow.to, view.cameraPosition, arrow.color)
            arrow.to.sub(arrow.from, dirY)
            val len = dirY.length()
            dirY.findSystem(dirX, dirZ)
            val s = len * 0.2
            fun addPt(dxi: Double, dzi: Double) {
                val anchor = arrow.from.mix(arrow.to, 0.6, dirY)
                dirX.mulAdd(dxi, anchor, anchor)
                dirZ.mulAdd(dzi, anchor, anchor)
                LineBuffer.putRelativeLine(anchor, arrow.to, view.cameraPosition, arrow.color)
            }
            addPt(+s, 0.0)
            addPt(-s, 0.0)
            addPt(0.0, +s)
            addPt(0.0, -s)
        }
        JomlPools.vec3d.sub(3)
    }

    private fun drawDebugLines(view: RenderView) {
        val lines = DebugShapes.debugLines
        for (i in lines.indices) {
            val line = lines[i]
            LineBuffer.putRelativeLine(line.p0, line.p1, view.cameraPosition, line.color)
        }
    }

    private fun drawDebugAABBs(view: RenderView) {
        val aabbs = DebugShapes.debugAABBs
        for (i in aabbs.indices) {
            val aabb = aabbs[i]
            DrawAABB.drawAABB(aabb.bounds, aabb.color, view.cameraPosition)
        }
    }

    private fun drawDebugTriangles(view: RenderView) {
        val triangles = DebugShapes.debugTriangles
        for (i in triangles.indices) {
            val tri = triangles[i]
            TriangleBuffer.putRelativeTriangle(tri.p0, tri.p1, tri.p2, view.cameraPosition, tri.color)
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
                view.cameraPosition, color
            )
        }
    }

    private fun drawDebugTexts(view: RenderView) {
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
            drawDebugTexts2(view, camPosition, v, x0, y0, sx, sy)
            DrawTexts.popBetterBlending(pbb)
        } else {
            GFXState.blendMode.use(me.anno.gpu.blending.BlendMode.DEFAULT) {
                drawDebugTexts2(view, camPosition, v, x0, y0, sx, sy)
            }
        }
    }

    private fun drawDebugTexts2(
        view: RenderView, camPosition: Vector3d, v: Vector4f,
        x0: Float, y0: Float, sx: Float, sy: Float
    ) {
        val texts = DebugShapes.debugTexts
        val cameraMatrix = view.cameraMatrix
        val ptb = DrawTexts.pushTrueBlending(true)
        val batch = DrawTexts.startSimpleBatch()
        for (index in texts.indices) {
            val text = texts[index]
            val pos = text.position
            val px = pos.x - camPosition.x
            val py = pos.y - camPosition.y
            val pz = pos.z - camPosition.z
            cameraMatrix.transform(v.set(px, py, pz, 1.0))
            if (v.w > 0f && v.x in -v.w..v.w && v.y in -v.w..v.w) {
                val vx = v.x * sx / v.w + x0
                val vy = v.y * sy / v.w + y0
                DrawTexts.drawSimpleTextCharByChar(
                    vx.toInt(), vy.toInt(), 0, text.text,
                    text.color, text.color.withAlpha(0),
                    AxisAlignment.CENTER, AxisAlignment.CENTER,
                    batched = true
                )
            }
        }
        DrawTexts.finishSimpleBatch(batch)
        DrawTexts.popTrueBlending(ptb)
    }

    fun drawDebugPoint(view: RenderView, p: Vector3d, color: Int) {
        val camPosition = view.cameraPosition
        val d = p.distance(view.cameraPosition) * 0.03 * tan(view.fovYRadians * 0.5)
        LineBuffer.putRelativeLine(
            p.x - d, p.y, p.z, p.x + d, p.y, p.z,
            camPosition, color
        )
        LineBuffer.putRelativeLine(
            p.x, p.y - d, p.z, p.x, p.y + d, p.z,
            camPosition, color
        )
        LineBuffer.putRelativeLine(
            p.x, p.y, p.z - d, p.x, p.y, p.z + d,
            camPosition, color
        )
    }

    fun drawLightCount(
        view: RenderView, w: Int, h: Int,
        x0: Int, y0: Int, x1: Int, y1: Int,
        renderer: Renderer, buffer: IFramebuffer, lightBuffer: IFramebuffer, deferred: DeferredSettings
    ) {
        GFXState.drawCall("LightCount") {
            // draw scene for depth
            view.drawScene(
                w, h,
                renderer, buffer,
                changeSize = true,
                hdr = false, // doesn't matter
                sky = false // doesn't matter, I think
            )
            view.pipeline.lightStage.visualizeLightCount = true

            val tex = Texture.texture(buffer, deferred, DeferredLayerType.DEPTH)
            view.drawSceneLights(buffer, tex.tex as Texture2D, tex.mask, lightBuffer)
            view.drawGizmos(lightBuffer, true)

            // todo special shader to better differentiate the values than black-white
            // (1 is extremely dark, nearly black)
            DrawTextures.drawTexture(
                x0, y0, x1 - x0, y1 - y0,
                lightBuffer.getTexture0(), true,
                white, null, true // lights are bright -> dim them down
            )
            view.pipeline.lightStage.visualizeLightCount = false
        }
    }

    fun drawAllBuffers(
        view: RenderView, w: Int, h: Int,
        x0: Int, y0: Int, x1: Int, y1: Int,
        renderer: Renderer, buffer: IFramebuffer, lightBuffer: IFramebuffer, deferred: DeferredSettings
    ) {
        GFXState.pushDrawCallName("AllBuffers")
        val layers = deferred.storageLayers
        val size = layers.size + 1 + GFX.supportsDepthTextures.toInt() /* 1 for light, 1 for depth */
        val colsRows = findGoodTileLayout(size, view.width, view.height)
        val cols = colsRows.x
        val rows = colsRows.y

        timeRendering("Scene", drawSceneTimer) {
            view.drawScene(w / cols, h / rows, renderer, buffer, changeSize = true, hdr = false)
        }

        timeRendering("Gizmos", drawGizmoTimer) {
            view.drawGizmos(buffer, true)
        }

        timeRendering("Light", drawLightsTimer) {
            val tex = Texture.texture(buffer, deferred, DeferredLayerType.DEPTH)
            view.drawSceneLights(buffer, tex.tex as Texture2D, tex.mask, lightBuffer)
        }

        val pbb = DrawTexts.pushBetterBlending(true)
        for (index in 0 until size) {
            GFXState.drawCall("Buffer[$index]") {
                // rows x N field
                val col = index % cols
                val x02 = x0 + WorkSplitter.partition(x1 - x0, col, cols)
                val x12 = x0 + WorkSplitter.partition(x1 - x0, col + 1, cols)
                val row = index / cols
                val y02 = y0 + WorkSplitter.partition(y1 - y0, row, rows)
                val y12 = y0 + WorkSplitter.partition(y1 - y0, row + 1, rows)

                var color = white
                var applyToneMapping = false
                val texture = when (index) {
                    size - (1 + GFX.supportsDepthTextures.toInt()) -> {
                        // draw the light buffer as the last stripe
                        color = (0x22 * 0x10101) or black
                        applyToneMapping = true
                        lightBuffer.getTexture0()
                    }
                    size - 1 -> buffer.depthTexture ?: missingTexture
                    else -> {
                        val texture = buffer.getTextureI(index)
                        applyToneMapping = texture.isHDR
                        texture
                    }
                }

                // y flipped, because it would be incorrect otherwise
                if (index == size - 1 && texture != missingTexture) {
                    DrawTextures.drawDepthTexture(x02, y02, x12 - x02, y12 - y02, texture)
                } else {
                    DrawTextures.drawTexture(
                        x02, y02, x12 - x02, y12 - y02, texture,
                        true, color, null, applyToneMapping
                    )
                }

                val f = 0.8f
                // draw alpha on right/bottom side
                if (index < layers.size) Clipping.clip2(
                    if (y12 - y02 > x12 - x02) x02 else Maths.mix(x02, x12, f),
                    if (y12 - y02 > x12 - x02) Maths.mix(y02, y12, f) else y02,
                    x12, y12
                ) { DrawTextures.drawTextureAlpha(x02, y02, x12 - x02, y12 - y02, texture) }
                // draw title
                DrawTexts.drawSimpleTextCharByChar(x02, y02, 2, texture.name)
            }
        }
        DrawTexts.popBetterBlending(pbb)
        GFXState.popDrawCallName()
    }

    fun drawAllLayers(
        view: RenderView, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int,
        renderer: Renderer, buffer: IFramebuffer, light: IFramebuffer, deferred: DeferredSettings
    ) {
        GFXState.pushDrawCallName("AllLayers")
        val size = deferred.layerTypes.size + 1 + GFX.supportsDepthTextures.toInt() /* 1 for light, 1 for depth */
        val colsRows = findGoodTileLayout(size, view.width, view.height)
        val cols = colsRows.x
        val rows = colsRows.y

        timeRendering("Scene", drawSceneTimer) {
            view.drawScene(w / cols, h / rows, renderer, buffer, changeSize = true, hdr = false)
        }

        timeRendering("Gizmos", drawGizmoTimer) {
            view.drawGizmos(buffer, true)
        }

        timeRendering("Lights", drawLightsTimer) {
            val depthTex = Texture.texture(buffer, deferred, DeferredLayerType.DEPTH)
            view.drawSceneLights(buffer, depthTex.tex as Texture2D, depthTex.mask, light)
        }

        // instead of drawing the raw buffers, draw the actual layers (color,roughness,metallic,...)

        val tw = w / cols
        val th = h / rows
        val tmp = FBStack["tmp-layers", tw, th, 4, false, 1, DepthBufferType.INTERNAL]
        val settings = DeferredRenderer.deferredSettings!!

        val pbb = DrawTexts.pushBetterBlending(true)
        for (index in 0 until size) {
            GFXState.drawCall("Layers[$index]") {
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
                var color = FrameTimings.textColor
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
                        useFrame(tmp) {
                            val shader = Renderers.attributeEffects[layer to settings]!!
                            shader.use()
                            DepthTransforms.bindDepthUniforms(shader)
                            settings.findTexture(buffer, layer)!!.bindTrulyNearest(0)
                            flat01.draw(shader)
                        }
                        texture = tmp.getTexture0()
                    }
                }

                // y flipped, because it would be incorrect otherwise
                if (index == size - 1 && texture != missingTexture) {
                    DrawTextures.drawDepthTexture(x02, y02, tw, th, texture)
                } else {
                    DrawTextures.drawTexture(
                        x02, y02, tw, th, texture, true, color,
                        null, applyTonemapping
                    )
                }
                DrawTexts.drawSimpleTextCharByChar(
                    (x02 + x12) / 2, (y02 + y12) / 2, 2,
                    name, AxisAlignment.CENTER, AxisAlignment.CENTER
                )
            }
        }
        DrawTexts.popBetterBlending(pbb)
        GFXState.popDrawCallName()
    }

    private val debugBuilder = StringBuilder(32)
    fun drawDebugStats(view: RenderView, drawnPrimitives0: Long, drawnInstances0: Long, drawCalls0: Long) {
        val pbb = DrawTexts.pushBetterBlending(true)
        val drawnPrimitives = PipelineStageImpl.drawnPrimitives - drawnPrimitives0
        val drawnInstances = PipelineStageImpl.drawnInstances - drawnInstances0
        val drawCalls = PipelineStageImpl.drawCalls - drawCalls0
        val usesBetterBlending = DrawTexts.canUseComputeShader()
        debugBuilder
            .formatIntTriplets(drawnPrimitives).append(" tris, ")
            .formatIntTriplets(drawnInstances).append(" inst, ")
            .formatIntTriplets(drawCalls).append(" calls")
        DrawTexts.drawSimpleTextCharByChar(
            view.x + 2, view.y + view.height + 1,
            0, debugBuilder,
            FrameTimings.textColor,
            FrameTimings.background.color.withAlpha(if (usesBetterBlending) 0 else 255),
            AxisAlignment.MIN, AxisAlignment.MAX
        )
        debugBuilder.clear()
        DrawTexts.popBetterBlending(pbb)
    }

    /**
     * Some devices don't support RenderDoc well,
     * so show all steps on screen for immediate debugging
     * */
    fun drawDebugSteps(view: RenderView) {
        val enabled = view.controlScheme?.settings?.showDebugFrames
        if (enabled != true) return
        GFXState.drawCall("drawDebugSteps") {
            drawDebugSteps1(view)
        }
    }

    private const val inspectorSize = 15
    private const val inspectorPadding = inspectorSize.shr(1)
    private val settings = ControlSettings()
    private val tmpTexture = Texture2D("debug", inspectorSize, inspectorSize, 1)
    private val tmpPixels = FloatArray(4 * inspectorSize * inspectorSize)
    private fun readPixelsAsFloatsAt(src: ITexture2D, mxi: Int, myi: Int): FloatArray {
        if (!tmpTexture.isCreated()) tmpTexture.create(TargetType.Float32x4)
        useFrame(tmpTexture) {
            renderPurely {
                val dx = inspectorPadding - mxi
                val dy = inspectorPadding - myi
                DrawTextures.drawTexture(dx, dy, src.width, src.height, src)
            }
        }
        tmpTexture.readFloatPixels(0, 0, inspectorSize, inspectorSize, tmpPixels)
        return tmpPixels
    }

    private fun handleDebugControls() {
        if (Input.isAltDown) {
            if (Input.wasKeyPressed(Key.KEY_ARROW_RIGHT)) settings.inspectedX++
            if (Input.wasKeyPressed(Key.KEY_ARROW_LEFT)) settings.inspectedX--
            if (Input.wasKeyPressed(Key.KEY_ARROW_UP)) settings.inspectedY++
            if (Input.wasKeyPressed(Key.KEY_ARROW_DOWN)) settings.inspectedY--
            if (Input.wasKeyPressed(Key.KEY_P)) settings.drawInspected = !settings.drawInspected
        }
    }

    private fun <V> List<Pair<String, V>>.distinctTextures(): List<Pair<String, V>> {
        if (isEmpty()) return emptyList() // shortcut
        val set = HashSet<V>()
        return filter { set.add(it.second) }
    }

    private fun findRelevantTextures(node: Node): List<Pair<String, ITexture2D>> {
        return node.outputs.mapNotNull { output ->
            if (output.type == "Texture") {
                val value = (output.currValue as? Texture)?.texOrNull
                if (value != null) Pair(output.name, value) else null
            } else null
        }.distinctTextures()
    }

    private fun findRelevantNodes(graph: FlowGraph): List<Pair<String, List<Pair<String, ITexture2D>>>> {
        return graph.nodes.mapNotNull { node ->
            val textures = findRelevantTextures(node)
            if (textures.isNotEmpty()) Pair(node.name, textures) else null
        }
    }

    private fun drawTextureInBig(view: RenderView, slotName: String, texture: ITexture2D) {
        if (isDepthFormat(texture.internalFormat) || slotName == "Depth") {
            // if is depth, draw display depth
            DrawTextures.drawDepthTexture(view.x, view.y, view.width, view.height, texture)
        } else {
            DrawTextures.drawTexture(
                view.x, view.y, view.width, view.height, texture,
                true, -1, null, texture.isHDR
            )
        }
    }

    fun drawDebugSteps1(view: RenderView) {

        // show color under cursor in bigger in a corner
        // show color under cursor as numbers on a side
        // use special keys/settings to define which frame is inspected
        // draw a border around what's inspected
        // optionally make what's inspected bigger

        val window = view.windowStack
        val mx = (window.mouseX - view.x) / view.width
        val my = (window.mouseY - view.y) / view.height

        handleDebugControls()

        val graph = view.renderMode.renderGraph ?: return
        val relevantNodes = findRelevantNodes(graph)
        if (relevantNodes.isEmpty()) return

        settings.inspectedX = clamp(settings.inspectedX, 0, relevantNodes.lastIndex)
        settings.inspectedY = clamp(settings.inspectedY, 0, relevantNodes.maxOf { it.second.lastIndex })

        val inspectedX = settings.inspectedX
        val inspectedY = clamp(settings.inspectedY, 0, relevantNodes[inspectedX].second.lastIndex)

        val (passName, outputs) = relevantNodes[inspectedX]
        val (slotName, texture) = outputs[outputs.lastIndex - inspectedY]

        val mxi = (mx * texture.width).toIntOr()
        val myi = (my * texture.height).toIntOr()
        val inspectedValues = readPixelsAsFloatsAt(texture, mxi, myi)

        if (settings.drawInspected) {
            drawTextureInBig(view, slotName, texture)
        }

        drawDebugSteps(view, relevantNodes, inspectedX, inspectedY)
        drawInspectedPixelData(view, passName, inspectedValues, texture, mxi, myi)
    }

    private fun drawDebugSteps(
        view: RenderView,
        relevantNodes: List<Pair<String, List<Pair<String, ITexture2D>>>>,
        inspectedX: Int, inspectedY: Int,
    ) {
        var nx = relevantNodes.size
        val ny = max(relevantNodes.maxOf { it.second.size }, 4)
        nx = max(ceilDiv(ny * view.width, view.height), nx)
        val sz = view.width / nx
        val cx = (nx - relevantNodes.size).shr(1) * sz
        val x0 = view.x + cx
        val y0 = view.y + view.height
        for (xi in relevantNodes.indices) {
            val (name, outputs) = relevantNodes[xi]
            for (yi in outputs.indices) {
                val (slotName, texture) = outputs[yi]
                val x2 = x0 + WorkSplitter.partition(xi + 0, view.width, nx)
                val x3 = x0 + WorkSplitter.partition(xi + 1, view.width, nx)
                val y = y0 - sz * (outputs.size - yi)
                val isInspected = inspectedX == xi && inspectedY == outputs.lastIndex - yi
                if (isDepthFormat(texture.internalFormat) || slotName == "Depth") {
                    // if is depth, draw display depth
                    DrawTextures.drawDepthTexture(x2, y, x3 - x2, sz, texture)
                } else {
                    DrawTextures.drawTexture(
                        x2, y, x3 - x2, sz, texture,
                        true, white, null, texture.isHDR
                    )
                }
                if (isInspected) {
                    drawBorder(x2, y, x3 - x2, sz, black, 2)
                    drawBorder(x2, y, x3 - x2, sz, UIColors.paleGoldenRod, 1)
                }
            }
            val x = x0 + WorkSplitter.partition(xi, view.width, nx)
            val y = y0 - sz + 1
            DrawTexts.drawSimpleTextCharByChar(
                x, y, 1, name,
                FrameTimings.textColor, view.background.color,
                AxisAlignment.MIN, AxisAlignment.MAX
            )
        }
    }

    private fun drawInspectedPixelData(
        view: RenderView, passName: String, values: FloatArray,
        texture: ITexture2D, xii: Int, yii: Int,
    ) {
        val fontSize = monospaceFont.sizeInt
        val numChannels = texture.channels
        val numLines = numChannels + 3
        val tileSize = fontSize * 2 / 5
        val x2 = view.x
        val menuHeight = numLines * fontSize + tileSize * inspectorSize
        val y2 = view.y + fontSize * 4

        fun drawLine(y: Int, text: String) {
            DrawTexts.drawSimpleTextCharByChar(
                x2, y2 + y * fontSize, 1,
                text, FrameTimings.textColor, view.background.color,
                AxisAlignment.MIN, AxisAlignment.MAX
            )
        }

        // draw info and colors and numbers
        val textBatch = DrawTexts.startSimpleBatch()
        drawLine(0, passName)
        drawLine(1, "${texture.name}, ${GLNames.getName(texture.internalFormat)} ${texture.samples}x, $xii,$yii")
        val offset = (inspectorPadding * inspectorSize + inspectorPadding) * 4
        for (i in 0 until numChannels) {
            drawLine(i + 2, "${"RGBA"[i]}: ${values[i + offset]}")
        }
        drawLine(numChannels + 2, "Controls: Alt + Arrows / P")
        DrawTexts.finishSimpleBatch(textBatch)

        // draw inspected color as rectangle
        val rectBatch = DrawRectangles.startBatch()
        val y3 = y2 + menuHeight - tileSize
        val isDepth = isDepthFormat(texture.internalFormat)
        for (yi in 0 until inspectorSize) {
            for (xi in 0 until inspectorSize) {
                val color = getColor(xi, yi, values, texture.isHDR, isDepth)
                val xj = x2 - 1 + xi * tileSize
                val yj = y3 - yi * tileSize
                drawRect(xj, yj, tileSize, tileSize, color)
                if (xi == inspectorPadding && yi == inspectorPadding) {
                    val borderColor = if (color.r() > 150) black else white
                    drawBorder(xj, yj, tileSize, tileSize, borderColor, 1)
                }
            }
        }
        DrawRectangles.finishBatch(rectBatch)
    }

    private fun getColor(xi: Int, yi: Int, values: FloatArray, isHDR: Boolean, isDepth: Boolean): Int {
        val offset = (xi + yi * inspectorSize) * 4
        val tmp3 = JomlPools.vec3f.borrow()
        tmp3.set(values, offset)
        if (isDepth) {
            tmp3.set(
                fract(log2(tmp3.x)),
                fract(log2(tmp3.y)),
                fract(log2(tmp3.z))
            )
        } else if (isHDR) {
            tmp3.toLinear()
            tonemapKt(tmp3)
            tmp3.toSRGB()
        }
        return tmp3.toRGB(255f)
    }
}