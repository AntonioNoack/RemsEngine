package me.anno.engine.ui.render

import me.anno.config.DefaultStyle.white4
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.shaders.effects.Bloom
import me.anno.engine.debug.DebugPoint
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
import me.anno.gpu.texture.GPUFiltering
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isKeyDown
import me.anno.input.Input.isShiftDown
import me.anno.studio.Build
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import me.anno.utils.Clock
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.mix
import me.anno.utils.maths.Maths.sq
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.joml.*
import org.joml.Math.toRadians
import org.lwjgl.opengl.GL45.*

// todo shadows
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
// todo show the scene
// todo drag stuff
// todo translate, rotate, scale with gizmos
// todo render in different modes: overdraw, color blindness, normals, color, before-post-process, with-post-process

// todo blend between multiple cameras, only allow 2? yes :)


// todo easily allow for multiple players in the same instance, with just player key mapping
// -> camera cannot be global, or todo it must be switched whenever the player changes

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

    val lightBuffer = deferred.createLightBuffer()
    val baseBuffer = deferred.createBaseBuffer()

    val showOverdraw get() = isKeyDown('n')

    fun updateTransform() {

        val radius = radius
        val camera = editorCamera
        val cameraNode = editorCameraNode
        cameraNode.transform.localRotation = rotation.toQuaternionDegrees()
        camera.far = 1e300
        camera.near = if (isKeyDown('r')) radius * 1e-2 else radius * 1e-10

        val rotation = cameraNode.transform.localRotation

        if (!position.isFinite) {
            me.anno.utils.LOGGER.warn("Invalid position $position")
            Thread.sleep(100)
        }
        if (!rotation.isFinite) {
            me.anno.utils.LOGGER.warn("Invalid rotation $rotation")
            Thread.sleep(100)
        }

        cameraNode.transform.localPosition = Vector3d(position)
            .add(rotation.transform(Vector3d(0.0, 0.0, radius)))

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
        drawScene(w, h, camera, camera, 1f, renderer, buffer, true)

        // clock.stop("drawing scene", 0.05)

        if (useDeferredRendering) {

            // bind all the required buffers: position, normal
            val lightBuffer = lightBuffer
            baseBuffer.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

            // todo for the scene, the environment map and shadow cascades need to be updated,
            // todo and used in the calculation
            drawSceneLights(camera, camera, 1f, Renderer.copyRenderer, buffer, lightBuffer)

            // clock.stop("drawing lights", 0.1)

            if (debugDeferredRendering) {

                for (index in 0 until size) {

                    // rows x N field
                    val col = index % cols
                    val x02 = x0 + (x1 - x0) * (col + 0) / cols
                    val x12 = x0 + (x1 - x0) * (col + 1) / cols
                    val row = index / cols
                    val y02 = y0 + (y1 - y0) * (row + 0) / 2
                    val y12 = y0 + (y1 - y0) * (row + 1) / 2

                    // draw the light buffer as the last stripe
                    val texture = if (index < layers.size) {
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

            } else {

                // todo calculate the colors via post processing
                // todo this would also allow us to easier visualize all the layers

                // todo post processing could do screen space reflections :)

                val bloomStrength = 0.5f
                val bloomOffset = 10f

                val useBloom = bloomOffset > 0f

                if(useBloom){

                    val tmp = FBStack["",w,h,4,true,1]
                    useFrame(tmp, copyRenderer){

                        val shader = PipelineLightStage.getPostShader(deferred)
                        shader.use()
                        shader.v1("applyToneMapping", !useBloom)

                        lightBuffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                        buffer.bindTextures(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

                        flat01.draw(shader)

                    }

                    Bloom.bloom(tmp.getColor0(), bloomOffset, bloomStrength, true)

                } else {

                    val shader = PipelineLightStage.getPostShader(deferred)
                    shader.use()
                    shader.v1("applyToneMapping", !useBloom)

                    lightBuffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    buffer.bindTextures(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

                    flat01.draw(shader)

                }
            }

            clock.stop("presenting deferred buffers", 0.1)

        } else GFX.copyNoAlpha(buffer)

        if (showSpecialBuffer) {
            DrawTexts.drawSimpleTextCharByChar(
                x, y, 2,
                if (showIds) "IDs" else DeferredLayerType.values()[selectedAttribute].glslName
            )
        }

        if (!isFinalRendering) {
            // show the shadow map for debugging purposes
            val light = library.selected
                .filterIsInstance<Entity>()
                .mapNotNull { e -> e.getComponentsInChildren(LightComponent::class).firstOrNull { it.hasShadow } }
                .firstOrNull()
            if (light != null) {
                val textures = light.shadowTextures
                if (textures != null) {
                    // draw the texture
                    when (val fb = textures.getOrNull(selectedAttribute)) {
                        is Framebuffer -> {
                            val texture = fb.depthTexture
                            if (texture != null && texture.isCreated && !texture.isDestroyed) {
                                val s = w / 3
                                drawTexture(x, y + s, s, -s, texture, true, -1, null)
                            }
                        }
                        is CubemapFramebuffer -> {
                            val texture = fb.depthTexture
                            if (texture != null && texture.isCreated && !texture.isDestroyed) {
                                val s = w / 4
                                drawProjection(x, y + s, s * 3 / 2, -s, texture, true, -1)
                            }
                        }
                    }
                }
            }
        }

        // clock.total("drawing the scene", 0.1)

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
            drawScene(w, h, camera, camera, 0f, idRenderer, buffer, false)
            drawGizmos(camPosition, false)
        }

        val depths = Screenshots.getPixels(diameter, 0, 0, px2, py2, buffer, depthRenderer) {
            drawScene(w, h, camera, camera, 0f, depthRenderer, buffer, false)
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

    fun getWorld(): Entity {
        return library.world
    }

    val tmp4f = Vector4f()

    val pipeline = Pipeline(deferred)
    val stage0 = PipelineStage(
        "default", Sorting.NO_SORTING, MAX_LIGHTS,
        null, DepthMode.GREATER, true, GL_BACK,
        pbrModelShader
    )

    init {
        pipeline.defaultStage = stage0
        pipeline.stages.add(stage0)
    }

    var entityBaseClickId = 0

    fun prepareDrawScene(
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
        val rot0 = t0.getUnnormalizedRotation(Quaternionf())
        val rot1 = t1.getUnnormalizedRotation(Quaternionf())

        if (!rot0.isFinite) rot0.identity()
        if (!rot1.isFinite) rot1.identity()

        val rot2 = rot0.slerp(rot1, blendFloat)
        val rot = Quaternionf(rot2).conjugate() // conjugate is quickly inverting, when already normalized

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

        pipeline.reset()
        pipeline.frustum.definePerspective(
            near, far, fovYRadians.toDouble(),
            width, height, aspectRatio.toDouble(),
            camPosition, camRotation.set(rot2),
        )

        camRotation.transform(camDirection.set(0.0, 0.0, -1.0))
        debugPoints.add(DebugPoint(Vector3d(camDirection).mul(20.0).add(camPosition), 0xff0000, -1))

        world.update()
        world.updateVisible()

        pipeline.fill(world, camPosition, worldScale)
        entityBaseClickId = pipeline.lastClickId

    }

    val reverseDepth get() = !isKeyDown('r')

    fun setClearDepth() {
        stage0.depthMode = depthMode
        pipeline.lightPseudoStage.depthMode = depthMode
        pipeline.stages.forEach { it.depthMode = depthMode }
    }

    val depthMode get() = if (reverseDepth) DepthMode.GREATER else DepthMode.FORWARD_LESS

    // todo we could do the blending of the scenes using stencil tests <3 (very efficient)
    //  - however it would limit us to a single renderer...
    // todo -> first just draw a single scene and later make it multiplayer
    fun drawScene(
        w: Int, h: Int,
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
        renderer: Renderer,
        dst: Framebuffer,
        changeSize: Boolean
    ) {

        val preDrawDepth = isKeyDown('b')

        if (preDrawDepth) {
            useFrame(w, h, changeSize, dst, cheapRenderer) {

                Frame.bind()

                if (renderer.isFakeColor) {
                    glClearColor(0f, 0f, 0f, 0f)
                } else {
                    tmp4f.set(previousCamera.clearColor).lerp(camera.clearColor, blending)
                    glClearColor(tmp4f.x, tmp4f.y, tmp4f.z, 1f)
                }

                setClearDepth()
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

                RenderState.depthMode.use(DepthMode.GREATER) {
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

                if (renderer.isFakeColor) {
                    glClearColor(0f, 0f, 0f, 0f)
                } else {
                    tmp4f.set(previousCamera.clearColor).lerp(camera.clearColor, blending)
                    glClearColor(tmp4f.x, tmp4f.y, tmp4f.z, 1f)
                }

                setClearDepth()
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            }

            if (!renderer.isFakeColor && !isFinalRendering) {
                useFrame(w, h, changeSize, dst, simpleNormalRenderer) {
                    drawGizmos(camPosition, true)
                    drawSelected()
                }
            } else if (renderer == idRenderer) {
                drawGizmos(camPosition, false)
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

            // Thread.sleep(500)

        }

    }

    private fun drawSelected() {
        if (library.fineSelection.isEmpty()) return
        // draw scaled, inverted object (for outline), which is selected
        RenderState.depthMode.use(depthMode) {
            RenderState.cullMode.use(GL_FRONT) {
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

    fun drawSceneLights(
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
                val scaleV = Vector3d()

                val world = getWorld()
                world.simpleTraversal(false) { entity ->

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

                if (drawGridLines) {
                    drawGrid(radius, worldScale)
                }

                drawDebug()

                LineBuffer.finish(cameraMatrix)
                PlaneShapes.finish()

            }
        }

    }

    fun drawDebug() {
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

        val MAX_LIGHTS = 32

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