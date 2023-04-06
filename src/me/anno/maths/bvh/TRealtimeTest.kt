package me.anno.maths.bvh

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DrawTexts
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
import me.anno.maths.Maths.max
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.Clock
import me.anno.utils.LOGGER
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.f1
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3f

object TRealtimeTest {

    @JvmStatic
    fun main(args: Array<String>) {
        ECSRegistry.init()
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

            val main = PanelListY(style)
            val controls = createControls(cameraPosition, cameraRotation, bvh, main)

            val list = CustomList(false, style)

            val scale = 4
            list.add(createCPUPanel(scale, cameraPosition, cameraRotation, fovZFactor, bvh, false))
            list.add(createCPUPanel(scale, cameraPosition, cameraRotation, fovZFactor, bvh, true))

            val testGPU = true
            if (testGPU) {

                /**
                 * GPU
                 * */
                val useComputeShader = true
                val useComputeBuffer = false // todo not working :/

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
                            GFXState.blendMode.use(BlendMode.DEFAULT) {
                                triangles.bindTrulyNearest(0)
                                blasNodes.bindTrulyNearest(1)
                                tlasNodes.bindTrulyNearest(2)
                                flat01.draw(shader)
                            }
                        }

                        drawResult(it)

                    })

                }
            }

            main.add(list)
            list.setWeight2(100f)
            main.setWeight2(100f)
            main

        }

    }

}