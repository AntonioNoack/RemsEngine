package me.anno.tests.promo

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.Events.addEvent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.sdf.shapes.SDFHyperBBox
import me.anno.tests.LOGGER
import me.anno.tests.shader.Snow.snowRenderMode
import me.anno.tests.shader.SnowLikeRain.rainRenderMode
import me.anno.ui.UIColors
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.types.Floats.toRadians

// todo: create a list of all visual effects, including images, so we can show them off a bit :)

// todo: create list of most samples with images
//  - start a sample,
//  - set camera angle / parameters right
//  - take a screenshot
//  - save it with appropriate name

// todo: run these samples on the web/with a space-optimized engine build

val width = 500
val height = 400

val sceneView by lazy {
    SceneView(PlayMode.EDITING, style)
}
val framebuffer = Framebuffer("promo", width, height, 1, TargetType.UInt8x4, DepthBufferType.NONE)
val dst = desktop.getChild("Promo")

// todo implement VR for the editor start menu???

fun main() {
    // todo select helmet for post-outline
    // todo why is the animation no longer playing?
    OfficialExtensions.initForTests()
    // ensure they're registered
    snowRenderMode.renderer
    rainRenderMode.renderer
    val scene = Entity().setPosition(-0.6, 0.0, 0.0)
    scene.add(MeshComponent(downloads.getChild("3d/DamagedHelmet.glb")))
    scene.add(
        Entity()
            .setPosition(1.2, -0.2, 0.0)
            .setScale(0.5)
            .add(PrefabCache[downloads.getChild("3d/Talking On Phone.fbx")]!!.createInstance() as Entity)
    )
    scene.add(
        Entity()
            .setPosition(1.2, 0.0, 0.0)
            .setScale(0.3)
            .add(SDFHyperBBox().apply {
                thickness = 0.2f
                rotation4d = rotation4d
                    .rotateY((-35f).toRadians())
                    .rotateX((47f).toRadians())
                    .rotateZ((-33f).toRadians())
            })
    )
    EditorState.prefabSource = scene.ref
    dst.tryMkdirs()
    sceneView.renderer.radius = 3.0
    sceneView.editControls.rotationTarget.set(-17.9, 58.3, 0.0)

    val renderModes = ArrayList(RenderMode.values.filter {
        it != RenderMode.GHOSTING_DEBUG && it != RenderMode.RAY_TEST
    })

    addEvent(3_000) {
        val renderView = sceneView.renderer
        renderView.setPosSize(0, 0, width, height)
        for (mode in renderModes) {
            renderScene(mode)
        }
        Engine.requestShutdown()
    }
    testUI3("Test", sceneView)
}

fun renderScene(renderMode: RenderMode) {
    LOGGER.info("Rendering ${renderMode.name}")
    FBStack.reset()
    useFrame(framebuffer) {
        framebuffer.clearColor(UIColors.midOrange)
        val renderView = sceneView.renderer
        renderView.renderMode = renderMode
        renderView.draw(0, 0, width, height)
    }
    LOGGER.info("Finished rendering ${renderMode.name}")
    framebuffer.getTexture0()
        .createImage(flipY = true, withAlpha = false)
        .write(dst.getChild("${renderMode.name.toAllowedFilename()}.png"))
    LOGGER.info("Finished saving ${renderMode.name}")
}