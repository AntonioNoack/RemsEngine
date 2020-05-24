package me.anno.ui.fx.physics

import me.anno.gpu.Shader
import org.joml.Vector3f

// todo accuracy > mass, redo this :)

abstract class ForceField {

    // todo particles size?
    // todo allow rotation of every force field?
    // todo more inputs

    abstract fun getForce(position: Vector3f, speed: Vector3f, particleId: Int, time: Float): Vector3f
    abstract fun getShaderUniforms(): String
    abstract fun getShaderForce(): String

    lateinit var shader: Shader
    var hasShader = false

    fun bind(){
        if(!hasShader){
            shader = Shader("" +
                    "attribute vec2 attr0;\n" +
                    "uniform vec2 inPosSize, outPosSize;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4(vec2(attr0.x, outPosSize.x + outPosSize.y * attr0.y)*2.-1.,0.0,1.0)\n" +
                    "   uv = vec2(attr0.x, inPosSize.x + inPosSize.y * attr0.y);\n" +
                    "}", "" +
                    "varying vec2 uv;\n", "" +
                    "layout(location = 0) out vec3 position;\n" +
                    "layout(location = 1) out vec3 velocity;\n" +
                    "layout(location = 2) out vec4 rotation;\n" +
                    "" +
                    "vec3 getForce(p, v, r){" +
                    // "   vec3 force = vec3(0.0);\n" +
                    "   ${getShaderForce()}\n" +
                    "   return force;\n" +
                    "}" +
                    "" +
                    "uniform float dt;\n" +
                    getShaderUniforms() +
                    "" +
                    "void main(){" +
                    "   vec3 p = texture(inPosition, uv);\n" +
                    "   vec3 v = texture(inVelocity, uv);\n" +
                    "   vec4 r = texture(inRotation, uv);\n" +
                    "   vec3 force = getForce(p, v, r);\n" +
                    "   position = p + v * dt;\n" +
                    "   velocity = v + force * dt;\n" +
                    "   rotation = r;\n" + // todo a function to control advanced rotation...
                    "}")
        }
        shader.use()
    }

    // todo test, apply inputs and outputs, todo grab outputs to check them :D
    fun apply(particles: ParticleGroup, frame: Int, time: Float){
        bind()
        particles.data0.bindTextures()
        // todo render onto same texture ... somehow...

    }

    // todo a function to render the particles...


}