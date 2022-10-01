package me.anno.engine.ui.render

// this list of imports is insane XD
import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.effects.OutlineEffect
import me.anno.ecs.components.camera.effects.SSAOEffect
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.shaders.effects.*
import me.anno.ecs.components.ui.CanvasComponent
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.debug.DebugShapes
import me.anno.engine.pbr.DeferredRenderer
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.PlaneShapes
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.DefaultSun.defaultSun
import me.anno.engine.ui.render.DefaultSun.defaultSunEntity
import me.anno.engine.ui.render.DrawAABB.drawAABB
import me.anno.engine.ui.render.ECSShaderLib.clearPbrModelShader
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.MovingGrid.drawGrid
import me.anno.engine.ui.render.Outlines.drawOutline
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.Renderers.cheapRenderer
import me.anno.engine.ui.render.Renderers.frontBackRenderer
import me.anno.engine.ui.render.Renderers.overdrawRenderer
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.engine.ui.render.Renderers.rawAttributeRenderers
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.engine.ui.render.Renderers.tonemapInvKt
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.clip2
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFX.shaderColor
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.M4x3Delta.mul4x3delta
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.popBetterBlending
import me.anno.gpu.drawing.DrawTexts.pushBetterBlending
import me.anno.gpu.drawing.DrawTextures.drawDepthTexture
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.DrawTextures.drawTextureAlpha
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.*
import me.anno.gpu.pipeline.LightPipelineStage
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Renderer.Companion.depthRenderer
import me.anno.gpu.shader.Renderer.Companion.idRenderer
import me.anno.gpu.shader.effects.DepthBasedAntiAliasing
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.input.Input.isKeyDown
import me.anno.input.Input.isShiftDown
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.roundDiv
import me.anno.mesh.Shapes
import me.anno.ui.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.ui.style.Style
import me.anno.utils.Clock
import me.anno.utils.Color.black
import me.anno.utils.Tabs
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

// todo create the different pipeline stages: opaque, transparent, post-processing, ...

// to do render the grid slightly off position, so we don't get flickering, always closer to the camera, proportional to radius
// (because meshes at 0 are very common and to be expected)

// done shadows
// todo usable editing of materials: own color + indent + super material selector
// todo + add & remove materials

// done render in different modes: overdraw, color blindness, normals, color, before-post-process, with-post-process
// done nice ui for that: drop down menus at top or bottom

// to do blend between multiple cameras, only allow 2? yes :)

// todo easily allow for multiple players in the same instance, with just player key mapping
// -> camera cannot be global, or todo it must be switched whenever the player changes

// todo we could do the blending of the scenes using stencil tests <3 (very efficient)
//  - however it would limit us to a single renderer...
// -> first just draw a single scene and later todo make it multiplayer

// todo make shaders of materials be references via a file? this will allow for visual shaders in the future

// todo define the render pipeline from the editor? maybe from the code in an elegant way? top level element?

/**
 * a panel that renders the scene;
 * no controls are provided by this class, it just draws
 * */
open class RenderView(val library: EditorState, var playMode: PlayMode, style: Style) : Panel(style) {

    private var bloomStrength = 0.5f
    private var bloomOffset = 10f
    private val useBloom get() = bloomOffset > 0f && renderMode != RenderMode.WITHOUT_POST_PROCESSING

    private val ssao = SSAOEffect()

    var controlScheme: ControlScheme? = null

    // can exist (game/game mode), but does not need to (editor)
    var localPlayer: LocalPlayer? = null

    val editorCamera = Camera()
    val editorCameraNode = Entity(editorCamera)

    val isFinalRendering get() = playMode != PlayMode.EDITING

    var renderMode = RenderMode.DEFAULT

    var radius = 50.0
        set(value) {
            field = clamp(value, 1e-130, 1e130)
        }

    open fun updateWorldScale() {
        worldScale = if (renderMode == RenderMode.MONO_WORLD_SCALE) 1.0 else 1.0 / radius
    }

    val position = Vector3d()
    val rotation = Vector3d(-20.0, 0.0, 0.0)

    private val deferredRenderer = DeferredRenderer
    private val deferred = deferredRenderer.deferredSettings!!

    val baseNBuffer = deferred.createBaseBuffer()
    private val baseSameDepth = baseNBuffer.attachFramebufferToDepth(1, false)
    val base1Buffer = Framebuffer("debug", 1, 1, 1, 4, false, DepthBufferType.TEXTURE)

    private val light1Buffer = base1Buffer.attachFramebufferToDepth(arrayOf(TargetType.FP16Target4))
    private val lightNBuffer = baseNBuffer.attachFramebufferToDepth(arrayOf(TargetType.FP16Target4))

    private val clock = Clock()
    private var entityBaseClickId = 0

    val pipeline = Pipeline(deferred)
    private val stage0 = PipelineStage(
        "default",
        Sorting.NO_SORTING,
        MAX_FORWARD_LIGHTS,
        null,
        DepthMode.CLOSER,
        true,
        CullMode.BACK,
        pbrModelShader
    )

    init {
        pipeline.defaultStage = stage0
        pipeline.stages.add(stage0)
    }

    override val canDrawOverBorders: Boolean = true

    override fun destroy() {
        super.destroy()
        // all framebuffers that we own need to be freed
        light1Buffer.destroy()
        lightNBuffer.destroy()
        baseSameDepth.destroy()
        base1Buffer.destroy()
        baseNBuffer.destroy()
        editorCameraNode.destroy()
        fsr22.destroy()
    }

    fun updateEditorCameraTransform() {

        val radius = radius
        val camera = editorCamera
        val cameraNode = editorCameraNode
        val tmpQ = JomlPools.quat4d.borrow()
        cameraNode.transform.localRotation = rotation.toQuaternionDegrees(tmpQ)
        camera.far = far
        camera.near = near
        /*if (renderMode == RenderMode.DEPTH) {
            0.2
        } else {
            if (reverseDepth) radius * 1e-10
            else radius * 1e-2
        }*/

        val rotation = cameraNode.transform.localRotation

        if (!position.isFinite) LOGGER.warn("Invalid position $position")
        if (!rotation.isFinite) LOGGER.warn("Invalid rotation $rotation")

        val tmp3d = JomlPools.vec3d.borrow()
        cameraNode.transform.localPosition = rotation.transform(tmp3d.set(0.0, 0.0, radius)).add(position)
        cameraNode.validateTransform()

        // println(cameraNode.transform.localTransform)

    }

