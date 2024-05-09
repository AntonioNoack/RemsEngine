package me.anno.engine.ui.render

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.ui.CanvasComponent
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.DefaultSun.defaultSun
import me.anno.engine.ui.render.DefaultSun.defaultSunEntity
import me.anno.engine.ui.render.DrawAABB.drawAABB
import me.anno.engine.ui.render.MovingGrid.drawGrid
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.Renderers.overdrawRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.DitherMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.M4x3Delta.mul4x3delta
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.deferred.DeferredRenderer
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.popBetterBlending
import me.anno.gpu.drawing.DrawTexts.pushBetterBlending
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.Screenshots
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.effects.FSR2v2
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.renderer.Renderer.Companion.depthRenderer
import me.anno.gpu.shader.renderer.Renderer.Companion.idRenderer
import me.anno.gpu.texture.Texture2D
import me.anno.graph.visual.render.RenderGraph
import me.anno.input.Input
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.UIColors
import me.anno.ui.UIColors.paleGoldenRod
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.black
import me.anno.utils.Color.convertABGR2ARGB
import me.anno.utils.Color.hex24
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.NumberFormatter.formatIntTriplets
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector4d
import org.joml.Vector4f
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

// todo make shaders of materials be references via a file (StaticRef)? this will allow for visual shaders in the future

// todo define custom render modes using files within the project, editable in a GraphEditor

/**
 * a panel that renders the scene;
 * no controls are provided by this class, it just draws
 * */
abstract class RenderView(var playMode: PlayMode, style: Style) : Panel(style) {

    abstract fun getWorld(): PrefabSaveable?

    private var bloomStrength = 0f // defined by the camera
    private var bloomOffset = 0f // defined by the camera

    var controlScheme: ControlScheme? = null

    var enableOrbiting = true

    // can exist (game/game mode), but does not need to (editor)
    var localPlayer: LocalPlayer? = null

    val editorCamera = Camera()
    val editorCameraNode = Entity(editorCamera)

    var renderMode = RenderMode.DEFAULT

    var radius = 50.0
        set(value) {
            field = clamp(value, 1e-130, 1e130)
        }

    open fun updateWorldScale() {
        worldScale = if (renderMode == RenderMode.MONO_WORLD_SCALE) 1.0 else 1.0 / radius
    }

    // todo move this to OrbitController
    val orbitCenter = Vector3d()
    val orbitRotation = Quaterniond()
        .rotateX((-30.0).toRadians())

    val buffers = RenderBuffers()

    private var entityBaseClickId = 0

    val pipeline = buffers.pipeline

    override val canDrawOverBorders get() = true

    override fun destroy() {
        super.destroy()
        // all framebuffers that we own need to be freed
        buffers.destroy()
        editorCameraNode.destroy()
        fsr22.destroy()
    }

