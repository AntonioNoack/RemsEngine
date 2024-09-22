package me.anno.tests.openxr

import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.opengl.GL46C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL46C.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL46C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL46C.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL46C.GL_DEPTH_TEST
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL46C.GL_LINEAR
import org.lwjgl.opengl.GL46C.GL_SCISSOR_TEST
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL46C.glBindFramebuffer
import org.lwjgl.opengl.GL46C.glBindVertexArray
import org.lwjgl.opengl.GL46C.glBlitNamedFramebuffer
import org.lwjgl.opengl.GL46C.glCheckFramebufferStatus
import org.lwjgl.opengl.GL46C.glClear
import org.lwjgl.opengl.GL46C.glClearColor
import org.lwjgl.opengl.GL46C.glCreateVertexArrays
import org.lwjgl.opengl.GL46C.glDisable
import org.lwjgl.opengl.GL46C.glEnable
import org.lwjgl.opengl.GL46C.glFramebufferTexture2D
import org.lwjgl.opengl.GL46C.glGenFramebuffers
import org.lwjgl.opengl.GL46C.glViewport
import org.lwjgl.openxr.XR10.XR_SPACE_LOCATION_ORIENTATION_VALID_BIT
import org.lwjgl.openxr.XrQuaternionf
import org.lwjgl.openxr.XrSpaceLocation
import org.lwjgl.openxr.XrVector3f

var window = 0L

val shader = Shader(
    "test", listOf(
        Variable(GLSLType.M4x4, "proj"),
        Variable(GLSLType.M4x4, "model"),
        Variable(GLSLType.M4x4, "view"),
        Variable(GLSLType.V3F, "aPos", VariableMode.ATTR),
        Variable(GLSLType.V2F, "aColor", VariableMode.ATTR),
    ), "" +
            "void main() {\n" +
            "	gl_Position = proj * view * model * vec4(aPos, 1.0);\n" +
            "	vertexColor = aColor;\n" +
            "}\n", listOf(
        Variable(GLSLType.V2F, "vertexColor")
    ), listOf(
        Variable(GLSLType.V3F, "uniformColor"),
        Variable(GLSLType.V4F, "result", VariableMode.OUT),
    ), "" +
            "void main() {\n" +
            "	result = uniformColor.x == 0.0 ? vec4(vertexColor, 1.0, 1.0) : vec4(uniformColor, 1.0);\n" +
            "}\n"
)

lateinit var buffer1: StaticBuffer

var vao = 0
var framebuffer = 0

fun initGL() {

    val vertices = floatArrayOf(
        -0.5f, -0.5f, -0.5f, 0.0f, 0.0f, 0.5f, -0.5f, -0.5f, 1.0f, 0.0f,
        0.5f, 0.5f, -0.5f, 1.0f, 1.0f, 0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
        -0.5f, 0.5f, -0.5f, 0.0f, 1.0f, -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,

        -0.5f, -0.5f, 0.5f, 0.0f, 0.0f, 0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
        0.5f, 0.5f, 0.5f, 1.0f, 1.0f, 0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
        -0.5f, 0.5f, 0.5f, 0.0f, 1.0f, -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,

        -0.5f, 0.5f, 0.5f, 1.0f, 0.0f, -0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
        -0.5f, -0.5f, -0.5f, 0.0f, 1.0f, -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
        -0.5f, -0.5f, 0.5f, 0.0f, 0.0f, -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,

        0.5f, 0.5f, 0.5f, 1.0f, 0.0f, 0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
        0.5f, -0.5f, -0.5f, 0.0f, 1.0f, 0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
        0.5f, -0.5f, 0.5f, 0.0f, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f, 0.0f,

        -0.5f, -0.5f, -0.5f, 0.0f, 1.0f, 0.5f, -0.5f, -0.5f, 1.0f, 1.0f,
        0.5f, -0.5f, 0.5f, 1.0f, 0.0f, 0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
        -0.5f, -0.5f, 0.5f, 0.0f, 0.0f, -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,

        -0.5f, 0.5f, -0.5f, 0.0f, 1.0f, 0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
        0.5f, 0.5f, 0.5f, 1.0f, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
        -0.5f, 0.5f, 0.5f, 0.0f, 0.0f, -0.5f, 0.5f, -0.5f, 0.0f, 1.0f
    )

    buffer1 = StaticBuffer(
        "cube", vertices, listOf(
            Attribute("aPos", 3),
            Attribute("aColor", 2)
        )
    )

    framebuffer = glGenFramebuffers()
    vao = glCreateVertexArrays()
    glBindVertexArray(vao)

    glEnable(GL_DEPTH_TEST)
}

