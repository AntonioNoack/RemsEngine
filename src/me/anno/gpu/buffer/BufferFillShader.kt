package me.anno.gpu.buffer

import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import org.joml.Vector3i

object BufferFillShader {
    private val fillShader = ComputeShader(
        "fill", Vector3i(64, 1, 1), listOf(
            Variable(GLSLType.V1I, "byteStart"),
            Variable(GLSLType.V1I, "byteLength"),
            Variable(GLSLType.V1I, "value")
        ), """
                layout(std430, binding=0) buffer values1 { int values[]; };
                void main() {
                    int idx = int(gl_GlobalInvocationID.x);

                    int wordStart = byteStart >> 2;
                    int wordEnd   = (byteStart + byteLength + 3) >> 2;
        
                    int wordIndex = wordStart + idx;
                    if (wordIndex >= wordEnd) return;
       
                    int byteOffset = wordIndex * 4 - byteStart;
                    int bytesRemaining = byteLength - byteOffset;
        
                    if (bytesRemaining >= 4 && byteOffset >= 0) {
                        // fully inside region: overwrite whole word
                        values[wordIndex] = value;
                    } else {
                        // edge word: preserve unaffected bytes
                        int oldVal = values[wordIndex];
                        int startByte = max(0, -byteOffset);
                        int endByte   = min(4, byteLength - byteOffset);
                        int mask = 0;
                        for (int b = startByte; b < endByte; ++b) {
                            mask |= 255 << (8 * b);
                        }
                        values[wordIndex] = (oldVal & ~mask) | (value & mask);
                    }
                }
                """.trimIndent()
    )

    fun fill(buffer: GPUBuffer, byteStart: Int, byteLength: Int, value: Int) {
        val start = byteStart shr 2
        val end = (byteStart + byteLength + 3) shr 2
        val shader = fillShader
        shader.use()
        shader.v1i("byteStart", byteStart)
        shader.v1i("byteLength", byteLength)
        shader.v1i("value", value)
        shader.bindBuffer(0, buffer)
        shader.runBySize(end - start, 0, 0)
    }
}