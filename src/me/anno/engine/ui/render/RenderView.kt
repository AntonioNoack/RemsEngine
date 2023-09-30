package me.anno.engine.ui.render

// this list of imports is insane XD
import me.anno.Time
import me.anno.config.DefaultConfig
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.shaders.SkyboxBase
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
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.MovingGrid.drawGrid
import me.anno.engine.ui.render.Outlines.drawOutline
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.Renderers.cheapRenderer
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.engine.ui.render.Renderers.rawAttributeRenderers
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.clip2
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.M4x3Delta.mul4x3delta
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.popBetterBlending
import me.anno.gpu.drawing.DrawTexts.pushBetterBlending
import me.anno.gpu.drawing.DrawTextures.drawDepthTexture
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.DrawTextures.drawTextureAlpha
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.*
import me.anno.gpu.pipeline.LightShaders.combineLighting
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Renderer.Companion.depthRenderer
import me.anno.gpu.shader.Renderer.Companion.idRenderer
import me.anno.gpu.shader.effects.FXAA
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.graph.render.RenderGraph
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Clock
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.*
import kotlin.math.*

// to do render the grid slightly off position, so we don't get flickering, always closer to the camera, proportional to radius
// (because meshes at 0 are very common and to be expected)

// done shadows

// done render in different modes: overdraw, color blindness, normals, color, before-post-process, with-post-process
// done nice ui for that: drop down menus at top or bottom

// to do blend between multiple cameras, only allow 2? yes :)

// todo easily allow for multiple players in the same instance, with just player key mapping
// -> camera cannot be global, or todo it must be switched whenever the player changes

// todo we could do the blending of the scenes using stencil tests <3 (very efficient)
//  - however it would limit us to a single renderer...
// -> first just draw a single scene and later todo make it multiplayer

// todo make shaders of materials be references via a file (StaticRef)? this will allow for visual shaders in the future

// todo define the render pipeline from the editor? maybe from the code in an elegant way? top level element?

/**
 * a panel that renders the scene;
 * no controls are provided by this class, it just draws
 * */
open class RenderView(val library: EditorState, var playMode: PlayMode, style: Style) : Panel(style) {

    private var bloomStrength = 0f // defined by the camera
    private var bloomOffset = 0f // defined by the camera
    private val useBloom get() = bloomStrength > 0f

    var controlScheme: ControlScheme? = null

    var enableOrbiting = true

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
    val rotation = Quaterniond()
        .rotateX(20.0.toRadians())

    private val deferred = DeferredRenderer.deferredSettings!!

    val baseNBuffer1 = deferred.createBaseBuffer()
    private val baseSameDepth1 = baseNBuffer1.attachFramebufferToDepth("baseSD1", 1, false)
    val base1Buffer = Framebuffer("base1", 1, 1, 1, 1, false, DepthBufferType.TEXTURE)
    val base8Buffer = Framebuffer("base8", 1, 1, 8, 1, false, DepthBufferType.TEXTURE)

    private val light1Buffer = base1Buffer.attachFramebufferToDepth("light1", arrayOf(TargetType.FP16Target4))
    private val lightNBuffer1 = baseNBuffer1.attachFramebufferToDepth("lightN1", arrayOf(TargetType.FP16Target4))

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

    override val canDrawOverBorders get() = true

    override fun destroy() {
        super.destroy()
        // all framebuffers that we own need to be freed
        light1Buffer.destroy()
        lightNBuffer1.destroy()
        baseSameDepth1.destroy()
        base1Buffer.destroy()
        base8Buffer.destroy()
        baseNBuffer1.destroy()
        editorCameraNode.destroy()
        fsr22.destroy()
        pipeline.destroy()
    }

    open fun updateEditorCameraTransform() {

        val radius = radius
        val camera = editorCamera
        val cameraNode = editorCameraNode

        if (!position.isFinite) LOGGER.warn("Invalid position $position")
        if (!rotation.isFinite) LOGGER.warn("Invalid rotation $rotation")

        camera.far = far
        camera.near = near

        val tmp3d = JomlPools.vec3d.borrow()
        cameraNode.transform.localPosition =
            if (enableOrbiting) rotation.transform(tmp3d.set(0.0, 0.0, radius)).add(position)
            else position
        cameraNode.transform.localRotation = rotation
        cameraNode.transform.teleportUpdate()
        cameraNode.validateTransform()
    }

