void main(){
    effect = mix(mask.a, dot(vec3(0.3), mask.rgb), useMaskColor);
    effect = mix(effect, 1.0 - effect, invertMask);

    vec2 pixelatingX = pixelating * 0.408;

    uv3 = (uv2 - 0.5) / pixelatingX + 500.0;

    #define mod3(n) (n<0) ? 2-((2-n)%3) : n%3

    // calculate hex pattern
    vec2 di = mat2(0.866, 0.0, -0.5, 1.0) * uv3;
    vec2 df = fract(di);

    ivec2 intCoords = ivec2(di);
    ivec2 localCoords = ivec2(mod3(intCoords.x), mod3(intCoords.y));

    df += vec2(localCoords);
    bool l1 = df.x > 1.0;
    bool l2 = df.y < 2.0;
    bool l3 = df.y > 1.0;
    bool l4 = df.x < 2.0;
    bool l6 = df.y > 3.0 - df.x;
    bool l7 = df.y > 2.0 - df.x;
    bool l8 = df.y < 4.0 - df.x;

    vec2 hexCellOffset = vec2(-.5,.5);

    if(l2){
        // everything below L2
        if(l1){
            // everything right of L1
            if(l6){
                if(!l4){
                    // S2
                    hexCellOffset = vec2(1.5,-0.5);
                } // else S4
            } else {
                if(!l3){
                    // S1
                    hexCellOffset = vec2(0.5,-1.5);
                } // else S4
            }
        } else if(!l7){
            // S0
            hexCellOffset = vec2(-1.5,-.5);
        }// else S4
    } else if(!l8) {
        // S3
        hexCellOffset = vec2(.5,1.5);
    }// else S4

    df = df-di-hexCellOffset-1.0;// -1, because we add localCoords, which is 1 on average
    df = mat2(1.155, 0.0, 0.577, 1.0) * df;
    uv3 = 1.0-df-500.0;// why is it inversed?

    uv3 = uv3 * pixelatingX + 0.5;
    color = mix(
    texture(tex, uv2),
    texture(tex, uv3),
    effect
    );
}