fun renderBlock(
    position: XrVector3f,
    orientation: XrQuaternionf,
    scale: Vector3f,
) {
    val modelMatrix = Matrix4f()
        .translate(position.x(), position.y(), position.z())
        .rotateQ(orientation.x(), orientation.y(), orientation.z(), orientation.w())
        .scale(scale)
    shader.m4x4("model", modelMatrix)
    buffer1.draw(shader)
}

fun renderRotatedCube(
    position: Vector3f,
    cubeSize: Float,
    rotation: Float,
) {
    val modelMatrix = Matrix4f()
        .translate(position)
        .rotateY(rotation.toRadians())
        .scale(cubeSize * 0.5f)
    shader.m4x4("model", modelMatrix)
    buffer1.draw(shader)
}

fun renderFrame1(
    w: Int, h: Int,
    predictedDisplayTime: Long,
    handLocations: XrSpaceLocation.Buffer?,
    projectionMatrix: Matrix4f,
    viewMatrix: Matrix4f,
    framebuffer: Int,
    image: Int,
    depthBuffer: Int,
) {

    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
    glViewport(0, 0, w, h)
    glDisable(GL_SCISSOR_TEST)

    if (framebuffer != 0) {
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, image, 0)
        if (depthBuffer > 0) {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthBuffer, 0)
        } else {
            // todo of task: use separately generated depth renderbuffer
        }
        assertEquals(GL_FRAMEBUFFER_COMPLETE, glCheckFramebufferStatus(GL_FRAMEBUFFER))
    }

    glClearColor(0f, 0f, 0.2f, 1f)
    glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

    shader.use()
    glBindVertexArray(vao)

    shader.m4x4("view", viewMatrix)
    shader.m4x4("proj", projectionMatrix)
    shader.v3f("uniformColor", 0f)

    val displayTimeSeconds = predictedDisplayTime / 1e9f
    val angle = (displayTimeSeconds * 360 * .25f) % 360
    val dist = 1.5f
    val height = 0.5f

    renderRotatedCube(Vector3f(0f, height, -dist), 0.33f, angle)
    renderRotatedCube(Vector3f(0f, height, dist), 0.33f, angle)
    renderRotatedCube(Vector3f(dist, height, 0f), 0.33f, angle)
    renderRotatedCube(Vector3f(-dist, height, 0f), 0.33f, angle)

    if (handLocations != null) {
        for (hand in 0 until 2) {
            if (hand == 0) {
                shader.v3f("uniformColor", 1f, 0.5f, 0.5f)
            } else {
                shader.v3f("uniformColor", 0.5f, 1f, 0.5f)
            }
            val isValid = handLocations[hand].locationFlags().hasFlag(XR_SPACE_LOCATION_ORIENTATION_VALID_BIT.toLong())
            if (!isValid) continue
            val scale = Vector3f(0.07f, 0.02f, 0.2f)
            renderBlock(
                handLocations[hand].pose().`position$`(),
                handLocations[hand].pose().orientation(),
                scale
            )
        }
    }

    GFX.check()
}

fun copyToFB1(framebuffer: Int, w: Int, h: Int) {
    // copy left eye to desktop window
    glfwGetWindowSize(window, ws, hs)
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glBlitNamedFramebuffer(
        framebuffer,
        0, 0, 0, w, h,
        0, 0, ws[0], hs[0],
        GL_COLOR_BUFFER_BIT,
        GL_LINEAR
    )
    glfwSwapBuffers(window)
}

val ws = IntArray(1)
val hs = IntArray(1)

fun createViewMatrix(viewMatrix: Matrix4f, pos: XrVector3f, orientation: XrQuaternionf) {
    viewMatrix.identity()
        .translate(pos.x(), pos.y(), pos.z())
        .rotateQ(orientation.x(), orientation.y(), orientation.z(), orientation.w())
        .invert()
}