    open fun updateEditorCameraTransform() {

        val radius = radius
        val camera = editorCamera
        val cameraNode = editorCameraNode

        if (!orbitCenter.isFinite) LOGGER.warn("Invalid position $orbitCenter")
        if (!orbitRotation.isFinite) LOGGER.warn("Invalid rotation $orbitRotation")

        camera.far = far
        camera.near = near

        val tmp3d = JomlPools.vec3d.borrow()
        cameraNode.transform.localPosition =
            if (enableOrbiting) orbitRotation.transform(tmp3d.set(0.0, 0.0, radius)).add(orbitCenter)
            else orbitCenter
        cameraNode.transform.localRotation = orbitRotation
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

        val drawnPrimitives0 = PipelineStageImpl.drawnPrimitives
        val drawnInstances0 = PipelineStageImpl.drawnInstances
        val drawCalls0 = PipelineStageImpl.drawCalls

        currentInstance = this

        // to see ghosting, if there is any
        val renderMode = renderMode
        if (renderMode == RenderMode.GHOSTING_DEBUG) Thread.sleep(250)

        updateEditorCameraTransform()

        val world = getWorld()

        setRenderState()

        val player = localPlayer
        var camera0 = player?.cameraState?.previousCamera ?: editorCamera
        val camera1 = player?.cameraState?.currentCamera ?: editorCamera
        var blending = player?.cameraState?.cameraBlendingProgress ?: 0f
        if (player == null) updateEditorCameraTransform()

        if (blending >= 1f) {
            blending = 1f
            camera0 = camera1
        }

        val aspectRatio = findAspectRatio()

        val t1 = Time.nanoTime
        prepareDrawScene(width, height, aspectRatio, camera0, camera1, blending, true)
        val t2 = Time.nanoTime
        FrameTimings.add(t2 - t1, UIColors.midOrange)

        setRenderState()
        pipeline.superMaterial = renderMode.superMaterial
        updatePipelineStage0(renderMode)

        val renderGraph = renderMode.renderGraph
        if (renderGraph != null) {
            // graph will draw all things
            RenderGraph.draw(this, this, renderGraph)
        } else {
            // we have to draw based on Renderer/RenderMode
            // todo this forgetting to clear, which looks weird...
            val renderer = renderMode.renderer ?: DeferredRenderer
            val buffer = findBuffer(renderer)
            updateSkybox(renderer)
            drawScene(x0, y0, x1, y1, renderer, buffer)
        }

        val t3 = Time.nanoTime
        FrameTimings.add(t3 - t1, UIColors.cornFlowerBlue)

        if (world == null) {
            drawTextCenter("Scene Not Found!")
        }

        if (playMode == PlayMode.EDITING) {
            DebugRendering.showShadowMapDebug(this)
            DebugRendering.showCameraRendering(this, x0, y0, x1, y1)
        }

        if (world is Entity && playMode != PlayMode.EDITING) {
            drawPrimaryCanvas(world, x0, y0, x1, y1)
        }

        if (playMode == PlayMode.EDITING) {
            drawDebugStats(drawnPrimitives0, drawnInstances0, drawCalls0)
        }

        updatePrevState()

        val t4 = Time.nanoTime
        FrameTimings.add(t4 - t1, paleGoldenRod)
    }

    fun updatePipelineStage0(renderMode: RenderMode) {
        val stage0 = pipeline.stages.firstOrNull() ?: return
        stage0.depthMode = depthMode
        stage0.blendMode = if (renderMode == RenderMode.OVERDRAW) BlendMode.ADD else null
        stage0.sorting = Sorting.FRONT_TO_BACK
        stage0.cullMode = if (renderMode != RenderMode.FRONT_BACK) CullMode.FRONT else CullMode.BOTH
    }

    fun findBuffer(renderer: Renderer): IFramebuffer {
        // multi-sampled buffer
        return when {
            // msaa, single target
            renderMode == RenderMode.MSAA_FORWARD -> buffers.base8Buffer
            // aliased, multi-target
            renderer == DeferredRenderer -> buffers.baseNBuffer1
            else -> buffers.base1Buffer
        }
    }

    fun updateSkybox(renderer: Renderer) {
        // blacklist for renderModes?
        if (renderer !in attributeRenderers.values && renderMode != RenderMode.LIGHT_COUNT) {
            pipeline.bakeSkybox(256)
        } else {
            pipeline.destroyBakedSkybox()
        }
    }

    fun findRowsCols(size: Int): IntPair {
        val rows = when {
            size % 2 == 0 -> 2
            size % 3 == 0 -> 3
            size > 12 -> 3
            size > 6 -> 2
            else -> 1
        }
        val cols = ceilDiv(size, rows)
        return IntPair(rows, cols)
    }

    fun findAspectRatio(): Float {
        var aspect = width.toFloat() / height

        val layers = buffers.deferred.storageLayers
        val size = when (renderMode) { /* 1 for light, 1 for depth */
            RenderMode.ALL_DEFERRED_BUFFERS -> layers.size + 1 + GFX.supportsDepthTextures.toInt()
            RenderMode.ALL_DEFERRED_LAYERS -> buffers.deferred.layerTypes.size + 1 + GFX.supportsDepthTextures.toInt()
            else -> 1
        }

        if (size > 1) {
            val (rows, cols) = findRowsCols(size)
            aspect *= rows.toFloat() / cols.toFloat()
        }

        return aspect
    }

