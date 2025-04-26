package me.anno.gpu

import me.anno.audio.streams.AudioStream
import me.anno.engine.EngineBase
import me.anno.gpu.GFX.resetFBStack
import me.anno.gpu.GFX.windows
import me.anno.gpu.VRRenderingRoutine.Companion.shallRenderVR
import me.anno.gpu.VRRenderingRoutine.Companion.vrRoutine
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.framebuffer.NullFramebuffer.setFrameNullSize
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.input.Input
import me.anno.input.Touch
import me.anno.utils.OS
import me.anno.utils.Sleep
import me.anno.utils.pooling.Pools
import me.anno.utils.pooling.Stack

object RenderStep {

    @JvmStatic
    private fun clearState() {
        GPUShader.invalidateBinding()
        Texture2D.destroyTextures()
        OpenGLBuffer.invalidateBinding()
        GFXState.invalidateState()
        Pools.freeUnusedEntries()
        AudioStream.byteBufferPool.freeUnusedEntries()
        Stack.resetAll()
        resetFBStack()

        whiteTexture.bind(0)
    }

    @JvmStatic
    fun beforeRenderSteps() {
        // clear states & reset
        clearState()
        Texture2D.resetBudget()
        Input.resetFrameSpecificKeyStates()

        // work stuff
        Touch.updateAll()
        Sleep.work(true)
    }

    @JvmStatic
    fun renderStep(window: OSWindow, doRender: Boolean) {
        clearState()
        setFrameNullSize(window)
        renderStepImpl(window, doRender)
        GFX.check()
    }

    @JvmStatic
    private fun renderStepImpl(window: OSWindow, doRender: Boolean) {
        val inst = EngineBase.instance
        if (inst != null && doRender) {
            val vrRoutine = vrRoutine
            if (shallRenderVR && vrRoutine != null && window == windows.firstOrNull()) {
                shallRenderVR = vrRoutine.drawFrame(window)
            } else {
                callOnGameLoop(inst, window)
            }
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