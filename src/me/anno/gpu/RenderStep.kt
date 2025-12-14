package me.anno.gpu

import me.anno.audio.streams.AudioStream
import me.anno.engine.EngineBase
import me.anno.engine.ui.vr.VRRenderingRoutine.Companion.shallRenderVR
import me.anno.engine.ui.vr.VRRenderingRoutine.Companion.vrRoutine
import me.anno.gpu.GFX.resetFBStack
import me.anno.gpu.GFX.windows
import me.anno.gpu.buffer.BufferState
import me.anno.gpu.buffer.GPUBuffer
import me.anno.gpu.framebuffer.NullFramebuffer.setFrameNullSize
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.builder.ShaderPrinting
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.input.Input
import me.anno.input.Touch
import me.anno.utils.OSFeatures
import me.anno.utils.Sleep
import me.anno.utils.pooling.Pools
import me.anno.utils.pooling.Stack

object RenderStep {

    @JvmStatic
    private fun clearState() {
        GFXState.bindVAO() // just in case
        GPUShader.invalidateBinding()
        Texture2D.invalidateBinding()
        Texture2D.destroyTextures()
        GPUBuffer.invalidateBinding()
        GFXContext.invalidateState()
        BufferState.invalidateBinding()
        Pools.freeUnusedEntries()
        AudioStream.byteBufferPool.freeUnusedEntries()
        Stack.resetAll()
        resetFBStack()

        whiteTexture.bind(0)
    }

    @JvmStatic
    fun beforeRenderSteps() {
        // clear states & reset
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
        ShaderPrinting.printFromBuffer()
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
        try {
            inst.onGameLoop(window, window.width, window.height)
        } catch (e: Exception) {
            e.printStackTrace()
            if (OSFeatures.canSleep) Thread.sleep(250)
        }
    }
}