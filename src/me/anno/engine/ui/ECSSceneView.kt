package me.anno.engine.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.mesh.MeshRenderer
import me.anno.engine.ECSWorld
import me.anno.gpu.RenderState.renderDefault
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.ui.base.Panel
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.mix
import org.joml.*
import org.lwjgl.opengl.GL11.glClearColor

// todo controls
// todo show the scene
// todo drag stuff
// todo translate, rotate, scale with gizmos
// todo render in different modes: overdraw, color blindness, normals, color, before-post-process, with-post-process

// todo blend between multiple cameras, only allow 2? yes :)


// todo easily allow for multiple players in the same instance, with just player key mapping
// -> camera cannot be global, or todo it must be switched whenever the player changes

class ECSSceneView(val world: ECSWorld) : Panel(style) {

    // todo a custom state, which stores all related rendering information

    // can exist (game/game mode), but does not need to (editor)
    // todo in the editor it becomes the prefab for a local player -> ui shall always be placed in the local player
    var localPlayer: LocalPlayer? = null

    var editorCamera = CameraComponent()
    val editorCameraNode = Entity(editorCamera)

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        // todo go through the rendering pipeline, and render everything

        // todo draw all local players in their respective fields
        // todo use customizable masks for the assignment (plus a button mapping)
        // todo if a color doesn't appear on the mapping, it doesn't need to be drawn
        // todo more important players can have a larger field
        // todo slope of these partial windows can be customized for nicer looks

        localPlayer = world.localPlayers.children.firstOrNull() as? LocalPlayer

        // todo find which sections shall be rendered for what camera
        val camera = localPlayer?.camera?.currentCamera ?: editorCamera

        val buffer = FBStack["scene", w, h, 4, false, 1]
        drawScene(w / 2f, h / 2f, w, h, camera, camera, 1f, buffer)


    }

    val stack = Matrix4dArrayList()
    val viewTransform = Matrix4d()

    val camTransformTmp = Matrix4x3d()
    val camInverseTmp = Matrix4d()
    val tmp3 = Vector3d()
    val tmp4f = Vector4f()

    // todo we could do the blending of the scenes using stencil tests <3 (very efficient)
    //  - however it would limit us to a single renderer...
    // todo -> first just draw a single scene and later make it multiplayer
    fun drawScene(
        centerX: Float,
        centerY: Float,
        w: Int,
        h: Int,
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
        dst: Framebuffer
    ) {

        // todo blending
        // todo align the camera perfectly

        val blend = clamp(blending, 0f, 1f).toDouble()
        val near = mix(previousCamera.near, camera.near, blend)
        val far = mix(previousCamera.far, camera.far, blend)

        stack.clear()

        // this needs to be separate from the stack
        // (for normal calculations and such)
        viewTransform.identity().perspective(
            1.5,
            w.toDouble() / h,
            near, far
        )

        // lerp the world transforms
        val camTransform = camTransformTmp
        camTransform.set(previousCamera.entity!!.transform.globalTransform)
        camTransform.lerp(camera.entity!!.transform.globalTransform, blend)

        val cameraPosition = camTransform.transformPosition(tmp3.set(0.0))
        val camInverse = camInverseTmp.set(camTransform).invert()

        // todo create the different pipeline stages: opaque, transparent, post-processing, ...
        useFrame(dst) {
            renderDefault {
                Frame.bind()
                tmp4f.set(previousCamera.clearColor).lerp(camera.clearColor, blend.toFloat())
                glClearColor(tmp4f.x, tmp4f.y, tmp4f.z, 1f)
                drawScene(viewTransform, camInverse, world, null)
            }
        }

    }

    fun drawScene(cam: Matrix4d, camInverse: Matrix4d, entity: Entity, parent: Entity?) {

        val transform = entity.transform
        if (transform.needsGlobalUpdate) {
            transform.update(parent?.transform)
        }

        val renderer = entity.getComponent<MeshRenderer>()
        if (renderer != null) {
            // todo render the mesh
            val meshes = renderer.meshes
            for (mesh in meshes) {

            }
        }

        // todo only if there is a flag, that says, that there is renderable stuff inside
        for (child in entity.children) {
            drawScene(cam, camInverse, child, entity)
        }

    }

}