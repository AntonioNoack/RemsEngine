package me.anno.gpu

import me.anno.audio.streams.AudioStream
import me.anno.engine.EngineBase
import me.anno.engine.Events
import me.anno.gpu.GFX.resetFBStack
import me.anno.gpu.GFX.shallRenderVR
import me.anno.gpu.GFX.vrRenderingRoutine
import me.anno.gpu.GFX.windows
import me.anno.gpu.GPUTasks.workGPUTasks
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.framebuffer.NullFramebuffer.setFrameNullSize
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.input.Input
import me.anno.utils.OS
import me.anno.utils.pooling.Pools

object RenderStep {

    @JvmStatic
    fun renderStep(window: OSWindow, doRender: Boolean) {

        GPUShader.invalidateBinding()
        Texture2D.destroyTextures()
        OpenGLBuffer.invalidateBinding()
        GFXState.invalidateState()

        Pools.freeUnusedEntries()
        AudioStream.bufferPool.freeUnusedEntries()

        setFrameNullSize(window)

        me.anno.utils.pooling.Stack.resetAll()

        resetFBStack()

        workGPUTasks(false)

        resetFBStack()

        // rendering and editor section

        Input.resetFrameSpecificKeyStates()

        Events.workEventTasks()

        setFrameNullSize(window)

        Texture2D.resetBudget()

        GFX.check()

        whiteTexture.bind(0)

        GFX.check()

        resetFBStack()

        val inst = EngineBase.instance
        if (inst != null && doRender) {
            if (shallRenderVR && window == windows.firstOrNull()) {
                shallRenderVR = vrRenderingRoutine!!.drawFrame(window)
            } else {
                callOnGameLoop(inst, window)
            }
            resetFBStack()
            GFX.check()
        }
    }

    fun callOnGameLoop(inst: EngineBase, window: OSWindow) {
        // in case of an error, we have to fix it,
        // so give us the best chance to do so:
        //  - on desktop, sleep a little, so we don't get too many errors
        //  - on web, just crash, we cannot sleep there
        if (OS.isWeb) {
            inst.onGameLoop(window, window.width, window.height)
        } else {
            try {
                inst.onGameLoop(window, window.width, window.height)
            } catch (e: Exception) {
                e.printStackTrace()
                Thread.sleep(250)
            }
        }
    }

}