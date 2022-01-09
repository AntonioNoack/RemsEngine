package me.anno.engine.ui.render

import me.anno.Build
import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.white4
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.LightComponentBase
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.light.PointLight.Companion.cubeMesh
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshBaseComponent
import me.anno.ecs.components.physics.BulletPhysics
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.shaders.effects.Bloom
import me.anno.ecs.components.shaders.effects.FSR
import me.anno.ecs.components.shaders.effects.ScreenSpaceAmbientOcclusion
import me.anno.ecs.components.shaders.effects.ScreenSpaceReflections
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.debug.DebugShapes.debugLines
import me.anno.engine.debug.DebugShapes.debugPoints
import me.anno.engine.debug.DebugShapes.debugRays
import me.anno.engine.gui.PlaneShapes
import me.anno.engine.pbr.DeferredRenderer
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.DefaultSun.defaultSun
import me.anno.engine.ui.render.DefaultSun.defaultSunEntity
import me.anno.engine.ui.render.DrawAABB.drawAABB
import me.anno.engine.ui.render.ECSShaderLib.clearingPbrModelShader
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.MovingGrid.drawGrid
import me.anno.engine.ui.render.Outlines.drawOutline
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.Renderers.cheapRenderer
import me.anno.engine.ui.render.Renderers.frontBackRenderer
import me.anno.engine.ui.render.Renderers.overdrawRenderer
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.OpenGL
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DepthBasedAntiAliasing
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures.drawProjection
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.*
import me.anno.gpu.pipeline.LightPipelineStage
import me.anno.gpu.pipeline.M4x3Delta.mul4x3delta
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.BaseShader.Companion.cullFaceColoringGeometry
import me.anno.gpu.shader.BaseShader.Companion.lineGeometry
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Renderer.Companion.depthRenderer
import me.anno.gpu.shader.Renderer.Companion.idRenderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.input.Input.isKeyDown
import me.anno.input.Input.isShiftDown
import me.anno.ui.base.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.style.Style
import me.anno.utils.Clock
import me.anno.utils.Tabs
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.mix
import me.anno.utils.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.AABBs
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.joml.Math.toRadians
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL45.*
import kotlin.math.max
import kotlin.math.tan


// todo buttons:
// todo start game -> always in new tab, so we can make changes while playing
// todo stop game
// todo restart game (just re-instantiate the whole scene)


// todo create the different pipeline stages: opaque, transparent, post-processing, ...

// todo render the grid slightly off position, so we don't get flickering, always closer to the camera, proportional to radius
// (because meshes at 0 are very common and to be expected)

// done shadows
// todo usable editing of materials: own color + indent + super material selector
// todo + add & remove materials

// todo import unity scenes

// optional, expensive cubic texture filtering? via custom shaders


// todo clickable & draggable gizmos, e.g. for translation, rotation scaling:
// todo thick arrows for 1d, and planes for 2d, and a center cube for 3d

// todo drag stuff onto surfaces using raytracing
// todo or drag and keep it in the same distance

// done render in different modes: overdraw, color blindness, normals, color, before-post-process, with-post-process
// done nice ui for that: drop down menus at top or bottom

// todo blend between multiple cameras, only allow 2? yes :)


// todo easily allow for multiple players in the same instance, with just player key mapping
// -> camera cannot be global, or todo it must be switched whenever the player changes

// todo reinhard tonemapping works often, but not always: {0,1}³ does not work, add spilling somehow
// todo also maybe it should be customizable...


// todo we could do the blending of the scenes using stencil tests <3 (very efficient)
//  - however it would limit us to a single renderer...
// -> first just draw a single scene and later todo make it multiplayer

