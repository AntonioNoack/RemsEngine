void main(){
    float progress = clamp(mix(mask.a, brightness(mask.rgb), useMaskColor), 0.0, 1.0);
    float smoothness2 = settings.y;
    // apply the sharpness to the progress: 0 = blurry, 1 = perfectly sharp
    progress = (progress - settings.x)/clamp(smoothness2, 1e-6, 1.0) + (1.0 - progress);
    progress = smoothstep(0.0, 1.0, progress);
    progress = mix(progress, 1.0-progress, invertMask);
    color = texture(tex, uv2);
    color.a *= progress;
}