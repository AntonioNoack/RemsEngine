package me.anno.engine.ui.render

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.ui.CanvasComponent
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnBeforeDraw
import me.anno.ecs.systems.OnDrawGUI
import me.anno.ecs.systems.Systems
import me.anno.engine.debug.DebugShapes
import me.anno.engine.inspector.Inspectable
import me.anno.engine.raycast.RayQuery
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.DebugRendering.drawDebugStats
import me.anno.engine.ui.render.DebugRendering.drawDebugSteps
import me.anno.engine.ui.render.DefaultSun.defaultSun
import me.anno.engine.ui.render.DefaultSun.defaultSunEntity
import me.anno.engine.ui.render.DrawAABB.drawAABB
import me.anno.engine.ui.render.MovingGrid.drawGrid
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.Renderers.simpleRenderer
import me.anno.engine.ui.render.RowColLayout.findGoodTileLayout
import me.anno.engine.ui.vr.DebugVRRendering.showStereoView
import me.anno.engine.ui.vr.VRRenderingRoutine
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.DitherMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.supportsClipControl
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.M4x3Delta.mul4x3delta
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.deferred.DeferredRenderer
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
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.shader.effects.FSR2v2
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.renderer.Renderer.Companion.depthRenderer
import me.anno.gpu.shader.renderer.Renderer.Companion.idRenderer
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.graph.visual.render.RenderGraph
import me.anno.graph.visual.render.effects.FSR2Node
import me.anno.graph.visual.render.effects.TAANode
import me.anno.graph.visual.render.effects.framegen.FrameGenInitNode
import me.anno.input.Input
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.UIColors
import me.anno.ui.UIColors.paleGoldenRod
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.black
import me.anno.utils.Color.convertABGR2ARGB
import me.anno.utils.Color.hex24
import me.anno.utils.GFXFeatures
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

// todo make shaders of materials be references via a file (StaticRef)? this will allow for visual shaders in the future

// todo define custom render modes using files within the project, editable in a GraphEditor

// todo read Nanite paper and find out how we can calculate meshlet hierarchies

/**
 * a panel that renders the scene;
 * no controls are provided by this class, it just draws
 * */
abstract class RenderView(var playMode: PlayMode, style: Style) : Panel(style) {

    abstract fun getWorld(): PrefabSaveable?

    var controlScheme: ControlScheme? = null

    var enableOrbiting = true

    // can exist (game/game mode), but does not need to (editor)
    var localPlayer: LocalPlayer? = null

    val editorCamera = Camera()
    val editorCameraNode = Entity()
        .add(editorCamera)

    var renderMode = RenderMode.DEFAULT
    var superMaterial = SuperMaterial.NONE
        set(value) {
            field = value
            pipeline.superMaterial = value.material
        }

    var radius = 10f
        set(value) {
            field = clamp(value, 1e-35f, 1e35f)
        }

    fun usesFrameGen(): Boolean {
        return renderMode.renderGraph?.nodes?.any2 { it is FrameGenInitNode } == true
    }

    fun skipUpdate(): Boolean {
        return FrameGenInitNode.skipThisFrame() && usesFrameGen()
    }

    // todo move this to OrbitController?
    val orbitCenter = Vector3d()
    val orbitRotation = Quaternionf()
        .rotateX((-30f).toRadians())

