package me.anno.engine.ui.render

import me.anno.config.DefaultStyle.white4
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.LightComponentBase
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.shaders.effects.Bloom
import me.anno.ecs.components.shaders.effects.FSR
import me.anno.engine.debug.DebugShapes.debugLines
import me.anno.engine.debug.DebugShapes.debugPoints
import me.anno.engine.debug.DebugShapes.debugRays
import me.anno.engine.gui.PlaneShapes
import me.anno.engine.pbr.DeferredRenderer
import me.anno.engine.ui.ECSTypeLibrary
import me.anno.engine.ui.ECSTypeLibrary.Companion.lastSelection
import me.anno.engine.ui.render.DrawAABB.drawAABB
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.MovingGrid.drawGrid
import me.anno.engine.ui.render.Outlines.drawOutline
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.Renderers.cheapRenderer
import me.anno.engine.ui.render.Renderers.overdrawRenderer
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.RenderState
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DepthBasedAntiAliasing
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures.drawProjection
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.*
import me.anno.gpu.pipeline.M4x3Delta.mul4x3delta
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineLightStage
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
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isKeyDown
import me.anno.input.Input.isShiftDown
import me.anno.studio.Build
import me.anno.ui.base.Panel
import me.anno.ui.editor.sceneView.Gizmos
import me.anno.ui.style.Style
import me.anno.utils.Clock
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.mix
import me.anno.utils.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.joml.Math.toRadians
import org.lwjgl.opengl.GL45.*

// done shadows
// todo usable editing of materials: own color + indent + super material selector
// todo + add & remove materials

// todo import unity scenes

// todo optional, expensive cubic texture filtering?

// todo drag assets into the scene
// todo drag materials onto mesh components

// todo optional blender like controls?


// todo clickable & draggable gizmos, e.g. for translation, rotation scaling:
// todo thick arrows for 1d, and planes for 2d, and a center cube for 3d

// todo drag stuff onto surfaces using raytracing
// todo or drag and keep it in the same distance

// todo controls
// done show the scene
// todo drag stuff
// todo translate, rotate, scale with gizmos

// done render in different modes: overdraw, color blindness, normals, color, before-post-process, with-post-process
// todo nice ui for that: drop down menues at top or bottom

// todo blend between multiple cameras, only allow 2? yes :)


// todo easily allow for multiple players in the same instance, with just player key mapping
// -> camera cannot be global, or todo it must be switched whenever the player changes

// todo reinhard tonemapping works often, but not always: {0,1}³ does not work, add spilling somehow
// todo also maybe it should be customizable...

