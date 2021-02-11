void main(){
    // mix in original?
    // can be done by reducing the effect strength and increasing the color strength
    effect = mix(mask.a, dot(vec3(0.3), mask.rgb), useMaskColor);
    effect = mix(effect, 1.0 - effect, invertMask);
    if (abs(effect) > 0.001){
        vec2 dir = (uv1 - offset) * -pixelating.y;
        float weightSum = 0;
        vec4 colorSum = vec4(0.0);
        float steps0 = clamp(dot(windowSize, abs(dir)), 1.0, 1000.0);
        float steps = exp(round(log(steps0))), invSteps = 1.0/floor(steps);
        int stepsI = int(steps);
        for (int i=0;i<stepsI;i++){
            float fi = float(i)*invSteps;
            float weight = 1.0 - fi;
            vec2 nextUV = uv2 + dir * fi;
            vec4 colorHere = texture(tex, nextUV);
            colorSum += weight * colorHere;
            weightSum += weight;
        }
        color = mix(texture(tex, uv2), colorSum / weightSum, effect);
    } else {
        color = texture(tex, uv2);
    }
}