    val buffers = RenderBuffers()
    val renderSize = RenderSize()

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
            if (enableOrbiting) orbitRotation.transform(tmp3d.set(0f, 0f, radius)).add(orbitCenter)
            else orbitCenter
        cameraNode.transform.localRotation = orbitRotation
        cameraNode.transform.teleportUpdate()
        cameraNode.validateTransform()
    }

    private fun tryRenderVRViews(): Boolean {
        val vrr = VRRenderingRoutine.vrRoutine
        if (vrr == null || !vrr.isActive) return false

        val leftTexture = vrr.leftTexture ?: return false
        val rightTexture = vrr.rightTexture ?: TextureLib.blackTexture
        if (!leftTexture.isCreated() || !rightTexture.isCreated()) return false

        val showBoth = controlScheme?.settings?.displayVRInRedCyan == true
        showStereoView(
            x, y, width, height,
            leftTexture, vrr.leftView,
            rightTexture, vrr.rightView,
            1f / vrr.previewGamma, showBoth
        )
        return true
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

        if (tryRenderVRViews()) return

        val drawnPrimitives0 = PipelineStageImpl.drawnPrimitives
        val drawnInstances0 = PipelineStageImpl.drawnInstances
        val drawCalls0 = PipelineStageImpl.drawCalls

        currentInstance = this

        // to see ghosting, if there is any
        val renderMode = renderMode
        if (renderMode == RenderMode.GHOSTING_DEBUG) Thread.sleep(250)

        val skipUpdate = FrameGenInitNode.skipThisFrame() && usesFrameGen()

        updateEditorCameraTransform()

        val world = getWorld()

        setRenderState()

        val t1 = Time.nanoTime

        val camera = localPlayer?.cameraState?.currentCamera ?: editorCamera
        val aspectRatio = findAspectRatio()
        val update = !skipUpdate
        prepareDrawScene(width, height, aspectRatio, camera, update, update)
        val t2 = Time.nanoTime
        FrameTimings.add(t2 - t1, UIColors.midOrange)

        setRenderState()
        updatePipelineStages(renderMode)

        render(x0, y0, x1, y1)

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
            drawDebugSteps(this)
            drawDebugStats(this, drawnPrimitives0, drawnInstances0, drawCalls0)
        }

        if (update) {
            updatePrevState()
        }

        val t4 = Time.nanoTime
        FrameTimings.add(t4 - t1, paleGoldenRod)
    }

    fun render(x0: Int, y0: Int, x1: Int, y1: Int) {
        renderSize.updateSize(width, height)
        val renderGraph = renderMode.renderGraph
        if (renderGraph != null) {
            // graph will draw all things
            RenderGraph.draw(this, this, renderGraph)
        } else {
            // we have to draw based on Renderer/RenderMode
            val renderer = renderMode.renderer ?: DeferredRenderer
            val buffer = findBuffer(renderer)
            updateSkybox(renderer)
            drawScene(x0, y0, x1, y1, renderer, buffer)
        }
    }

    fun updatePipelineStages(renderMode: RenderMode) {
        val blendMode = if (renderMode == RenderMode.OVERDRAW || renderMode == RenderMode.TRIANGLE_SIZE)
            BlendMode.ADD else null
        val depthMode = depthMode
        val cullMode = if (renderMode != RenderMode.FRONT_BACK) CullMode.FRONT else CullMode.BOTH
        val stage0 = pipeline.stages.firstOrNull()
        if (stage0 != null) {
            stage0.depthMode = depthMode
            stage0.blendMode = blendMode
            stage0.cullMode = cullMode
        }
        val stage2 = pipeline.stages.getOrNull(PipelineStage.DECAL.id)
        if (stage2 != null) {
            stage2.depthMode = depthMode
            stage2.blendMode = blendMode
            stage2.cullMode = cullMode
        }
    }

    fun findBuffer(renderer: Renderer): IFramebuffer {
        return when {
            // used for FSR2, ALL_DEFERRED_LAYERS, ALL_DEFERRED_BUFFERS and LIGHT_COUNT at the moment
            renderer == DeferredRenderer -> buffers.baseNBuffer1
            else -> buffers.base1Buffer
        }
    }

    fun updateSkybox(renderer: Renderer) {
        // blacklist for renderModes?
        if (renderer !in attributeRenderers.values &&
            renderMode != RenderMode.LIGHT_COUNT &&
            renderMode != RenderMode.LIGHT_SUM &&
            renderMode != RenderMode.LIGHT_SUM_MSAA
        ) pipeline.bakeSkybox(256)
        else pipeline.destroyBakedSkybox()
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
            val colsRows = findGoodTileLayout(size, width, height)
            val cols = colsRows.x
            val rows = colsRows.y
            aspect *= rows.toFloat() / cols.toFloat()
        }

        return aspect
    }

    fun drawPrimaryCanvas(world: Entity, x0: Int, y0: Int, x1: Int, y1: Int) {
        world.forAllComponentsInChildren(CanvasComponent::class, false) { comp ->
            if (comp.space == CanvasComponent.Space.CAMERA_SPACE) {
                comp.width = x1 - x0
                comp.height = y1 - y0
                comp.render(this)
                val texture = comp.framebuffer!!.getTexture0()
                drawTexture(x0, y0, x1 - x0, y1 - y0, texture, -1, null)
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

    val fsr22 by lazy { FSR2v2() }

    fun drawScene(
        x0: Int, y0: Int, x1: Int, y1: Int,
        renderer: Renderer, buffer: IFramebuffer
    ) {

        val w = renderSize.renderWidth
        val h = renderSize.renderHeight

        when (renderMode) {
            RenderMode.LIGHT_COUNT -> {
                val lightBuffer = if (buffer == buffers.base1Buffer) buffers.light1Buffer else buffers.lightNBuffer1
                DebugRendering.drawLightCount(
                    this, w, h, x0, y0, x1, y1,
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
                timeRendering("Scene", DebugRendering.drawSceneTimer) {
                    drawScene(w, h, renderer, buffer, changeSize = true, hdr = false, sky = true)
                }
                timeRendering("Gizmos", DebugRendering.drawGizmoTimer) {
                    drawGizmos(buffer, true)
                }
                timeRendering("Final", DebugRendering.drawFinalTimer) {
                    drawTexture(x0, y0, x1 - x0, y1 - y0, buffer.getTexture0(), true, -1, null)
                }
            }
        }
    }

    fun resolveClick(px: Float, py: Float): Pair<Entity?, Component?> {
        val world = getWorld()
        if (world != null) {

            // refill is necessary when shadows are calculated,
            // because that overrides clickIds
            pipeline.clear()
            pipeline.fill(world)


            val diameter = 5

            val px2 = px.toInt() - x
            val py2 = py.toInt() - y

            // must be exactly RGBA x UNSIGNED_BYTE for WebGL
            val idBuffer = FBStack["click", width, height, TargetType.UInt8x4, 1, DepthBufferType.INTERNAL]
            val ids = Screenshots.getU8RGBAPixels(diameter, px2, py2, idBuffer, idRenderer) {
                GFXState.ditherMode.use(DitherMode.DITHER2X2) {
                    idBuffer.clearColor(0, true)
                    drawScene(width, height, idRenderer, idBuffer, changeSize = false, hdr = false, sky = false)
                }
            }

            for (idx in ids.indices) {
                ids[idx] = ids[idx] and 0xffffff
            }

            // must be exactly RED x FLOAT for WebGL
            val depthBuffer = FBStack["click", width, height, TargetType.Float32x1, 1, DepthBufferType.INTERNAL]
            val depths = Screenshots.getFP32RPixels(diameter, px2, py2, depthBuffer, depthRenderer) {
                GFXState.ditherMode.use(DitherMode.DITHER2X2) {
                    depthBuffer.clearColor(0, true)
                    drawScene(width, height, depthRenderer, depthBuffer, changeSize = false, hdr = false, sky = false)
                }
            }

            val clickedIdBGR = Screenshots.getClosestId(
                diameter, ids, depths,
                if (inverseDepth != supportsClipControl) -10 else +10
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

    private fun findFOV(camera: Camera): Float {
        return if (camera.isPerspective) camera.fovY else camera.fovOrthographic * 0.5f
    }

    fun prepareDrawScene(
        width: Int, height: Int, aspectRatio: Float, camera: Camera, update: Boolean,
        fillPipeline: Boolean
    ) {

        val world = getWorld()

        // must be called before we define our render settings
        // so lights don't override our settings, or we'd have to repeat our definition
        validateTransform(world)

        near = camera.near
        far = camera.far
        val isPerspective = camera.isPerspective
        val fov = findFOV(camera)

        // this needs to be separate from the stack
        // (for normal calculations and such)
        if (isPerspective) {
            val centerX = camera.center.x
            val centerY = camera.center.y
            setPerspectiveCamera(fov, aspectRatio, centerX, centerY)
        } else {
            setOrthographicCamera(fov, aspectRatio)
        }

        if (renderMode.renderGraph?.nodes?.any2 { it is FSR2Node } == true) {
            fsr22.jitter(cameraMatrix, width, height)
        } else if (renderMode.renderGraph?.nodes?.any2 { it is TAANode } == true) {
            TAANode.jitterAndStore(cameraMatrix, width, height)
        }

        if (!cameraMatrix.isFinite) {
            cameraMatrix.identity()
            LOGGER.warn("Set matrix to identity, because it was non-finite! $cameraMatrix")
        }

        val t1 = camera.entity?.transform?.getValidDrawMatrix()

        val camRot = cameraRotation
        if (t1 != null) {
            t1.getUnnormalizedRotation(camRot)
            if (!camRot.isFinite) camRot.identity()
        } else camRot.identity()

        // rotate camera matrix
        cameraMatrix.rotateInv(camRot)
        // calculate camera direction at the center of the screen
        camRot.transform(cameraDirection.set(0f, 0f, -1f)).safeNormalize()

        // calculate inverse camera matrix
        cameraMatrix.invert(cameraMatrixInv)

        if (t1 != null) t1.getTranslation(cameraPosition)
        else cameraPosition.set(0.0)

        // camera matrix and mouse position to ray direction
        if (update) {
            val window = window
            if (window != null) {
                updateMouseRay(window.mouseX, window.mouseY)
            } else {
                mousePosition.set(cameraPosition)
                mouseDirection.set(cameraDirection)
            }
        }

        currentInstance = this

        if (fillPipeline) {
            OnBeforeDraw.onBeforeDrawing()
            definePipeline(width, height, aspectRatio, fov, world)
        }
    }

    fun setPerspectiveCamera(fov: Float, aspectRatio: Float, centerX: Float, centerY: Float) {
        val fovYRadians = fov.toRadians()
        this.fovXRadians = 2f * atan(tan(fovYRadians * 0.5f) * aspectRatio)
        this.fovYRadians = fovYRadians
        fovXCenter = 0.5f + centerX // correct???
        fovYCenter = 0.5f + centerY // correct???
        Perspective.setPerspective(
            cameraMatrix, fovYRadians, aspectRatio,
            near, far, centerX, centerY
        )
    }

    fun setOrthographicCamera(fov: Float, aspectRatio: Float) {
        val n = near
        val f = min(far, n * 1e7f)
        fovXRadians = fov * aspectRatio
        fovYRadians = fov // not really defined
        fovXCenter = 0.5f
        fovYCenter = 0.5f
        val sceneScaleXY = 1f / fov
        val sceneScaleZ = 1f / (f - n)
        val inverseDepth = inverseDepth
        val m22 = 2f * if (inverseDepth) +sceneScaleZ else -sceneScaleZ
        val z0 = if (inverseDepth) -f * sceneScaleZ else -n * sceneScaleZ
        cameraMatrix.set(
            height * sceneScaleXY / width, 0f, 0f, 0f,
            0f, sceneScaleXY, 0f, 0f,
            0f, 0f, m22, 0f,
            0f, 0f, z0 * 2f - 1f, 1f
        )
    }

    fun validateTransform(world: PrefabSaveable?) {
        if (world is Entity) {
            world.validateTransform()
        }
    }

    fun definePipeline(
        width: Int, height: Int, aspectRatio: Float,
        fov: Float, world: PrefabSaveable?
    ) {
        pipeline.clear()
        if (isPerspective) {
            pipeline.frustum.definePerspective(
                near, far, fovYRadians,
                height, aspectRatio, cameraPosition,
                cameraRotation,
            )
        } else {
            pipeline.frustum.defineOrthographic(
                fov, aspectRatio, near, far, width,
                cameraPosition, cameraRotation
            )
        }

        pipeline.disableReflectionCullingPlane()
        pipeline.ignoredEntity = null
        setRenderState() // needed for light matrix calculation (camSpaceToLightSpace)
        if (world != null) pipeline.fill(world)
        controlScheme?.fill(pipeline)
        // if the scene would be dark, define lights, so we can see something
        addDefaultLightsIfRequired(pipeline, world, this)
    }

    private val inverseDepth
        get() = renderMode == RenderMode.INVERSE_DEPTH

    val depthMode: DepthMode
        get() {
            val base = when (renderMode) {
                RenderMode.NO_DEPTH -> DepthMode.ALWAYS
                RenderMode.INVERSE_DEPTH -> DepthMode.FAR
                else -> DepthMode.CLOSE
            }
            return if (supportsClipControl && isPerspective) base
            else base.reversedMode
        }

    fun drawScene(
        w: Int, h: Int,
        renderer: Renderer,
        dst: IFramebuffer,
        changeSize: Boolean,
        hdr: Boolean,
        sky: Boolean = true
    ) {
        GFX.check()
        pipeline.applyToneMapping = !hdr
        useFrame(w, h, changeSize, dst, renderer) {

            Frame.bind()
            var depthMode = depthMode
            val stage0 = pipeline.stages.firstOrNull()
            if (renderMode == RenderMode.INVERSE_DEPTH) {
                depthMode = depthMode.reversedMode
            }
            GFXState.depthMode.use(depthMode) {
                stage0?.depthMode = depthMode
                dst.clearDepth()
            }

            if (pipeline.defaultStage.blendMode != null) {
                dst.clearColor(0)
            }

            GFX.check()
            var skyI = sky
            if (skyI && renderMode == RenderMode.NO_DEPTH) {
                pipeline.drawSky() // sky must be drawn first in this mode
                skyI = false
            }
            pipeline.singlePassWithSky(skyI)
            GFX.check()
        }
    }

    fun drawSceneLights(deferred: IFramebuffer, deferredDepth: Texture2D, depthMask: Vector4f, dst: IFramebuffer) {
        useFrame(deferred.width, deferred.height, true, dst, copyRenderer) {
            dst.clearColor(0)
            pipeline.lightStage.bindDraw(pipeline, deferred, deferredDepth, depthMask)
        }
    }

    fun drawGizmos(
        framebuffer: IFramebuffer,
        drawGridLines: Boolean,
        drawDebug: Boolean = true
    ) {
        useFrame(framebuffer, simpleRenderer) {
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

        val cameraMatrix = RenderState.cameraMatrix
        val world = getWorld()
        stack.set(cameraMatrix)

        controlScheme?.drawGizmos()

        // much faster than depthTraversal, because we only need visible elements anyway
        if (world != null && drawAABBs) {
            pipeline.traverse(world) { entity ->
                val aabb1 = entity.getGlobalBounds()
                val hit1 = aabb1.testLine(mousePosition, mouseDirection, 1e10)
                drawAABB(aabb1, if (hit1) aabbColorHovered else aabbColorDefault)
                if (entity.hasRenderables) {
                    entity.forAllComponents(Renderable::class, false) { component ->
                        val aabb2 = component.getGlobalBounds()
                        if (aabb2 != null) {
                            val hit2 = aabb2.testLine(mousePosition, mouseDirection, 1e10)
                            drawAABB(aabb2, if (hit2) aabbColorHovered else aabbColorDefault)
                        }
                    }
                }
                LineBuffer.drawIf1M(cameraMatrix)
            }
        }

        // traverse over visible & selected
        for (component in EditorState.selection) {
            if (component is Component && component.isEnabled && component is OnDrawGUI) {
                val entity = component.entity
                if (entity == null || pipeline.frustum.contains(entity.getGlobalBounds())) {

                    val stack = stack
                    if (entity != null) {
                        val transform = entity.transform
                        val globalTransform = transform.globalTransform
                        stack.pushMatrix()
                        stack.mul4x3delta(globalTransform)
                    }

                    component.onDrawGUI(pipeline, true)

                    if (entity != null) {
                        stack.popMatrix()
                    }

                    LineBuffer.drawIf1M(cameraMatrix)
                }
            }
        }

        if (world is Component && world is OnDrawGUI && world !in EditorState.selection) {
            world.onDrawGUI(pipeline, world.isSelectedIndirectly)
        }

        val systems = Systems.readonlySystems
        for (i in systems.indices) {
            val system = systems[i] as? OnDrawGUI ?: continue
            system.onDrawGUI(pipeline, system is Inspectable && system in EditorState.selection)
        }

        drawGrid(pipeline, drawGridMask)

        if (drawDebugShapes) DebugRendering.drawDebugShapes(this, cameraMatrix)
        DebugShapes.removeExpired()

        LineBuffer.finish(cameraMatrix)
    }

    /**
     * get the mouse direction from this camera
     * todo for other cameras: can be used for virtual mice
     * */
    fun getMouseRayDirection(mx: Float, my: Float, dst: Vector3f): Vector3f {
        val rx = (mx - x) / width * 2.0 - 1.0
        val ry = (my - y) / height * 2.0 - 1.0
        return getRelativeMouseRayDirection(rx, -ry, dst)
    }

    fun getMouseRayPosition(mx: Float, my: Float, dst: Vector3d): Vector3d {
        val rx = (mx - x) / width * 2.0 - 1.0
        val ry = (my - y) / height * 2.0 - 1.0
        return getRelativeMouseRayPosition(rx, -ry, dst)
    }

    fun getRelativeMouseRayDirection(
        rx: Double, // -1 .. 1
        ry: Double, // -1 .. 1
        dst: Vector3f = Vector3f()
    ): Vector3f {
        return if (isPerspective) {
            val z = if (depthMode.reversedDepth) 1f else -1f
            cameraMatrixInv
                .transformProject(rx.toFloat(), ry.toFloat(), z, dst)
                .safeNormalize()
        } else {
            // todo is this the correct way? should be cameraRotationInv...
            cameraRotation.transform(0f, 0f, -1f, dst)
        }
    }

    fun getRelativeMouseRayPosition(rx: Double, ry: Double, dst: Vector3d): Vector3d {
        return if (isPerspective) {
            dst.set(cameraPosition)
        } else {
            val tmp = Vector3f()
            // todo is this the correct way?
            cameraMatrixInv.transformDirection(tmp.set(rx, ry, 0.0))
            dst.set(tmp).add(cameraPosition)
        }
    }

    private fun updateMouseRay(x: Float, y: Float) {
        getMouseRayPosition(x, y, mousePosition)
        getMouseRayDirection(x, y, mouseDirection)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        updateMouseRay(x, y)
        super.onMouseMoved(x, y, dx, dy)
    }

    fun rayQuery(maxDistance: Double = 1e9): RayQuery {
        return RayQuery(mousePosition, Vector3d(mouseDirection), maxDistance)
    }

    var fovXRadians = 1f
    var fovYRadians = 1f
    var fovXCenter = 0.5f
    var fovYCenter = 0.5f

    var near = 1e-3f
    var far = 1e10f

    val isPerspective: Boolean
        get() = abs(cameraMatrix.m33 - 1f) > 1e-5f

    val cameraMatrix = Matrix4f()
    val cameraMatrixInv = Matrix4f()
    val cameraPosition = Vector3d()
    val cameraDirection = Vector3f()
    val cameraRotation = Quaternionf()
    val mousePosition = Vector3d()
    val mouseDirection = Vector3f()

    val prevCamMatrix = Matrix4f()
    val prevCamMatrixInv = Matrix4f()
    val prevCamPosition = Vector3d()
    val prevCamRotation = Quaternionf()

    fun updatePrevState() {
        prevCamMatrix.set(cameraMatrix)
        prevCamMatrixInv.set(cameraMatrixInv)
        prevCamPosition.set(cameraPosition)
        prevCamRotation.set(cameraRotation)
    }

    fun setRenderState() {
        RenderState.aspectRatio = width.toFloat() / height.toFloat()

        RenderState.cameraMatrix.set(cameraMatrix)
        RenderState.cameraMatrixInv.set(cameraMatrixInv)
        RenderState.cameraPosition.set(cameraPosition)
        RenderState.cameraRotation.set(cameraRotation)
        RenderState.calculateDirections(isPerspective, false)

        RenderState.prevCameraMatrix.set(prevCamMatrix)
        RenderState.prevCameraPosition.set(prevCamPosition)
        RenderState.prevCameraRotation.set(prevCamRotation)

        RenderState.fovXRadians = fovXRadians
        RenderState.fovYRadians = fovYRadians
        RenderState.fovXCenter = fovXCenter
        RenderState.fovYCenter = fovYCenter
        RenderState.near = near
        RenderState.far = far

        pipeline.superMaterial = superMaterial.material
    }

    companion object {

        private val LOGGER = LogManager.getLogger(RenderView::class)

        /**
         * maximum number of lights used for forward rendering
         * todo forward plus rendering?
         * */
        val MAX_FORWARD_LIGHTS get() = if (GFXFeatures.isOpenGLES) 8 else 32

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
                    is Entity -> world.getGlobalBounds()
                    is Component -> {
                        val bounds = AABBd()
                        world.fillSpace(Matrix4x3(), bounds)
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
                        transform.setLocalPosition(bounds.centerX, bounds.centerY, bounds.centerZ)
                        transform.setLocalScale(3f / max(bounds.deltaX, max(bounds.deltaY, bounds.deltaZ)).toFloat())
                        transform.teleportUpdate()
                        transform.validate()
                        defaultSun.onUpdate()
                        defaultSun.rootOverride = null
                        rv.setRenderState() // camera position needs to be reset
                    }
                }
                defaultSun.fill(pipeline, defaultSunEntity.transform)
            }
        }
    }
}