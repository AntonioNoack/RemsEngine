package me.anno.tests.promo

import me.anno.Engine
import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.Events.addEvent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.DraggingControlSettings
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.TextureCache
import me.anno.graph.visual.render.effects.FSR2Node
import me.anno.io.files.Reference.getReference
import me.anno.sdf.shapes.SDFHyperBBox
import me.anno.tests.LOGGER
import me.anno.tests.shader.Snow.snowRenderMode
import me.anno.tests.shader.SnowLikeRain.rainRenderMode
import me.anno.ui.UIColors
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.OS.res
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Floats.toRadians
import kotlin.math.tan

// create a list of all visual effects, including images, so we can show them off a bit :)
// -> https://remsengine.phychi.com/?s=learn/rendermodes

// create list of most samples with images
//  - start a sample,
//  - set camera angle / parameters right
//  - take a screenshot
//  - save it with appropriate name

// to do: run these samples on the web/with a space-optimized engine build
// -> good enough: https://remsengine.phychi.com/?s=learn/rendermodes

val width = 1200
val height = 800

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
    val scene = Entity().setPosition(-0.6, -0.15, 0.0)
    scene.add(MeshComponent(downloads.getChild("3d/DamagedHelmet.glb")))
    scene.add(
        Entity()
            .setPosition(1.2, -0.2, 0.0)
            .setScale(0.5f)
            .add(PrefabCache[downloads.getChild("3d/Talking On Phone.fbx")].waitFor()!!.createInstance() as Entity)
    )
    scene.add(
        Entity()
            .setPosition(1.2, 0.0, 0.0)
            .setScale(0.3f)
            .add(SDFHyperBBox().apply {
                thickness = 0.2f
                rotation4d = rotation4d
                    .rotateY((-35f).toRadians())
                    .rotateX((47f).toRadians())
                    .rotateZ((-33f).toRadians())
            })
    )
    // add glass object to scene, so we have something with opacity != 1
    scene.add(
        Entity()
            .setPosition(0.7, -0.2, 1.0)
            .setScale(0.2f)
            .add(
                MeshComponent(
                    IcosahedronModel.createIcosphere(2).ref,
                    getReference("materials/Glass.json")
                )
            )

    )
    EditorState.prefabSource = scene.ref
    EditorState.select(scene)
    dst.tryMkdirs()

    val fov = 10f
    sceneView.renderView.radius = 1f / tan(fov.toRadians() * 0.5f)
    sceneView.editControls.rotationTargetDegrees.set(-17.9, 58.3, 0.0)
    addEvent {
        (sceneView.editControls as DraggingControls).settings.fovY = fov
    }

    val renderModes = ArrayList(RenderMode.values.filter {
        it != RenderMode.GHOSTING_DEBUG && it != RenderMode.RAY_TEST
    })

    fun renderNextImage() {
        val mode = renderModes.removeLastOrNull()
        if (mode != null) {
            RenderState.viewIndex = 2 // use different slot to use different FBs
            renderScene(mode)
            RenderState.viewIndex = 0
            addEvent(1, ::renderNextImage)
        } else Engine.requestShutdown()
    }

    if (false) addEvent(3_000) {
        renderNextImage()
    }
    testUI3("PromoGenerator", sceneView)
}

fun renderScene(renderMode: RenderMode) {
    LOGGER.info("Rendering ${renderMode.nameDesc.englishName}")
    FBStack.reset()

    // to do why are some images randomly just orange???
    val renderView = sceneView.renderView
    renderView.setPosSize(0, 0, width, height)
    (sceneView.editControls.settings as? DraggingControlSettings)?.renderMode = renderMode
    renderView.renderMode = renderMode
    renderView.renderSize.resize(width, height, Time.nanoTime)
    val times = if (renderView.usesFrameGen() ||
        renderMode.renderGraph?.nodes?.any2 { it is FSR2Node } == true
    ) 50 else 1

    if (renderMode == RenderMode.UV) {
        // ensure UVs texture is valid
        TextureCache[res.getChild("textures/UVChecker.png")].waitFor()
    }

    for (i in 0 until times) {
        useFrame(framebuffer) {
            framebuffer.clearColor(UIColors.midOrange)
            sceneView.renderView.draw(0, 0, width, height)
        }
        Time.updateTime(1.0 / 30.0, Time.nanoTime)
    }

    LOGGER.info("Finished rendering ${renderMode.nameDesc.englishName}")
    framebuffer.getTexture0()
        .createImage(flipY = false, withAlpha = false)
        .write(dst.getChild("${renderMode.nameDesc.englishName.toAllowedFilename()}.webp"))
    LOGGER.info("Finished saving ${renderMode.nameDesc.englishName}")
}