package me.anno.ui.canvas

import me.anno.gpu.shader.FlatSymbols.circleAngleLogic
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.PIf
import me.anno.utils.GFXFeatures

// instBounds must always overlap with instScissor, or pos0,pos1 will be invalid (larger than expected using negative size)
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
    texBounds = instTexBounds;
    scissor = instScissor - dstOffset.xyxy;
    fgColor = instFgColor;
    bgColor = instBgColor;
    mode = instMode;
    uv01 = uvFactor;
}
                    """.trimIndent(), listOf(
        Variable(GLSLType.V4I, "bounds").flat(),
        Variable(GLSLType.V4I, "texBounds").flat(),
        Variable(GLSLType.V4I, "scissor").flat(),
        Variable(GLSLType.V4F, "fgColor").flat(),
        Variable(GLSLType.V4F, "bgColor").flat(),
        Variable(GLSLType.V1I, "mode").flat(),
        Variable(GLSLType.V2F, "uv"),
        Variable(GLSLType.V2F, "uv01"),
    ), listOf(
        Variable(GLSLType.S2D, "atlasTexture"),
        Variable(GLSLType.V4F, "subpixelOffsets"),
        Variable(GLSLType.V4F, "result", VariableMode.OUT),
    ), """
float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

float sampleText(vec2 diff, vec2 uvf) {
    // Axis-aligned footprint approximation.
    vec2 uvMin = max(uvf - diff * 0.5, vec2(texBounds.xy));
    vec2 uvMax = min(uvf + diff * 0.5, vec2(texBounds.zw));
    ivec2 uvMinI = ivec2(floor(uvMin));
    ivec2 uvMaxI = ivec2(ceil(uvMax));
    
    // todo? split this into border sum and center sum (much cheaper, much more)
    float sum = 0.0;
    for (int y = uvMinI.y; y < uvMaxI.y; y++) {
        for (int x = uvMinI.x; x < uvMaxI.x; x++) {
            vec3 msd = texelFetch(atlasTexture, ivec2(x,y), 0).rgb;
            float value = clamp(median(msd.r, msd.g, msd.b) - 0.25, 0.0, 0.5);

            vec2 cellMin = vec2(x, y);
            vec2 cellMax = cellMin + 1.0;

            vec2 overlap = max(
                vec2(0.0),
                min(uvMax, cellMax) - max(uvMin, cellMin)
            );

            float weight = overlap.x * overlap.y;
            sum += value * weight;
        }
    }
    return sum * 2.0 / ((uvMax.x-uvMin.x) * (uvMax.y-uvMin.y));
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
        case ${CanvasDrawMode.CIRCLE.ordinal}:
            vec2 uv2 = uv01 - 0.5;
            float r2 = dot(uv2,uv2);
            float dist = 0.25 - r2;
            float dr = 1.0 / fwidth(dist);
            float alpha = clamp(dist * dr + 0.5, 0.0, 1.0);
            if (texBounds.x > 0) {
                float innerRadius = float(texBounds.x) * ${0.5f / 65536};
                innerRadius *= innerRadius;
                alpha *= clamp((r2 - innerRadius) * dr + 0.5, 0.0, 1.0);
            }
            if (alpha > 0.0 && texBounds.z != texBounds.w) {
                vec2 angleLimits = vec2(texBounds.zw) * ${PIf * 4f / 65536} - $PIf;
                $circleAngleLogic
                alpha *= angleAlpha;
            }
            result.a *= alpha;
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
            vec2 uvf = uv * ts;
            
            vec2 dx = dFdx(uvf);
            vec2 dy = dFdy(uvf);
            vec2 diff = abs(dx) + abs(dy); // = fwidth
        ${
        // for weak gpus, disable subpixel rendering for multi-sdf
        // if size is <= 1:1, we can just sample directly
        if (GFXFeatures.hasWeakGPU) {
            """
            float opacity;
            if (diff.x + diff.y < 2.0) {
                uvf += 0.5;
                ivec2 uvi = clamp(ivec2(uvf), texBounds.xy, texBounds.zw-1);
                vec3 px00 = texelFetch(atlasTexture, uvi+ivec2(0,0), 0).rgb;
                vec3 px01 = texelFetch(atlasTexture, uvi+ivec2(0,1), 0).rgb;
                vec3 px10 = texelFetch(atlasTexture, uvi+ivec2(1,0), 0).rgb;
                vec3 px11 = texelFetch(atlasTexture, uvi+ivec2(1,1), 0).rgb;
                vec2 uvG = uvf - vec2(uvi);
                vec3 msdG = mix(mix(px00, px01, uvG.y), mix(px10, px11, uvG.y), uvG.x);
                float sd = median(msdG.r, msdG.g, msdG.b);
                float screenRange = max(1.0 / fwidth(sd), 1.0);
                float screenPxDistance = screenRange * (sd - 0.5);
                opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);
            } else {
                 opacity = sampleText(diff, uvf);
            }
            if (opacity < 0.05) discard;
            result = mix(bgColor, fgColor, opacity);
            break;
                """.trimIndent()
        } else {
            // todo if it is rotated, subpixel pattern may be incorrect... check that
            //  (in that case, we can just disable subpixel rendering, too)
            """
            vec3 opacity;
            if(diff.x + diff.y < 2.0) {
                
                uvf += 0.5;
            
                ivec2 uvi = clamp(ivec2(uvf), texBounds.xy, texBounds.zw-2);
                vec3 px00 = texelFetch(atlasTexture, uvi+ivec2(0,0), 0).rgb;
                vec3 px01 = texelFetch(atlasTexture, uvi+ivec2(0,1), 0).rgb;
                vec3 px10 = texelFetch(atlasTexture, uvi+ivec2(1,0), 0).rgb;
                vec3 px11 = texelFetch(atlasTexture, uvi+ivec2(1,1), 0).rgb;
                vec2 uvG = uvf - vec2(uvi);
                vec3 msdG = mix(mix(px00, px01, uvG.y), mix(px10, px11, uvG.y), uvG.x);
                float sdG = median(msdG.r, msdG.g, msdG.b);
                vec2 dir = abs(vec2(dFdx(sdG), dFdy(sdG)));
                vec2 uvR = uvG + subpixelOffsets.xy * dir;
                vec2 uvB = uvG + subpixelOffsets.zw * dir;
                vec3 msdR = mix(mix(px00, px01, uvR.y), mix(px10, px11, uvR.y), uvR.x);
                vec3 msdB = mix(mix(px00, px01, uvB.y), mix(px10, px11, uvB.y), uvB.x);
                float sdR = median(msdR.r, msdR.g, msdR.b);
                float sdB = median(msdB.r, msdB.g, msdB.b);
                float screenRange = 1.0 / clamp(dir.x + dir.y, 0.001, 1.0);
                vec3 screenPxDistance = screenRange * (vec3(sdR,sdG,sdB) - 0.5);
                opacity = clamp(screenPxDistance + 0.5, vec3(0.0), vec3(1.0));
            } else {
                opacity = vec3(
                    sampleText(diff,uvf + diff * subpixelOffsets.xy),
                    sampleText(diff,uvf),
                    sampleText(diff,uvf + diff * subpixelOffsets.zw)
                );
            }
            if (opacity.g < 0.05) discard;
            result = mix(bgColor, fgColor, opacity.rgbg);
            break;
                """.trimIndent()
        }
    }
        default:
            result.g = 0.0;
            break;
    }
}
                    """.trimIndent()
)