class RenderView(
    val library: EditorState,
    val mode: Mode,
    style: Style
) : Panel(style) {

    enum class Mode {
        EDITING,
        PLAY_TESTING,
        PLAYING
    }

    enum class RenderMode(val dlt: DeferredLayerType? = null) {
        DEFAULT,
        WITHOUT_POST_PROCESSING,
        CLICK_IDS,
        DEPTH,
        FORCE_DEFERRED,
        FORCE_NON_DEFERRED,
        ALL_DEFERRED_LAYERS,
        ALL_DEFERRED_BUFFERS,

        COLOR(DeferredLayerType.COLOR),
        NORMAL(DeferredLayerType.NORMAL),
        EMISSIVE(DeferredLayerType.EMISSIVE),
        ROUGHNESS(DeferredLayerType.ROUGHNESS),
        METALLIC(DeferredLayerType.METALLIC),
        POSITION(DeferredLayerType.POSITION),
        TRANSLUCENCY(DeferredLayerType.TRANSLUCENCY),
        OCCLUSION(DeferredLayerType.OCCLUSION),
        SHEEN(DeferredLayerType.SHEEN),
        ANISOTROPY(DeferredLayerType.ANISOTROPIC),

        // ALPHA, // currently not defined
        LIGHT_SUM, // todo implement dust-light-spilling for impressive fog
        LIGHT_COUNT,

        SSAO,
        SS_REFLECTIONS,

        INVERSE_DEPTH,
        OVERDRAW, // todo overdraw seems to be missing all cubes... why?
        MSAA_X8,
        WITH_PRE_DRAW_DEPTH,
        MONO_WORLD_SCALE,
        GHOSTING_DEBUG,
        FSR_SQRT2,
        FSR_X2,
        FSR_X4,
        NEAREST_X4,
        LINES, FRONT_BACK,

        SHOW_AABB,
        PHYSICS,

        RAY_TEST,

    }

    private var bloomStrength = 0.5f
    private var bloomOffset = 10f
    private val useBloom get() = bloomOffset > 0f && renderMode != RenderMode.WITHOUT_POST_PROCESSING

    private val ssaoRadius = 0.2f // 0.1 of world size looks pretty good :)
    private val ssaoSamples get() = max(1, DefaultConfig["gpu.ssao.samples", 128])
    private val ssaoStrength get() = max(1e-3f, DefaultConfig["gpu.ssao.strength", 1f])

    var controlScheme: ControlScheme? = null

    // can exist (game/game mode), but does not need to (editor)
    // todo in the editor it becomes the prefab for a local player -> ui shall always be placed in the local player
    var localPlayer: LocalPlayer? = null

    var editorCamera = CameraComponent()
    val editorCameraNode = Entity(editorCamera)

    val isFinalRendering get() = mode != Mode.EDITING

    var renderMode = RenderMode.DEFAULT

    var radius = 50.0
    val worldScale get() = if (renderMode == RenderMode.MONO_WORLD_SCALE) 1.0 else 1.0 / radius
    var position = Vector3d()
    var rotation = Vector3d(-20.0, 0.0, 0.0)

    private val deferredRenderer = DeferredRenderer
    private val deferred = deferredRenderer.deferredSettings!!

    private val baseNBuffer = deferred.createBaseBuffer()
    private val baseSameDepth = baseNBuffer.attachFramebufferToDepth(1, false)
    private val base1Buffer = Framebuffer("debug", 1, 1, 1, 4, false, DepthBufferType.TEXTURE)

    // val lightBuffer = deferred.createLightBuffer()
    // val lightBuffer = baseBuffer.attachFramebufferToDepth(1, deferred.settingsV1.fpLights)//deferred.createLightBuffer()
    // no depth is currently required on this layer
    private val lightBuffer = Framebuffer("lights", w, h, 1, 1, deferred.settingsV1.fpLights, DepthBufferType.NONE)

    fun updateEditorCameraTransform() {

        val radius = radius
        val camera = editorCamera
        val cameraNode = editorCameraNode
        cameraNode.transform.localRotation = rotation.toQuaternionDegrees(JomlPools.quat4d.borrow())
        camera.far = 1e300
        camera.near = if (renderMode == RenderMode.DEPTH) {
            0.2
        } else {
            if (reverseDepth) radius * 1e-10
            else radius * 1e-2
        }

        val rotation = cameraNode.transform.localRotation

        if (!position.isFinite) {
            LOGGER.warn("Invalid position $position")
            Thread.sleep(100)
        }
        if (!rotation.isFinite) {
            LOGGER.warn("Invalid rotation $rotation")
            Thread.sleep(100)
        }

        val tmp3d = JomlPools.vec3d.borrow()
        cameraNode.transform.localPosition = rotation.transform(tmp3d.set(0.0, 0.0, radius)).add(position)
        cameraNode.validateTransform()

        // println(cameraNode.transform.localTransform)

    }

    override fun tickUpdate() {
        super.tickUpdate()
        // we could optimize that: if not has updated in some time, don't redraw
        invalidateDrawing()
    }

    private val clock = Clock()
    private var lastPhysics: BulletPhysics? = null

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        clock.start()

        // to see ghosting, if there is any
        if (renderMode == RenderMode.GHOSTING_DEBUG) Thread.sleep(250)

        updateEditorCameraTransform()

        val world = getWorld()
        if (world is Entity) {
            if (lastPhysics != world.physics) {
                library.syncMaster.nextSession()
                lastPhysics = world.physics
            }
        }

        if (isKeyDown(GLFW.GLFW_KEY_PAUSE)) {
            getWorld().simpleTraversal(false) {
                if (it is Entity && it.hasComponentInChildren(MeshBaseComponent::class)) {
                    val transform = it.transform
                    println("${Tabs.spaces(2 * it.depthInHierarchy)}'${it.name}':\n${transform.localTransform}\n${transform.globalTransform}")
                }
                false
            }
        }

        // done go through the rendering pipeline, and render everything

        // todo draw all local players in their respective fields
        // todo use customizable masks for the assignment (plus a button mapping)
        // todo if a color doesn't appear on the mapping, it doesn't need to be drawn
        // todo more important players can have a larger field
        // todo slope of these partial windows can be customized for nicer looks

        // localPlayer = world.localPlayers.children.firstOrNull() as? LocalPlayer

        // todo find which sections shall be rendered for what camera
        val camera = localPlayer?.camera?.currentCamera ?: editorCamera
        if (localPlayer == null) updateEditorCameraTransform()

        val showIds = renderMode == RenderMode.CLICK_IDS
        val showOverdraw = renderMode == RenderMode.OVERDRAW
        val showSpecialBuffer = showIds || showOverdraw || isKeyDown('j')
        var useDeferredRendering = when (renderMode) {
            RenderMode.DEFAULT,
            RenderMode.CLICK_IDS,
            RenderMode.DEPTH,
            RenderMode.FSR_X4, RenderMode.FSR_SQRT2,
            RenderMode.FSR_X2, RenderMode.NEAREST_X4,
            RenderMode.GHOSTING_DEBUG,
            RenderMode.INVERSE_DEPTH,
            RenderMode.WITHOUT_POST_PROCESSING,
            RenderMode.LINES, RenderMode.FRONT_BACK,
            RenderMode.MSAA_X8 -> false
            else -> true
        }

        stage0.blendMode = if (showOverdraw) BlendMode.ADD else null
        stage0.sorting = Sorting.NO_SORTING
        /*if (isShiftDown) Sorting.FRONT_TO_BACK
        else if (isControlDown) Sorting.BACK_TO_FRONT
        else Sorting.NO_SORTING*/

        var aspect = w.toFloat() / h

        val layers = deferred.settingsV1.layers
        val size = when (renderMode) {
            RenderMode.ALL_DEFERRED_BUFFERS -> layers.size + 1
            RenderMode.ALL_DEFERRED_LAYERS -> deferred.layerTypes.size// + 1
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

        stage0.cullMode = if (isFinalRendering) GL_BACK else 0

        clock.stop("initialization", 0.05)

        prepareDrawScene(w, h, aspect, camera, camera, 1f, true)
        if (pipeline.hasTooManyLights() || useBloom) useDeferredRendering = true

        clock.stop("preparing", 0.05)

        var renderer = when (renderMode) {
            RenderMode.OVERDRAW -> overdrawRenderer
            RenderMode.CLICK_IDS -> idRenderer
            RenderMode.FORCE_DEFERRED, RenderMode.SSAO, RenderMode.SS_REFLECTIONS -> {
                useDeferredRendering = true
                DeferredRenderer
            }
            RenderMode.FORCE_NON_DEFERRED, RenderMode.MSAA_X8, RenderMode.LINES -> {
                useDeferredRendering = false
                pbrRenderer
            }
            RenderMode.FRONT_BACK -> {
                useDeferredRendering = false
                frontBackRenderer
            }
            else -> if (useDeferredRendering)
                DeferredRenderer else pbrRenderer
        }

        if (renderMode.dlt != null) {
            useDeferredRendering = true
            renderer = attributeRenderers[renderMode.dlt!!]!!
        }

        val buffer = when {
            renderer == DeferredRenderer -> baseNBuffer
            renderMode == RenderMode.MSAA_X8 -> FBStack["", w, h, 4, false, 8, true]
            else -> base1Buffer
        }

        drawScene(
            x0, y0, x1, y1, camera, renderer,
            buffer, useDeferredRendering,
            size, cols, rows, layers.size
        )

        if (showSpecialBuffer) {
            DrawTexts.drawSimpleTextCharByChar(
                x + 20, y, 2,
                renderMode.name
            )
        }

        if (useDeferredRendering) {
            DrawTexts.drawSimpleTextCharByChar(
                x + w, y, 2,
                "DEFERRED", AxisAlignment.MAX
            )
        }

        if (!isFinalRendering) {
            showShadowMapDebug()
            showCameraRendering(x0, y0, x1, y1)
        }

        // clock.total("drawing the scene", 0.1)

    }

    private fun drawScene(
        x0: Int, y0: Int, x1: Int, y1: Int,
        camera: CameraComponent,
        renderer: Renderer, buffer: Framebuffer,
        useDeferredRendering: Boolean,
        size: Int, cols: Int, rows: Int, layersSize: Int
    ) {

        var w = x1 - x0
        var h = y1 - y0

        when (renderMode) {
            RenderMode.FSR_SQRT2 -> {
                // 12/17 ~ 0.706 ~ sqrt 1/2
                w = w * 12 / 17
                h = h * 12 / 17
            }
            RenderMode.FSR_X2 -> {
                w /= 2
                h /= 2
            }
            RenderMode.FSR_X4, RenderMode.NEAREST_X4 -> {
                w /= 4
                h /= 4
            }
            else -> {
            }
        }

        // clock.stop("drawing scene", 0.05)

        var dstBuffer = buffer

        if (useDeferredRendering) {

            // bind all the required buffers: position, normal

            if (renderMode.dlt != null) {
                drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)
                GFX.copyNoAlpha(buffer)
                return
            }

            when (renderMode) {
                RenderMode.DEPTH -> {
                    drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)
                    drawTexture(x, y + h, w, -h, buffer.depthTexture!!, true, -1, null)
                    return
                }
                RenderMode.LIGHT_SUM -> {
                    drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)
                    val lightBuffer = lightBuffer
                    buffer.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    drawSceneLights(camera, camera, 1f, copyRenderer, buffer, lightBuffer)
                    drawTexture(x, y + h, w, -h, lightBuffer.getColor0(), true, -1, null)
                    return
                }
                RenderMode.LIGHT_COUNT -> {
                    val lightBuffer = lightBuffer
                    pipeline.lightPseudoStage.visualizeLightCount = true
                    drawSceneLights(camera, camera, 1f, copyRenderer, buffer, lightBuffer)
                    drawTexture(x, y + h, w, -h, lightBuffer.getColor0(), true, -1, null)
                    pipeline.lightPseudoStage.visualizeLightCount = false
                    return
                }
                RenderMode.SSAO -> {
                    // 0.1f as radius seems pretty ideal with our world scale :)
                    drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)
                    var strength = ssaoStrength
                    if (strength < 0.01f) strength = 1f
                    val ssao = ScreenSpaceAmbientOcclusion.compute(
                        buffer, deferred, cameraMatrix,
                        ssaoRadius, strength, ssaoSamples
                    )
                    drawTexture(x, y + h, w, -h, ssao, true, -1, null)
                    return
                }
                RenderMode.SS_REFLECTIONS -> {
                    drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)
                    val lightBuffer = lightBuffer
                    buffer.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    drawSceneLights(camera, camera, 1f, copyRenderer, buffer, lightBuffer)
                    val illuminated = FBStack["ill", w, h, 4, true, 1, false]
                    useFrame(illuminated, copyRenderer) {
                        // apply lighting; don't write depth
                        OpenGL.depthMask.use(false) {
                            val shader = LightPipelineStage.getPostShader(deferred)
                            shader.use()
                            shader.v1("applyToneMapping", !useBloom)
                            buffer.bindTextures(2, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                            whiteTexture.bind(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP) // ssao
                            lightBuffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                            flat01.draw(shader)
                        }
                    }
                    val result = ScreenSpaceReflections.compute(buffer, illuminated, deferred, cameraMatrix, true)
                    drawTexture(x, y + h, w, -h, result, true, -1, null)
                    return
                }
                RenderMode.ALL_DEFERRED_BUFFERS -> {

                    drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)
                    val lightBuffer = lightBuffer
                    buffer.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    drawSceneLights(camera, camera, 1f, copyRenderer, buffer, lightBuffer)

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
                            buffer.textures[index]
                        } else {
                            lightBuffer.textures[0]
                        }

                        // y flipped, because it would be incorrect otherwise
                        drawTexture(x02, y12, x12 - x02, y02 - y12, texture, true, -1, null)
                        DrawTexts.drawSimpleTextCharByChar(
                            x02, y02, 2,
                            texture.name
                        )

                    }
                    return
                }
                RenderMode.ALL_DEFERRED_LAYERS -> {

                    // instead of drawing the raw buffers, draw the actual layers (color,roughness,metallic,...)
                    // todo also draw light as last one

                    val tw = w / cols
                    val th = h / rows
                    val tmp = FBStack["tmp-layers", tw, th, 4, false, 1, true]
                    for (index in 0 until size) {

                        // rows x N field
                        val col = index % cols
                        val x02 = x + (x1 - x0) * (col + 0) / cols
                        // val x12 = x + (x1 - x0) * (col + 1) / cols
                        val row = index / cols
                        val y02 = y + (y1 - y0) * (row + 0) / rows
                        val y12 = y + (y1 - y0) * (row + 1) / rows

                        // draw the light buffer as the last stripe
                        val layer = deferred.layerTypes[index]
                        val layerRenderer = attributeRenderers[layer]!!

                        drawScene(tw, th, camera, camera, 1f, layerRenderer, tmp, false, !useDeferredRendering)

                        val texture = tmp.getColor0()
                        // y flipped, because it would be incorrect otherwise
                        drawTexture(x02, y12, tw, -th, texture, true, -1, null)
                        DrawTexts.drawSimpleTextCharByChar(
                            x02, y02, 2,
                            layer.name
                        )

                    }
                    return

                }
                else -> {

                    if (renderer != DeferredRenderer) {
                        drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)
                        drawTexture(x, y + h, w, -h, buffer.getColor0(), true, -1, null)
                        return
                    }

                    if (buffer != baseNBuffer) throw IllegalStateException("Expected baseBuffer, but got ${buffer.name} for $renderMode")

                    drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)
                    val lightBuffer = lightBuffer
                    buffer.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    drawSceneLights(camera, camera, 1f, copyRenderer, buffer, lightBuffer)

                    val ssao = ScreenSpaceAmbientOcclusion.compute(
                        buffer, deferred, cameraMatrix,
                        ssaoRadius, ssaoStrength, ssaoSamples
                    )

                    // todo calculate the colors via post processing
                    // todo this would also allow us to easier visualize all the layers

                    // use the existing depth buffer for the 3d ui
                    val dstBuffer0 = baseSameDepth

                    if (useBloom) {

                        val illuminated = FBStack["", w, h, 4, true, 1, false]
                        useFrame(illuminated, copyRenderer) {// apply post processing

                            val shader = LightPipelineStage.getPostShader(deferred)
                            shader.use()
                            shader.v1("applyToneMapping", false)
                            shader.v3("ambientLight", pipeline.ambient)

                            buffer.bindTextures(2, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                            ssao.bind(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                            lightBuffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

                            flat01.draw(shader)

                        }

                        // screen space reflections
                        val ssReflections = ScreenSpaceReflections.compute(
                            buffer, illuminated, deferred, cameraMatrix,
                            false
                        )// else illuminated.getColor0()

                        useFrame(w, h, true, dstBuffer0) {

                            // don't write depth
                            OpenGL.depthMask.use(false) {
                                Bloom.bloom(ssReflections, bloomOffset, bloomStrength, true)
                            }

                            // todo use msaa for gizmos
                            // or use anti-aliasing, that works on color edges
                            // and supports lines
                            drawGizmos(camPosition, true)
                            drawSelected()
                        }

                    } else {

                        useFrame(w, h, true, dstBuffer0) {

                            // don't write depth
                            OpenGL.depthMask.use(false) {

                                val shader = LightPipelineStage.getPostShader(deferred)
                                shader.use()
                                shader.v1("applyToneMapping", true)

                                buffer.bindTextures(2, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                                ssao.bind(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                                lightBuffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

                                flat01.draw(shader)

                            }

                            drawGizmos(camPosition, true)
                            drawSelected()

                        }

                    }

                    // anti-aliasing
                    dstBuffer = FBStack["RenderView-dst", w, h, 4, false, 1, false]
                    useFrame(w, h, true, dstBuffer) {
                        DepthBasedAntiAliasing.render(dstBuffer0.getColor0(), buffer.depthTexture!!)
                    }

                }
            }

            clock.stop("presenting deferred buffers", 0.1)

        } else {
            // supports bloom
            // screen-space reflections cannot be supported
            if (useBloom) {
                pipeline.applyToneMapping = false
                val tmp = FBStack["scene", w, h, 4, true, buffer.samples, true]
                drawScene(w, h, camera, camera, 1f, renderer, tmp, changeSize = true, true)
                useFrame(w, h, true, dstBuffer) {
                    Bloom.bloom(tmp.getColor0(), bloomOffset, bloomStrength, true)
                }
            } else {
                drawScene(w, h, camera, camera, 1f, renderer, buffer, changeSize = true, true)
            }
        }

        val useFSR = when (renderMode) {
            RenderMode.FSR_X2, RenderMode.FSR_SQRT2, RenderMode.FSR_X4 -> true
            else -> false
        }

        if (useFSR) {
            val flipY = isShiftDown // works without difference, so maybe it could be removed...
            val tw = x1 - x0
            val th = y1 - y0
            val tmp = FBStack["fsr", tw, th, 4, false, 1, false]
            useFrame(tmp) { FSR.upscale(dstBuffer.getColor0(), 0, 0, tw, th, flipY) }
            // afterwards sharpen
            FSR.sharpen(tmp.getColor0(), 0.5f, x, y, tw, th, flipY)
        } else {
            // we could optimize that one day, when the shader graph works
            GFX.copyNoAlpha(dstBuffer)
        }

    }

    private fun showShadowMapDebug() {
        // show the shadow map for debugging purposes
        val light = library.selection
            .filterIsInstance<Entity>()
            .mapNotNull { e ->
                e.getComponentsInChildren(LightComponentBase::class).firstOrNull {
                    if (it is LightComponent) it.hasShadow else true
                }
            }
            .firstOrNull()
        if (light != null) {
            val texture: ITexture2D? = when (light) {
                is LightComponent -> light.shadowTextures?.firstOrNull()?.depthTexture
                is EnvironmentMap -> light.texture?.textures?.firstOrNull()
                is PlanarReflection -> light.lastBuffer
                else -> null
            }
            // draw the texture
            when (texture) {
                is CubemapTexture -> {
                    val s = w / 4
                    drawProjection(x, y + s, s * 3 / 2, -s, texture, true, -1)
                }
                is ITexture2D -> {
                    if (isShiftDown && light is PlanarReflection) {
                        drawTexture(x, y + h, w, -h, texture, true, 0x33ffffff, null)
                    } else {
                        val s = w / 3
                        drawTexture(x, y + s, s, -s, texture, true, -1, null)
                    }
                }
            }
        }
    }

    private fun showCameraRendering(x0: Int, y0: Int, x1: Int, y1: Int) {
        val camera = library.selection
            .filterIsInstance<Entity>()
            .mapNotNull { e -> e.getComponentsInChildren(CameraComponent::class).firstOrNull() }
            .firstOrNull()
        if (camera != null && !isShiftDown) {
            // calculate size of sub camera
            val w = (x1 - x0 + 1) / 3
            val h = (y1 - y0 + 1) / 3
            val buffer = base1Buffer
            val renderer = pbrRenderer
            val useDeferredRendering = false
            prepareDrawScene(w, h, w.toFloat() / h, camera, camera, 0f, false)
            drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)
            drawTexture(x1 - w, y1, w, -h, buffer.getColor0(), true, -1, null)
            // prepareDrawScene needs to be reset afterwards, because we seem to have a kind-of-bug somewhere
            val camera2 = editorCamera
            prepareDrawScene(this.w, this.h, this.w.toFloat() / this.h, camera2, camera2, 0f, false)
        }
    }

    fun resolveClick(
        px: Float,
        py: Float
    ): Pair<Entity?, Component?> {

        // pipeline should already be filled
        val camera = editorCamera
        val buffer = FBStack["click", GFX.width, GFX.height, 4, false, 1, true]

        val diameter = 5

        val px2 = px.toInt() - x
        val py2 = py.toInt() - y

        val ids = Screenshots.getPixels(diameter, 0, 0, px2, py2, buffer, idRenderer) {
            drawScene(w, h, camera, camera, 0f, idRenderer, buffer, changeSize = false, true)
            drawGizmos(camPosition, false)
            controlScheme?.drawGizmos()
        }

        val depths = Screenshots.getPixels(diameter, 0, 0, px2, py2, buffer, depthRenderer) {
            drawScene(w, h, camera, camera, 0f, depthRenderer, buffer, changeSize = false, true)
            drawGizmos(camPosition, false)
            controlScheme?.drawGizmos()
        }

        val clickedId = Screenshots.getClosestId(diameter, ids, depths, if (reverseDepth) -10 else +10)
        val world = getWorld()
        val clicked = if (clickedId == 0 || world !is Entity) null
        else pipeline.findDrawnSubject(clickedId, world)
        // LOGGER.info("$clickedId -> $clicked")
        // val ids2 = world.getComponentsInChildren(MeshComponent::class, false).map { it.clickId }
        // LOGGER.info(ids2.joinToString())
        // LOGGER.info(clickedId in ids2)
        return Pair(clicked as? Entity, clicked as? Component)

    }

    private fun getWorld(): PrefabSaveable {
        return library.world
    }

    private val tmp4f = Vector4f()

    val pipeline = Pipeline(deferred)
    private val stage0 = PipelineStage(
        "default", Sorting.NO_SORTING, MAX_FORWARD_LIGHTS,
        null, DepthMode.GREATER, true, GL_BACK,
        pbrModelShader
    )

    init {
        pipeline.defaultStage = stage0
        pipeline.stages.add(stage0)
    }

    private var entityBaseClickId = 0

    private val tmpRot0 = Quaternionf()
    private val tmpRot1 = Quaternionf()
    private fun prepareDrawScene(
        width: Int,
        height: Int,
        aspectRatio: Float,
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
        update: Boolean
    ) {

        val world = getWorld()
        if (update && world is Entity) {
            world.invalidateVisibility()
        }

        val blend = clamp(blending, 0f, 1f).toDouble()
        val blendF = blend.toFloat()

        val near = mix(previousCamera.near, camera.near, blend)
        val far = mix(previousCamera.far, camera.far, blend)
        val fov = mix(previousCamera.fovY, camera.fovY, blending)
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

        val fovYRadians = toRadians(fov)
        Companion.fovYRadians = fovYRadians

        val centerX = mix(previousCamera.center.x, camera.center.x, blendF)
        val centerY = mix(previousCamera.center.x, camera.center.y, blendF)

        // this needs to be separate from the stack
        // (for normal calculations and such)
        Perspective.setPerspective(
            cameraMatrix,
            fovYRadians,
            aspectRatio,
            (near * worldScale).toFloat(),
            (far * worldScale).toFloat(),
            centerX, centerY
        )
        cameraMatrix.rotate(rot)
        if (!cameraMatrix.isFinite) throw RuntimeException(
            "camera matrix is NaN, " +
                    "by setPerspective, $fovYRadians, $aspectRatio, $near, $far, $worldScale, $rot"
        )

        // lerp the world transforms
        val camTransform = camTransform
        camTransform.set(previousCamera.entity!!.transform.globalTransform)
        camTransform.lerp(camera.entity!!.transform.globalTransform, blend)

        camTransform.transformPosition(camPosition.set(0.0))
        camInverse.set(camTransform).invert()
        camRotation.set(rotInv)

        camRotation.transform(camDirection.set(0.0, 0.0, -1.0))
        camDirection.normalize()

        // camera matrix and mouse position to ray direction
        if (update) {
            val window = window!!
            getMouseRayDirection(window.mouseX, window.mouseY, mouseDir)
        }

        // debugPoints.add(DebugPoint(Vector3d(camDirection).mul(20.0).add(camPosition), 0xff0000, -1))

        currentInstance = this

        if (update && world is Entity) {

            world.update()

            world.updateVisible()

            world.validateTransform()

        }

        if (world is Material && world.prefab?.source?.exists != true) {
            throw IllegalStateException("Material must have source")
        }

        pipeline.reset()
        pipeline.frustum.definePerspective(
            near, far, fovYRadians.toDouble(),
            width, height, aspectRatio.toDouble(),
            camPosition, camRotation,
        )
        pipeline.disableReflectionCullingPlane()
        pipeline.ignoredEntity = null
        pipeline.fill(world, camPosition, worldScale)
        if (pipeline.lightPseudoStage.size <= 0 && pipeline.ambient.dot(1f, 1f, 1f) <= 0f) {
            // define lights, so we can see something
            pipeline.ambient.set(0.5f)
            pipeline.lightPseudoStage.add(defaultSun, defaultSunEntity)
        }
        entityBaseClickId = pipeline.lastClickId

    }

    private val reverseDepth get() = renderMode != RenderMode.INVERSE_DEPTH

    private fun setClearDepth() {
        stage0.depthMode = depthMode
        pipeline.lightPseudoStage.depthMode = depthMode
        pipeline.stages.forEach { it.depthMode = depthMode }
    }

    private val depthMode get() = if (reverseDepth) DepthMode.GREATER else DepthMode.FORWARD_LESS

    private fun setClearColor(
        renderer: Renderer,
        previousCamera: CameraComponent, camera: CameraComponent, blending: Float,
        doDrawGizmos: Boolean
    ) {
        val useInverseTonemappedColor: Boolean = !doDrawGizmos
        if (renderer.isFakeColor) {
            glClearColor(0f, 0f, 0f, 0f)
        } else {
            val c = tmp4f
            c.set(previousCamera.clearColor).lerp(camera.clearColor, blending)
            if (useInverseTonemappedColor) {
                // inverse reinhard tonemapping
                glClearColor(c.x / (1f - c.x), c.y / (1f - c.y), c.z / (1f - c.z), 1f)
            } else {
                glClearColor(c.x, c.y, c.z, 1f)
            }
        }
    }

    private fun clearDeferred() {
        Frame.bind()
        OpenGL.blendMode.use(null) {
            OpenGL.depthMode.use(DepthMode.ALWAYS) {
                // don't write depth, only all buffers
                OpenGL.depthMask.use(false) {
                    // draw huge cube with default values for all buffers
                    val shader = clearingPbrModelShader.value
                    shader.use()
                    shader.m4x4("transform", cameraMatrix)
                    cubeMesh.draw(shader, 0)
                }
            }
        }
    }

    private fun drawScene(
        w: Int, h: Int,
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
        renderer: Renderer,
        dst: Framebuffer,
        changeSize: Boolean,
        doDrawGizmos: Boolean
    ) {

        GFX.check()

        val isDeferred = dst.targets.size > 1
        val specialClear = isDeferred && renderer === DeferredRenderer

        val preDrawDepth = renderMode == RenderMode.WITH_PRE_DRAW_DEPTH
        if (preDrawDepth) {
            useFrame(w, h, changeSize, dst, cheapRenderer) {

                Frame.bind()

                OpenGL.depthMode.use(depthMode) {
                    setClearDepth()
                    setClearColor(renderer, previousCamera, camera, blending, doDrawGizmos)
                    glClear(if (specialClear) GL_DEPTH_BUFFER_BIT else GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                }

                OpenGL.depthMode.use(depthMode) {
                    OpenGL.cullMode.use(GL_BACK) {
                        OpenGL.blendMode.use(null) {
                            stage0.draw(pipeline, cameraMatrix, camPosition, worldScale)
                        }
                    }
                }
                stage0.depthMode = DepthMode.EQUALS

            }
        }

        useFrame(w, h, changeSize, dst, renderer) {

            if (!preDrawDepth) {
                Frame.bind()
                OpenGL.depthMode.use(depthMode) {
                    setClearDepth()
                    setClearColor(renderer, previousCamera, camera, blending, doDrawGizmos)
                    glClear(if (specialClear) GL_DEPTH_BUFFER_BIT else GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                }
            }

            if (specialClear) clearDeferred()

            if (doDrawGizmos) {
                if (!renderer.isFakeColor && !isFinalRendering) {
                    useFrame(w, h, changeSize, dst, simpleNormalRenderer) {
                        drawGizmos(camPosition, true)
                        drawSelected()
                    }
                } else if (renderer == idRenderer) {
                    drawGizmos(camPosition, false)
                }
            }

            val canRenderDebug = Build.isDebug
            val renderNormals = canRenderDebug && renderMode == RenderMode.FRONT_BACK
            val renderLines = canRenderDebug && renderMode == RenderMode.LINES

            GFX.check()

            if (renderNormals || renderLines) {
                val shader = if (renderLines) lineGeometry else cullFaceColoringGeometry
                OpenGL.geometryShader.use(shader) {
                    pipeline.draw(cameraMatrix, camPosition, worldScale)
                }
            } else pipeline.draw(cameraMatrix, camPosition, worldScale)

            GFX.check()

        }

    }

    private fun drawSelected() {
        if (library.fineSelection.isEmpty() && library.selection.isEmpty()) return
        // draw scaled, inverted object (for outline), which is selected
        OpenGL.depthMode.use(depthMode) {
            OpenGL.cullMode.use(GL_FRONT) { // inverse cull mode
                for (selected in library.fineSelection) {
                    when (selected) {
                        is Entity -> drawOutline(selected, worldScale)
                        is MeshBaseComponent -> {
                            val mesh = selected.getMesh() ?: continue
                            drawOutline(selected, mesh, worldScale)
                        }
                        is Component -> drawOutline(selected.entity ?: continue, worldScale)
                    }
                }
            }
        }
    }

    private fun drawSceneLights(
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
        renderer: Renderer,
        src: Framebuffer,
        dst: Framebuffer
    ) {

        useFrame(w, h, true, dst, renderer) {

            Frame.bind()

            tmp4f.set(previousCamera.clearColor).lerp(camera.clearColor, blending)

            glClearColor(0f, 0f, 0f, 0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            pipeline.lightPseudoStage.bindDraw(src, cameraMatrix, camPosition, worldScale)

        }

    }

    private val selectedColor = Vector4f(1f, 1f, 0.7f, 1f)

    private fun drawGizmos(camPosition: Vector3d, drawGridLines: Boolean) {

        // draw UI

        // now works, after making the physics async :)
        // maybe it just doesn't work with the physics debugging together
        val drawAABBs = renderMode == RenderMode.SHOW_AABB

        OpenGL.blendMode.use(BlendMode.DEFAULT) {
            OpenGL.depthMode.use(depthMode) {

                stack.set(cameraMatrix)

                Companion.worldScale = worldScale

                val maximumCircleDistance = 200f
                val maxCircleLenSq = sq(maximumCircleDistance).toDouble()

                var clickId = entityBaseClickId
                val scaleV = JomlPools.vec3d.create()

                val world = getWorld()

                // much faster than depthTraversal, because we only need visible elements anyways
                pipeline.traverse(world) { entity ->

                    val transform = entity.transform
                    val globalTransform = transform.globalTransform

                    val doDrawCircle = camPosition.distanceSquared(
                        globalTransform.m30(),
                        globalTransform.m31(),
                        globalTransform.m32()
                    ) < maxCircleLenSq

                    val nextClickId = clickId++
                    GFX.drawnId = nextClickId
                    entity.clickId = nextClickId

                    val stack = stack
                    stack.pushMatrix()
                    stack.mul4x3delta(globalTransform, camPosition, worldScale)

                    // only draw the circle, if its size is larger than ~ a single pixel
                    if (doDrawCircle) {
                        scale = globalTransform.getScale(scaleV).dot(0.3, 0.3, 0.3)
                        // val ringColor = if (entity == EditorState.lastSelection) selectedColor else white4
                        // PlaneShapes.drawCircle(globalTransform, ringColor.toARGB())
                        // Transform.drawUICircle(stack, 0.5f / scale.toFloat(), 0.7f, ringColor)
                    }

                    val components = entity.components
                    for (i in components.indices) {
                        val component = components[i]
                        if (component.isEnabled && component !is MeshBaseComponent) {
                            // mesh components already got their id
                            val componentClickId = clickId++
                            component.clickId = componentClickId
                            GFX.drawnId = componentClickId
                            component.onDrawGUI()
                        }
                    }

                    stack.popMatrix()

                    if (drawAABBs) {
                        val aabb = entity.aabb
                        if (AABBs.testLineAABB(aabb, camPosition, mouseDir, 1e10))
                        // if (aabb.testRay(Rayd(camPosition, ControlScheme.mouseDir)))
                            drawAABB(aabb, worldScale, 1.0, 1.0, 1.0)
                        else
                            drawAABB(aabb, worldScale, 1.0, 0.7, 0.7)

                    }

                }

                JomlPools.vec3d.sub(1)

                if (drawGridLines) {
                    drawGrid(radius, worldScale)
                }

                drawDebug()

                LineBuffer.finish(cameraMatrix)
                PlaneShapes.finish()

            }
        }

    }

    private fun drawDebug() {
        val worldScale = worldScale
        val points = debugPoints
        val lines = debugLines
        val rays = debugRays
        val camPosition = camPosition
        for (index in points.indices) {
            val point = points[index]
            // visualize a point
            drawDebugPoint(point.position, point.color)
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
            val length = radius * 100.0
            drawDebugPoint(pos, color)
            LineBuffer.putRelativeLine(
                pos,
                pos.x + dir.x * length,
                pos.y + dir.y * length,
                pos.z + dir.z * length,
                camPosition, worldScale,
                color
            )
        }
        val time = GFX.gameTime
        points.removeIf { it.timeOfDeath < time }
        lines.removeIf { it.timeOfDeath < time }
        rays.removeIf { it.timeOfDeath < time }
    }

    private fun drawDebugPoint(p: Vector3d, color: Int) {
        val d = p.distance(camPosition) * 0.01
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

    /**
     * get the mouse direction from this camera
     * todo for other cameras: can be used for virtual mice
     * */
    fun getMouseRayDirection(
        cx: Float = window!!.mouseX,
        cy: Float = window!!.mouseY,
        dst: Vector3d = Vector3d()
    ): Vector3d {
        val rx = (cx - x) / w * +2f - 1f
        val ry = (cy - y) / h * -2f + 1f
        val tan = tan(fovYRadians * 0.5f)
        val aspectRatio = w.toFloat() / h
        val dir = dst.set((rx * tan * aspectRatio).toDouble(), (ry * tan).toDouble(), -1.0)
        camRotation.transform(dir)
        dir.normalize()
        return dst
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        getMouseRayDirection(x, y, JomlPools.vec3d.create())
        super.onMouseMoved(x, y, dx, dy)
    }

    companion object {

        private val LOGGER = LogManager.getLogger(RenderView::class)

        /**
         * maximum number of lights used for forward rendering
         * when this number is surpassed, the engine switches to deferred rendering automatically
         * */
        val MAX_FORWARD_LIGHTS = 32

        var scale = 1.0
        var worldScale = 1.0
        val stack = Matrix4fArrayList()

        var fovYRadians = 1f

        val cameraMatrix = Matrix4f()

        val camTransform = Matrix4x3d()
        val camInverse = Matrix4d()
        val camPosition = Vector3d()
        val camDirection = Vector3d()
        val camRotation = Quaterniond()
        val mouseDir = Vector3d()

        val scaledMin = Vector4d()
        val scaledMax = Vector4d()
        val tmpVec4f = Vector4d()

        lateinit var currentInstance: RenderView

    }

}