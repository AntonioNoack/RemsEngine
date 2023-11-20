void main(){
    effect = mix(mask.a, dot(vec3(0.3), mask.rgb), useMaskColor);
    effect = mix(effect, 1.0 - effect, invertMask);
    uv3 = (uv2 - 0.5) / pixelating;
    uv4 = mat2(1.0, 1.0, 0.6, -0.6) * uv3;
    uv4 = fract(uv4);
    if(uv4.x > uv4.y) uv4.x -= 0.5;
    uv4 = mat2(0.5, 0.833, 0.5, -0.833) * uv4 - vec2(0.25, 0.0);
    uv3 = (uv3 - uv4) * pixelating + 0.5;
    color = mix(
    texture(tex, uv2),
    texture(tex, uv3),
    effect
    );
}