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
        Variable(GLSLType.V1F, "invAtlasSize"),
        Variable(GLSLType.V2I, "dstOffset"),
        Variable(GLSLType.M4x4, "transform"),
    ), """
                        void main() {
                           vec2 pos0 = vec2(max(instBounds.xy, instScissor.xy));
                           vec2 pos1 = vec2(min(instBounds.zw, instScissor.zw));
                           vec2 pos = mix(pos0, pos1, positions);
                           vec2 uvFactor = (pos - vec2(instBounds.xy)) / vec2(instBounds.zw - instBounds.xy);
                           uvFactor = clamp(uvFactor, vec2(0.0), vec2(1.0));
                           pos -= vec2(dstOffset);
                           pos = pos * invRenderSize * 2.0 - 1.0;
                           pos.y = -pos.y;
                           uv = mix(vec2(instTexBounds.xw), vec2(instTexBounds.zy), uvFactor) * invAtlasSize;
                           gl_Position = matMul(transform, vec4(pos, 0.0, 1.0));
                           
                           bounds = instBounds;
                           scissor = instScissor - dstOffset.xyxy;
                           fgColor = instFgColor;
                           bgColor = instBgColor;
                           mode = instMode;
                        }
                    """.trimIndent(), listOf(
        Variable(GLSLType.V4I, "bounds").flat(),
        Variable(GLSLType.V4I, "scissor").flat(),
        Variable(GLSLType.V4F, "fgColor").flat(),
        Variable(GLSLType.V4F, "bgColor").flat(),
        Variable(GLSLType.V1I, "mode").flat(),
        Variable(GLSLType.V2F, "uv"),
    ), listOf(
        Variable(GLSLType.S2D, "atlasTexture"),
        Variable(GLSLType.V4F, "subpixelOffsets"),
        Variable(GLSLType.V4F, "result", VariableMode.OUT),
        // todo for SDFs, clamp uvi to the given bounds
    ), """
                        float median(float r, float g, float b) {
                            return max(min(r, g), min(max(r, g), b));
                        }
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
                                    vec4 text = texture(atlasTexture, uv);
                                    if (text.a > 0.0) { // emoji
                                        text.a = (text.a - ${1f / 255f}) * ${255f / 254f};
                                        if (text.a < 0.0) discard;
                                        result = text;
                                    } else { // text
                                        if (dot(text.rgb, vec3(1.0)) < 0.01) discard;
                                        int coordsX = int(gl_FragCoord.x);
                                        if (coordsX == scissor.x || coordsX == scissor.z) text.rgb = text.ggg; // hide subpixel-rendering on the edges
                                        result.rgb = mix(bgColor.rgb, fgColor.rgb, text.rgb);
                                        result.a = fgColor.a * max(text.r,max(text.g,text.b));
                                    }
                                    break;
                                case ${CanvasDrawMode.MULTI_SDF.ordinal}:
                                    vec2 ts = vec2(textureSize(atlasTexture,0));
                                    vec2 uvf = uv * ts + 0.5;
                                    ivec2 uvi = ivec2(uvf);
                                    vec3 px00 = texelFetch(atlasTexture, uvi+ivec2(0,0), 0).rgb;
                                    vec3 px01 = texelFetch(atlasTexture, uvi+ivec2(0,1), 0).rgb;
                                    vec3 px10 = texelFetch(atlasTexture, uvi+ivec2(1,0), 0).rgb;
                                    vec3 px11 = texelFetch(atlasTexture, uvi+ivec2(1,1), 0).rgb;
                                    vec2 uvG = uvf - vec2(uvi);
                                    float screenSize = dFdx(uv.x) * ts.x;
                                    vec2 uvR = uvG + subpixelOffsets.xy * screenSize;
                                    vec2 uvB = uvG + subpixelOffsets.zw * screenSize;
                                    vec3 msdR = mix(
                                        mix(px00, px01, uvR.y),
                                        mix(px10, px11, uvR.y),
                                        uvR.x
                                    );
                                    vec3 msdG = mix(
                                        mix(px00, px01, uvG.y),
                                        mix(px10, px11, uvG.y),
                                        uvG.x
                                    );
                                    vec3 msdB = mix(
                                        mix(px00, px01, uvB.y),
                                        mix(px10, px11, uvB.y),
                                        uvB.x
                                    );
                                    float sdR = median(msdR.r, msdR.g, msdR.b);
                                    float sdG = median(msdG.r, msdG.g, msdG.b);
                                    float sdB = median(msdB.r, msdB.g, msdB.b);
                                    float screenRange = max(2.0 / screenSize, 1.0);
                                    vec3 screenPxDistance = screenRange * (vec3(sdR,sdG,sdB) - 0.5);
                                    vec3 opacity = clamp(screenPxDistance + 0.5, vec3(0.0), vec3(1.0));
                                    result = mix(bgColor, fgColor, opacity.rgbg);
                                    break;
                                default:
                                    float c = float(int(dot(gl_FragCoord.xy,vec2(1.0))) & 4) * 0.333;
                                    result = vec4(c,0.0,c,1.0);
                                    break;
                            }
                        }
                    """.trimIndent()
)