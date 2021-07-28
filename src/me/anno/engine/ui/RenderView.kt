package me.anno.engine.ui

import me.anno.config.DefaultStyle.white4
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.player.LocalPlayer
import me.anno.engine.pbr.DeferredRenderer
import me.anno.engine.ui.ECSTypeLibrary.Companion.lastSelection
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.RenderState
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.ShaderLib.pbrModelShader
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.PipelineStage.Companion.mul4x3delta
import me.anno.gpu.shader.BaseShader.Companion.cullFaceColoringGeometry
import me.anno.gpu.shader.BaseShader.Companion.lineGeometry
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.input.Input
import me.anno.input.Input.mouseKeysDown
import me.anno.objects.Transform
import me.anno.studio.Build
import me.anno.ui.base.Panel
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.style.Style
import me.anno.utils.Clock
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.max
import me.anno.utils.Maths.mix
import me.anno.utils.Maths.pow
import me.anno.utils.Maths.sq
import me.anno.utils.types.AABBs.isEmpty
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.joml.*
import org.joml.Math.toRadians
import org.lwjgl.opengl.GL11.*

// todo click on stuff to select it


// todo controls
// todo show the scene
// todo drag stuff
// todo translate, rotate, scale with gizmos
// todo render in different modes: overdraw, color blindness, normals, color, before-post-process, with-post-process

// todo blend between multiple cameras, only allow 2? yes :)


// todo easily allow for multiple players in the same instance, with just player key mapping
// -> camera cannot be global, or todo it must be switched whenever the player changes

class RenderView(val world: Entity, val mode: Mode, style: Style) : Panel(style) {

    enum class Mode {
        EDITING,
        PLAY_TESTING,
        PLAYING
    }

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

    var renderer = DeferredRenderer
    val deferred = renderer.deferredSettings!!

    val lightBuffer = deferred.createLightBuffer()
    val baseBuffer = deferred.createBaseBuffer()

