package me.anno.tests.openxr

import me.anno.gpu.RenderDoc
import me.anno.openxr.FPSPrinter
import me.anno.openxr.OpenXR
import me.anno.openxr.copyToFB1
import me.anno.openxr.createProjectionFov
import me.anno.openxr.createViewMatrix
import me.anno.openxr.farZ
import me.anno.openxr.initFramebuffers
import me.anno.openxr.initGL
import me.anno.openxr.nearZ
import me.anno.openxr.renderFrame1
import me.anno.openxr.window
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwSwapInterval
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.opengl.GL
import org.lwjgl.openxr.XR10.xrDestroyInstance
import org.lwjgl.openxr.XrFovf
import org.lwjgl.openxr.XrQuaternionf
import org.lwjgl.openxr.XrSpaceLocation
import org.lwjgl.openxr.XrVector3f

/**
 * My test device: Meta Quest 3
 * Working with SteamVR
 * Not yet working on Meta Link
 * */
fun main() {
    val debugRendering = false
    if (debugRendering) {
        RenderDoc.forceLoadRenderDoc()
    }
    initGLFW()
    if (debugRendering) {
        runSimpleRenderLoop()
    } else {
        runOpenXRRenderLoop()
    }
}

fun runOpenXRRenderLoop() {

    // we don't want to be limited by the desktop refresh rate, OpenXR has its own limiters
    glfwSwapInterval(0)

    val xr = object : OpenXR(window) {
        override fun copyToDesktopWindow(framebuffer: Int, w: Int, h: Int) {
            copyToFB1(framebuffer, w, h)
        }

        override fun renderFrame(
            viewIndex: Int, w: Int, h: Int, predictedDisplayTime: Long, handLocations: XrSpaceLocation.Buffer?,
            framebuffer: Int, colorTexture: Int, depthTexture: Int
        ) {
            val view = views[viewIndex]
            createViewMatrix(viewMatrix, view.pose().`position$`(), view.pose().orientation())
            renderFrame1(
                w, h, predictedDisplayTime, handLocations,
                projectionMatrix, viewMatrix, framebuffer, colorTexture, depthTexture
            )
        }
    }
    initFramebuffers()
    initGL()
    val printer = FPSPrinter()
    while (!glfwWindowShouldClose(window)) {
        glfwPollEvents()
        xr.renderFrameMaybe()
        printer.nextFrame()
    }

    xrDestroyInstance(xr.instance)
    finishGLFW()
}

fun initGLFW() {
    assertTrue(glfwInit())
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
    // glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    window = glfwCreateWindow(800, 600, "OpenXR", 0, 0)
    assertNotEquals(0, window)
    glfwMakeContextCurrent(window)
    GL.createCapabilities()
}

fun runSimpleRenderLoop() {
    initGL()
    val fov = XrFovf.calloc()
        .angleUp(45f.toRadians())
        .angleDown((-45f).toRadians())
        .angleRight(45f.toRadians())
        .angleLeft((-45f).toRadians())
    val projectionMatrix = Matrix4f()
    createProjectionFov(projectionMatrix, fov, nearZ, farZ)
    val viewMatrix = Matrix4f()
    val pos = XrVector3f.calloc()
    val rot0 = Quaternionf()
    val rot = XrQuaternionf.calloc()
    val ws = IntArray(1)
    val hs = IntArray(1)
    val printer = FPSPrinter()
    while (!glfwWindowShouldClose(window)) {
        glfwPollEvents()
        glfwGetWindowSize(window, ws, hs)
        rot0.rotateY(0.1f)
        rot.set(rot0.x, rot0.y, rot0.z, rot0.w)
        createViewMatrix(viewMatrix, pos, rot)
        renderFrame1(
            ws[0], hs[0], 0, null, projectionMatrix,
            viewMatrix, 0, 0, 0
        )
        glfwSwapBuffers(window)
        printer.nextFrame()
    }
}

fun finishGLFW() {
    glfwDestroyWindow(window)
    glfwTerminate()
}