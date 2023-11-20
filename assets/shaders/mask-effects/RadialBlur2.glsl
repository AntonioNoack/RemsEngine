void main(){
    // other masking: masking of where rays start; has slight artifacts
    // and may be less intuitive
    vec2 dir = (uv1 - offset) * -pixelating.y;
    float weightSum = 0.0;
    vec4 colorSum = vec4(0.0);
    float steps0 = clamp(dot(windowSize, abs(dir)), 1.0, 1000.0);
    float steps = exp(round(log(steps0))), invSteps = 1.0/floor(steps);
    int stepsI = int(steps);
    for(int i=0;i<stepsI;i++){
        float fi = float(i)*invSteps;
        vec2 nextUV = uv2 + dir * fi;
        mask = texture(maskTex, nextUV);
        effect = mix(mask.a, dot(vec3(0.3), mask.rgb), useMaskColor);
        effect = mix(effect, 1.0 - effect, invertMask);
        vec4 colorHere = texture(tex, nextUV);
        float weight = i == 0 ? 1.0 : max(0, effect-fi);
        colorSum += weight * colorHere;
        weightSum += weight;
    }
    color = mix(texture(tex, uv2), colorSum / weightSum, effect);
}