    override fun onUpdate() {
        super.onUpdate()
        // we could optimize that: if not has updated in some time, don't redraw
        invalidateDrawing()
        updateWorldScale()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        val count0 = PipelineStage.drawnTriangles

        clock.start()

        // to see ghosting, if there is any
        val renderMode = renderMode
        if (renderMode == RenderMode.GHOSTING_DEBUG) Thread.sleep(250)

        updateEditorCameraTransform()

        setRenderState()

        val world = getWorld()
        if (isKeyDown(GLFW.GLFW_KEY_PAUSE)) {
            world?.simpleTraversal(false) {
                if (it is Entity && it.hasComponentInChildren(MeshComponentBase::class)) {
                    val transform = it.transform
                    println("${Tabs.spaces(2 * it.depthInHierarchy)}'${it.name}':\n${transform.localTransform}\n${transform.globalTransform}")
                }
                false
            }
        }

        setRenderState()

        // done go through the rendering pipeline, and render everything

        // todo draw all local players in their respective fields
        // todo use customizable masks for the assignment (plus a button mapping)
        // todo if a color doesn't appear on the mapping, it doesn't need to be drawn
        // todo more important players can have a larger field
        // todo slope of these partial windows can be customized for nicer looks

        // localPlayer = world.localPlayers.children.firstOrNull() as? LocalPlayer

        // todo find which sections shall be rendered for what camera
        val localPlayer = localPlayer
        var camera0 = localPlayer?.cameraState?.previousCamera ?: editorCamera
        val camera1 = localPlayer?.cameraState?.currentCamera ?: editorCamera
        var blending = localPlayer?.cameraState?.cameraBlendingProgress ?: 0f
        if (localPlayer == null) updateEditorCameraTransform()

        if (blending >= 1f) {
            blending = 1f
            camera0 = camera1
        }

        val showIds = renderMode == RenderMode.CLICK_IDS
        val showOverdraw = renderMode == RenderMode.OVERDRAW
        val showSpecialBuffer = showIds || showOverdraw || isKeyDown('j')
        var useDeferredRendering = when (renderMode) {
            RenderMode.DEFAULT, RenderMode.CLICK_IDS, RenderMode.DEPTH, RenderMode.FSR_X4,
            RenderMode.FSR_MSAA_X4, RenderMode.FSR_SQRT2, RenderMode.FSR_X2, RenderMode.NEAREST_X4,
            RenderMode.GHOSTING_DEBUG, RenderMode.INVERSE_DEPTH, RenderMode.WITHOUT_POST_PROCESSING,
            RenderMode.LINES, RenderMode.LINES_MSAA, RenderMode.FRONT_BACK, RenderMode.UV,
            RenderMode.SHOW_TRIANGLES, RenderMode.MSAA_X8 -> false
            else -> true
        }

        stage0.blendMode = if (showOverdraw) BlendMode.ADD else null
        stage0.sorting = Sorting.FRONT_TO_BACK

        var aspect = w.toFloat() / h

        val layers = deferred.settingsV1.layers
        val size = when (renderMode) {
            RenderMode.ALL_DEFERRED_BUFFERS -> layers.size + 1 /* 1 for light */
            RenderMode.ALL_DEFERRED_LAYERS -> deferred.layerTypes.size + 1 /* 1 for light */
            else -> 1
        }

        val rows = when {
            size % 2 == 0 -> 2
            size % 3 == 0 -> 3
            size > 12 -> 3
            size > 6 -> 2
            else -> 1
        }

        val cols = (size + rows - 1) / rows
        if (size > 1) {
            aspect *= rows.toFloat() / cols.toFloat()
        }

        stage0.cullMode = if (renderMode != RenderMode.FRONT_BACK) CullMode.BACK else CullMode.BOTH

        clock.stop("initialization", 0.05)

        prepareDrawScene(w, h, aspect, camera0, camera1, blending, true)

        setRenderState()

        // todo only if the render-mode/camera-effects use light information
        if (pipeline.hasTooManyLights() || useBloom) useDeferredRendering = true

        clock.stop("preparing", 0.05)

        var renderer = when (renderMode) {
            RenderMode.OVERDRAW -> overdrawRenderer
            RenderMode.CLICK_IDS -> idRenderer
            RenderMode.FORCE_DEFERRED, RenderMode.SSAO, RenderMode.SS_REFLECTIONS -> {
                useDeferredRendering = true
                DeferredRenderer
            }
            RenderMode.FORCE_NON_DEFERRED, RenderMode.MSAA_X8,
            RenderMode.FSR_MSAA_X4, RenderMode.LINES,
            RenderMode.LINES_MSAA -> {
                useDeferredRendering = false
                pbrRenderer
            }
            RenderMode.FRONT_BACK -> {
                useDeferredRendering = false
                frontBackRenderer
            }
            RenderMode.SHOW_TRIANGLES -> {
                useDeferredRendering = false
                Renderer.triangleVisRenderer
            }
            else -> renderMode.renderer ?: (if (useDeferredRendering) DeferredRenderer else pbrRenderer)
        }

        val dlt = renderMode.dlt
        if (dlt != null) {
            useDeferredRendering = false
            renderer = attributeRenderers[dlt]!!
        }

        // multi-sampled buffer
        val buffer = when {
            renderMode == RenderMode.MSAA_X8 ||
                    renderMode == RenderMode.LINES_MSAA ||
                    renderMode == RenderMode.FSR_MSAA_X4 ->
                FBStack["", w, h, 4, false, 8, true]
            renderer == DeferredRenderer -> baseNBuffer
            else -> base1Buffer
        }

        drawScene(
            x0, y0, x1, y1,
            camera0, camera1, blending,
            renderer, buffer,
            useDeferredRendering,
            size, cols, rows, layers.size
        )

        val pbb = pushBetterBlending(true)
        if (world == null) {
            drawSimpleTextCharByChar(
                x + w / 2, y + h / 2, 4, "World Not Found!", AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        }

        if (showSpecialBuffer) {
            drawSimpleTextCharByChar(
                x + 20, y, 2, renderMode.name
            )
        }

        if (useDeferredRendering) {
            drawSimpleTextCharByChar(
                x + w, y, 2, "DEFERRED", AxisAlignment.MAX
            )
        }
        popBetterBlending(pbb)

        if (!isFinalRendering) {
            DebugRendering.showShadowMapDebug(this)
            DebugRendering.showCameraRendering(this, x0, y0, x1, y1)
        }

        if (world is Entity && playMode != PlayMode.EDITING) {
            world.getComponentsInChildren(CanvasComponent::class, false) { comp ->
                if (comp.space == CanvasComponent.Space.CAMERA_SPACE) {
                    comp.width = x1 - x0
                    comp.height = y1 - y0
                    comp.render()
                    val texture = comp.framebuffer!!.getTexture0()
                    drawTexture(x0, y1, x1 - x0, y0 - y1, texture, -1, null)
                }
                false
            }
        }

        if (playMode == PlayMode.EDITING) {
            pushBetterBlending(true)
            val count1 = PipelineStage.drawnTriangles
            val deltaCount = count1 - count0
            drawSimpleTextCharByChar(
                x + DrawTexts.monospaceFont.sizeInt / 4,
                y + h - 2 - DrawTexts.monospaceFont.sizeInt,
                2,
                deltaCount.toString(),
                FrameTimings.textColor,
                FrameTimings.backgroundColor and 0xffffff
            )
            popBetterBlending(pbb)
        }

        updatePrevState()
        // clock.total("drawing the scene", 0.1)

    }

    // be more conservative with framebuffer size changes,
    // because they are expensive -> only change every 20th frame
    private val mayChangeSize get() = (Engine.frameIndex % 20 == 0)

    private val fsr22 by lazy { FSR2v2() }

    private fun drawScene(
        x0: Int, y0: Int, x1: Int, y1: Int,
        camera0: Camera, camera1: Camera, blending: Float,
        renderer: Renderer,
        buffer: IFramebuffer,
        useDeferredRendering: Boolean,
        size: Int, cols: Int, rows: Int, layersSize: Int
    ) {

        var w = x1 - x0
        var h = y1 - y0

        when (renderMode) {
            RenderMode.FSR_SQRT2 -> {
                // 12/17 ~ 0.706 ~ sqrt 1/2
                w = roundDiv(w * 12, 17)
                h = roundDiv(h * 12, 17)
            }
            RenderMode.FSR_X2 -> {
                w = (w + 1) / 2
                h = (h + 1) / 2
            }
            RenderMode.FSR_X4, RenderMode.FSR_MSAA_X4, RenderMode.NEAREST_X4 -> {
                w = (w + 2) / 4
                h = (h + 2) / 4
            }
            RenderMode.FSR2_V2 -> {
                w = (w + 1) / 8
                h = (h + 1) / 8
            }
            else -> {
            }
        }

        w = max(w, 1)
        h = max(h, 1)

        val s0 = w * h
        val s1 = buffer.w * buffer.h
        val mayChangeSize = mayChangeSize || (w * h < 1024) || min(s0, s1) * 2 <= max(s0, s1)
        if (!mayChangeSize) {
            w = buffer.w
            h = buffer.h
        }

        // clock.stop("drawing scene", 0.05)

        var dstBuffer = buffer

        val renderMode = renderMode
        var isHDR = false
        val useFSR = when (renderMode) {
            RenderMode.FSR_X2,
            RenderMode.FSR_SQRT2,
            RenderMode.FSR_X4,
            RenderMode.FSR_MSAA_X4 -> true
            else -> false
        }
        when {
            useDeferredRendering -> {
                when {
                    renderMode.dlt != null -> {
                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, buffer,
                            changeSize = true,
                            hdr = false
                        )
                        drawGizmos(buffer, true)
                        GFX.copyNoAlpha(buffer)
                        return
                    }
                    renderMode == RenderMode.FSR2_V2 -> {
                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, buffer,
                            changeSize = true,
                            hdr = true
                        )
                        val motion = FBStack["motion", w, h, 4, BufferQuality.HIGH_16, 1, true]
                        drawScene(
                            w, h, camera0, camera1, blending, rawAttributeRenderers[DeferredLayerType.MOTION]!!,
                            motion, changeSize = false, hdr = true
                        )

                        val lightBuffer = lightNBuffer
                        drawSceneLights(w, h, camera0, camera1, blending, copyRenderer, buffer, lightBuffer)

                        val ssao = blackTexture
                        val pw = x1 - x0
                        val ph = y1 - y0

                        // use the existing depth buffer for the 3d ui
                        val dstBuffer0 = baseSameDepth
                        useFrame(w, h, true, dstBuffer0) {
                            // don't write depth
                            GFXState.depthMask.use(false) {
                                val shader = LightPipelineStage.getPostShader(deferred)
                                shader.use()
                                shader.v1b("applyToneMapping", true)
                                buffer.bindTextures(2, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                                ssao.bindTrulyNearest(shader, "ambientOcclusion")
                                lightBuffer.bindTexture0(
                                    shader,
                                    "finalLight",
                                    GPUFiltering.TRULY_NEAREST,
                                    Clamping.CLAMP
                                )
                                flat01.draw(shader)
                            }
                        }

                        fsr22.calculate(
                            dstBuffer0.getTexture0() as Texture2D,
                            buffer.depthTexture as Texture2D,
                            deferred.findTexture(buffer, DeferredLayerType.NORMAL) as Texture2D,
                            motion.getTexture0() as Texture2D,
                            pw, ph
                        )

                        // todo which depth buffer is this using?
                        // we would need the upscaled depth buffer here, ideally
                        /*clearDepth()
                        val tmp = JomlPools.mat4f.create()
                        tmp.set(cameraMatrix)
                        fsr22.unjitter(cameraMatrix, camRotation, pw, ph)
                        drawGizmos(world, camPosition, true)
                        drawSelected()
                        cameraMatrix.set(tmp)
                        JomlPools.mat4f.sub(1)*/

                        return
                    }
                    renderMode == RenderMode.DEPTH -> {
                        val depth = FBStack["depth", w, h, 1, false, 1, true]
                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, depth,
                            changeSize = true,
                            hdr = false
                        )
                        drawGizmos(depth, true)
                        drawDepthTexture(x, y, w, h, depth.depthTexture!!)
                        return
                    }
                    renderMode == RenderMode.LIGHT_SUM -> {
                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, buffer,
                            changeSize = true,
                            hdr = false
                        )
                        val lightBuffer = if (buffer == base1Buffer) light1Buffer else lightNBuffer
                        drawSceneLights(w, h, camera0, camera1, blending, copyRenderer, buffer, lightBuffer)
                        drawGizmos(lightBuffer, true)
                        drawTexture(x, y + h - 1, w, -h, lightBuffer.getTexture0(), true, -1, null)
                        return
                    }
                    renderMode == RenderMode.LIGHT_COUNT -> {
                        // draw scene for depth
                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, buffer,
                            changeSize = true,
                            hdr = false // doesn't matter
                        )
                        val lightBuffer = if (buffer == base1Buffer) light1Buffer else lightNBuffer
                        pipeline.lightPseudoStage.visualizeLightCount = true
                        drawSceneLights(w, h, camera0, camera1, blending, copyRenderer, buffer, lightBuffer)
                        drawGizmos(lightBuffer, false)
                        // todo special shader to better differentiate the values than black-white
                        // (1 is extremely dark, nearly black)
                        drawTexture(
                            x, y + h - 1, w, -h,
                            lightBuffer.getTexture0(), true,
                            -1, null, true // lights are bright -> dim them down
                        )
                        pipeline.lightPseudoStage.visualizeLightCount = false
                        return
                    }
                    renderMode == RenderMode.SSAO -> {
                        // 0.1f as radius seems pretty ideal with our world scale :)
                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, buffer,
                            changeSize = true,
                            hdr = false
                        )
                        drawGizmos(buffer, true)
                        val strength = max(ssao.strength, 0.01f)
                        val ssao = ScreenSpaceAmbientOcclusion.compute(
                            buffer, deferred, cameraMatrix, ssao.radius, strength, ssao.samples
                        )
                        val tex = ssao ?: buffer.getTexture0()
                        drawTexture(x, y + h - 1, w, -h, tex, true, -1, null)
                        if (ssao == null) {
                            // write warning message
                            val pbb = pushBetterBlending(true)
                            drawSimpleTextCharByChar(
                                x + w / 2,
                                y + h / 2,
                                2,
                                "SSAO not supported",
                                AxisAlignment.CENTER,
                                AxisAlignment.CENTER
                            )
                            popBetterBlending(pbb)
                        }
                        return
                    }
                    renderMode == RenderMode.SS_REFLECTIONS -> {
                        val hdr = useBloom
                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, buffer,
                            changeSize = true,
                            hdr = hdr
                        )
                        drawGizmos(buffer, true)
                        val lightBuffer = if (buffer == base1Buffer) light1Buffer else lightNBuffer
                        drawSceneLights(w, h, camera0, camera1, blending, copyRenderer, buffer, lightBuffer)
                        val illuminated = FBStack["ill", w, h, 4, true, 1, false]
                        useFrame(illuminated, copyRenderer) {
                            // apply lighting; don't write depth
                            GFXState.depthMask.use(false) {
                                val shader = LightPipelineStage.getPostShader(deferred)
                                shader.use()
                                shader.v1b("applyToneMapping", hdr)
                                buffer.bindTextures(2, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                                whiteTexture.bind(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP) // ssao
                                lightBuffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                                flat01.draw(shader)
                            }
                        }
                        val result = ScreenSpaceReflections.compute(
                            buffer, illuminated.getTexture0(),
                            deferred, cameraMatrix, hdr
                        )
                        drawTexture(x, y + h - 1, w, -h, result ?: buffer.getTexture0(), true, -1, null)
                        if (result == null) {
                            val pbb = pushBetterBlending(true)
                            drawSimpleTextCharByChar(
                                x + w / 2, y + h / 2, 2, "SSR not supported", AxisAlignment.CENTER, AxisAlignment.CENTER
                            )
                            popBetterBlending(pbb)
                        }
                        return
                    }
                    renderMode == RenderMode.ALL_DEFERRED_BUFFERS -> {

                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, buffer,
                            changeSize = true,
                            hdr = false
                        )
                        drawGizmos(buffer, true)

