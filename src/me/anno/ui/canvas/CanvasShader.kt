package me.anno.ui.canvas

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

object CanvasShader : Shader(
    "CanvasShader",
    listOf(
        Variable(GLSLType.V4I, "instBounds", VariableMode.ATTR),
        Variable(GLSLType.V4I, "instScissor", VariableMode.ATTR),
        Variable(GLSLType.V4I, "instTexBounds", VariableMode.ATTR),
        Variable(GLSLType.V4F, "instFgColor", VariableMode.ATTR),
        Variable(GLSLType.V4F, "instBgColor", VariableMode.ATTR),
        Variable(GLSLType.V1I, "instMode", VariableMode.ATTR),
        Variable(GLSLType.V2F, "positions", VariableMode.ATTR),
        Variable(GLSLType.V2F, "invRenderSize"),
        Variable(GLSLType.V2F, "invAtlasSize"),
        Variable(GLSLType.V2I, "dstOffset"),
        Variable(GLSLType.M4x4, "transform"),
    ), """
                        void main() {
                           bounds = instBounds;
                           scissor = instScissor;
                           texBounds = instTexBounds;
                           fgColor = instFgColor;
                           bgColor = instBgColor;
                           mode = instMode;
                           vec2 pos = mix(vec2(instBounds.xy + dstOffset), vec2(instBounds.zw + dstOffset), positions);
                           pos = pos * invRenderSize * 2.0 - 1.0;
                           pos.y = -pos.y;
                           uv = mix(vec2(instTexBounds.xw), vec2(instTexBounds.zy), positions) * invAtlasSize;
                           gl_Position = matMul(transform, vec4(pos, 0.0, 1.0));
                        }
                    """.trimIndent(), listOf(
        Variable(GLSLType.V4I, "bounds").flat(),
        Variable(GLSLType.V4I, "scissor").flat(),
        Variable(GLSLType.V4I, "texBounds").flat(),
        Variable(GLSLType.V4F, "fgColor").flat(),
        Variable(GLSLType.V4F, "bgColor").flat(),
        Variable(GLSLType.V1I, "mode").flat(),
        Variable(GLSLType.V2F, "uv"),
    ), listOf(
        Variable(GLSLType.S2D, "atlasTexture"),
        Variable(GLSLType.V4F, "result", VariableMode.OUT),
    ), """
                        void main() {
                            result = fgColor;
                            switch (mode & 0xff) {
                                case ${CanvasDrawMode.RECTANGLE.ordinal}: break;
                                case ${CanvasDrawMode.TEXTURE.ordinal}:
                                    result *= texture(atlasTexture, uv);
                                    break;
                                case ${CanvasDrawMode.TEXTURE_NO_ALPHA.ordinal}:
                                    result.rgb *= texture(atlasTexture, uv).rgb;
                                    break;
                                case ${CanvasDrawMode.TEXT.ordinal}:
                                    vec4 factor = texture(atlasTexture, uv).rgbg;
                                    if (dot(factor.rgb, vec3(1.0)) < 0.01) discard;
                                    result = mix(bgColor, fgColor, factor);
                                    break;
                                default:
                                    float c = float(int(dot(gl_FragCoord.xy,vec2(1.0))) & 4) * 0.333;
                                    result = vec4(c,0.0,c,1.0);
                                    break;
                            }
                        }
                    """.trimIndent()
)