    override fun onUpdate() {
        super.onUpdate()
        // we could optimize that: if not has updated in some time, don't redraw
        invalidateDrawing()
        updateWorldScale()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        val drawnPrimitives0 = PipelineStage.drawnPrimitives
        val drawCalls0 = PipelineStage.drawCalls

        currentInstance = this

        clock.start()

        if (!GFX.supportsDepthTextures) {
            when (renderMode) {
                // todo most ways need a way to work without depth textures...
                // todo -> or we could render the depth to a FP16/32 target XD
                RenderMode.DEFAULT -> {
                    renderMode = RenderMode.NON_DEFERRED
                }
            }
        }

        // to see ghosting, if there is any
        val renderMode = renderMode
        if (renderMode == RenderMode.GHOSTING_DEBUG) Thread.sleep(250)

        updateEditorCameraTransform()

        val world = getWorld()

        setRenderState()

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

        var aspect = width.toFloat() / height

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

        clock.stop("initialization", 0.05)

        prepareDrawScene(width, height, aspect, camera0, camera1, blending, true)

        setRenderState()

        clock.stop("preparing", 0.05)

        val renderGraph = renderMode.renderGraph
        if (renderGraph != null) {
            // some things are just too easy ^^
            RenderGraph.draw(this, this, renderGraph)
        } else {

            stage0.blendMode = if (renderMode == RenderMode.OVERDRAW) BlendMode.ADD else null
            stage0.sorting = Sorting.FRONT_TO_BACK
            stage0.cullMode = if (renderMode != RenderMode.FRONT_BACK) CullMode.BACK else CullMode.BOTH

            var useDeferredRendering = when (renderMode) {
                RenderMode.CLICK_IDS, RenderMode.DEPTH, RenderMode.NO_DEPTH, RenderMode.FSR_MSAA_X4,
                RenderMode.GHOSTING_DEBUG, RenderMode.INVERSE_DEPTH,
                RenderMode.UV, RenderMode.MSAA_NON_DEFERRED -> false
                else -> true
            }

            val renderer = when (renderMode) {
                RenderMode.NON_DEFERRED, RenderMode.MSAA_NON_DEFERRED,
                RenderMode.FSR_MSAA_X4 -> {
                    useDeferredRendering = false
                    pbrRenderer
                }
                else -> renderMode.renderer ?: (if (useDeferredRendering) DeferredRenderer else pbrRenderer)
            }

            // multi-sampled buffer
            val buffer = when {
                // msaa, single target
                renderMode == RenderMode.MSAA_NON_DEFERRED ||
                        renderMode == RenderMode.LINES_MSAA ||
                        renderMode == RenderMode.FSR_MSAA_X4 -> base8Buffer
                // aliased, multi-target
                renderer == DeferredRenderer -> baseNBuffer1
                else -> base1Buffer
            }

            // blacklist for renderModes?
            if (renderer !in attributeRenderers.values &&
                renderMode != RenderMode.LIGHT_COUNT
            ) {
                pipeline.bakeSkybox(256)
            } else {
                pipeline.destroyBakedSkybox()
            }

            drawScene(
                x0, y0, x1, y1,
                renderer, buffer, useDeferredRendering,
                size, cols, rows, layers.size
            )
        }

        val pbb = pushBetterBlending(true)
        if (world == null) {
            drawSimpleTextCharByChar(
                x + width / 2, y + height / 2, 4,
                if (library.prefabSource == InvalidRef)
                    "Undefined Scene!" else "Scene Not Found!",
                AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        }

        popBetterBlending(pbb)

        if (!isFinalRendering) {
            DebugRendering.showShadowMapDebug(this)
            DebugRendering.showCameraRendering(this, x0, y0, x1, y1)
        }

        if (world is Entity && playMode != PlayMode.EDITING) {
            world.firstComponentInChildren(CanvasComponent::class, false) { comp ->
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
            val drawnPrimitives = PipelineStage.drawnPrimitives - drawnPrimitives0
            val drawCalls = PipelineStage.drawCalls - drawCalls0
            val usesBetterBlending = DrawTexts.canUseComputeShader()
            drawSimpleTextCharByChar(
                x + 2,
                y + height - 1 - DrawTexts.monospaceFont.sizeInt,
                2, if (drawCalls == 1L) "$drawnPrimitives tris, 1 draw call"
                else "$drawnPrimitives tris, $drawCalls draw calls",
                FrameTimings.textColor,
                FrameTimings.backgroundColor.withAlpha(if (usesBetterBlending) 0 else 255)
            )
            popBetterBlending(pbb)
        }

        updatePrevState()
        // clock.total("drawing the scene", 0.1)
    }

    // be more conservative with framebuffer size changes,
    // because they are expensive -> only change every 20th frame
    private val mayChangeSize get() = (Time.frameIndex % 20 == 0)

    private val fsr22 by lazy { FSR2v2() }

    fun drawScene(
        x0: Int, y0: Int, x1: Int, y1: Int,
        renderer: Renderer, buffer: IFramebuffer, useDeferredRendering: Boolean,
        size: Int, cols: Int, rows: Int,
        layersSize: Int
    ) {

        var w = x1 - x0
        var h = y1 - y0

        when (renderMode) {
            RenderMode.FSR2_X2 -> {
                w = (w + 1) / 2
                h = (h + 1) / 2
            }
            RenderMode.FSR2_X8 -> {
                w = (w + 1) / 8
                h = (h + 1) / 8
            }
            else -> {
            }
        }

        w = max(w, 1)
        h = max(h, 1)

        val s0 = w * h
        val s1 = buffer.width * buffer.height
        val mayChangeSize = mayChangeSize || (w * h < 1024) || min(s0, s1) * 2 <= max(s0, s1)
        if (!mayChangeSize) {
            w = buffer.width
            h = buffer.height
        }

        var dstBuffer = buffer

        val useFSR = when (renderMode) {
            RenderMode.FSR_MSAA_X4 -> true
            else -> false
        }

        when {
            useDeferredRendering -> {
                when {
                    renderMode == RenderMode.FSR2_X8 || renderMode == RenderMode.FSR2_X2 -> {
                        drawScene(
                            w, h, renderer, buffer,
                            changeSize = true, hdr = true
                        )
                        val motion = FBStack["motion", w, h, 4, BufferQuality.HIGH_16, 1, true]
                        val motion1 = rawAttributeRenderers[DeferredLayerType.MOTION]
                        drawScene(w, h, motion1, motion, changeSize = false, hdr = true)

                        val lightBuffer = lightNBuffer1
                        drawSceneLights(buffer, lightBuffer)

                        val ssao = blackTexture
                        val pw = x1 - x0
                        val ph = y1 - y0

                        // use the existing depth buffer for the 3d ui
                        val dstBuffer0 = baseSameDepth1
                        useFrame(w, h, true, dstBuffer0) {
                            // don't write depth
                            GFXState.depthMask.use(false) {
                                combineLighting(
                                    deferred, true, pipeline.ambient,
                                    buffer, lightBuffer, ssao
                                )
                            }
                        }

                        val tmp1 = FBStack["fsr", pw, ph, 4, false, 1, true]
                        useFrame(tmp1) {
                            GFXState.depthMode.use(DepthMode.CLOSE) {
                                tmp1.clearDepth()
                                GFXState.blendMode.use(null) {
                                    fsr22.calculate(
                                        dstBuffer0.getTexture0() as Texture2D,
                                        buffer.depthTexture as Texture2D,
                                        deferred.findTexture(buffer, DeferredLayerType.NORMAL) as Texture2D,
                                        motion.getTexture0() as Texture2D,
                                        pw, ph
                                    )
                                }
                                // todo why is it still jittering?
                                val tmp = JomlPools.mat4f.create()
                                tmp.set(cameraMatrix)
                                fsr22.unjitter(cameraMatrix, cameraRotation, pw, ph)
                                // setRenderState()
                                drawGizmos(GFXState.currentBuffer, true, drawDebug = true)
                                drawSelected()
                                cameraMatrix.set(tmp)
                                // setRenderState()
                                JomlPools.mat4f.sub(1)
                            }
                        }

                        drawTexture(x0, y0 + ph, pw, -ph, tmp1.getTexture0(), true)
                        return
                    }
                    renderMode == RenderMode.DEPTH -> {
                        val depth = FBStack["depth", w, h, 1, false, 1, true]
                        drawScene(
                            w, h, renderer, depth,
                            changeSize = true, hdr = false
                        )
                        drawGizmos(depth, true)
                        drawDepthTexture(x, y, w, h, depth.depthTexture!!)
                        return
                    }
                    renderMode == RenderMode.LIGHT_COUNT -> {
                        // draw scene for depth
                        drawScene(
                            w, h,
                            renderer, buffer,
                            changeSize = true,
                            hdr = false // doesn't matter
                        )
                        val lightBuffer = if (buffer == base1Buffer) light1Buffer else lightNBuffer1
                        pipeline.lightStage.visualizeLightCount = true
                        drawSceneLights(buffer, lightBuffer)
                        drawGizmos(lightBuffer, true)
                        // todo special shader to better differentiate the values than black-white
                        // (1 is extremely dark, nearly black)
                        drawTexture(
                            x, y + h - 1, w, -h,
                            lightBuffer.getTexture0(), true,
                            -1, null, true // lights are bright -> dim them down
                        )
                        pipeline.lightStage.visualizeLightCount = false
                        return
                    }
                    renderMode == RenderMode.ALL_DEFERRED_BUFFERS -> {

                        drawScene(w, h, renderer, buffer, changeSize = true, hdr = false)
                        drawGizmos(buffer, true)

                        val lightBuffer = if (buffer == base1Buffer) light1Buffer else lightNBuffer1
                        drawSceneLights(buffer, lightBuffer)

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

                        drawScene(w, h, renderer, buffer, changeSize = true, hdr = false)
                        drawGizmos(buffer, true)
                        drawSceneLights(buffer, lightNBuffer1)

                        // instead of drawing the raw buffers, draw the actual layers (color,roughness,metallic,...)

                        val tw = w / cols
                        val th = h / rows
                        val tmp = FBStack["tmp-layers", tw, th, 4, false, 1, true]
                        val settings = DeferredRenderer.deferredSettings!!

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
                            var applyTonemapping = false
                            if (index < deferred.layerTypes.size) {
                                // draw the light buffer as the last stripe
                                val layer = deferred.layerTypes[index]
                                name = layer.name
                                useFrame(tmp) {
                                    val shader = Renderers.attributeEffects[layer to settings]!!
                                    shader.use()
                                    settings.findTexture(buffer, layer)!!.bindTrulyNearest(0)
                                    SimpleBuffer.flat01.draw(shader)
                                }
                                texture = tmp.getTexture0()
                            } else {
                                texture = lightNBuffer1.getTexture0()
                                applyTonemapping = true // not really doing much visually...
                                name = "Light"
                            }

                            // y flipped, because it would be incorrect otherwise
                            drawTexture(
                                x02, y12, tw, -th, texture, true, white,
                                null, applyTonemapping
                            )
                            drawSimpleTextCharByChar(
                                (x02 + x12) / 2, (y02 + y12) / 2, 2,
                                name, AxisAlignment.CENTER, AxisAlignment.CENTER
                            )
                        }
                        popBetterBlending(pbb)
                        return
                    }
                    renderer == DeferredRenderer -> {
                        dstBuffer = drawSceneDeferred(
                            buffer, w, h, baseNBuffer1, lightNBuffer1, baseSameDepth1,
                            renderer, deferred
                        )
                    }
                    else -> {
                        drawScene(
                            w, h, renderer, buffer,
                            changeSize = true, hdr = false
                        )
                        drawGizmos(buffer, true)
                        drawTexture(x, y + h, w, -h, buffer.getTexture0(), true, -1, null)
                        return
                    }
                }
                clock.stop("presenting deferred buffers", 0.1)
            }

            useBloom -> {
                // supports bloom
                // todo support SSR via calculated normals
                val tmp = FBStack["scene", w, h, 4, true, buffer.samples, true]
                drawScene(w, h, renderer, tmp, changeSize = true, hdr = true)
                drawGizmos(tmp, true)
                useFrame(w, h, true, dstBuffer) {
                    Bloom.bloom(tmp.getTexture0(), bloomOffset, bloomStrength, true)
                }
            }

            else -> {
                drawScene(w, h, renderer, buffer, changeSize = true, useFSR)
                drawGizmos(buffer, true)
            }
        }

        when {
            useFSR -> {
                val tw = x1 - x0
                val th = y1 - y0
                val tmp = FBStack["fsr", tw, th, 4, false, 1, false]
                useFrame(tmp) {
                    FSR.upscale(
                        dstBuffer.getTexture0(), 0, 0, tw, th,
                        flipY = false, applyToneMapping = false,
                        withAlpha = false
                    )
                }
                // afterwards sharpen
                FSR.sharpen(tmp.getTexture0(), 0.5f, x, y, tw, th, false)
            }
            else -> {
                // we could optimize that one day, when the shader graph works
                GFX.copyNoAlpha(dstBuffer)
            }
        }
    }

    fun drawSceneDeferred(
        buffer: IFramebuffer, w: Int, h: Int,
        baseBuffer: IFramebuffer, lightBuffer: IFramebuffer, baseSameDepth: IFramebuffer,
        renderer: Renderer, deferredSettings: DeferredSettingsV2
    ): IFramebuffer {

        if (buffer != baseBuffer)
            throw IllegalStateException("Expected baseBuffer, but got ${buffer.name} for $renderMode")

        pipeline.deferred = deferredSettings

        drawScene(w, h, renderer, buffer, changeSize = true, hdr = true)
        drawSceneLights(buffer, lightBuffer)

        val sett = SSAOSettings
        val ssao = ScreenSpaceAmbientOcclusion.compute(
            buffer, deferred, cameraMatrix, sett.strength, sett.samples, sett.enable2x2Blur
        )

        // use the existing depth buffer for the 3d ui
        if (useBloom) {

            val illuminated = FBStack["", w, h, 4, true, buffer.samples, false]
            useFrame(illuminated, copyRenderer) { // apply post-processing
                combineLighting(
                    deferred, false, pipeline.ambient,
                    buffer, lightBuffer, ssao
                )
            }

            // screen space reflections
            val ssReflections = ScreenSpaceReflections.compute(
                buffer, illuminated.getTexture0(), deferred, cameraMatrix,
                false
            ) ?: illuminated.getTexture0()

            useFrame(w, h, true, baseSameDepth) {

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

            useFrame(w, h, true, baseSameDepth) {

                // don't write depth
                GFXState.depthMask.use(false) {
                    combineLighting(
                        deferred, applyToneMapping = true, pipeline.ambient,
                        buffer, lightBuffer, ssao
                    )
                }

                drawGizmos(true)
                drawSelected()

            }
        }

        // anti-aliasing
        val dstBuffer = FBStack["RenderView-dst", buffer.width, buffer.height, 4, false, 1, false]
        useFrame(w, h, true, dstBuffer) {
            FXAA.render(baseSameDepth.getTexture0())
        }
        return dstBuffer
    }

    fun resolveClick(px: Float, py: Float, drawDebug: Boolean = false): Pair<Entity?, Component?> {

        // pipeline should already be filled
        val ws = windowStack
        val buffer = FBStack["click", ws.width, ws.height, 4, true, 1, true]

        val diameter = 5

        val px2 = px.toInt() - x
        val py2 = py.toInt() - y

        val world = getWorld()

        val ids = Screenshots.getU8RGBAPixels(diameter, px2, py2, buffer, idRenderer) {
            drawScene(width, height, idRenderer, buffer, changeSize = false, hdr = false)
            drawGizmos(drawGridLines = false, drawDebug)
        }

        for (idx in ids.indices) {
            ids[idx] = ids[idx] and 0xffffff
        }

        val depths = Screenshots.getFP32RPixels(diameter, px2, py2, buffer, depthRenderer) {
            drawScene(width, height, depthRenderer, buffer, changeSize = false, hdr = false)
            drawGizmos(drawGridLines = false, drawDebug)
        }

        val clickedIdBGR = Screenshots.getClosestId(diameter, ids, depths, if (reverseDepth) -10 else +10)
        val clickedId = Maths.convertABGR2ARGB(clickedIdBGR).and(0xffffff)
        val clicked = if (clickedId == 0 || world !is Entity) null
        else pipeline.findDrawnSubject(clickedId, world)
        /*LOGGER.info("Found: ${ids.joinToString { it.toString(16) }} x ${depths.joinToString()} -> $clickedId -> $clicked")
        val ids2 = (world as? Entity)?.getComponentsInChildren(MeshComponent::class, false)
            ?.joinToString { it.clickId.toString(16) }
        LOGGER.info("Available: $ids2")*/
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

    private val tmpRot0 = Quaterniond()
    private val tmpRot1 = Quaterniond()
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

        // must be called before we define our render settings
        // so lights don't override our settings, or we'd have to repeat our definition
        if (update) {
            updateWorld(world)
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

        bloomStrength = mix(previousCamera.bloomStrength, camera.bloomStrength, blendF)
        bloomOffset = mix(previousCamera.bloomOffset, camera.bloomOffset, blendF)

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
            this.fovXRadians = 2f * atan(tan(fovYRadians * 0.5f) * aspectRatio)
            this.fovYRadians = fovYRadians
            Perspective.setPerspective(
                cameraMatrix, fovYRadians, aspectRatio,
                scaledNear.toFloat(), scaledFar.toFloat(), centerX, centerY
            )
        } else {
            scaledNear = max(scaledNear, worldScale * 0.001)
            scaledFar = min(scaledFar, worldScale * 1000.0)
            fovXRadians = fov * aspectRatio
            fovYRadians = fov // not really defined
            val sceneScaleXY = 1f / fov
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

        if (renderMode == RenderMode.FSR2_X8 || renderMode == RenderMode.FSR2_X2) {
            fsr22.jitter(cameraMatrix, width, height)
        }

        if (!cameraMatrix.isFinite) {
            cameraMatrix.identity()
            LOGGER.warn("Set matrix to identity, because it was non-finite! $cameraMatrix")
        }

        val t0 = previousCamera.entity?.transform?.run {
            validate()
            getDrawMatrix()
        }
        val t1 = camera.entity?.transform?.run {
            validate()
            getDrawMatrix()
        }


        val rot0 = tmpRot0
        val rot1 = tmpRot1
        if (t0 != null) t0.getUnnormalizedRotation(tmpRot0) else tmpRot0.identity()
        if (t1 != null) t1.getUnnormalizedRotation(tmpRot1) else tmpRot1.identity()
        if (!rot0.isFinite) rot0.identity()
        if (!rot1.isFinite) rot1.identity()

        val camRot = rot0.slerp(rot1, blend)
        val camRotInv = camRot.conjugate(rot1)

        cameraMatrix.rotate(JomlPools.quat4f.borrow().set(camRotInv))

        cameraRotation.set(camRot)
        cameraRotation.transform(cameraDirection.set(0.0, 0.0, -1.0)).normalize()

        if (t0 != null) t0.getTranslation(cameraPosition)
        else cameraPosition.set(0.0)
        if (t1 != null) cameraPosition.lerp(t1.getTranslation(Vector3d()), blend)
        else cameraPosition.mul(1.0 - blend)

        // camera matrix and mouse position to ray direction
        if (update) {
            val window = window!!
            getMouseRayDirection(window.mouseX, window.mouseY, mouseDirection)
        }

        prevCamMatrix.set(lastCamMat)
        prevCamPosition.set(lastCamPos)
        prevCamRotation.set(lastCamRot)
        prevWorldScale = lastWorldScale

        currentInstance = this

        definePipeline(width, height, aspectRatio, fov, world)
    }

    fun updateWorld(world: PrefabSaveable?) {
        when (world) {
            is Entity -> {
                world.update()
                world.validateTransform()
            }

            is Component -> {
                world.onUpdate()
            }
        }
    }

    fun definePipeline(
        width: Int, height: Int, aspectRatio: Float,
        fov: Float, world: PrefabSaveable?
    ) {

        pipeline.clear()
        if (isPerspective) {
            pipeline.frustum.definePerspective(
                near, far, fovYRadians.toDouble(),
                width, height, aspectRatio.toDouble(),
                cameraPosition, cameraRotation,
            )
        } else {
            pipeline.frustum.defineOrthographic(
                fov.toDouble(), aspectRatio.toDouble(), near, far, width,
                cameraPosition, cameraRotation
            )
        }

        pipeline.disableReflectionCullingPlane()
        pipeline.ignoredEntity = null
        pipeline.resetClickId()
        setRenderState() // needed for light matrix calculation (camSpaceToLightSpace)
        if (world != null) pipeline.fill(world)
        controlScheme?.fill(pipeline)
        // if the scene would be dark, define lights, so we can see something
        addDefaultLightsIfRequired(pipeline)
        entityBaseClickId = pipeline.lastClickId
    }

    private val reverseDepth get() = renderMode != RenderMode.INVERSE_DEPTH

    fun setClearDepth() {
        stage0.depthMode = depthMode
        /**
         * the depth mode is a little convoluted:
         * - we need to draw the backsides (far) of lights to ensure that we can see them, even if we are inside them
         * - we want to use the depth test for optimization
         * - if we choose the normal mode, we wouldn't see it ->
         * - use the inverse mode, and discard lights that are too close instead of those that are too far
         * */
        // todo re-enable, when we know, why the directional light doesn't like this
        // pipeline.lightPseudoStage.depthMode = invDepthMode
    }

    private val depthMode
        get() = if (renderMode == RenderMode.NO_DEPTH) DepthMode.ALWAYS
        else if (reverseDepth) DepthMode.CLOSER else DepthMode.FORWARD_CLOSER

    fun drawScene(
        w: Int, h: Int,
        renderer: Renderer,
        dst: IFramebuffer,
        changeSize: Boolean,
        hdr: Boolean
    ) {

        GFX.check()

        pipeline.applyToneMapping = !hdr

        val depthPrepass = renderMode == RenderMode.WITH_DEPTH_PREPASS
        if (depthPrepass) {
            useFrame(w, h, changeSize, dst, cheapRenderer) {

                Frame.bind()

                GFXState.depthMode.use(depthMode) {
                    setClearDepth()
                    dst.clearDepth()
                    // doesn't work, makes everything gray :/
                    /*GFXState.blendMode.use(null) {
                        GFXState.depthMask.use(false) {
                            pipeline.drawSky0(pipeline.skybox, stage0)
                        }
                    }*/
                }

                GFXState.depthMode.use(depthMode) {
                    GFXState.blendMode.use(null) {
                        stage0.drawDepths(pipeline)
                    }
                }
                stage0.depthMode = DepthMode.EQUALS

            }
        }

        useFrame(w, h, changeSize, dst, renderer) {

            if (!depthPrepass) {
                Frame.bind()
                GFXState.depthMode.use(depthMode) {
                    setClearDepth()
                    dst.clearDepth()
                }
            }

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
                    is Entity -> drawOutline(selected)
                    is SkyboxBase -> {}
                    is MeshComponentBase -> {
                        val mesh = selected.getMesh() ?: continue
                        drawOutline(selected, mesh)
                    }

                    is Component -> drawOutline(selected.entity ?: continue)
                }
            }
        }
    }

    fun drawSceneLights(deferred: IFramebuffer, dst: IFramebuffer) {
        useFrame(deferred.width, deferred.height, true, dst, copyRenderer) {
            dst.clearColor(0)
            pipeline.lightStage.bindDraw(deferred, cameraMatrix, cameraPosition, worldScale)
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
    }

    var drawGridWhenPlaying = false
    var drawGridWhenEditing = true

    fun drawGizmos(
        drawGridLines: Boolean,
        drawDebug: Boolean = true,
        drawAABBs: Boolean = false
    ) {
        GFXState.blendMode.use(BlendMode.DEFAULT) {
            GFXState.depthMode.use(depthMode) {
                val drawGrid = drawGridLines && if (playMode == PlayMode.EDITING)
                    drawGridWhenEditing else drawGridWhenPlaying
                drawGizmos1(drawGrid, drawDebug, drawAABBs)
            }
        }
    }

    fun drawGizmos1(
        drawGrid: Boolean,
        drawDebugShapes: Boolean,
        drawAABBs: Boolean
    ) {

        val world = getWorld()
        stack.set(cameraMatrix)

        controlScheme?.drawGizmos()

        var clickId = entityBaseClickId

        // much faster than depthTraversal, because we only need visible elements anyway
        if (world != null) pipeline.traverse(world) { entity ->

            val transform = entity.transform
            val globalTransform = transform.globalTransform

            /*val doDrawCircle = camPosition.distanceSquared(
                globalTransform.m30,
                globalTransform.m31,
                globalTransform.m32
            ) < maxCircleLenSq*/

            val nextClickId = clickId++
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
                if (component.isEnabled) {
                    // mesh components already got their ID
                    if (component !is MeshComponentBase && component !is MeshSpawner) {
                        val componentClickId = clickId++
                        component.clickId = componentClickId
                    }
                    component.onDrawGUI(component.isSelectedIndirectly)
                }
            }

            stack.popMatrix()

            if (drawAABBs) {
                val aabb1 = entity.aabb
                val hit1 = aabb1.testLine(cameraPosition, mouseDirection, 1e10)
                drawAABB(aabb1, if (hit1) aabbColorHovered else aabbColorDefault)
                if (entity.hasRenderables) for (i in components.indices) {
                    val component = components[i]
                    if (component.isEnabled && component is MeshComponentBase) {
                        val aabb2 = component.globalAABB
                        val hit2 = aabb2.testLine(cameraPosition, mouseDirection, 1e10)
                        drawAABB(aabb2, if (hit2) aabbColorHovered else aabbColorDefault)
                    }
                }
            }

            LineBuffer.drawIf1M(cameraMatrix)

        }

        if (world is Component) {
            if (world !is MeshComponentBase && world !is MeshSpawner) {
                // mesh components already got their ID
                val componentClickId = clickId++
                world.clickId = componentClickId
            }
            world.onDrawGUI(world.isSelectedIndirectly)
        }

        if (drawGrid) {
            drawGrid(radius)
        }

        if (drawDebugShapes) {
            DebugRendering.drawDebugShapes(this)
        }
        DebugShapes.removeExpired()

        LineBuffer.finish(cameraMatrix)
        PlaneShapes.finish()
    }

    /**
     * get the mouse direction from this camera
     * todo for other cameras: can be used for virtual mice
     * */
    fun getMouseRayDirection(
        cx: Float = window!!.mouseX,
        cy: Float = window!!.mouseY,
        dst: Vector3d = Vector3d()
    ): Vector3d {
        val rx = (cx - x) / width * 2.0 - 1.0
        val ry = (cy - y) / height * 2.0 - 1.0
        return getRelativeMouseRayDirection(rx, -ry, dst)
    }

    fun getRelativeMouseRayDirection(
        rx: Double, // -1 .. 1
        ry: Double, // -1 .. 1
        dst: Vector3d = Vector3d()
    ): Vector3d {
        val tanHalfFoV = tan(fovYRadians * 0.5)
        val aspectRatio = width.toFloat() / height
        dst.set(rx * tanHalfFoV * aspectRatio, ry * tanHalfFoV, -1.0)
        return cameraRotation.transform(dst).normalize()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        getMouseRayDirection(x, y, JomlPools.vec3d.create())
        super.onMouseMoved(x, y, dx, dy)
    }

    var worldScale = 1.0

    var fovXRadians = 1f
    var fovYRadians = 1f

    var near = 1e-3
    var scaledNear = 1e-3

    // infinity
    var far = 1e10
    var scaledFar = 1e10
    var isPerspective = true

    val cameraMatrix = Matrix4f()
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

        RenderState.aspectRatio = width.toFloat() / height
        RenderState.worldScale = worldScale
        RenderState.prevWorldScale = prevWorldScale

        RenderState.cameraMatrix.set(cameraMatrix)
        RenderState.cameraPosition.set(cameraPosition)
        RenderState.cameraRotation.set(cameraRotation)
        RenderState.calculateDirections(isPerspective)

        RenderState.prevCameraMatrix.set(prevCamMatrix)
        RenderState.prevCameraPosition.set(prevCamPosition)

        RenderState.fovXRadians = fovXRadians
        RenderState.fovYRadians = fovYRadians
        RenderState.near = scaledNear.toFloat()
        RenderState.far = scaledFar.toFloat()
    }

    override val className: String get() = "RenderView"

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

        fun addDefaultLightsIfRequired(pipeline: Pipeline) {
            if (pipeline.lightStage.size <= 0 && pipeline.ambient.dot(1f, 1f, 1f) <= 0f) {
                pipeline.ambient.set(0.5f)
                defaultSun.fill(pipeline, defaultSunEntity, 0)
            }
        }

        object SSAOSettings {
            var strength = 1f
            var samples = max(1, DefaultConfig["gpu.ssao.samples", 128])
            var enable2x2Blur = true
        }
    }
}