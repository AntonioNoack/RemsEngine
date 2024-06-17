package me.anno.tests.openxr

import me.anno.gpu.GFX
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.opengl.GL46C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL46C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL46C.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL46C.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL46C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL46C.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL46C.GL_DEPTH_TEST
import org.lwjgl.opengl.GL46C.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL46C.GL_LINEAR
import org.lwjgl.opengl.GL46C.GL_LINK_STATUS
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL46C.GL_TRIANGLES
import org.lwjgl.opengl.GL46C.GL_TRUE
import org.lwjgl.opengl.GL46C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL46C.glAttachShader
import org.lwjgl.opengl.GL46C.glBindBuffer
import org.lwjgl.opengl.GL46C.glBindFramebuffer
import org.lwjgl.opengl.GL46C.glBindVertexArray
import org.lwjgl.opengl.GL46C.glBlitNamedFramebuffer
import org.lwjgl.opengl.GL46C.glBufferData
import org.lwjgl.opengl.GL46C.glCheckFramebufferStatus
import org.lwjgl.opengl.GL46C.glClear
import org.lwjgl.opengl.GL46C.glClearColor
import org.lwjgl.opengl.GL46C.glCompileShader
import org.lwjgl.opengl.GL46C.glCreateProgram
import org.lwjgl.opengl.GL46C.glCreateShader
import org.lwjgl.opengl.GL46C.glDeleteShader
import org.lwjgl.opengl.GL46C.glDrawArrays
import org.lwjgl.opengl.GL46C.glEnable
import org.lwjgl.opengl.GL46C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL46C.glFramebufferTexture2D
import org.lwjgl.opengl.GL46C.glGenBuffers
import org.lwjgl.opengl.GL46C.glGenVertexArrays
import org.lwjgl.opengl.GL46C.glGetProgrami
import org.lwjgl.opengl.GL46C.glGetShaderi
import org.lwjgl.opengl.GL46C.glGetUniformLocation
import org.lwjgl.opengl.GL46C.glLinkProgram
import org.lwjgl.opengl.GL46C.glScissor
import org.lwjgl.opengl.GL46C.glShaderSource
import org.lwjgl.opengl.GL46C.glUniform3f
import org.lwjgl.opengl.GL46C.glUniformMatrix4fv
import org.lwjgl.opengl.GL46C.glUseProgram
import org.lwjgl.opengl.GL46C.glVertexAttribPointer
import org.lwjgl.opengl.GL46C.glViewport
import org.lwjgl.openxr.XR10.XR_SPACE_LOCATION_ORIENTATION_VALID_BIT
import org.lwjgl.openxr.XrQuaternionf
import org.lwjgl.openxr.XrSpaceLocation
import org.lwjgl.openxr.XrVector3f

const val vertexShader = "#version 330 core\n" +
        "#extension GL_ARB_explicit_uniform_location : require\n" +
        "layout(location = 0) in vec3 aPos;\n" +
        "layout(location = 1) in vec2 aColor;\n" +
        "layout(location = 2) uniform mat4 model;\n" +
        "layout(location = 3) uniform mat4 view;\n" +
        "layout(location = 4) uniform mat4 proj;\n" +
        "out vec2 vertexColor;\n" +
        "void main() {\n" +
        "	gl_Position = proj * view * model * vec4(aPos, 1.0);\n" +
        "	vertexColor = aColor;\n" +
        "}\n"

const val fragmentShader = "#version 330 core\n" +
        "#extension GL_ARB_explicit_uniform_location : require\n" +
        "layout(location = 0) out vec4 FragColor;\n" +
        "layout(location = 5) uniform vec3 uniformColor;\n" +
        "in vec2 vertexColor;\n" +
        "void main() {\n" +
        "	FragColor = uniformColor.x == 0.0 ? vec4(vertexColor, 1.0, 1.0) : vec4(uniformColor, 1.0);\n" +
        "}\n"

var window = 0L

var program = 0
var vao = 0
var vbo = 0

