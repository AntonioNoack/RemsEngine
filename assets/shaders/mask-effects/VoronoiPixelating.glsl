void main(){
    effect = mix(mask.a, dot(vec3(0.3), mask.rgb), useMaskColor);
    effect = mix(effect, 1.0 - effect, invertMask);
    uv3 = (uv2 - 0.5) / pixelating;

    vec2 n = floor(uv3);
    vec2 f = fract(uv3);

    // Voronoi & Noise from Inigo Quilez,
    // https://www.shadertoy.com/view/ldl3W8

    float md = 8.0;
    vec2 mv;
    for(int j=-1;j<=1;j++)
    for(int i=-1;i<=1;i++) {
        vec2 g = vec2(float(i), float(j));
        vec2 p = n + g;
        // white noise
        vec2 o = fract(sin(vec2(dot(p,vec2(127.1,311.7)),dot(p,vec2(269.5,183.3))))*43758.5453);
        vec2 r = g + o - f;
        float d = dot(r,r);
        if(d < md) {
            md = d;
            mv = r;
        }
    }

    uv3 += mv;
    uv3 = uv3 * pixelating + 0.5;

    color = mix(texture(tex, uv2), texture(tex, uv3), effect);
}