                        val lightBuffer = if (buffer == base1Buffer) light1Buffer else lightNBuffer
                        drawSceneLights(w, h, camera0, camera1, blending, copyRenderer, buffer, lightBuffer)

                        val pbb = pushBetterBlending(true)
                        for (index in 0 until size) {

                            // rows x N field
                            val col = index % cols
                            val x02 = x + (x1 - x0) * (col + 0) / cols
                            val x12 = x + (x1 - x0) * (col + 1) / cols
                            val row = index / cols
                            val y02 = y + (y1 - y0) * (row + 0) / rows
                            val y12 = y + (y1 - y0) * (row + 1) / rows

                            // draw the light buffer as the last stripe
                            val texture = if (index < layersSize) {
                                buffer.getTextureI(index)
                            } else lightBuffer.getTexture0()

                            // y flipped, because it would be incorrect otherwise
                            val name = if (texture is Texture2D) texture.name else texture.toString()
                            drawTexture(x02, y12, x12 - x02, y02 - y12, texture, true)
                            val f = 0.8f
                            if (index < layersSize) clip2(
                                if (y12 - y02 > x12 - x02) x02 else mix(x02, x12, f),
                                if (y12 - y02 > x12 - x02) mix(y02, y12, f) else y02,
                                x12,
                                y12
                            ) { // draw alpha on right/bottom side
                                drawTextureAlpha(x02, y12, x12 - x02, y02 - y12, texture)
                            }
                            drawSimpleTextCharByChar(x02, y02, 2, name)

                        }
                        popBetterBlending(pbb)
                        return
                    }
                    renderMode == RenderMode.ALL_DEFERRED_LAYERS -> {

                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, buffer,
                            changeSize = true,
                            hdr = false
                        )
                        drawGizmos(buffer, true)
                        drawSceneLights(w, h, camera0, camera1, blending, copyRenderer, buffer, lightNBuffer)

                        // instead of drawing the raw buffers, draw the actual layers (color,roughness,metallic,...)

                        val tw = w / cols
                        val th = h / rows
                        val tmp = FBStack["tmp-layers", tw, th, 4, false, 1, true]
                        val settings = deferredRenderer.deferredSettings!!
                        val layers = HashMap(settings.layerTypes.associateWith {
                            settings.findTexture(buffer, it)!!
                                .wrapAsFramebuffer()
                        })

                        val pbb = pushBetterBlending(true)
                        for (index in 0 until size) {

                            // rows x N field
                            val col = index % cols
                            val x02 = x + (x1 - x0) * (col + 0) / cols
                            val x12 = x + (x1 - x0) * (col + 1) / cols
                            val row = index / cols
                            val y02 = y + (y1 - y0) * (row + 0) / rows
                            val y12 = y + (y1 - y0) * (row + 1) / rows

                            val name: String
                            val texture: ITexture2D
                            if (index < deferred.layerTypes.size) {
                                // draw the light buffer as the last stripe
                                val layer = deferred.layerTypes[index]
                                name = layer.name
                                /*val layerRenderer = attributeRenderers[layer]!!
                                drawScene(
                                    tw, th, camera0, camera1,
                                    blending, layerRenderer, tmp,
                                    false,
                                    doDrawGizmos = false,
                                    toneMappedColors = false
                                )
                                drawGizmos(world, tmp, renderer, camPosition, true) */
                                // instead of drawing the scene again, use a post-processing shader
                                useFrame(tmp) {
                                    val layerRenderer = Renderers.attributeEffects[layer to settings]!!
                                    layerRenderer.render(buffer, settings, layers)
                                }
                                texture = tmp.getTexture0()
                            } else {
                                texture = lightNBuffer.getTexture0()
                                name = "Light"
                            }

                            // y flipped, because it would be incorrect otherwise
                            drawTexture(x02, y12, tw, -th, texture, true, -1, null)
                            drawSimpleTextCharByChar(
                                (x02 + x12) / 2, (y02 + y12) / 2, 2,
                                name, AxisAlignment.CENTER, AxisAlignment.CENTER
                            )

                        }
                        popBetterBlending(pbb)
                        return
                    }
                    renderer != DeferredRenderer -> {
                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, buffer,
                            changeSize = true,
                            hdr = false
                        )
                        drawGizmos(buffer, true)
                        drawTexture(x, y + h, w, -h, buffer.getTexture0(), true, -1, null)
                        return
                    }
                    else -> {

                        if (buffer != baseNBuffer)
                            throw IllegalStateException("Expected baseBuffer, but got ${buffer.name} for $renderMode")

                        drawScene(
                            w, h, camera0, camera1,
                            blending, renderer, buffer,
                            true,
                            hdr = true
                        )

                        val lightBuffer = if (buffer == base1Buffer) light1Buffer else lightNBuffer
                        drawSceneLights(w, h, camera0, camera1, blending, copyRenderer, buffer, lightBuffer)

                        val ssaoStrength = ssao.strength
                        val ssao = if (ssaoStrength > 0f) ScreenSpaceAmbientOcclusion.compute(
                            buffer, deferred, cameraMatrix, ssao.radius, ssaoStrength, ssao.samples
                        ) ?: blackTexture else blackTexture

                        // use the existing depth buffer for the 3d ui
                        val dstBuffer0 = baseSameDepth
                        if (useBloom) {

                            val illuminated = FBStack["", w, h, 4, true, 1, false]
                            useFrame(illuminated, copyRenderer) { // apply post-processing

                                val shader = LightPipelineStage.getPostShader(deferred)
                                shader.use()
                                shader.v1b("applyToneMapping", false)
                                shader.v3f("ambientLight", pipeline.ambient)

                                buffer.bindTextures(2, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                                ssao.bindTrulyNearest(shader, "ambientOcclusion")
                                lightBuffer.bindTexture0(
                                    shader,
                                    "finalLight",
                                    GPUFiltering.TRULY_NEAREST,
                                    Clamping.CLAMP
                                )

                                flat01.draw(shader)

                            }

                            // screen space reflections
                            val ssReflections = ScreenSpaceReflections.compute(
                                buffer, illuminated.getTexture0(), deferred, cameraMatrix, false
                            ) ?: illuminated.getTexture0()

                            useFrame(w, h, true, dstBuffer0) {

                                // don't write depth
                                GFXState.depthMask.use(false) {
                                    Bloom.bloom(ssReflections, bloomOffset, bloomStrength, true)
                                }

                                // todo use msaa for gizmos
                                // or use anti-aliasing, that works on color edges
                                // and supports lines
                                drawGizmos(true)
                                drawSelected()
                            }

                        } else {

                            useFrame(w, h, true, dstBuffer0) {

                                // don't write depth
                                GFXState.depthMask.use(false) {

                                    val shader = LightPipelineStage.getPostShader(deferred)
                                    shader.use()
                                    shader.v1b("applyToneMapping", true)

                                    buffer.bindTextures(2, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                                    ssao.bindTrulyNearest(shader, "ambientOcclusion")
                                    lightBuffer.bindTexture0(
                                        shader, "finalLight", GPUFiltering.TRULY_NEAREST, Clamping.CLAMP
                                    )

                                    flat01.draw(shader)

                                }

                                drawGizmos(true)
                                drawSelected()

                            }

                        }

                        // anti-aliasing
                        if (buffer.depthTexture != null) {
                            dstBuffer = FBStack["RenderView-dst", w, h, 4, false, 1, false]
                            useFrame(w, h, true, dstBuffer) {
                                DepthBasedAntiAliasing.render(dstBuffer0.getTexture0(), buffer.depthTexture!!)
                            }
                        } else LOGGER.warn("Depth buffer is null! ${buffer::class}")

                    }
                }
                clock.stop("presenting deferred buffers", 0.1)
            }
            useBloom -> {
                // supports bloom
                // screen-space reflections cannot be supported
                val tmp = FBStack["scene", w, h, 4, true, buffer.samples, true]
                drawScene(
                    w, h, camera0, camera1,
                    blending, renderer, tmp,
                    changeSize = true,
                    hdr = true
                )
                drawGizmos(tmp, true)
                useFrame(w, h, true, dstBuffer) {
                    Bloom.bloom(tmp.getTexture0(), bloomOffset, bloomStrength, true)
                }
            }
            else -> {
                val hdr = useFSR || renderMode.effect != null
                isHDR = hdr
                drawScene(
                    w, h, camera0, camera1,
                    blending, renderer, buffer,
                    changeSize = true,
                    hdr
                )
                drawGizmos(buffer, true)
            }
        }

        val effect = renderMode.effect
        when {
            useFSR -> {
                val flipY = isShiftDown // works without difference, so maybe it could be removed...
                val tw = x1 - x0
                val th = y1 - y0
                val tmp = FBStack["fsr", tw, th, 4, false, 1, false]
                useFrame(tmp) { FSR.upscale(dstBuffer.getTexture0(), 0, 0, tw, th, flipY, false) }
                // afterwards sharpen
                FSR.sharpen(tmp.getTexture0(), 0.5f, x, y, tw, th, flipY)
            }
            effect != null -> {
                val map = if (effect is OutlineEffect) {
                    // reset+set old selection IDs; not efficient
                    // just for testing used here
                    getWorld()?.forAll {
                        if (it is MeshComponentBase) it.groupId = 0
                    }
                    for (thing in EditorState.selection) {
                        (thing as? PrefabSaveable)?.forAll {
                            if (it is MeshComponentBase) it.groupId = 1
                        }
                    }
                    val ids = FBStack["ids", w, h, 4, true, buffer.samples, true]
                    drawScene(
                        w, h, camera0, camera1,
                        blending, idRenderer, ids,
                        changeSize = false,
                        hdr = false
                    )
                    hashMapOf(
                        DeferredLayerType.SDR_RESULT to dstBuffer,
                        DeferredLayerType.ID to ids
                    )
                } else hashMapOf(DeferredLayerType.SDR_RESULT to dstBuffer)
                effect.render(dstBuffer, deferred, map)
                GFX.copyNoAlpha(map[DeferredLayerType.SDR_RESULT]!!)
            }
            isHDR -> drawTexture(
                x, y + h, w, -h,
                dstBuffer.getTexture0(),
                ignoreAlpha = true,
                color = -1, tiling = null,
                applyToneMapping = true
            )
            else -> {
                // we could optimize that one day, when the shader graph works
                GFX.copyNoAlpha(dstBuffer)
            }
        }

    }

    fun resolveClick(px: Float, py: Float, drawDebug: Boolean = false): Pair<Entity?, Component?> {

        // pipeline should already be filled
        val camera = editorCamera
        val ws = windowStack
        val buffer = FBStack["click", ws.width, ws.height, 4, true, 1, true]

        val diameter = 5

        val px2 = px.toInt() - x
        val py2 = py.toInt() - y

        val world = getWorld()

        val ids = Screenshots.getU8RGBAPixels(diameter, px2, py2, buffer, idRenderer) {
            drawScene(
                w, h, camera, camera, 0f, idRenderer, buffer,
                changeSize = false, hdr = false
            )
            drawGizmos(drawGridLines = false, drawDebug)
        }

        for (idx in ids.indices) {
            ids[idx] = ids[idx] and 0xffffff
        }

        val depths = Screenshots.getFP32RPixels(diameter, px2, py2, buffer, depthRenderer) {
            drawScene(
                w, h, camera, camera, 0f, depthRenderer, buffer,
                changeSize = false, hdr = false
            )
            drawGizmos(drawGridLines = false, drawDebug)
        }

        val clickedId = Screenshots.getClosestId(diameter, ids, depths, if (reverseDepth) -10 else +10)
        val clicked = if (clickedId == 0 || world !is Entity) null
        else pipeline.findDrawnSubject(clickedId, world)
        // LOGGER.info("${ids.joinToString()} x ${depths.joinToString()} -> $clickedId -> $clicked")
        // val ids2 = world.getComponentsInChildren(MeshComponent::class, false).map { it.clickId }
        // LOGGER.info(ids2.joinToString())
        // LOGGER.info(clickedId in ids2)
        return Pair(clicked as? Entity, clicked as? Component)

    }

    fun getWorld(): PrefabSaveable? {
        return try {
            library.prefab?.getSampleInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val tmp4f = Vector4f()

    private val tmpRot0 = Quaternionf()
    private val tmpRot1 = Quaternionf()
    fun prepareDrawScene(
        width: Int,
        height: Int,
        aspectRatio: Float,
        camera: Camera,
        previousCamera: Camera,
        blending: Float,
        update: Boolean
    ) {

        val world = getWorld()
        if (update && world is Entity) {
            world.invalidateVisibility()
        }

        // must be called before we define our render settings
        // so lights don't override our settings, or we'd have to repeat our definition
        if (update) {
            when (world) {
                is Entity -> {
                    world.update()
                    world.updateVisible()
                    world.validateTransform()
                }
                is Component -> {
                    world.onUpdate()
                    world.onVisibleUpdate()
                }
            }
        }

        val blend = clamp(blending, 0f, 1f).toDouble()
        val blendF = blend.toFloat()

        val near = mix(previousCamera.near, camera.near, blend)
        val far = mix(previousCamera.far, camera.far, blend)
        val isPerspective = camera.isPerspective
        val fov = if (isPerspective) {
            mix(previousCamera.fovY, camera.fovY, blending)
        } else {
            mix(previousCamera.fovOrthographic, camera.fovOrthographic, blending)
        }
        val t0 = previousCamera.entity!!.transform.globalTransform
        val t1 = camera.entity!!.transform.globalTransform
        val rot0 = t0.getUnnormalizedRotation(tmpRot0)
        val rot1 = t1.getUnnormalizedRotation(tmpRot1)

        bloomOffset = mix(previousCamera.bloomOffset, camera.bloomOffset, blendF)
        bloomStrength = mix(previousCamera.bloomStrength, camera.bloomStrength, blendF)

        if (!rot0.isFinite) rot0.identity()
        if (!rot1.isFinite) rot1.identity()

        val rotInv = rot0.slerp(rot1, blendF)
        val rot = rot1.set(rotInv).conjugate() // conjugate is quickly inverting, when already normalized

        val centerX = mix(previousCamera.center.x, camera.center.x, blendF)
        val centerY = mix(previousCamera.center.x, camera.center.y, blendF)

        // this needs to be separate from the stack
        // (for normal calculations and such)
        var scaledNear = (near * worldScale)
        var scaledFar = (far * worldScale)
        this.scaledNear = scaledNear
        this.scaledFar = scaledFar
        this.isPerspective = isPerspective
        if (isPerspective) {
            val fovYRadians = fov.toRadians()
            this.fovYRadians = fovYRadians
            Perspective.setPerspective(
                cameraMatrix, fovYRadians, aspectRatio, scaledNear.toFloat(), scaledFar.toFloat(), centerX, centerY
            )
        } else {
            scaledNear = max(scaledNear, worldScale * 0.001)
            scaledFar = min(scaledFar, worldScale * 1000.0)
            fovYRadians = fov // not really defined
            val sceneScaleXY = 2f / fov // 2, because OpenGL goes from -1 to +1
            val n: Float
            val f: Float
            // todo some devices may not support 01-range, so make this optional
            val range01 = reverseDepth
            if (reverseDepth) {
                // range is 0 .. 1
                n = scaledNear.toFloat()
                f = scaledFar.toFloat()
            } else {
                // range is -1 .. 1 instead of 0 .. 1
                n = scaledFar.toFloat()
                f = scaledNear.toFloat()
            }
            val sceneScaleZ = 1f / (f - n)
            val reverseDepth = GFXState.depthMode.currentValue.reversedDepth
            var m22 = if (reverseDepth) +sceneScaleZ else -sceneScaleZ
            var z0 = 1f - n * sceneScaleZ
            if (!range01) {
                m22 *= 2f
                z0 = z0 * 2f - 1f
            }
            // todo respect near
            cameraMatrix.set(
                height * sceneScaleXY / width, 0f, 0f, 0f,
                0f, sceneScaleXY, 0f, 0f,
                0f, 0f, m22, 0f,
                0f, 0f, z0, 1f
            )
        }

        if (renderMode == RenderMode.FSR2_V2) {
            fsr22.jitter(cameraMatrix, width, height)
        }

        if (!cameraMatrix.isFinite) {
            cameraMatrix.identity()
            LOGGER.warn("Set matrix to identity, because it was non-finite! $cameraMatrix")
        }

        cameraMatrix.rotate(rot)

        // lerp the world transforms
        val camTransform = camTransform
        camTransform.set(previousCamera.entity!!.transform.globalTransform)
        camTransform.lerp(camera.entity!!.transform.globalTransform, blend)

        camTransform.transformPosition(cameraPosition.set(0.0))
        camInverse.set(camTransform).invert()
        cameraRotation.set(rotInv)

        cameraRotation.transform(cameraDirection.set(0.0, 0.0, -1.0))
        cameraDirection.normalize()

        // camera matrix and mouse position to ray direction
        if (update) {
            val window = window!!
            getMouseRayDirection(window.mouseX, window.mouseY, mouseDirection)
        }

        // debugPoints.add(DebugPoint(Vector3d(camDirection).mul(20.0).add(camPosition), 0xff0000, -1))
        prevCamMatrix.set(lastCamMat)
        prevCamPosition.set(lastCamPos)
        prevCamRotation.set(lastCamRot)
        prevWorldScale = lastWorldScale

        currentInstance = this

        if (world is Material && world.prefab?.source?.exists != true) {
            throw IllegalStateException("Material must have source")
        }

        pipeline.clear()
        if (isPerspective) {
            pipeline.frustum.definePerspective(
                near, far, fovYRadians.toDouble(),
                width, height, aspectRatio.toDouble(),
                cameraPosition, cameraRotation,
            )
        } else {
            pipeline.frustum.defineOrthographic(
                fov.toDouble(), aspectRatio.toDouble(), near, far, width, cameraPosition, cameraRotation
            )
            // pipeline.frustum.showPlanes()
        }
        pipeline.disableReflectionCullingPlane()
        pipeline.ignoredEntity = null
        pipeline.resetClickId()
        if (world != null) pipeline.fill(world, cameraPosition, worldScale)
        controlScheme?.fill(pipeline)
        // if the scene would be dark, define lights, so we can see something
        if (pipeline.lightPseudoStage.size <= 0 && pipeline.ambient.dot(1f, 1f, 1f) <= 0f) {
            pipeline.ambient.set(0.5f)
            defaultSun.fill(pipeline, defaultSunEntity, 0, cameraPosition, worldScale)
        }
        entityBaseClickId = pipeline.lastClickId

    }

    private val reverseDepth get() = renderMode != RenderMode.INVERSE_DEPTH

    private fun setClearDepth() {
        val stages = pipeline.stages
        for (index in stages.indices) {
            stages[index].depthMode = depthMode
        }
        stage0.depthMode = depthMode
        /**
         * the depth mode is a little convoluted:
         * - we need to draw the backsides (far) of lights to ensure that we can see them, even if we are inside them
         * - we want to use the depth test for optimization
         * - if we choose the normal mode, we wouldn't see it ->
         * - use the inverse mode, and discard lights that are too close instead of those that are too far
         * */
        pipeline.lightPseudoStage.depthMode = invDepthMode
    }

    private val depthMode get() = if (reverseDepth) DepthMode.CLOSER else DepthMode.FORWARD_CLOSER
    private val invDepthMode get() = if (reverseDepth) DepthMode.FARTHER else DepthMode.FORWARD_FARTHER

    var skipClear = false

    private fun clearColor(
        previousCamera: Camera, camera: Camera,
        blending: Float, hdr: Boolean
    ) {
        // this can be skipped, if we have a sky
        if (skipClear) return
        Frame.bind()
        GFXState.blendMode.use(null) {
            GFXState.depthMode.use(DepthMode.ALWAYS) {
                // don't write depth, only all buffers
                GFXState.depthMask.use(false) {
                    // draw a huge cube with default values for all buffers
                    val shader = clearPbrModelShader.value
                    shader.use()
                    if (isPerspective) {
                        shader.m4x4("transform", cameraMatrix)
                        shader.m4x4("prevTransform", prevCamMatrix)
                    } else {
                        // this buffer can be very small in orthographic case, because the camera matrix contains scale, which it shouldn't;
                        // aspect ratio shouldn't matter, because it's orthographic = the same direction and position at infinity anyway
                        val tmpQ = JomlPools.quat4f.borrow()
                        val tmp1 = JomlPools.mat4f.borrow()
                        tmp1
                            .identity()
                            .scale(2f, 2f, 0.3f)
                            .rotate(tmpQ.set(cameraRotation))
                        shader.m4x4("transform", tmp1)
                        tmp1
                            .identity()
                            .scale(2f, 2f, 0.3f)
                            .rotate(tmpQ.set(prevCamRotation))
                        shader.m4x4("prevTransform", tmp1)
                    }
                    val c = tmp4f
                    c.set(previousCamera.clearColor).lerp(camera.clearColor, blending)
                    // inverse tonemapping
                    if (hdr) tonemapInvKt(c)
                    shader.v4f("color", c.x, c.y, c.z, 1f)
                    shaderColor(shader, "tint", -1)
                    Shapes.smoothCube.back.drawMeshPurely(shader)
                }
            }
        }
    }

    fun drawScene(
        w: Int, h: Int,
        currentCamera: Camera,
        previousCamera: Camera,
        blending: Float,
        renderer: Renderer,
        dst: IFramebuffer,
        changeSize: Boolean,
        hdr: Boolean
    ) {

        GFX.check()

        pipeline.applyToneMapping = !hdr

        val preDrawDepth = renderMode == RenderMode.WITH_PRE_DRAW_DEPTH
        if (preDrawDepth) {
            useFrame(w, h, changeSize, dst, cheapRenderer) {

                Frame.bind()

                GFXState.depthMode.use(depthMode) {
                    setClearDepth()
                    dst.clearDepth()
                }

                GFXState.depthMode.use(depthMode) {
                    GFXState.cullMode.use(CullMode.BACK) {
                        GFXState.blendMode.use(null) {
                            stage0.drawColors(pipeline)
                        }
                    }
                }
                stage0.depthMode = DepthMode.EQUALS

            }
        }

        useFrame(w, h, changeSize, dst, renderer) {

            if (!preDrawDepth) {
                Frame.bind()
                GFXState.depthMode.use(depthMode) {
                    setClearDepth()
                    dst.clearDepth()
                }
            }

            clearColor(previousCamera, currentCamera, blending, hdr)
            GFX.check()
            pipeline.draw()
            GFX.check()

        }

    }

    private fun drawSelected() {
        if (library.selection.isEmpty()) return
        // draw scaled, inverted object (for outline), which is selected
        GFXState.depthMode.use(depthMode) {
            for (selected in library.selection) {
                when (selected) {
                    is Entity -> drawOutline(selected, worldScale)
                    is MeshComponentBase -> {
                        val mesh = selected.getMesh() ?: continue
                        drawOutline(selected, mesh, worldScale)
                    }
                    is Component -> drawOutline(selected.entity ?: continue, worldScale)
                }
            }
        }
    }

    private fun drawSceneLights(
        w: Int, h: Int,
        camera: Camera,
        previousCamera: Camera,
        blending: Float,
        renderer: Renderer,
        deferred: IFramebuffer,
        dst: IFramebuffer
    ) {
        useFrame(w, h, true, dst, renderer) {
            Frame.bind()
            tmp4f.set(previousCamera.clearColor).lerp(camera.clearColor, blending)
            dst.clearColor(0)
            pipeline.lightPseudoStage.bindDraw(deferred, cameraMatrix, cameraPosition, worldScale)
        }
    }

    private fun drawGizmos(
        framebuffer: IFramebuffer,
        drawGridLines: Boolean,
        drawDebug: Boolean = true
    ) {
        useFrame(framebuffer, simpleNormalRenderer) {
            drawGizmos(drawGridLines, drawDebug)
        }
        (0L).takeLowestOneBit()
    }

    fun drawGizmos(drawGridLines: Boolean, drawDebug: Boolean = true) {

        if (playMode != PlayMode.EDITING) {
            DebugShapes.clear()
            return
        }

        val world = getWorld()
        // draw UI

        // now works, after making the physics async :)
        // maybe it just doesn't work with the physics debugging together
        val drawAABBs = renderMode == RenderMode.SHOW_AABB

        GFXState.blendMode.use(BlendMode.DEFAULT) {
            GFXState.depthMode.use(depthMode) {

                stack.set(cameraMatrix)

                controlScheme?.drawGizmos()

                //val maximumCircleDistance = 200f
                //val maxCircleLenSq = sq(maximumCircleDistance).toDouble()

                var clickId = entityBaseClickId
                //val scaleV = JomlPools.vec3d.create()

                // much faster than depthTraversal, because we only need visible elements anyways
                if (world != null) pipeline.traverse(world) { entity ->

                    val transform = entity.transform
                    val globalTransform = transform.globalTransform

                    /*val doDrawCircle = camPosition.distanceSquared(
                        globalTransform.m30,
                        globalTransform.m31,
                        globalTransform.m32
                    ) < maxCircleLenSq*/

                    val nextClickId = clickId++
                    GFX.drawnId = nextClickId
                    entity.clickId = nextClickId

                    val stack = stack
                    stack.pushMatrix()
                    stack.mul4x3delta(globalTransform, cameraPosition, worldScale)

                    // only draw the circle, if its size is larger than ~ a single pixel
                    /*if (doDrawCircle) {
                        scale = globalTransform.getScale(scaleV).dot(0.3, 0.3, 0.3)
                        val ringColor = if (entity == EditorState.lastSelection) selectedColor else white4
                        PlaneShapes.drawCircle(globalTransform, ringColor.toARGB())
                        drawUICircle(stack, 0.5f / scale.toFloat(), 0.7f, ringColor)
                    }*/

                    val components = entity.components
                    for (i in components.indices) {
                        val component = components[i]
                        if (component.isEnabled && component !is MeshComponentBase) {
                            // mesh components already got their ID
                            val componentClickId = clickId++
                            component.clickId = componentClickId
                            GFX.drawnId = componentClickId
                            component.onDrawGUI(component.isSelectedIndirectly)
                        }
                    }

                    stack.popMatrix()

                    if (drawAABBs) {
                        val aabb = entity.aabb
                        val hit = aabb.testLine(cameraPosition, mouseDirection, 1e10)
                        drawAABB(aabb, worldScale, if (hit) aabbColorHovered else aabbColorDefault)
                    }

                    LineBuffer.drawIf1M(cameraMatrix)

                }

                // JomlPools.vec3d.sub(1)

                if (drawGridLines) {
                    drawGrid(radius)
                }

                if (drawDebug) {
                    DebugRendering.drawDebug(this)
                } else {
                    DebugShapes.clear()
                }

                LineBuffer.finish(cameraMatrix)
                PlaneShapes.finish()

            }
        }

    }

    /**
     * get the mouse direction from this camera
     * todo for other cameras: can be used for virtual mice
     * */
    fun getMouseRayDirection(
        cx: Float = window!!.mouseX, cy: Float = window!!.mouseY, dst: Vector3d = Vector3d()
    ): Vector3d {
        val rx = (cx - x) / w * +2.0 - 1.0
        val ry = (cy - y) / h * -2.0 + 1.0
        val tanHalfFoV = tan(fovYRadians * 0.5)
        val aspectRatio = w.toFloat() / h
        val dir = dst.set(rx * tanHalfFoV * aspectRatio, ry * tanHalfFoV, -1.0)
        cameraRotation.transform(dir)
        dir.normalize()
        return dst
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        getMouseRayDirection(x, y, JomlPools.vec3d.create())
        super.onMouseMoved(x, y, dx, dy)
    }

    var worldScale = 1.0

    var fovYRadians = 1f

    var near = 1e-10
    var scaledNear = 1e-10

    // infinity
    var far = 1e10
    var scaledFar = 1e10
    var isPerspective = true

    val cameraMatrix = Matrix4f()

    val camTransform = Matrix4x3d()
    val camInverse = Matrix4d()
    val cameraPosition = Vector3d()
    val cameraDirection = Vector3d()
    val cameraRotation = Quaterniond()
    val mouseDirection = Vector3d()

    val prevCamRotation = Quaterniond()
    val prevCamMatrix = Matrix4f()
    val prevCamPosition = Vector3d()
    var prevWorldScale = worldScale

    private val lastCamPos = Vector3d()
    private val lastCamRot = Quaterniond()
    private val lastCamMat = Matrix4f()
    private var lastWorldScale = worldScale

    fun updatePrevState() {
        lastCamPos.set(cameraPosition)
        lastCamRot.set(cameraRotation)
        lastCamMat.set(cameraMatrix)
        lastWorldScale = worldScale
    }

    fun setRenderState() {

        RenderState.worldScale = worldScale
        RenderState.prevWorldScale = prevWorldScale

        RenderState.cameraPosition.set(cameraPosition)
        RenderState.cameraRotation.set(cameraRotation)
        RenderState.calculateDirections()
        RenderState.cameraDirection.set(cameraDirection)
        RenderState.cameraMatrix.set(cameraMatrix)

        RenderState.prevCamMatrix.set(prevCamMatrix)
        RenderState.prevCameraPosition.set(prevCamPosition)

        RenderState.isPerspective = isPerspective
        RenderState.fovYRadians = fovYRadians

    }

    companion object {

        private val LOGGER = LogManager.getLogger(RenderView::class)

        /**
         * maximum number of lights used for forward rendering
         * when this number is surpassed, the engine switches to deferred rendering automatically
         * todo forward plus rendering?
         * */
        val MAX_FORWARD_LIGHTS = 32

        val stack = Matrix4fArrayList()

        val scaledMin = Vector4d()
        val scaledMax = Vector4d()
        val tmpVec4d = Vector4d()

        var currentInstance: RenderView? = null

        val aabbColorDefault = -1
        val aabbColorHovered = 0xffaaaa or black

    }

}