fun initGL() {
    val vsi = glCreateShader(GL_VERTEX_SHADER)
    glShaderSource(vsi, vertexShader)
    glCompileShader(vsi)
    assertEquals(GL_TRUE, glGetShaderi(vsi, GL_COMPILE_STATUS))

    val fsi = glCreateShader(GL_FRAGMENT_SHADER)
    glShaderSource(fsi, fragmentShader)
    glCompileShader(fsi)
    assertEquals(GL_TRUE, glGetShaderi(fsi, GL_COMPILE_STATUS))

    program = glCreateProgram()
    glAttachShader(program, vsi)
    glAttachShader(program, fsi)
    glLinkProgram(program)
    assertEquals(GL_TRUE, glGetProgrami(program, GL_LINK_STATUS))

    glDeleteShader(vsi)
    glDeleteShader(fsi)

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

    vbo = glGenBuffers()
    vao = glGenVertexArrays()
    glBindVertexArray(vao)
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW)
    glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * 4, 0)
    glEnableVertexAttribArray(0)

    glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * 4, 3 * 4)
    glEnableVertexAttribArray(1)

    glEnable(GL_DEPTH_TEST)
}

val buffer = FloatArray(16)

fun Matrix4f.ptr(): FloatArray {
    get(buffer)
    return buffer
}

fun renderBlock(
    position: XrVector3f,
    orientation: XrQuaternionf,
    scale: Vector3f,
    modelLoc: Int,
) {
    val modelMatrix = Matrix4f()
        .translate(position.x(), position.y(), position.z())
        .rotateQ(orientation.x(), orientation.y(), orientation.z(), orientation.w())
        .scale(scale)
    glUniformMatrix4fv(modelLoc, false, modelMatrix.ptr())
    glDrawArrays(GL_TRIANGLES, 0, 36)
}

fun renderRotatedCube(
    position: Vector3f,
    cubeSize: Float,
    rotation: Float,
    modelLoc: Int,
) {
    val modelMatrix = Matrix4f()
        .translate(position)
        .rotateY(rotation.toRadians())
        .scale(cubeSize * 0.5f)
    glUniformMatrix4fv(modelLoc, false, modelMatrix.ptr())
    glDrawArrays(GL_TRIANGLES, 0, 36)
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
    glScissor(0, 0, w, h)

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

    glUseProgram(program)
    glBindVertexArray(vao)

    val modelLoc = glGetUniformLocation(program, "model")
    val colorLoc = glGetUniformLocation(program, "uniformColor")
    val viewLoc = glGetUniformLocation(program, "view")
    glUniformMatrix4fv(viewLoc, false, viewMatrix.ptr())
    val projLoc = glGetUniformLocation(program, "proj")
    glUniformMatrix4fv(projLoc, false, projectionMatrix.ptr())

    glUniform3f(colorLoc, 0f, 0f, 0f)
    val displayTimeSeconds = predictedDisplayTime / 1e9f
    val angle = (displayTimeSeconds * 360 * .25f) % 360
    val dist = 1.5f
    val height = 0.5f

    renderRotatedCube(Vector3f(0f, height, -dist), 0.33f, angle, modelLoc)
    renderRotatedCube(Vector3f(0f, height, dist), 0.33f, angle, modelLoc)
    renderRotatedCube(Vector3f(dist, height, 0f), 0.33f, angle, modelLoc)
    renderRotatedCube(Vector3f(-dist, height, 0f), 0.33f, angle, modelLoc)

    if (handLocations != null) {
        for (hand in 0 until 2) {
            if (hand == 0) {
                glUniform3f(colorLoc, 1f, 0.5f, 0.5f)
            } else {
                glUniform3f(colorLoc, 0.5f, 1f, 0.5f)
            }
            val isValid = handLocations[hand].locationFlags().hasFlag(XR_SPACE_LOCATION_ORIENTATION_VALID_BIT.toLong())
            if (!isValid) continue
            val scale = Vector3f(0.07f, 0.02f, 0.2f)
            renderBlock(
                handLocations[hand].pose().`position$`(),
                handLocations[hand].pose().orientation(),
                scale, modelLoc
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
