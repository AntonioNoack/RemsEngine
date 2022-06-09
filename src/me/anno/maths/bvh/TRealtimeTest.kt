package me.anno.maths.bvh

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.engine.ECSRegistry
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.*
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.max
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.Clock
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.toRGB
import me.anno.utils.LOGGER
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.f1
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.pow

object TRealtimeTest {

    @JvmStatic
    fun main(args: Array<String>) {
        ECSRegistry.initNoGFX()
        val clock = Clock()
        val (tlas, cameraPosition, cameraRotation, fovZFactor) = createSampleTLAS(16)
        clock.stop("Loading & Generating TLAS", 0.0)
        run(tlas, cameraPosition, cameraRotation, fovZFactor)
        LOGGER.debug("Finished")
    }
    
    fun run(
        bvh: TLASNode,
        cameraPosition: Vector3f, cameraRotation: Quaternionf,
        fovZFactor: Float
    ) {

        LogManager.disableLogger("WorkSplitter")

        testUI {

            DefaultConfig["debug.renderdoc.enabled"] = true

            val main = PanelListY(style)
            val controls = OrbitControls()
            val camera = Camera()
            val camEntity = Entity()
            val base = Entity()

            camEntity.add(camera)
            base.add(camEntity)

            controls.camera = camera
            controls.position.set(cameraPosition)
            controls.base = base
            controls.radius = 1f
            controls.movementSpeed = 0.02f * bvh.bounds.volume().pow(1f / 3f)
            controls.rotationSpeed = 0.15f
            controls.friction = 20f

            val window = GFX.someWindow
            var lx = window.mouseX
            var ly = window.mouseY
            var lz = Input.mouseWheelSumY
            main.add(SpyPanel(style) {
                if (window.windowStack.inFocus.any2 { it is TestDrawPanel }) {
                    if (Input.isRightDown) {
                        val dx = window.mouseX - lx
                        val dy = window.mouseY - ly
                        controls.onMouseMoved(0f, 0f, dx, dy)
                    }
                    if (Input.wasKeyPressed('v')) {
                        StudioBase.instance!!.toggleVsync()
                    }
                    val dw = Input.mouseWheelSumY - lz
                    controls.onMouseWheel(0f, 0f, 0f, dw, true)
                }
                lx = window.mouseX
                ly = window.mouseY
                lz = Input.mouseWheelSumY
                controls.onUpdate()
                controls.throwWarning()
                base.validateTransform()
                camEntity.validateTransform()
                cameraPosition.set(camEntity.transform.globalPosition)
                cameraRotation.set(camEntity.transform.globalRotation)
            })

            val list = CustomList(false, style)
            var isCPUComputing = false
            var cpuSpeed = -1L
            var cpuFPS = 0L

            fun computeSpeed(w: Int, h: Int, dt: Long) = dt / (w * h)

            val pipeline = ProcessingGroup("trt", 1f)

            /**
             * CPU
             * */
            val cpuTexture = Texture2D("cpu", 1, 1, 1)
            cpuTexture.createRGBA()
            list.add(TestDrawPanel {

                it.clear()

                val scale = 4

                // render cpu side
                // render at lower resolution because of performance
                val w = it.w / scale
                val h = it.h / scale

                val parent = it.uiParent!!
                val cx = (parent.w * 0.5f - it.x) / scale
                val cy = (parent.h * 0.5f) / scale - 1 // 1 for offset; one is mirrored on y, the other not
                val fovZ = -parent.h * fovZFactor / scale

                fun nextFrame() {
                    val tmpPos = Vector3f(cameraPosition)
                    val tmpRot = Quaternionf(cameraRotation)
                    pipeline += {
                        val cpuBuffer = Texture2D.bufferPool[w * h * 4, false, false]
                        val t0 = System.nanoTime()
                        pipeline.processBalanced2d(0, 0, w, h, 8, 1) { x0, y0, x1, y1 ->
                            JomlPools.reset()
                            for (y in y0 until y1) {
                                for (x in x0 until x1) {
                                    val dir = JomlPools.vec3f.create()
                                    dir.set(x - cx, cy - y, fovZ).normalize()
                                    tmpRot.transform(dir)
                                    val hit = localResult.get()
                                    hit.normalWS.set(0.0)
                                    val maxDistance = 1e15
                                    hit.distance = maxDistance
                                    bvh.intersect(tmpPos, dir, hit)
                                    val color = if (hit.normalWS.lengthSquared() > 0.0) {
                                        val normal = hit.normalWS
                                        normal.normalize()
                                        normal.mul(0.5).add(0.5, 0.5, 0.5)
                                        normal.toRGB()
                                    } else sky1 // Maths.mixARGB2(sky0, sky1, 0.1f * length(dirX, dirY))
                                    JomlPools.vec3f.sub(1)
                                    val i = (x + y * w) * 4
                                    cpuBuffer.put(i, color.r().toByte())
                                    cpuBuffer.put(i + 1, color.g().toByte())
                                    cpuBuffer.put(i + 2, color.b().toByte())
                                    cpuBuffer.put(i + 3, color.a().toByte())
                                }
                            }
                        }
                        val t1 = System.nanoTime()
                        val dt = max(t1 - t0, 1L)
                        cpuSpeed = computeSpeed(w, h, dt)
                        cpuFPS = SECONDS_TO_NANOS / dt
                        GFX.addGPUTask("trt", 1) {
                            cpuTexture.w = w
                            cpuTexture.h = h
                            cpuTexture.createRGBA(cpuBuffer, false)
                            // Texture2D.bufferPool.returnBuffer(cpuBuffer)
                            isCPUComputing = false
                        }
                    }
                }

                if (!isCPUComputing) {
                    isCPUComputing = true
                    nextFrame()
                }

                DrawTextures.drawTexture(it.x, it.y, it.w, it.h, cpuTexture, true, -1, null)
                DrawTexts.drawSimpleTextCharByChar(it.x + 4, it.y + it.h - 50, 2, "$cpuSpeed ns/e, $cpuFPS fps")

            })

            /**
             * GPU
             * */
            val useComputeShader = true
            val useComputeBuffer = false
            val scale = 4

            val avgBuffer = Framebuffer("avg", 1, 1, 1, 1, true, DepthBufferType.NONE)

            val drawShader = Shader(
                "draw", coordsList, coordsVShader, uvList, listOf(
                    Variable(GLSLType.V1B, "enableToneMapping"),
                    Variable(GLSLType.V1F, "brightness"),
                    Variable(GLSLType.S2D, "tex"),
                    Variable(GLSLType.V4F, "result", VariableMode.OUT)
                ), "" +
                        "void main(){\n" +
                        "   vec3 color = texture(tex,uv).rgb;\n" +
                        "   if(enableToneMapping) color /= (color + brightness);\n" +
                        "   result = vec4(color, 1.0);\n" +
                        "}"
            )

            val lastPos = Vector3f()
            val lastRot = Quaternionf()
            var lastDrawMode = -1

            fun getDrawMode() = Input.isKeyDown('x').toInt(1) +
                    (Input.isKeyDown('y') || Input.isKeyDown('z')).toInt(2)

            var frameIndex = 0
            fun prepareShader(shader: OpenGLShader, it: Panel) {

                it.clone()

                val w = it.w / scale
                val h = it.h / scale

                val drawMode = getDrawMode()

                // if camera was moved, set frameIndex back to zero
                if (lastDrawMode != drawMode ||
                    lastPos.distance(cameraPosition) > controls.radius * 0.01f ||
                    lastRot.difference(cameraRotation, JomlPools.quat4f.borrow()).angle() > 1e-3f
                ) frameIndex = 0

                if (avgBuffer.w != w || avgBuffer.h != h) {
                    avgBuffer.w = w
                    avgBuffer.h = h
                    avgBuffer.destroy()
                    avgBuffer.create()
                    frameIndex = 0
                }

                val parent = it.uiParent!!
                val cx = (parent.w * 0.5f - it.x) / scale
                val cy = (parent.h * 0.5f) / scale
                val fovZ = -parent.h * fovZFactor / scale

                shader.use()
                shader.v2i("size", w, h)
                shader.v3f("worldPos", cameraPosition)
                shader.v4f("worldRot", cameraRotation)
                shader.v3f("cameraOffset", cx, cy, fovZ)
                shader.v3f("sky0", sky0)
                shader.v3f("sky1", sky1)
                shader.v1i("drawMode", drawMode)
                shader.v1i("frameIndex", frameIndex)

                lastPos.set(cameraPosition)
                lastRot.set(cameraRotation)
                lastDrawMode = drawMode

                frameIndex++

            }

            fun drawResult(it: Panel) {

                val tex = avgBuffer.getTexture0()

                val drawMode = lastDrawMode
                val enableAutoExposure = drawMode == 0
                val enableToneMapping = drawMode == 0

                // draw with auto-exposure
                val brightness = if (enableAutoExposure and enableToneMapping)
                    max(Reduction.reduce(tex, Reduction.MAX).x, 1e-7f) else 1f
                drawShader.use()
                drawShader.v1f("brightness", 0.2f * brightness)
                drawShader.v1b("enableToneMapping", enableToneMapping)
                tex.bindTrulyNearest(0)
                flat01.draw(drawShader)

                val gpuFPS = Engine.currentFPS.f1()
                DrawTexts.drawSimpleTextCharByChar(it.x + 4, it.y + it.h - 50, 2, "$gpuFPS fps, $frameIndex spp")

            }

            val clock = Clock()
            if (useComputeShader) {

                if (useComputeBuffer) {

                    val (shader, triangles, blasNodes, tlasNodes) = createComputeShaderV2(bvh)
                    clock.stop("Creating Shader & Uploading Data", 0.0)

                    list.add(TestDrawPanel {

                        // render gpu side
                        prepareShader(shader, it)
                        val avgTexture = avgBuffer.getTexture0()
                        shader.bindBuffer(0, triangles)
                        shader.bindBuffer(1, blasNodes)
                        shader.bindBuffer(2, tlasNodes)
                        shader.bindTexture(3, avgTexture as Texture2D, ComputeTextureMode.READ_WRITE)
                        shader.runBySize(avgBuffer.w, avgBuffer.h)

                        drawResult(it)

                    })

                } else {

                    val (shader, triangles, blasNodes, tlasNodes) = createComputeShader(bvh)
                    clock.stop("Creating Shader & Uploading Data", 0.0)

                    list.add(TestDrawPanel {

                        // render gpu side
                        prepareShader(shader, it)
                        val avgTexture = avgBuffer.getTexture0()
                        shader.bindTexture(0, triangles, ComputeTextureMode.READ)
                        shader.bindTexture(1, blasNodes, ComputeTextureMode.READ)
                        shader.bindTexture(2, tlasNodes, ComputeTextureMode.READ)
                        shader.bindTexture(3, avgTexture as Texture2D, ComputeTextureMode.READ_WRITE)
                        shader.runBySize(avgBuffer.w, avgBuffer.h)

                        drawResult(it)

                    })
                }
            } else {

                val (shader, triangles, blasNodes, tlasNodes) = createGraphicsShader(bvh)
                clock.stop("Creating Shader & Uploading Data", 0.0)

                list.add(TestDrawPanel {

                    // render gpu side
                    prepareShader(shader, it)
                    useFrame(avgBuffer) {
                        OpenGL.blendMode.use(BlendMode.DEFAULT) {
                            triangles.bindTrulyNearest(0)
                            blasNodes.bindTrulyNearest(1)
                            tlasNodes.bindTrulyNearest(2)
                            flat01.draw(shader)
                        }
                    }

                    drawResult(it)

                })

            }

            main.add(list)
            list.setWeight(100f)
            main.setWeight(100f)
            main

        }

    }

}