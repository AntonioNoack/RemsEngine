package me.anno.tests.openxr

import me.anno.gpu.GFX
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11C.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11C.GL_LINEAR
import org.lwjgl.opengl.GL11C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11C.GL_TRIANGLES
import org.lwjgl.opengl.GL11C.GL_TRUE
import org.lwjgl.opengl.GL11C.glClear
import org.lwjgl.opengl.GL11C.glDrawArrays
import org.lwjgl.opengl.GL11C.glEnable
import org.lwjgl.opengl.GL11C.glScissor
import org.lwjgl.opengl.GL11C.glViewport
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30C.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL30C.GL_FLOAT
import org.lwjgl.opengl.GL30C.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL30C.GL_LINK_STATUS
import org.lwjgl.opengl.GL30C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL30C.glAttachShader
import org.lwjgl.opengl.GL30C.glBindBuffer
import org.lwjgl.opengl.GL30C.glBindFramebuffer
import org.lwjgl.opengl.GL30C.glBindVertexArray
import org.lwjgl.opengl.GL30C.glBufferData
import org.lwjgl.opengl.GL30C.glCheckFramebufferStatus
import org.lwjgl.opengl.GL30C.glClearColor
import org.lwjgl.opengl.GL30C.glCompileShader
import org.lwjgl.opengl.GL30C.glCreateProgram
import org.lwjgl.opengl.GL30C.glCreateShader
import org.lwjgl.opengl.GL30C.glDeleteShader
import org.lwjgl.opengl.GL30C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL30C.glFramebufferTexture2D
import org.lwjgl.opengl.GL30C.glGenBuffers
import org.lwjgl.opengl.GL30C.glGenFramebuffers
import org.lwjgl.opengl.GL30C.glGenVertexArrays
import org.lwjgl.opengl.GL30C.glGetProgrami
import org.lwjgl.opengl.GL30C.glGetShaderi
import org.lwjgl.opengl.GL30C.glGetUniformLocation
import org.lwjgl.opengl.GL30C.glLinkProgram
import org.lwjgl.opengl.GL30C.glShaderSource
import org.lwjgl.opengl.GL30C.glUniform3f
import org.lwjgl.opengl.GL30C.glUniformMatrix4fv
import org.lwjgl.opengl.GL30C.glUseProgram
import org.lwjgl.opengl.GL30C.glVertexAttribPointer
import org.lwjgl.opengl.GL45C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL45C.glBlitNamedFramebuffer
import org.lwjgl.openxr.XrFovf
import org.lwjgl.openxr.XrQuaternionf
import org.lwjgl.openxr.XrSpaceLocation
import org.lwjgl.openxr.XrVector3f
import kotlin.math.tan

val vertexShader = "#version 330 core\n" +
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

val fragmentShader = "#version 330 core\n" +
        "#extension GL_ARB_explicit_uniform_location : require\n" +
        "layout(location = 0) out vec4 FragColor;\n" +
        "layout(location = 5) uniform vec3 uniformColor;\n" +
        "in vec2 vertexColor;\n" +
        "void main() {\n" +
        "	FragColor = vec4(vertexColor, 1.0, 1.0);\n" +
        "}\n"

var program = 0
var vao = 0
var vbo = 0

fun initGL(framebuffers: Array<IntArray>) {
    for (i in framebuffers.indices) {
        glGenFramebuffers(framebuffers[i])
    }

    val vsi = glCreateShader(GL_VERTEX_SHADER)
    glShaderSource(vsi, vertexShader)
    glCompileShader(vsi)
    assertEquals(GL_TRUE, glGetShaderi(vsi, GL_COMPILE_STATUS))

    val fsi = glCreateShader(GL_FRAGMENT_SHADER)
    GL20C.glShaderSource(fsi, fragmentShader)
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
    position: Vector3f,
    orientation: Quaternionf,
    radi: Vector3f,
    modelLoc: Int,
) {
    val modelMatrix = Matrix4f()
        .translate(position)
        .rotate(orientation)
        .scale(radi)
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

fun renderFrame(
    w: Int, h: Int,
    predictedDisplayTime: Long,
    viewIndex: Int,
    handLocations: Array<XrSpaceLocation>?,
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
        if (hasDepth) {
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
    val projLoc = GL20C.glGetUniformLocation(program, "proj")
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

    // todo render controllers, when we have implemented them

    if (viewIndex == 0) {
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

    GFX.check()
}

val ws = IntArray(1)
val hs = IntArray(1)

fun createProjectionFov(projectionMatrix: Matrix4f, fov: XrFovf, nearZ: Float, farZ: Float) {
    val tanAngleLeft = tan(fov.angleLeft())
    val tanAngleRight = tan(fov.angleRight())
    val tanAngleUp = tan(fov.angleUp())
    val tanAngleDown = tan(fov.angleDown())
    val tanAngleWidth = tanAngleRight - tanAngleLeft
    val tanAngleHeight = tanAngleUp - tanAngleDown
    val offsetZ = nearZ + 0f
    if (false) {
        projectionMatrix.set(
            // reverse depth
            2f / tanAngleWidth, 0f, (tanAngleRight + tanAngleLeft) / tanAngleWidth, 0f,
            0f, 2f / tanAngleHeight, (tanAngleUp + tanAngleDown) / tanAngleHeight, 0f,
            0f, 0f, -1f, -(nearZ + offsetZ),
            0f, 0f, -1f, 0f
        ).transpose()
    } else {
        projectionMatrix.set(
            // normal projection
            2f / tanAngleWidth, 0f, (tanAngleRight + tanAngleLeft) / tanAngleWidth, 0f,
            0f, 2f / tanAngleHeight, (tanAngleUp + tanAngleDown) / tanAngleHeight, 0f,
            0f, 0f, -(farZ + offsetZ) / (farZ - nearZ), -(farZ * (nearZ + offsetZ)) / (farZ - nearZ),
            0f, 0f, -1f, 0f
        ).transpose()
    }
}

fun createViewMatrix(viewMatrix: Matrix4f, pos: XrVector3f, orientation: XrQuaternionf) {
    viewMatrix.identity()
        .translate(pos.x(), pos.y(), pos.z())
        .rotate(Quaternionf(orientation.x(), orientation.y(), orientation.z(), orientation.w()))
        .invert()
}
