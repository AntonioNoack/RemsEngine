void main(){
    // blur? is already good enough, I think...
    inverseEffect = clamp(mask.a, 0.0, 1.0);
    float similarity = settings.x;
    float smoothness = settings.y;
    float spill = settings.z;
    vec4 keyColor = mask;
    color = texture(tex, uv2);
    float isGrayscale = 1.0 - clamp((maxV3(keyColor.rgb)-minV3(keyColor.rgb))*50.0, 0.0, 1.0);
    float chromaDistance = distance(RGBtoUV(color.rgb), RGBtoUV(keyColor.rgb));
    float baseMask = mix(chromaDistance - similarity, distance(keyColor.rgb, color.rgb), isGrayscale);
    float fullMask = pow(clamp(baseMask / smoothness, 0.0, 1.0), 1.5);
    if (invertMask < 0.5){
        float spillValue = pow(clamp(baseMask / spill, 0.0, 1.0), 1.5);
        float grayscale = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
        color.rgb = mix(color.rgb, vec3(grayscale), (1.0-spillValue) * (1.0-effect));
        color.a *= fullMask * inverseEffect;
    } else {
        color.rgb = keyColor.rgb;
        color.a *= (1.0-fullMask) * inverseEffect;
    }
}