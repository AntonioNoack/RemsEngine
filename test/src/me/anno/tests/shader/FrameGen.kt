package me.anno.tests.shader

import me.anno.animation.Type
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MaterialCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState.blendMode
import me.anno.gpu.GFXState.depthMode
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.*
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.image.ImageGPUCache
import me.anno.input.Input
import me.anno.maths.Maths.pow
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.input.IntInput
import me.anno.utils.OS.downloads
import org.joml.Matrix4x3d
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.GL_POINT_SMOOTH
import org.lwjgl.opengl.GL11C.*
import kotlin.math.max

fun main() {

    // todo this has bad graphics, because I'm using Pipeline directly
    // todo instead, use RenderGraph/RenderView

    // test something like DLSS3: Frame Generation
    // (to understand its challenges)
    // input: frame0, frame1, motion0-1,
    // output: frame0.5

    // to do can we inject into the renderer to show every second frame only?

    // val path = getReference("E:/Assets/Sources/POLYGON_War_Pack_Source_Files.zip/POLYGON_War_Demo_Scene.fbx/Scene.json") // fps increases
    val path = downloads.getChild("3d/DamagedHelmet.glb") // fps decreases
    val scene = PrefabCache[path]!!.getSampleInstance() as Entity
    testUI3("FrameGen") {
        DefaultConfig["debug.renderdoc.enabled"] = true
        StudioBase.instance?.enableVSync = false
        var interFrames = 2
        val renderPanel = object : Panel(style) {

            val settings = DeferredSettings(
                listOf(
                    DeferredLayerType.COLOR,
                    DeferredLayerType.MOTION
                )
            )

            val pipeline = Pipeline(settings)

            val samples = 1
            var data0 = settings.createBaseBuffer("FrameGen-0", samples)
            var data1 = settings.createBaseBuffer("FrameGen-1", samples)

            var zoom = 0.5f
            val rotation = Vector3f()

            // to avoid a search, draw each pixel onto where it is expected to be
            //  additive mode
            //  use depth as well for occlusion
            //  GL_POINTS
            //  finally, normalize by weight, and ask neighbors if necessary

            val mixShader = Shader(
                "mix", listOf(
                    Variable(GLSLType.V1I, "width"),
                    Variable(GLSLType.V2F, "invSize"),
                    Variable(GLSLType.S2D, "motionTex"),
                    Variable(GLSLType.S2D, "depthTex"),
                    Variable(GLSLType.V1F, "dt01"),
                ), "" +
                        "void main(){\n" +
                        "   int id = gl_InstanceID;\n" +
                        "   float u = (float(id % width) + 0.5) * invSize.x;\n" +
                        "   float v = (float(id / width) + 0.5) * invSize.y;\n" +
                        "   uv = vec2(u,v);\n" +
                        "   vec3 duv = dt01 * texture(motionTex, uv).xyz;\n" +
                        "   float depth = texture(depthTex, uv).x;\n" +
                        "   gl_Position = vec4(u*2.0-1.0+duv.x,v*2.0-1.0+duv.y,depth+duv.z,1.0);\n" +
                        "}", ShaderLib.uvList,
                listOf(
                    Variable(GLSLType.V4F, "result", VariableMode.OUT),
                    Variable(GLSLType.S2D, "colorTex"),
                ), "" +
                        "void main() {\n" +
                        "   vec4 color = texture(colorTex, uv);\n" +
                        "   result = vec4(color.rgb, 1.0);\n" +
                        "}\n"
            )

            val normShader = Shader(
                "mix", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
                listOf(
                    Variable(GLSLType.V1F, "dt"),
                    Variable(GLSLType.S2D, "colorTexX"),
                    Variable(GLSLType.S2D, "colorTex0"),
                    Variable(GLSLType.S2D, "colorTex1"),
                    Variable(GLSLType.S2D, "motionTex1"),
                    Variable(GLSLType.V4F, "result", VariableMode.OUT),
                ), "" +
                        "void main() {\n" +
                        "   ivec2 uvi = ivec2(uv * textureSize(colorTexX,0));\n" +
                        "   vec4 color = texelFetch(colorTexX,uvi,0);\n" +
                        "   if(color.a == 0.0) {\n" +
                        "       for(int i=2;i<=4;i+=2){\n" +
                        "           int di = i>>1;\n" +
                        "           for(int j=0;j<i;j++) {\n" +
                        "               color += texelFetch(colorTexX,uvi+ivec2(j-di,-di),0);\n" +
                        "               color += texelFetch(colorTexX,uvi+ivec2(+di,j-di),0);\n" +
                        "               color += texelFetch(colorTexX,uvi+ivec2(di-j,+di),0);\n" +
                        "               color += texelFetch(colorTexX,uvi+ivec2(-di,di-j),0);\n" +
                        "           }\n" +
                        "           if(color.a > 0.0) break;\n" +
                        "       }\n" +
                        "   }\n" +
                        "   result = vec4(color.a > 0.0 ? " +
                        "       color.rgb/color.a :\n" +
                        "       texture(colorTex0, uv+dt*texture(motionTex1,uv).xy).xyz,\n" +
                        "   1.0);\n" +
                        "}\n"
            )

            val renderer = SimpleRenderer(
                "deferred", settings,
                Renderer.colorRenderer.getPostProcessing(0)
            )

            override fun onUpdate() {
                super.onUpdate()
                invalidateDrawing()
            }

            override val canDrawOverBorders = true

            var frameIndex = Int.MAX_VALUE

            val ptBuffer = Mesh().apply {
                drawMode = GL_POINTS
            }

            val accu = Framebuffer("accu", 1, 1, TargetType.FloatTarget4, DepthBufferType.INTERNAL)

            override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                if (Input.isLeftDown || Input.isRightDown) {
                    val speed = 10f / height
                    rotation.x += dy * speed
                    rotation.y += dx * speed
                }
            }

            override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
                zoom *= pow(1e6f, -dy / height)
            }

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                // no longer necessary, sky now fills everything
                // drawBackground(x0, y0, x1, y1)

                data0.ensure()
                data1.ensure()

                // todo GL_LINE_SMOOTH looks really nice for debug rendering ->
                // todo why is it breaking the FrameGen demo? (much too bright)
                if (Input.isShiftDown) {
                    glEnable(GL_POINT_SMOOTH)
                    glPointSize(1f)
                } else glDisable(GL_POINT_SMOOTH)

                // keep textures loaded
                scene.firstComponentInChildren(MeshComponent::class) {
                    for (m in it.materials) {
                        MaterialCache[m, true]?.listTextures()?.forEach { tex ->
                            ImageGPUCache[tex, true]
                        }
                    }
                    for (m in it.getMesh()!!.materials) {
                        MaterialCache[m, true]?.listTextures()?.forEach { tex ->
                            ImageGPUCache[tex, true]
                        }
                    }
                    false
                }

                if (frameIndex < interFrames) {

                    drawBackground(x0, y0, x1, y1)

                    val filter = GPUFiltering.TRULY_LINEAR
                    val clamp = Clamping.CLAMP
                    val dt = (frameIndex + 0.5f) / interFrames
                    useFrame(width, height, true, accu) {
                        depthMode.use(DepthMode.CLOSE) {
                            blendMode.use(BlendMode.PURE_ADD) {

                                // draw the interpolate :)
                                accu.clearColor(0, true)

                                val shader = mixShader
                                shader.use()
                                data1.getTextureI(1).bind(shader, "motionTex", filter, clamp)
                                shader.v2f("invSize", 1f / width, 1f / height)
                                shader.v1i("width", width)
                                shader.v1f("dt01", dt - 1f)
                                data1.getTextureI(0).bind(shader, "colorTex", filter, clamp)
                                data1.depthTexture!!.bind(shader, "depthTex", filter, clamp)
                                ptBuffer.proceduralLength = width * height // one point per pixel
                                ptBuffer.draw(shader, 0)
                            }
                        }
                    }

                    // normalize and blur result
                    val shader = normShader
                    shader.use()
                    shader.v1f("dt", -dt * 0.5f)
                    accu.getTexture0().bindTrulyNearest(shader, "colorTexX")
                    data0.getTextureI(0).bind(shader, "colorTex0", filter, clamp)
                    data1.getTextureI(0).bind(shader, "colorTex1", filter, clamp)
                    data1.getTextureI(1).bind(shader, "motionTex1", filter, clamp)
                    flat01.draw(shader)

                    frameIndex++

                } else {

                    val tmp = data0
                    data0 = data1
                    data1 = tmp

                    val cameraMatrix = RenderState.cameraMatrix
                    val prevCamMatrix = RenderState.prevCameraMatrix
                    val cameraRotation = RenderState.cameraRotation
                    val prevCamRotation = RenderState.prevCameraRotation
                    val cameraPosition = RenderState.cameraPosition
                    val cameraDirection = RenderState.cameraDirection

                    // render the current frame
                    scene.validateAABBs()
                    val aabb = scene.aabb
                    val sz = max(max(aabb.deltaX, aabb.deltaY), aabb.deltaZ).toFloat()
                    val sx = sz * zoom
                    val wf = width.toFloat()
                    val hf = height.toFloat()
                    val s = sx * max(1f, wf / hf)
                    val t = sx * max(1f, hf / wf)

                    cameraPosition.set(0.0)
                    cameraRotation.identity()
                        .rotateX(rotation.x.toDouble())
                        .rotateY(rotation.y.toDouble())

                    cameraMatrix.identity()
                        .ortho(-s, +s, -t, +t, +sz, -sz, true)
                        .rotate(Quaternionf(cameraRotation))

                    cameraDirection
                        .set(0.0, 0.0, 1.0)
                        .rotate(cameraRotation)

                    pipeline.clear()
                    pipeline.frustum.defineOrthographic(
                        Matrix4x3d()
                            .rotate(cameraRotation)
                            .scale(s.toDouble(), t.toDouble(), sz.toDouble()),
                        width, cameraPosition, cameraRotation
                    )
                    scene.validateTransform()
                    pipeline.fill(scene)

                    useFrame(width, height, true, data1, renderer) {
                        data1.clearDepth()
                        pipeline.draw()
                    }

                    // present the previous frame
                    drawTexture(x, y + height, width, -height, data0.getTexture0(), true)

                    // update
                    prevCamMatrix.set(cameraMatrix)
                    prevCamRotation.set(cameraRotation)

                    frameIndex = 0

                }

            }

        }
        val list = PanelListY(style)
        renderPanel.weight = 1f
        list.add(renderPanel)
        list.add(IntInput("Interpolated Frames", "", interFrames, Type.LONG_PLUS, style)
            .setChangeListener { interFrames = it.toInt() })
        list
    }

}