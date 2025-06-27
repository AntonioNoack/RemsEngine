package me.anno.tests.shader

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.io.saveable.Saveable
import me.anno.mesh.Shapes
import me.anno.tests.shader.SnowLikeRain.rainControl
import me.anno.tests.shader.SnowLikeRain.rainRenderMode
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.types.Floats.toRadians

// get rain effect working like snow in https://www.glslsandbox.com/e#36547.0
// get effect working in 3d like snow

object SnowLikeRain {
    val rainControl = SnowControl().apply {
        color.set(3f)
        density = 1.3f
        velocity.set(0f, -8f, 0f)
        flakeSize = 0.005f
        elongation = 30f
        // tilt rain a bit
        worldRotation.rotateX((15f).toRadians())
    }
    val rainNode = SnowNode().apply {
        snowControl = rainControl
    }
    val rainRenderGraph = createSnowGraph(rainNode)
    val rainRenderMode = RenderMode("Rain", rainRenderGraph)
}

fun main() {
    testUI("Snow-Like Rain") {
        val list = CustomList(false, style)

        // 2d
        val shader = Shader(
            "rain", emptyList(), coordsUVVertexShader, uvList, listOf(
                Variable(GLSLType.V1F, "time"),
                Variable(GLSLType.V2F, "uvScale"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "float rain(vec2 uv, float scale) {\n" +
                    "   float w=smoothstep(1.,0.,-uv.y*(scale/10.));if(w<.1)return 0.;\n" +
                    "   uv+=time/scale;uv.y+=time*8./scale;uv.x+=sin(uv.y+time*.05)/scale;\n" +
                    "   uv*=scale;vec2 s=floor(uv),f=fract(uv),p;float k=3.,d;\n" +
                    "   p=.5+.35*sin(11.*fract(sin((s+p+scale)*mat2(7,3,6,5))*5.))-f;d=length(p);k=min(d,k);\n" +
                    "   k=smoothstep(0.,k,sin(f.x+f.y)*0.01);\n" +
                    "   return k*w;\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   vec2 uv2 = vec2(3.0,2.0) * uv * uvScale;\n" +
                    "   float c=0.0;\n" +
                    "   c+=rain(uv2,30.)*.3;\n" +
                    "   c+=rain(uv2,20.)*.5;\n" +
                    "   c+=rain(uv2,15.)*.8;\n" +
                    "   c+=rain(uv2,10.);\n" +
                    "   c+=rain(uv2,8.);\n" +
                    "   c+=rain(uv2,6.);\n" +
                    "   c+=rain(uv2,5.);\n" +
                    "   result = vec4(mix(vec3(0.1,0.2,0.3),vec3(1),c),1);\n" +
                    "}"
        )
        list.add(TestDrawPanel {
            shader.use()
            shader.v1f("time", Time.gameTime)
            shader.v2f("uvScale", it.width.toFloat() / it.height, 1f)
            SimpleBuffer.flat01.draw(shader)
        }, 1f)

        // 3d
        val scene = Entity("Scene")
        scene.add(MeshComponent(Shapes.flatCube.front))
        scene.add(rainControl)
        Saveable.registerCustomClass(SnowControl())
        list.add(SceneView.createSceneUI(scene) {
            it.renderView.renderMode = rainRenderMode
        }, 2f)
        list
    }
}