    fun drawPrimaryCanvas(world: Entity, x0: Int, y0: Int, x1: Int, y1: Int) {
        world.forAllComponentsInChildren(CanvasComponent::class, false) { comp ->
            if (comp.space == CanvasComponent.Space.CAMERA_SPACE) {
                comp.width = x1 - x0
                comp.height = y1 - y0
                comp.render()
                val texture = comp.framebuffer!!.getTexture0()
                drawTexture(x0, y1, x1 - x0, y0 - y1, texture, -1, null)
            }
        }
    }

    fun drawTextCenter(text: String) {
        val pbb = pushBetterBlending(true)
        drawSimpleTextCharByChar(
            x + width / 2, y + height / 2, 4, text,
            AxisAlignment.CENTER, AxisAlignment.CENTER
        )
        popBetterBlending(pbb)
    }

    fun drawDebugStats(drawnPrimitives0: Long, drawnInstances0: Long, drawCalls0: Long) {
        val pbb = pushBetterBlending(true)
        val drawnPrimitives = PipelineStageImpl.drawnPrimitives - drawnPrimitives0
        val drawnInstances = PipelineStageImpl.drawnInstances - drawnInstances0
        val drawCalls = PipelineStageImpl.drawCalls - drawCalls0
        val usesBetterBlending = DrawTexts.canUseComputeShader()
        drawSimpleTextCharByChar(
            x + 2, y + height + 1,
            0, "${formatIntTriplets(drawnPrimitives)} tris, " +
                    "${formatIntTriplets(drawnInstances)} inst, " +
                    "${formatIntTriplets(drawCalls)} calls",
            FrameTimings.textColor,
            FrameTimings.backgroundColor.withAlpha(if (usesBetterBlending) 0 else 255),
            AxisAlignment.MIN, AxisAlignment.MAX
        )
        popBetterBlending(pbb)
    }

    // be more conservative with framebuffer size changes,
    // because they are expensive -> only change every 20th frame
    private val mayChangeSize get() = (Time.frameIndex % 20 == 0)

    private val fsr22 by lazy { FSR2v2() }

