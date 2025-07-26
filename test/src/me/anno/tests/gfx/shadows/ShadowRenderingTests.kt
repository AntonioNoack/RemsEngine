package me.anno.tests.gfx.shadows

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.light.sky.SkyboxBase
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.OfficialExtensions
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderMode.Companion.opaqueNodeSettings
import me.anno.engine.ui.render.RenderView1
import me.anno.gpu.Blitting
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.RenderDoc
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.scene.RenderForwardNode
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.jvm.HiddenOpenGLContext
import me.anno.tests.FlakyTest
import me.anno.utils.Color.black
import me.anno.utils.Color.toVecRGB
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertGreaterThan
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import org.lwjgl.glfw.GLFW
import java.awt.Robot
import java.awt.event.KeyEvent
import kotlin.math.hypot

// todo implement components vs baseline raytracing shadow tests
class ShadowRenderingTests {
    companion object {

        val write = false

        private val resolution = 512
        private val cameraPosition = Vector3d(0.0, 0.0, 12.0)

        private val skyColorI = 0x77aacc or black
        private val shadowColor = skyColorI
        private val lightColor = 0xffffff or black
        private val cubeColor = 0x505860 or black
    }

    private fun createCubeTestScene(): Entity {
        val scene = Entity()
        Entity("Floor", scene)
            .add(MeshComponent(plane))
            .setPosition(0.0, -4.0, 0.0)
            .setScale(100f)
        Entity("Centerpiece", scene)
            .add(MeshComponent(flatCube, DefaultAssets.steelMaterial))
        scene.add(SkyboxBase().apply {
            skyColorI.toVecRGB(skyColor)
        })
        return scene
    }

    @Test
    fun testDirectionalLight() {

        OfficialExtensions.initForTests()

        val scene = createCubeTestScene()
        val shadowDir = Vector3f(1f, 1f, 0.5f).normalize(1f)
        val baseline = renderBaseline(scene) { _, _ -> shadowDir }
        if (write) baseline.write(desktop.getChild("DirectionalBaseline.png"))

        val light = DirectionalLight()
        light.color.set(50f)
        light.shadowMapCascades = 1
        light.autoUpdate = 1

        Entity("Light", scene)
            .setRotation(
                Quaternionf().rotateTo(
                    0f, 0f, -1f,
                    -shadowDir.x, -shadowDir.y, -shadowDir.z
                )
            )
            .setScale(10f)
            .add(light)

        val rendered = renderGraphics(scene)
        if (write) rendered.write(desktop.getChild("DirectionalRendered.png"))
        compareImages(baseline, rendered, 986)
    }

    private fun compareImages(imageA: Image, imageB: Image, requiredMatchPercentile: Int) {
        val colors = intArrayOf(skyColorI, lightColor, cubeColor).map { it.toVecRGB() }
        assertEquals(imageA.width, imageB.width)
        assertEquals(imageA.height, imageB.height)

        var numCorrectPixels = 0
        val colorA = Vector3f()
        val colorB = Vector3f()
        imageA.forEachPixel { x, y ->
            imageA.getRGB(x, y).toVecRGB(colorA)
            imageB.getRGB(x, y).toVecRGB(colorB)
            val categoryA = colors.minBy { it.distance(colorA) }
            val categoryB = colors.minBy { it.distance(colorB) }
            if (categoryA == categoryB) numCorrectPixels++
        }

        println("Matching: $numCorrectPixels / ${imageA.width * imageA.height}")
        assertGreaterThan(numCorrectPixels, imageA.width * imageA.height * requiredMatchPercentile / 1000)
    }

    @Test
    @FlakyTest
    fun testSpotLight() {

        OfficialExtensions.initForTests()

        val lightPos = Vector3d(2.45, 0.64, 0.72)
        val shadowDir = Vector3f(1f, 1f, 0.5f).normalize(1f)
        val lightRot = Quaternionf().rotateTo(
            0f, 0f, -1f,
            -shadowDir.x, -shadowDir.y, -shadowDir.z
        )

        val scene = createCubeTestScene()
        val baseline = renderBaseline(scene) { pos, dst ->
            val proj = pos.sub(lightPos, dst).rotateInv(lightRot)
            if (hypot(proj.x, proj.y) < -proj.z)
                pos.sub(lightPos, dst).normalize(-1f)
            else null
        }
        if (write) baseline.write(desktop.getChild("SpotBaseline.png"))

        val light = SpotLight()
        light.color.set(50f)
        light.shadowMapCascades = 1
        light.autoUpdate = 1

        Entity("Light", scene)
            .setPosition(lightPos)
            .setRotation(lightRot)
            .setScale(50f)
            .add(light)

        // testSceneWithUI("SpotLight", scene)

        val rendered = renderGraphics(scene)
        if (write) rendered.write(desktop.getChild("SpotRendered.png"))
        compareImages(baseline, rendered, 986)
    }

