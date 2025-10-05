package me.anno.tests.shader

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.graph.visual.render.effects.SnowSettings
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestEngine.Companion.testUI

// get snow effect working like in https://www.glslsandbox.com/e#36547.0
// done get this and rain working in 3d
// done: why is the sky black??? was using SSR instead of color
// todo render snow as SDF instead (?), so we can apply lighting to it without extra cost.
// our snow is transparent though... so that's a little more complicated...

// todo when it's snowing, the sky exactly has snow-flake color, and that everywhere

fun main() {
    // todo synthetic motion blur in 3d
    // todo make close snow flakes out of focus
    testUI("Snow") {
        val list = CustomList(false, style)

        // 2d
        val shader = Shader(
            "snow", emptyList(), coordsUVVertexShader, uvList, listOf(
                Variable(GLSLType.V1F, "time"),
                Variable(GLSLType.V2F, "uvScale"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "float snow(vec2 uv, float scale) {\n" +
                    "   float w=smoothstep(1.,0.,-uv.y*(scale/10.));if(w<.1)return 0.;\n" +
                    "   uv+=time/scale;uv.y+=time*2./scale;uv.x+=sin(uv.y+time*.5)/scale;\n" +
                    "   uv*=scale;vec2 s=floor(uv),f=fract(uv),p;float k=3.,d;\n" +
                    "   p=.5+.35*sin(11.*fract(sin((s+p+scale)*mat2(7,3,6,5))*5.))-f;d=length(p);k=min(d,k);\n" +
                    "   k=smoothstep(0.,k,sin(f.x+f.y)*0.01);\n" +
                    "   return k*w;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "   vec2 uv2 = uv * uvScale;\n" +
                    "   float c=smoothstep(1.,0.3,clamp(uv2.y*.3+.8,0.,.75));\n" +
                    "   c+=snow(uv2,30.)*.3;\n" +
                    "   c+=snow(uv2,20.)*.5;\n" +
                    "   c+=snow(uv2,15.)*.8;\n" +
                    "   c+=snow(uv2,10.);\n" +
                    "   c+=snow(uv2,8.);\n" +
                    "   c+=snow(uv2,6.);\n" +
                    "   c+=snow(uv2,5.);\n" +
                    "   result = vec4(mix(vec3(0.1,0.2,0.3),vec3(1),c),1);\n" +
                    "}"
        )
        list.add(TestDrawPanel {
            shader.use()
            shader.v1f("time", Time.gameTime)
            shader.v2f("uvScale", it.width.toFloat() / it.height, 1f)
            flat01.draw(shader)
        }, 1f)

        // 3d
        val scene = Entity("Scene")
        scene.add(MeshComponent(flatCube.front))
        scene.add(SnowSettings())
        registerCustomClass(SnowSettings())
        list.add(SceneView.createSceneUI(scene) {
            it.renderMode = RenderMode.SNOW
        }, 2f)
        list
    }
}