    fun drawScene(
        x0: Int, y0: Int, x1: Int, y1: Int,
        renderer: Renderer, buffer: IFramebuffer
    ) {

        var w = x1 - x0
        var h = y1 - y0

        val scale = when (renderMode) {
            RenderMode.FSR2_X2 -> 2
            RenderMode.FSR2_X8 -> 8
            else -> 1
        }
        if (scale > 1) {
            w = ceilDiv(w, scale)
            h = ceilDiv(h, scale)
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

        when (renderMode) {
            RenderMode.FSR2_X8, RenderMode.FSR2_X2 -> {
                drawScene(w, h, renderer, buffer, changeSize = true, hdr = true)
                fsr22.render(
                    this, w, h, x0, y0, x1, y1, buffer, buffers.deferred,
                    buffers.lightNBuffer1, buffers.baseSameDepth1
                )
            }
            RenderMode.LIGHT_COUNT -> {
                val lightBuffer = if (buffer == buffers.base1Buffer) buffers.light1Buffer else buffers.lightNBuffer1
                DebugRendering.drawLightCount(
                    this, x0, y0, w, h,
                    renderer, buffer, lightBuffer, buffers.deferred
                )
            }
            RenderMode.ALL_DEFERRED_BUFFERS -> {
                val lightBuffer = if (buffer == buffers.base1Buffer) buffers.light1Buffer else buffers.lightNBuffer1
                DebugRendering.drawAllBuffers(
                    this, w, h, x0, y0, x1, y1,
                    renderer, buffer, lightBuffer, buffers.deferred
                )
            }
            RenderMode.ALL_DEFERRED_LAYERS -> {
                DebugRendering.drawAllLayers(
                    this, w, h, x0, y0, x1, y1,
                    renderer, buffer, buffers.lightNBuffer1, buffers.deferred
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
    }

    fun resolveClick(px: Float, py: Float): Pair<Entity?, Component?> {
        val world = getWorld()
        if (world != null) {

            // refill is necessary when shadows are calculated,
            // because that overrides clickIds
            pipeline.clear()
            pipeline.resetClickId()
            pipeline.fill(world)

            val ws = windowStack
            val buffer = FBStack["click", ws.width, ws.height, 4, true, 1, DepthBufferType.INTERNAL]

            val diameter = 5

            val px2 = px.toInt() - x
            val py2 = py.toInt() - y

            val ids = Screenshots.getU8RGBAPixels(diameter, px2, py2, buffer, idRenderer) {
                GFXState.ditherMode.use(DitherMode.DITHER2X2) {
                    buffer.clearColor(0, true)
                    drawScene(width, height, idRenderer, buffer, changeSize = false, hdr = false)
                }
            }

            for (idx in ids.indices) {
                ids[idx] = ids[idx] and 0xffffff
            }

            // todo some clicks work on SDFSphere, others don't... why???
            val depths = Screenshots.getFP32RPixels(diameter, px2, py2, buffer, depthRenderer) {
                GFXState.ditherMode.use(DitherMode.DITHER2X2) {
                    buffer.clearDepth()
                    drawScene(width, height, depthRenderer, buffer, changeSize = false, hdr = false)
                }
            }

            val clickedIdBGR = Screenshots.getClosestId(
                diameter, ids, depths,
                if (inverseDepth != GFX.supportsClipControl) -10 else +10
            )
            val clickedId = convertABGR2ARGB(clickedIdBGR).and(0xffffff)
            val clicked = if (clickedId == 0) null
            else pipeline.findDrawnSubject(clickedId, world)
            if (true) {
                LOGGER.info("Found: ${ids.joinToString { hex24(convertABGR2ARGB(it)) }} x ${depths.joinToString()} -> $clickedId -> $clicked")
                val ids2 = world.listOfAll
                    .filterIsInstance<Component>()
                    .filter { it is Renderable }
                    .map { it.clickId.toString(16) }
                    .toList()
                LOGGER.info("Available: $ids2")
            }
            return Pair(clicked as? Entity, clicked as? Component)
        } else return Pair(null, null)
    }

    fun prepareDrawScene(
        width: Int,
        height: Int,
        aspectRatio: Float,
        currCam: Camera,
        prevCam: Camera,
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

        val near = mix(prevCam.near, currCam.near, blend)
        val far = mix(prevCam.far, currCam.far, blend)
        val isPerspective = currCam.isPerspective
        val fov = if (isPerspective) {
            mix(prevCam.fovY, currCam.fovY, blending)
        } else {
            mix(prevCam.fovOrthographic, currCam.fovOrthographic, blending)
        }

        bloomStrength = mix(prevCam.bloomStrength, currCam.bloomStrength, blendF)
        bloomOffset = mix(prevCam.bloomOffset, currCam.bloomOffset, blendF)

        val centerX = mix(prevCam.center.x, currCam.center.x, blendF)
        val centerY = mix(prevCam.center.x, currCam.center.y, blendF)

        // this needs to be separate from the stack
        // (for normal calculations and such)
        this.scaledNear = (near * worldScale)
        this.scaledFar = (far * worldScale)
        this.isPerspective = isPerspective
        if (isPerspective) {
            setPerspectiveCamera(fov, aspectRatio, centerX, centerY)
        } else {
            setOrthographicCamera(fov, aspectRatio)
        }

        if (renderMode == RenderMode.FSR2_X8 || renderMode == RenderMode.FSR2_X2) {
            fsr22.jitter(cameraMatrix, width, height)
        }

        if (!cameraMatrix.isFinite) {
            cameraMatrix.identity()
            LOGGER.warn("Set matrix to identity, because it was non-finite! $cameraMatrix")
        }

        val t0 = prevCam.entity?.transform?.getValidDrawMatrix()
        val t1 = currCam.entity?.transform?.getValidDrawMatrix()

        val rot0 = JomlPools.quat4d.create()
        val rot1 = JomlPools.quat4d.create()
        if (t0 != null) t0.getUnnormalizedRotation(rot0) else rot0.identity()
        if (t1 != null) t1.getUnnormalizedRotation(rot1) else rot1.identity()
        if (!rot0.isFinite) rot0.identity()
        if (!rot1.isFinite) rot1.identity()

        val camRot = rot0.slerp(rot1, blend)
        val camRotInv = camRot.conjugate(rot1)

        cameraMatrix.rotate(camRotInv)
        cameraRotation.set(camRot)
        cameraRotation.transform(cameraDirection.set(0.0, 0.0, -1.0)).normalize()
        JomlPools.quat4d.sub(2)

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

    fun setPerspectiveCamera(fov: Float, aspectRatio: Float, centerX: Float, centerY: Float) {
        val fovYRadians = fov.toRadians()
        this.fovXRadians = 2f * atan(tan(fovYRadians * 0.5f) * aspectRatio)
        this.fovYRadians = fovYRadians
        Perspective.setPerspective(
            cameraMatrix, fovYRadians, aspectRatio,
            scaledNear.toFloat(), scaledFar.toFloat(), centerX, centerY
        )
    }

    fun setOrthographicCamera(fov: Float, aspectRatio: Float) {
        val scaledNear = max(scaledNear, worldScale * 0.001)
        val scaledFar = min(scaledFar, worldScale * 1000.0)
        fovXRadians = fov * aspectRatio
        fovYRadians = fov // not really defined
        val sceneScaleXY = 1f / fov
        val n: Float
        val f: Float
        // todo some devices may not support 01-range, so make this optional
        val range01 = inverseDepth
        if (range01) {
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
        addDefaultLightsIfRequired(pipeline, world, this)
        entityBaseClickId = pipeline.lastClickId
    }

    private val inverseDepth get() = renderMode == RenderMode.INVERSE_DEPTH

    private val depthMode: DepthMode
        get() = if (GFX.supportsClipControl) {
            if (renderMode == RenderMode.NO_DEPTH) DepthMode.ALWAYS
            else if (inverseDepth) DepthMode.FAR else DepthMode.CLOSE
        } else {
            if (renderMode == RenderMode.NO_DEPTH) DepthMode.FORWARD_ALWAYS
            else if (inverseDepth) DepthMode.FORWARD_FAR else DepthMode.FORWARD_CLOSE
        }

    fun drawScene(
        w: Int, h: Int,
        renderer: Renderer,
        dst: IFramebuffer,
        changeSize: Boolean,
        hdr: Boolean
    ) {
        GFX.check()
        pipeline.applyToneMapping = !hdr
        useFrame(w, h, changeSize, dst, renderer) {

            Frame.bind()
            val depthMode = depthMode
            val stage0 = pipeline.stages.firstOrNull()
            GFXState.depthMode.use(depthMode) {
                stage0?.depthMode = depthMode
                dst.clearDepth()
            }

            if (renderer == overdrawRenderer) {
                dst.clearColor(0)
            }

            GFX.check()
            pipeline.singlePassWithSky(true)
            GFX.check()

        }
    }

    fun drawSceneLights(deferred: IFramebuffer, deferredDepth: Texture2D, depthMask: Vector4f, dst: IFramebuffer) {
        useFrame(deferred.width, deferred.height, true, dst, copyRenderer) {
            dst.clearColor(0)
            pipeline.lightStage.bindDraw(
                deferred, deferredDepth, depthMask,
                cameraMatrix, cameraPosition, worldScale
            )
        }
    }

    fun drawGizmos(
        framebuffer: IFramebuffer,
        drawGridLines: Boolean,
        drawDebug: Boolean = true
    ) {
        useFrame(framebuffer, simpleNormalRenderer) {
            drawGizmos(drawGridLines, drawDebug)
        }
    }

    var drawGridWhenPlaying = 0
    var drawGridWhenEditing = 2

    val drawGrid
        get() = if (playMode == PlayMode.EDITING)
            drawGridWhenEditing else drawGridWhenPlaying

    fun drawGizmos(
        drawGridLines: Boolean,
        drawDebug: Boolean = true,
        drawAABBs: Boolean = false
    ) {
        GFXState.blendMode.use(BlendMode.DEFAULT) {
            GFXState.depthMode.use(depthMode) {
                val drawGrid = if (drawGridLines) drawGrid else 0
                drawGizmos1(drawGrid, drawDebug, drawAABBs)
            }
        }
    }

    fun drawGizmos1(
        drawGridMask: Int,
        drawDebugShapes: Boolean,
        drawAABBs: Boolean
    ) {

        val world = getWorld()
        stack.set(cameraMatrix)

        controlScheme?.drawGizmos()

        // much faster than depthTraversal, because we only need visible elements anyway
        if (world != null && drawAABBs) {
            pipeline.traverse(world) { entity ->
                val aabb1 = entity.aabb
                val hit1 = aabb1.testLine(cameraPosition, mouseDirection, 1e10)
                drawAABB(aabb1, if (hit1) aabbColorHovered else aabbColorDefault)
                if (entity.hasRenderables) {
                    val components = entity.components
                    for (i in components.indices) {
                        val component = components[i]
                        if (component.isEnabled) {
                            when (component) {
                                is MeshComponentBase -> {
                                    val aabb2 = component.globalAABB
                                    val hit2 = aabb2.testLine(cameraPosition, mouseDirection, 1e10)
                                    drawAABB(aabb2, if (hit2) aabbColorHovered else aabbColorDefault)
                                }
                                is MeshSpawner -> {
                                    val aabb2 = component.globalAABB
                                    val hit2 = aabb2.testLine(cameraPosition, mouseDirection, 1e10)
                                    drawAABB(aabb2, if (hit2) aabbColorHovered else aabbColorDefault)
                                }
                            }
                        }
                    }
                }
                LineBuffer.drawIf1M(cameraMatrix)
            }
        }

        // traverse over visible & selected
        // c in EditorState.selection ||
        //                entity?.anyInHierarchy { it == EditorState.lastSelection } == true
        for (component in EditorState.selection) {
            if (component is Component && component.isEnabled) {
                val entity = component.entity
                if (entity == null || pipeline.frustum.isVisible(entity.aabb)) {

                    val stack = stack
                    if (entity != null) {
                        val transform = entity.transform
                        val globalTransform = transform.globalTransform
                        stack.pushMatrix()
                        stack.mul4x3delta(globalTransform, cameraPosition, worldScale)
                    }

                    component.onDrawGUI(true)

                    if (entity != null) {
                        stack.popMatrix()
                    }

                    LineBuffer.drawIf1M(cameraMatrix)
                }
            }
        }

        if (world is Component && world !in EditorState.selection) {
            world.onDrawGUI(world.isSelectedIndirectly)
        }

        drawGrid(drawGridMask)

        if (drawDebugShapes) {
            DebugRendering.drawDebugShapes(this)
        }
        DebugShapes.removeExpired()

        LineBuffer.finish(cameraMatrix)
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

        fun addDefaultLightsIfRequired(pipeline: Pipeline, world: PrefabSaveable?, rv: RenderView?) {
            // todo when we have our directional lights within the skybox somehow, remove this
            if (pipeline.lightStage.size <= 0) {
                // also somehow calculate the required bounds, and calculate shadows for the default sun
                val bounds = when (world) {
                    is Entity -> world.getBounds()
                    is Component -> {
                        val bounds = AABBd()
                        world.fillSpace(Matrix4x3d(), bounds)
                        bounds
                    }
                    else -> null
                }
                if (rv != null && Input.isShiftDown && false) {
                    if (bounds == null || bounds.isEmpty() || bounds.volume > 1e38) {
                        defaultSun.shadowMapCascades = 0
                        defaultSun.onUpdate()
                    } else {
                        rv.setRenderState() // camera position needs to be reset
                        defaultSun.rootOverride = world
                        defaultSun.shadowMapCascades = 1
                        // calculate good position for sun
                        val transform = defaultSunEntity.transform
                        transform.localPosition = transform.localPosition
                            .set(bounds.centerX, bounds.centerY, bounds.centerZ)
                        transform.localScale = transform.localScale
                            .set(3.0 / max(bounds.deltaX, max(bounds.deltaY, bounds.deltaZ)))
                        transform.teleportUpdate()
                        transform.validate()
                        defaultSun.onUpdate()
                        defaultSun.rootOverride = null
                        rv.setRenderState() // camera position needs to be reset
                    }
                }
                defaultSun.fill(pipeline, defaultSunEntity, 0)
            }
        }
    }
}