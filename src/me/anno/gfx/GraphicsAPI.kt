package me.anno.gfx

import me.anno.gpu.DepthMode
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.builder.Variable
import me.anno.image.Image
import org.joml.Vector4f
import java.nio.Buffer

// todo define this for OpenGL
// todo use this
interface GraphicsAPI {

    fun render(
        shader: Shader, colorTargets: List<Target>, depthTarget: Target,
        primitiveAttributes: Map<String, DataBuffer>,
        instanceAttributes: Map<String, DataBuffer>,// drawArrays/drawElements
        indices: DataBuffer?, // draw/drawInstanced
        drawMode: DrawMode,
        // these for primitives and instances
        primitivesOffset: Long, primitivesLength: Long,
        instancesOffset: Long, instancesLength: Long,
        viewport: Viewport, scissor: Viewport,
        blending: BlendMode?, depthMode: DepthMode, depthMask: Boolean
    )

    fun compute(
        shader: Shader,
        groupCountX: Int, groupCountY: Int, groupCountZ: Int
    )

    fun compileComputeShader(
        name: String, variables: List<Variable>, compute: String
    ): ComputeShader

    fun compileGraphicsShader(
        name: String, vertexVars: List<Variable>, vertex: String,
        varying: List<Variable>, fragmentVars: List<Variable>, fragment: String
    ): GraphicsShader

    // todo could be done using a shader...
    fun clear(target: Target, value: Vector4f) {
    }

    fun createTexture2D(image: Image): Texture
    fun createTexture3D(image: Image, sizeZ: Int): Texture
    fun createTexture2DArray(image: Image): Texture
    fun createTextureCube(images: Array<Image>): Texture

    fun createTexture2D(width: Int, height: Int, samples: Int, format: TargetType): Texture
    fun createTexture3D(width: Int, height: Int, depth: Int, samples: Int, format: TargetType): Texture
    fun createTexture2DArray(width: Int, height: Int, depth: Int, samples: Int, format: TargetType): Texture
    fun createTextureCube(size: Int, format: TargetType): Texture

    fun createBuffer(data: Buffer): DataBuffer
    fun updateBuffer(buffer: DataBuffer, data: Buffer, offset: Long)

    fun createRenderbuffer(width: Int, height: Int, samples: Int, format: TargetType): Target
}