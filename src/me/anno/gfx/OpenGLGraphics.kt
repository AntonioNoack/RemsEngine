package me.anno.gfx

import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.builder.Variable
import me.anno.image.Image
import org.lwjgl.opengl.GL31C.*
import java.nio.Buffer

object OpenGLGraphics : GraphicsAPI {

    private var lastBlending: BlendMode? = BlendMode.DST_ALPHA
    private var lastDepth: DepthMode = DepthMode.FORWARD_DIFFERENT
    private var lastDepthMask: Boolean? = null

    override fun render(
        shader: Shader,
        colorTargets: List<Target>,
        depthTarget: Target,
        primitiveAttributes: Map<String, DataBuffer>,
        instanceAttributes: Map<String, DataBuffer>,
        indices: DataBuffer?,
        drawMode: DrawMode,
        primitivesOffset: Long, primitivesLength: Long,
        instancesOffset: Long, instancesLength: Long,
        viewport: Viewport,
        scissor: Viewport,
        blending: BlendMode?,
        depthMode: DepthMode,
        depthMask: Boolean
    ) {

        if (blending != lastBlending) {
            // blending
            if (blending == null) {
                glDisable(GL_BLEND)
            } else {
                glEnable(GL_BLEND)
                blending.apply()
            }
            lastBlending = blending
        }

        if (depthMode != lastDepth) {
            // depth
            glEnable(GL_DEPTH_TEST)
            glDepthFunc(depthMode.id)
            lastDepth = depthMode
        }

        if (depthMask != lastDepthMask) {
            glDepthMask(depthMask)
            lastDepthMask = depthMask
        }

        // todo bind all uniforms
        shader.bindings

        val hasElements = indices != null
        if (instancesOffset == 0L && instancesLength == 1L) {
            // draw
            if (hasElements) {
                glDrawElements(0, 0, 0, 0)
            } else {
                glDrawArrays(0, 0, 0)
            }
        } else {
            // draw instanced
            if (hasElements) {
                glDrawElementsInstanced(0, 0, 0, 0, 0)
            } else {
                glDrawArraysInstanced(0, 0, 0, 0)
            }
        }
    }

    override fun compute(shader: Shader, groupCountX: Int, groupCountY: Int, groupCountZ: Int) {
        TODO("Not yet implemented")
    }

    override fun compileComputeShader(name: String, variables: List<Variable>, compute: String): ComputeShader {
        TODO("Not yet implemented")
    }

    override fun compileGraphicsShader(
        name: String,
        vertexVars: List<Variable>,
        vertex: String,
        varying: List<Variable>,
        fragmentVars: List<Variable>,
        fragment: String
    ): GraphicsShader {
        TODO("Not yet implemented")
    }

    override fun createTexture2D(image: Image): Texture {
        TODO("Not yet implemented")
    }

    override fun createTexture2D(width: Int, height: Int, samples: Int, format: TargetType): Texture {
        TODO("Not yet implemented")
    }

    override fun createTexture3D(image: Image, sizeZ: Int): Texture {
        TODO("Not yet implemented")
    }

    override fun createTexture3D(
        width: Int,
        height: Int,
        depth: Int,
        samples: Int,
        format: TargetType
    ): Texture {
        TODO("Not yet implemented")
    }

    override fun createTexture2DArray(image: Image): Texture {
        TODO("Not yet implemented")
    }

    override fun createTexture2DArray(
        width: Int,
        height: Int,
        depth: Int,
        samples: Int,
        format: TargetType
    ): Texture {
        TODO("Not yet implemented")
    }

    override fun createTextureCube(images: Array<Image>): Texture {
        TODO("Not yet implemented")
    }

    override fun createTextureCube(size: Int, format: TargetType): Texture {
        TODO("Not yet implemented")
    }

    override fun createBuffer(data: Buffer): DataBuffer {
        TODO("Not yet implemented")
    }

    override fun updateBuffer(buffer: DataBuffer, data: Buffer, offset: Long) {
        TODO("Not yet implemented")
    }

    override fun createRenderbuffer(width: Int, height: Int, samples: Int, format: TargetType): Target {
        TODO("Not yet implemented")
    }
}