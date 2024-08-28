package me.anno.tests.promo

import me.anno.Engine
import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.Events.addEvent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.graph.visual.render.effects.FSR2Node
import me.anno.sdf.shapes.SDFHyperBBox
import me.anno.tests.LOGGER
import me.anno.tests.shader.Snow.snowRenderMode
import me.anno.tests.shader.SnowLikeRain.rainRenderMode
import me.anno.ui.UIColors
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.structures.lists.Lists.any2
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
    EditorState.select(scene)
    dst.tryMkdirs()

    sceneView.renderView.radius = 1.5
    sceneView.editControls.rotationTarget.set(-17.9, 58.3, 0.0)

    val renderModes = ArrayList(RenderMode.values.filter {
        it != RenderMode.GHOSTING_DEBUG && it != RenderMode.RAY_TEST
    })

    fun renderNextImage() {
        val mode = renderModes.removeLastOrNull()
        if (mode != null) {
            RenderState.viewIndex = 2 // use different slot to use different FBs
            val renderView = sceneView.renderView
            renderView.setPosSize(0, 0, width, height)
            renderScene(mode)
            RenderState.viewIndex = 0
            addEvent(1, ::renderNextImage)
        } else Engine.requestShutdown()
    }

    addEvent(3_000) {
        renderNextImage()
    }
    testUI3("PromoGenerator", sceneView)
}

fun renderScene(renderMode: RenderMode) {
    LOGGER.info("Rendering ${renderMode.nameDesc.englishName}")
    FBStack.reset()

    // todo why is only one FrameGen method working???
    // todo why is deferred MSAA not showing up???
    sceneView.renderView.renderMode = renderMode
    val times = if (sceneView.renderView.usesFrameGen() ||
        renderMode.renderGraph?.nodes?.any2 { it is FSR2Node } == true
    ) 50 else 1
    for (i in 0 until times) {
        useFrame(framebuffer) {
            framebuffer.clearColor(UIColors.midOrange)
            sceneView.renderView.draw(0, 0, width, height)
        }
        Time.updateTime(1.0 / 30.0, Time.nanoTime)
    }

    LOGGER.info("Finished rendering ${renderMode.nameDesc.englishName}")
    framebuffer.getTexture0()
        .createImage(flipY = true, withAlpha = false)
        .write(dst.getChild("${renderMode.nameDesc.englishName.toAllowedFilename()}.png"))
    LOGGER.info("Finished saving ${renderMode.nameDesc.englishName}")
}