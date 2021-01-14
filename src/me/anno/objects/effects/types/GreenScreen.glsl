void main(){
    // blur? is already good enough, I think...
    inverseEffect = clamp(mask.a, 0, 1);
    float similarity = greenScreenSettings.x;
    float smoothness = greenScreenSettings.y;
    float spill = greenScreenSettings.z;
    vec4 keyColor = mask;
    color = texture(tex, uv2);
    float chromaDistance = distance(RGBtoUV(color.rgb), RGBtoUV(keyColor.rgb));
    float baseMask = chromaDistance - similarity;
    float fullMask = pow(clamp(baseMask / smoothness, 0, 1), 1.5);
    if(invertMask < 0.5){
        float spillValue = pow(clamp(baseMask / spill, 0, 1), 1.5);
        float grayscale = dot(color.rgb, vec3(0.2126,0.7152,0.0722));
        color.rgb = mix(color.rgb, vec3(grayscale), (1-spillValue) * (1-effect));
        color.a *= fullMask * inverseEffect;
    } else {
        color.rgb = keyColor.rgb;
        color.a *= (1-fullMask) * inverseEffect;
    }
}