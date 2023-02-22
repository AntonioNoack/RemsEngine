package me.anno.tests.shader

import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {

    // todo develop what is needed to calculate finalPosition from depth


    // todo make this into two passes:
    // todo first downscale and calculate coc, then apply

    /**
    uniform sampler2D uTexture; //Image to be processed
    uniform sampler2D uDepth; //Linear depth, where 1.0 == far plane
    uniform vec2 uPixelSize; //The size of a pixel: vec2(1.0/width, 1.0/height)
    uniform float uFar; // Far plane

    const float GOLDEN_ANGLE = 2.39996323;
    const float MAX_BLUR_SIZE = 20.0;
    const float RAD_SCALE = 0.5; // Smaller = nicer blur, larger = faster

    float getBlurSize(float depth, float focusPoint, float focusScale)
    {
    float coc = clamp((1.0 / focusPoint - 1.0 / depth)*focusScale, -1.0, 1.0);
    return abs(coc) * MAX_BLUR_SIZE;
    }

    vec3 depthOfField(vec2 texCoord, float focusPoint, float focusScale)
    {
    float centerDepth = texture(uDepth, texCoord).r * uFar;
    float centerSize = getBlurSize(centerDepth, focusPoint, focusScale);
    vec3 color = texture(uTexture, vTexCoord).rgb;
    float tot = 1.0;
    float radius = RAD_SCALE;
    for (float ang = 0.0; radius<MAX_BLUR_SIZE; ang += GOLDEN_ANGLE)
    {
    vec2 tc = texCoord + vec2(cos(ang), sin(ang)) * uPixelSize * radius;
    vec3 sampleColor = texture(uTexture, tc).rgb;
    float sampleDepth = texture(uDepth, tc).r * uFar;
    float sampleSize = getBlurSize(sampleDepth, focusPoint, focusScale);
    if (sampleDepth > centerDepth)
    sampleSize = clamp(sampleSize, 0.0, centerSize*2.0);
    float m = smoothstep(radius-0.5, radius+0.5, sampleSize);
    color += mix(color/tot, sampleColor, m);
    tot += 1.0;   radius += RAD_SCALE/radius;
    }
    return color /= tot;
    }
     */

    // todo test DOF
    val scene = PrefabCache[downloads.getChild("3d/SSR.glb")]!!

    testSceneWithUI(scene) {
        // it.renderer.renderMode = RenderMode.DEPTH_OF_FIELD
    }

}