    val baseRenderer = object : Renderer(false, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): String {
            return "" +
                    // todo define all light positions, radii, types, and colors
                    // todo use the lights to illuminate the model
                    // todo maybe apply color map as a final step...
                    "void main(){\n" +
                    "   finalColor *= 0.6 - 0.4 * normalize(finalNormal).x;\n" +
                    "}"
        }
    }

    // todo different control schemes of the camera like in MagicaVoxel
    var radius = 50.0
    var position = Vector3d()
    var rotation = Vector3d()

    var movement = Vector3d()

    override fun tickUpdate() {
        super.tickUpdate()
        // we could optimize that: if not has updated in some time, don't redraw
        invalidateDrawing()
    }

    fun checkMovement() {
        val dt = GFX.deltaTime
        val factor = clamp(1.0 - 20.0 * dt, 0.0, 1.0)
        movement.mul(factor)
        val s = (1.0 - factor) * 0.035
        if (isInFocus) {
            if (Input.isKeyDown('a')) movement.x -= s
            if (Input.isKeyDown('d')) movement.x += s
            if (Input.isKeyDown('w')) movement.z -= s
            if (Input.isKeyDown('s')) movement.z += s
            if (Input.isKeyDown('q')) movement.y -= s
            if (Input.isKeyDown('e')) movement.y += s
        }
        val normXZ = !Input.isShiftDown // todo use UI toggle instead
        val rotQuad = rotation.toQuaternionDegrees()
        val right = rotQuad.transform(Vector3d(1.0, 0.0, 0.0))
        val forward = rotQuad.transform(Vector3d(0.0, 0.0, 1.0))
        val up = if (normXZ) {
            right.y = 0.0
            forward.y = 0.0
            right.normalize()
            forward.normalize()
            Vector3d(0.0, 1.0, 0.0)
        } else {
            rotQuad.transform(Vector3d(0.0, 1.0, 0.0))
        }
        position.x += movement.dot(right.x, up.x, forward.x) * radius
        position.y += movement.dot(right.y, up.y, forward.y) * radius
        position.z += movement.dot(right.z, up.z, forward.z) * radius
    }

    override fun onKeyDown(x: Float, y: Float, key: Int) {
        super.onKeyDown(x, y, key)
        invalidateDrawing()
    }

    override fun onKeyUp(x: Float, y: Float, key: Int) {
        super.onKeyUp(x, y, key)
        invalidateDrawing()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (1 in mouseKeysDown) {
            // right mouse key down -> move the camera
            val speed = -500f / max(GFX.height, h)
            rotation.x = clamp(rotation.x + dy * speed, -90.0, 90.0)
            rotation.y += dx * speed
            editorCameraNode.transform.localRotation = rotation.toQuaternionDegrees()
            invalidateDrawing()
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        val factor = pow(0.5f, (dx + dy) / 16f)
        radius *= factor
        editorCamera.far *= factor
        editorCamera.near *= factor
        invalidateDrawing()
    }

    val clock = Clock()

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        clock.start()

        checkMovement()

        clock.stop("movement", 0.01)

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
            val rotation = editorCameraNode.transform.localRotation
            editorCameraNode.transform.localPosition =
                Vector3d(position).add(rotation.transform(Vector3d(0.0, 0.0, radius)))
            editorCameraNode.validateTransforms()
        }

        val useDeferredRendering = Input.isKeyDown('k')
        val samples = if (Input.isKeyDown('p')) 8 else 1
        val buffer = if (useDeferredRendering) baseBuffer else FBStack["scene", w, h, 4, false, samples]

        // todo if not deferred, assign the lights to the shader...
        val renderer = if (useDeferredRendering) renderer else baseRenderer

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
        if (useDeferredRendering) {
            aspect *= rows.toFloat() / cols.toFloat()
        }

        stage0.cullMode = if (isFinalRendering) GL_BACK else 0

        clock.stop("initialization", 0.05)

        prepareDrawScene(w / 2f, h / 2f, w, h, aspect, camera, camera, 1f)

        clock.stop("preparing", 0.05)

        drawScene(camera, camera, 1f, renderer, buffer)

        clock.stop("drawing scene", 0.05)

        if (useDeferredRendering) {

            // bind all the required buffers: position, normal
            val lightBuffer = lightBuffer
            baseBuffer.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

            // todo for the scene, the environment map and shadow cascades need to be updated,
            // todo and used in the calculation
            drawSceneLights(camera, camera, 1f, Renderer.copyRenderer, lightBuffer)

            clock.stop("drawing lights")

            // todo calculate the colors via post processing
            // todo this would also allow us to easier visualize all the layers


            // todo post processing could do screen space reflections :)

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

                // y flipped, because it would be incorrect otherwise
                DrawTextures.drawTexture(x02, y12, x12 - x02, y02 - y12, texture, true, -1, null)

            }

            clock.stop("presenting deferred buffers", 0.1)


        } else {

            GFX.copy(buffer)

        }

        clock.total("drawing the scene", 0.1)

    }

    val tmp4f = Vector4f()

    val pipeline = Pipeline()
    val stage0 = PipelineStage(
        "default", PipelineStage.Sorting.NO_SORTING,
        null, DepthMode.LESS, true, GL_BACK,
        pbrModelShader
    )

    init {
        pipeline.defaultStage = stage0
        pipeline.stages.add(stage0)
    }

    fun prepareDrawScene(
        centerX: Float,
        centerY: Float,
        w: Int,
        h: Int,
        aspectRatio: Float,
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
    ) {

        val blend = clamp(blending, 0f, 1f).toDouble()
        val blendFloat = blend.toFloat()

        val near = mix(previousCamera.near, camera.near, blend)
        val far = mix(previousCamera.far, camera.far, blend)
        val fov = mix(previousCamera.fov, camera.fov, blending)
        val rot0 = previousCamera.entity!!.transform.globalTransform.getUnnormalizedRotation(Quaternionf())
        val rot1 = camera.entity!!.transform.globalTransform.getUnnormalizedRotation(Quaternionf())
        val rot2 = rot0.slerp(rot1, blendFloat)
        val rot = Quaternionf(rot2).conjugate() // conjugate is quickly inverting, when already normalized

        val fovYRadians = toRadians(fov)

        // this needs to be separate from the stack
        // (for normal calculations and such)
        viewTransform.identity().perspective(
            fovYRadians,
            aspectRatio,
            near.toFloat(), far.toFloat()
        )
        viewTransform.rotate(rot)

        // lerp the world transforms
        val camTransform = camTransform
        camTransform.set(previousCamera.entity!!.transform.globalTransform)
        camTransform.lerp(camera.entity!!.transform.globalTransform, blend)

        camTransform.transformPosition(camPosition.set(0.0))
        camInverse.set(camTransform).invert()

        pipeline.reset()
        pipeline.calculatePlanes(
            near, far,
            fovYRadians.toDouble(),
            aspectRatio.toDouble(),
            Quaterniond(rot2), //
            camPosition // camPosition
        )
        pipeline.fill(world)

    }

    // todo we could do the blending of the scenes using stencil tests <3 (very efficient)
    //  - however it would limit us to a single renderer...
    // todo -> first just draw a single scene and later make it multiplayer
    fun drawScene(
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
        renderer: Renderer,
        dst: Framebuffer
    ) {

        useFrame(w, h, true, dst, renderer) {

            Frame.bind()

            tmp4f.set(previousCamera.clearColor).lerp(camera.clearColor, blending)
            glClearColor(tmp4f.x, tmp4f.y, tmp4f.z, 1f)
            glClearDepth(1.0)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            drawGizmos(camPosition)

            val canRenderDebug = Build.isDebug
            val renderNormals = canRenderDebug && Input.isKeyDown('n')
            val renderLines = canRenderDebug && Input.isKeyDown('l')

            if (renderNormals || renderLines) {

                val shader = if (renderLines) lineGeometry else cullFaceColoringGeometry
                RenderState.geometryShader.use(shader) {
                    pipeline.draw(viewTransform, camPosition)
                }

            } else pipeline.draw(viewTransform, camPosition)

        }

    }

    fun drawSceneLights(
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
        renderer: Renderer,
        dst: Framebuffer
    ) {

        useFrame(w, h, true, dst, renderer) {

            Frame.bind()

            tmp4f.set(previousCamera.clearColor).lerp(camera.clearColor, blending)
            glClearColor(0f, 0f, 0f, 0f)
            glClearDepth(1.0)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            pipeline.lightPseudoStage.bindDraw(viewTransform, camPosition)

        }

    }

    val selectedColor = Vector4f(1f, 1f, 0.7f, 1f)

    fun drawGizmos(camPosition: Vector3d) {

        // draw UI
        if (!isFinalRendering) {

            // now works, after making the physics async :)
            // maybe it just doesn't work with the physics debugging together
            val drawAABBs = Input.isKeyDown('o')

            RenderState.blendMode.use(BlendMode.DEFAULT) {
                RenderState.depthMode.use(DepthMode.LESS) {

                    stack.set(viewTransform)

                    val maximumCircleDistance = 50f
                    val maxCircleLenSq = sq(maximumCircleDistance).toDouble()

                    world.simpleTraversal(false) { entity ->

                        entity as Entity

                        val transform = entity.transform
                        val globalTransform = transform.globalTransform

                        // only draw the circle, if its size is larger than ~ a single pixel
                        if (camPosition.distanceSquared(
                                globalTransform.m30(),
                                globalTransform.m31(),
                                globalTransform.m32()
                            ) < maxCircleLenSq
                        ) {

                            stack.pushMatrix()
                            stack.mul4x3delta(globalTransform, camPosition)

                            scale = globalTransform.getScale(Vector3d()).dot(0.3, 0.3, 0.3)
                            val ringColor = if (entity == lastSelection) selectedColor else white4
                            Transform.drawUICircle(stack, 0.2f / scale.toFloat(), 0.7f, ringColor)

                            val components = entity.components
                            for (i in components.indices) {
                                components[i].onDrawGUI()
                            }

                            stack.popMatrix()

                        }

                        if (drawAABBs) drawAABB(entity)

                        false
                    }

                    stack.pushMatrix()
                    stack.mul4x3delta(Matrix4x3d(), camPosition)

                    // todo move the grid
                    RenderState.blendMode.use(BlendMode.ADD) {
                        // draw grid
                        // scale it based on the radius (movement speed)
                        Grid.draw(stack, radius.toFloat())
                    }

                    stack.popMatrix()

                    if (drawAABBs) LineBuffer.finish(stack)

                }
            }
        }

    }

    fun drawAABB(entity: Entity) {

        // draw the aabb
        val aabb = entity.aabb
        if (aabb.isEmpty()) return

        // to do draw 3 main axis like bullet?

        val x = (aabb.minX - camPosition.x).toFloat()
        val y = (aabb.minY - camPosition.y).toFloat()
        val z = (aabb.minZ - camPosition.z).toFloat()

        val dx = (aabb.maxX - aabb.minX).toFloat()
        val dy = (aabb.maxY - aabb.minY).toFloat()
        val dz = (aabb.maxZ - aabb.minZ).toFloat()

        for (i in 0 until 4) {
            val a = ((i shr 0) and 1).toFloat() * dx
            val b = ((i shr 1) and 1).toFloat() * dy
            LineBuffer.addLine(
                Vector3f(x + a, y + b, z + 0f),
                Vector3f(x + a, y + b, z + dz),
                1.0, 0.0, 0.0
            )
        }

        for (i in 0 until 4) {
            val a = ((i shr 0) and 1).toFloat() * dx
            val b = ((i shr 1) and 1).toFloat() * dz
            LineBuffer.addLine(
                Vector3f(x + a, y + 0f, z + b),
                Vector3f(x + a, y + dy, z + b),
                1.0, 0.0, 0.0
            )
        }

        for (i in 0 until 4) {
            val a = ((i shr 0) and 1).toFloat() * dy
            val b = ((i shr 1) and 1).toFloat() * dz
            LineBuffer.addLine(
                Vector3f(x + 0f, y + a, z + b),
                Vector3f(x + dx, y + a, z + b),
                1.0, 0.0, 0.0
            )
        }

    }

    companion object {

        var scale = 1.0
        val stack = Matrix4fArrayList()

        val viewTransform = Matrix4f()

        val camTransform = Matrix4x3d()
        val camInverse = Matrix4d()
        val camPosition = Vector3d()

    }

}