    // todo this test is completely broken :(
    @Test
    fun testPointLight() {

        OfficialExtensions.initForTests()

        val lightPos = Vector3d(2.45, 0.64, 0.72)

        val scene = createCubeTestScene()
        val baseline = renderBaseline(scene) { pos, dst ->
            pos.sub(lightPos, dst).normalize(-1f)
        }
        if (write) baseline.write(desktop.getChild("PointBaseline.png"))

        val light = SpotLight()
        light.color.set(50f)
        light.shadowMapCascades = 1
        light.autoUpdate = 1

        Entity("Light", scene)
            .setPosition(lightPos)
            .setScale(50f)
            .add(light)

        // testSceneWithUI("PointLight", scene)

        val rendered = renderGraphics(scene)
        if (write) rendered.write(desktop.getChild("PointRendered.png"))
        compareImages(baseline, rendered, 986)
    }

    fun renderBaseline(
        scene: Entity,
        getDirectionTowardsLight: (position: Vector3d, dst: Vector3f) -> Vector3f?
    ): IntImage {

        // given a pixel position, project it onto the plane/box, calculate the direction towards the light,
        //  do a ray check and evaluate that

        val image = IntImage(resolution, resolution, true)
        val invZ = 1f / resolution
        val center = resolution * 0.5f
        val maxDistance = 1e6
        val lightDir = Vector3f()
        val query = RayQuery(Vector3d(), Vector3f(), maxDistance)
        val cameraDir = Vector3d()

        fun clearQuery() {
            query.start.fma(maxDistance, query.direction, query.end)
            query.result.distance = maxDistance
            query.result.component = null
            query.result.mesh = null
        }

        image.forEachPixel { x, y ->

            val rx = (x - center) * invZ
            val ry = (center - y) * invZ

            query.start.set(cameraPosition)
            cameraDir.set(rx, ry, -1f).normalize()
            query.direction.set(cameraDir)
            clearQuery()

            val color = if (Raycast.raycast(scene, query)) {
                val hitCube = query.result.mesh == flatCube
                if (!hitCube) {
                    val hitPosition = query.result.positionWS
                    val direction = getDirectionTowardsLight(hitPosition, lightDir)
                    if (direction != null) {
                        query.start.set(hitPosition).fma(-0.0001, query.result.geometryNormalWS)
                        query.direction.set(direction)
                        clearQuery()

                        val isInShadow = Raycast.raycast(scene, query)
                        if (isInShadow) shadowColor else lightColor
                    } else shadowColor
                } else cubeColor
            } else skyColorI
            image.setRGB(x, y, color)
        }
        return image
    }

    private val renderMode = RenderMode(
        "Shadows", QuickPipeline()
            .then1(RenderForwardNode(), opaqueNodeSettings)
            .finish()
    )

    private var texture: ITexture2D? = null
    fun renderGraphics0(scene: Entity): Image {
        val view = RenderView1(PlayMode.PLAYING, scene, style)
        view.setPosSize(0, 0, resolution, resolution)

        // todo why are our settings identical with 53° FOV????
        // todo why is the depth reversed??? if we can fix that, we can get a 99.5% score
        view.editorCamera.fovYDegrees = 53f
        view.orbitCenter.set(0.0)
        view.orbitRotation.identity()
        view.radius = cameraPosition.length().toFloat()
        view.near = 0.01f
        view.far = 1000f

        // calculate shadows
        // directional light only rendered on 2nd frame why ever
        Systems.world = scene
        repeat(3) {
            Systems.onUpdate()
        }

        val fb = Framebuffer(
            "Shadows", resolution, resolution, 1,
            TargetType.UInt8x4, DepthBufferType.TEXTURE
        )
        view.renderMode = renderMode

        useFrame(fb) {
            view.draw(0, 0, resolution, resolution)
        }

        texture = fb.getTexture0()

        return fb.getTexture0()
            .createImage(flipY = false, withAlpha = true)
            .cloneToIntImage()
    }

    fun renderGraphics(scene: Entity): Image {
        return if (true) {
            HiddenOpenGLContext.createOpenGL()
            renderGraphics0(scene)
        } else {
            lateinit var image: Image
            captureWithRenderDoc {
                image = renderGraphics0(scene)
                texture!!
            }
            image
        }
    }

    fun captureWithRenderDoc(execution: () -> ITexture2D) {
        RenderDoc.forceLoadRenderDoc()
        HiddenOpenGLContext.createOpenGL()
        val window = HiddenOpenGLContext.window.pointer
        GLFW.glfwShowWindow(window)
        GLFW.glfwFocusWindow(window)

        repeat(10) {
            NullFramebuffer.clearColor(0x7788ff or black)
            GLFW.glfwSwapBuffers(window)
        }

        val robot = Robot()
        robot.keyPress(KeyEvent.VK_F12)
        Thread.sleep(100)
        robot.keyRelease(KeyEvent.VK_F12)
        Thread.sleep(100)

        GLFW.glfwSwapBuffers(window)

        val texture = execution()

        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwWaitEventsTimeout(0.0)
            NullFramebuffer.bindDirectly()
            useFrame(NullFramebuffer) {
                Blitting.copyColor(texture, true)
            }
            GLFW.glfwSwapBuffers(window)
        }
    }
}