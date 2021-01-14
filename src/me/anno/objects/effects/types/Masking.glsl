void main(){
    vec4 maskColor = vec4(
    mix(vec3(1.0), mask.rgb, useMaskColor),
    mix(mask.a, 1.0-mask.a, invertMask)
    );
    color = texture(tex, uv2) * maskColor;
}