class RenderView(
    val library: ECSTypeLibrary,
    val mode: Mode,
    style: Style
) : Panel(style) {

    enum class Mode {
        EDITING,
        PLAY_TESTING,
        PLAYING
    }
    // to do scene scale, which is premultiplied with everything to allow stuff outside the 1e-38 - 1e+38 range?
    // not really required, since our universe just has a scale of 1e-10 (~ size of an atom) - 1e28 (~ size of the observable universe) meters


    // todo buttons:
    // todo start game -> always in new tab, so we can make changes while playing
    // todo stop game
    // todo restart game (just re-instantiate the whole scene)


    // todo create the different pipeline stages: opaque, transparent, post-processing, ...


    // can exist (game/game mode), but does not need to (editor)
    // todo in the editor it becomes the prefab for a local player -> ui shall always be placed in the local player
    var localPlayer: LocalPlayer? = null

    var editorCamera = CameraComponent()
    val editorCameraNode = Entity(editorCamera)

    var isFinalRendering = false

    var selectedAttribute = 0

    // todo different control schemes of the camera like in MagicaVoxel
    var radius = 50.0
    val worldScale get() = if (isKeyDown('f')) 1.0 else 1.0 / radius
    var position = Vector3d()
    var rotation = Vector3d(-20.0, 0.0, 0.0)

    init {
        updateTransform()
    }

    var deferredRenderer = DeferredRenderer
    val deferred = deferredRenderer.deferredSettings!!

    val baseBuffer = deferred.createBaseBuffer()
    val baseSameDepth = baseBuffer.attachFramebufferToDepth(1, false)

    // val lightBuffer = deferred.createLightBuffer()
    // val lightBuffer = baseBuffer.attachFramebufferToDepth(1, deferred.settingsV1.fpLights)//deferred.createLightBuffer()
    // no depth is currently required on this layer
    val lightBuffer = Framebuffer("lights", w, h, 1, 1, deferred.settingsV1.fpLights, DepthBufferType.NONE)

    val showOverdraw get() = isKeyDown('n')

    fun updateTransform() {

        val radius = radius
        val camera = editorCamera
        val cameraNode = editorCameraNode
        cameraNode.transform.localRotation = rotation.toQuaternionDegrees(JomlPools.quat4d.borrow())
        camera.far = 1e300
        camera.near = if (reverseDepth) radius * 1e-10 else radius * 1e-2

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

        // println(cameraNode.transform.localTransform)

        cameraNode.validateTransforms()

    }

    override fun tickUpdate() {
        super.tickUpdate()
        // we could optimize that: if not has updated in some time, don't redraw
        invalidateDrawing()
    }

    val clock = Clock()

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        clock.start()

        // to see ghosting
        // currently I see no ghosting...
        if (isKeyDown('v')) Thread.sleep(250)


        // todo go through the rendering pipeline, and render everything

        // todo draw all local players in their respective fields
        // todo use customizable masks for the assignment (plus a button mapping)
        // todo if a color doesn't appear on the mapping, it doesn't need to be drawn
        // todo more important players can have a larger field
        // todo slope of these partial windows can be customized for nicer looks

        // localPlayer = world.localPlayers.children.firstOrNull() as? LocalPlayer

        // todo find which sections shall be rendered for what camera
        val camera = localPlayer?.camera?.currentCamera ?: editorCamera
        if (localPlayer == null) {
            // todo calculate camera location
            updateTransform()
        }

        val showIds = isKeyDown('g')
        val showOverdraw = showOverdraw
        val showSpecialBuffer = showIds || showOverdraw || isKeyDown('j')
        var useDeferredRendering = !showSpecialBuffer && isKeyDown('k')
        val samples = if (isKeyDown('p')) 8 else 1

        stage0.blendMode = if (showOverdraw) BlendMode.ADD else null
        stage0.sorting = if (isShiftDown) Sorting.FRONT_TO_BACK
        else if (isControlDown) Sorting.BACK_TO_FRONT
        else Sorting.NO_SORTING

        var aspect = w.toFloat() / h

        val layers = deferred.settingsV1.layers
        val size = layers.size + 1
        val rows = when {
            size % 2 == 0 -> 2
            size % 3 == 0 -> 3
            size > 12 -> 3
            size > 6 -> 2
            else -> 1
        }
        val cols = (size + rows - 1) / rows
        val debugDeferredRendering = useDeferredRendering && isShiftDown
        if (debugDeferredRendering) {
            aspect *= rows.toFloat() / cols.toFloat()
        }

        stage0.cullMode = if (isFinalRendering) GL_BACK else 0

        clock.stop("initialization", 0.05)

        prepareDrawScene(w / 2f, h / 2f, w, h, aspect, camera, camera, 1f)
        if (pipeline.hasTooManyLights()) useDeferredRendering = true

        clock.stop("preparing", 0.05)

        val renderer = when {
            showOverdraw -> overdrawRenderer
            showIds -> idRenderer
            showSpecialBuffer -> attributeRenderers[selectedAttribute]
            useDeferredRendering -> deferredRenderer
            else -> pbrRenderer
        }

        val buffer = if (useDeferredRendering) baseBuffer else FBStack["scene", w, h, 4, false, samples]

        drawScene(
            x0, y0, x1, y1, camera, renderer,
            buffer, useDeferredRendering, debugDeferredRendering,
            size, cols, rows, layers.size
        )

        // todo draw gui on top of the buffer
        // todo copy the depth information, or keep it there from the start

        if (showSpecialBuffer) {
            DrawTexts.drawSimpleTextCharByChar(
                x, y, 2,
                if (showIds) "IDs" else DeferredLayerType.values()[selectedAttribute].glslName
            )
        }

        if (!isFinalRendering) {
            showShadowMapDebug()
        }

        // clock.total("drawing the scene", 0.1)

    }

    private fun drawScene(
        x0: Int, y0: Int, x1: Int, y1: Int,
        camera: CameraComponent,
        renderer: Renderer, buffer: Framebuffer,
        useDeferredRendering: Boolean,
        debugDeferredRendering: Boolean,
        size: Int, cols: Int, rows: Int, layersSize: Int
    ) {

        var w = x1 - x0
        var h = y1 - y0

        val useFSR = isKeyDown('f')
        if (useFSR) {
            w = w * 2 / 3
            h = h * 2 / 3
        }

        drawScene(w, h, camera, camera, 1f, renderer, buffer, true, !useDeferredRendering)

        // clock.stop("drawing scene", 0.05)

        var dstBuffer = buffer

        if (useDeferredRendering) {

            // bind all the required buffers: position, normal
            val lightBuffer = lightBuffer
            baseBuffer.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

            // todo for the scene, the environment map and shadow cascades need to be updated,
            // todo and used in the calculation
            drawSceneLights(camera, camera, 1f, copyRenderer, buffer, lightBuffer)

            // todo draw scene depth with msaa


            // clock.stop("drawing lights", 0.1)

            if (debugDeferredRendering) {

                useFrame(dstBuffer) {
                    for (index in 0 until size) {

                        // rows x N field
                        val col = index % cols
                        val x02 = (x1 - x0) * (col + 0) / cols
                        val x12 = (x1 - x0) * (col + 1) / cols
                        val row = index / cols
                        val y02 = (y1 - y0) * (row + 0) / rows
                        val y12 = (y1 - y0) * (row + 1) / rows

                        // draw the light buffer as the last stripe
                        val texture = if (index < layersSize) {
                            buffer.textures[index]
                        } else {
                            lightBuffer.textures[0]
                        }

                        // todo instead of drawing the raw buffers, draw the actual layers (color,roughness,metallic,...)

                        // y flipped, because it would be incorrect otherwise
                        drawTexture(x02, y12, x12 - x02, y02 - y12, texture, true, -1, null)
                        DrawTexts.drawSimpleTextCharByChar(
                            x02, y02, 2,
                            texture.name
                        )

                    }
                }

            } else {

                // todo calculate the colors via post processing
                // todo this would also allow us to easier visualize all the layers

                // todo post processing could do screen space reflections :)


                val bloomStrength = 0.5f
                val bloomOffset = 10f

                val useBloom = bloomOffset > 0f

                // use the existing depth buffer for the 3d ui
                val dstBuffer0 = baseSameDepth

                if (useBloom) {

                    val tmp = FBStack["", w, h, 4, true, 1]
                    useFrame(tmp, copyRenderer) {// apply post processing

                        val shader = PipelineLightStage.getPostShader(deferred)
                        shader.use()
                        shader.v1("applyToneMapping", false)

                        lightBuffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                        buffer.bindTextures(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

                        flat01.draw(shader)

                    }

                    useFrame(w, h, true, dstBuffer0) {

                        // don't write depth
                        RenderState.depthMask.use(false) {
                            Bloom.bloom(tmp.getColor0(), bloomOffset, bloomStrength, true)
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
                        RenderState.depthMask.use(false) {
                            val shader = PipelineLightStage.getPostShader(deferred)
                            shader.use()
                            shader.v1("applyToneMapping", !useBloom)

                            lightBuffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                            buffer.bindTextures(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

                            flat01.draw(shader)
                        }

                        drawGizmos(camPosition, true)
                        drawSelected()

                    }

                }

                // anti-aliasing
                dstBuffer = FBStack["dst", w, h, 4, false, 1]
                useFrame(w, h, true, dstBuffer) {
                    DepthBasedAntiAliasing.render(dstBuffer0.getColor0(), buffer.depthTexture!!)
                }

            }

            clock.stop("presenting deferred buffers", 0.1)

        }

        if (useFSR) {
            val flipY = isShiftDown
            val tw = x1 - x0
            val th = y1 - y0
            val tmp = FBStack["fsr", tw, th, 4, false, 1]
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
                is LightComponent -> light.shadowTextures?.getOrNull(selectedAttribute)?.depthTexture
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

    fun resolveClick(
        px: Float,
        py: Float
    ): Pair<Entity?, Component?> {

        // pipeline should already be filled
        val camera = editorCamera
        val buffer = FBStack["click", GFX.width, GFX.height, 4, false, 1]

        val diameter = 5

        val px2 = px.toInt() - x
        val py2 = py.toInt() - y

        val ids = Screenshots.getPixels(diameter, 0, 0, px2, py2, buffer, idRenderer) {
            drawScene(w, h, camera, camera, 0f, idRenderer, buffer, changeSize = false, true)
            drawGizmos(camPosition, false)
        }

        val depths = Screenshots.getPixels(diameter, 0, 0, px2, py2, buffer, depthRenderer) {
            drawScene(w, h, camera, camera, 0f, depthRenderer, buffer, changeSize = false, true)
            drawGizmos(camPosition, false)
        }

        val clickedId = Screenshots.getClosestId(diameter, ids, depths, if (reverseDepth) -10 else +10)
        val clicked = if (clickedId == 0) null else pipeline.findDrawnSubject(clickedId, getWorld())
        // LOGGER.info("$clickedId -> $clicked")
        // val ids2 = world.getComponentsInChildren(MeshComponent::class, false).map { it.clickId }
        // LOGGER.info(ids2.joinToString())
        // LOGGER.info(clickedId in ids2)
        return Pair(clicked as? Entity, clicked as? Component)

    }

    private fun getWorld(): Entity {
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

    var entityBaseClickId = 0

    private val tmpRot0 = Quaternionf()
    private val tmpRot1 = Quaternionf()
    private fun prepareDrawScene(
        centerX: Float,
        centerY: Float,
        width: Int,
        height: Int,
        aspectRatio: Float,
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
    ) {

        val world = getWorld()
        world.invalidateVisibility()

        val blend = clamp(blending, 0f, 1f).toDouble()
        val blendFloat = blend.toFloat()

        val near = mix(previousCamera.near, camera.near, blend)
        val far = mix(previousCamera.far, camera.far, blend)
        val fov = mix(previousCamera.fovY, camera.fovY, blending)
        val t0 = previousCamera.entity!!.transform.globalTransform
        val t1 = camera.entity!!.transform.globalTransform
        val rot0 = t0.getUnnormalizedRotation(tmpRot0)
        val rot1 = t1.getUnnormalizedRotation(tmpRot1)

        if (!rot0.isFinite) rot0.identity()
        if (!rot1.isFinite) rot1.identity()

        val rotInv = rot0.slerp(rot1, blendFloat)
        val rot = rot1.set(rotInv).conjugate() // conjugate is quickly inverting, when already normalized

        val fovYRadians = toRadians(fov)

        // this needs to be separate from the stack
        // (for normal calculations and such)
        Perspective.setPerspective(
            cameraMatrix,
            fovYRadians,
            aspectRatio,
            (near * worldScale).toFloat(),
            (far * worldScale).toFloat()
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
        // debugPoints.add(DebugPoint(Vector3d(camDirection).mul(20.0).add(camPosition), 0xff0000, -1))

        world.update()

        world.updateVisible()

        world.getComponentsInChildren(PlanarReflection::class) {
            pipeline.reset()
            it.draw(pipeline, w, h, cameraMatrix, camPosition, camRotation, worldScale) { pos, rot ->
                pipeline.frustum.definePerspective(
                    near, far, fovYRadians.toDouble(),
                    width, height, aspectRatio.toDouble(),
                    pos, rot
                )
            }
            false
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
        entityBaseClickId = pipeline.lastClickId

    }

    private val reverseDepth get() = !isKeyDown('r')

    private fun setClearDepth() {
        stage0.depthMode = depthMode
        pipeline.lightPseudoStage.depthMode = depthMode
        pipeline.stages.forEach { it.depthMode = depthMode }
    }

    val depthMode get() = if (reverseDepth) DepthMode.GREATER else DepthMode.FORWARD_LESS

    // todo we could do the blending of the scenes using stencil tests <3 (very efficient)
    //  - however it would limit us to a single renderer...
    // todo -> first just draw a single scene and later make it multiplayer

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

        val preDrawDepth = isKeyDown('b')

        if (preDrawDepth) {
            useFrame(w, h, changeSize, dst, cheapRenderer) {

                Frame.bind()

                RenderState.depthMode.use(depthMode) {
                    setClearColor(renderer, previousCamera, camera, blending, doDrawGizmos)
                    setClearDepth()
                    glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                }

                RenderState.depthMode.use(depthMode) {
                    RenderState.cullMode.use(GL_BACK) {
                        RenderState.blendMode.use(null) {
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

                RenderState.depthMode.use(depthMode) {
                    setClearColor(renderer, previousCamera, camera, blending, doDrawGizmos)
                    setClearDepth()
                    glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                }

            }

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
            val renderNormals = canRenderDebug && isKeyDown('n')
            val renderLines = canRenderDebug && isKeyDown('l')

            GFX.check()

            if (renderNormals || renderLines) {
                val shader = if (renderLines) lineGeometry else cullFaceColoringGeometry
                RenderState.geometryShader.use(shader) {
                    pipeline.draw(cameraMatrix, camPosition, worldScale)
                }
            } else pipeline.draw(cameraMatrix, camPosition, worldScale)

            GFX.check()

        }

    }

    private fun drawSelected() {
        if (library.fineSelection.isEmpty() && library.selection.isEmpty()) return
        // draw scaled, inverted object (for outline), which is selected
        RenderState.depthMode.use(depthMode) {
            RenderState.cullMode.use(GL_FRONT) { // inverse cull mode
                for (selected in library.selection) {
                    when (selected) {
                        is Entity -> {
                            // todo draw gizmos depending on mode
                            // println("drawing translate gizmos")
                            val transform = selected.transform.globalTransform
                            val pos = transform.getTranslation(JomlPools.vec3d.create()).sub(camPosition)
                            Gizmos.drawTranslateGizmos(
                                cameraMatrix,
                                pos,// mul world scale?
                                10.0, -12
                            )
                            JomlPools.vec3d.sub(1)
                        }
                        else -> {
                            LOGGER.info("todo draw selected: ${selected.javaClass.simpleName}")
                        }
                    }
                }
                for (selected in library.fineSelection) {
                    when (selected) {
                        is Entity -> drawOutline(selected, worldScale)
                        is MeshComponent -> {
                            val mesh = MeshCache[selected.mesh, false] ?: continue
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
        val drawAABBs = isKeyDown('o')

        RenderState.blendMode.use(BlendMode.DEFAULT) {
            RenderState.depthMode.use(depthMode) {

                stack.set(cameraMatrix)

                Companion.worldScale = worldScale

                val maximumCircleDistance = 200f
                val maxCircleLenSq = sq(maximumCircleDistance).toDouble()

                var clickId = entityBaseClickId
                val scaleV = JomlPools.vec3d.create()

                val world = getWorld()
                world.depthFirstTraversal(false) { entity ->

                    entity as Entity

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
                        val ringColor = if (entity == lastSelection) selectedColor else white4
                        // PlaneShapes.drawCircle(globalTransform, ringColor.toARGB())
                        // Transform.drawUICircle(stack, 0.5f / scale.toFloat(), 0.7f, ringColor)
                    }

                    val components = entity.components
                    for (i in components.indices) {
                        val component = components[i]
                        if (component !is MeshComponent) {
                            // mesh components already got their id
                            val componentClickId = clickId++
                            component.clickId = componentClickId
                            GFX.drawnId = componentClickId
                            component.onDrawGUI(this)
                        }
                    }

                    stack.popMatrix()

                    if (drawAABBs) drawAABB(entity, worldScale)

                    false
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
        val debugPoints = debugPoints
        val debugLines = debugLines
        val debugRays = debugRays
        for (point in debugPoints) {
            // visualize a point
            val delta = point.position.distance(camPosition) * 0.05
            LineBuffer.putRelativeLine(
                point.position, Vector3d(point.position).add(delta, 0.0, 0.0),
                camPosition, worldScale, point.color
            )
            LineBuffer.putRelativeLine(
                point.position, Vector3d(point.position).add(0.0, delta, 0.0),
                camPosition, worldScale, point.color
            )
            LineBuffer.putRelativeLine(
                point.position, Vector3d(point.position).add(0.0, 0.0, delta),
                camPosition, worldScale, point.color
            )
        }
        for (line in debugLines) {
            LineBuffer.putRelativeLine(
                line.p0, line.p1, camPosition, worldScale,
                line.color
            )
        }
        for (ray in debugRays) {
            LineBuffer.putRelativeLine(
                ray.start, Vector3d(ray.direction).mul(radius * 100.0).add(ray.start), camPosition, worldScale,
                ray.color
            )
        }
        val time = GFX.gameTime
        debugPoints.removeIf { it.timeOfDeath < time }
        debugLines.removeIf { it.timeOfDeath < time }
        debugRays.removeIf { it.timeOfDeath < time }
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

        val cameraMatrix = Matrix4f()

        val camTransform = Matrix4x3d()
        val camInverse = Matrix4d()
        val camPosition = Vector3d()
        val camDirection = Vector3d()
        val camRotation = Quaterniond()

        val scaledMin = Vector4d()
        val scaledMax = Vector4d()
        val tmpVec4f = Vector4d()

    }

}