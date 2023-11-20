vec3 hsluv_lengthOfRayUntilIntersect(float theta, vec3 x, vec3 y) {
    vec3 len = y / (sin(theta) - x * cos(theta));
    if (len.r < 0.0) len.r=1000.0;
    if (len.g < 0.0) len.g=1000.0;
    if (len.b < 0.0) len.b=1000.0;
    return len;
}
float hsluv_maxChromaForLH(float L, float H) {
    float hrad = radians(H);
    mat3 m2 = mat3(
    3.2409699419045214, -0.96924363628087983, 0.055630079696993609,
    -1.5373831775700935, 1.8759675015077207, -0.20397695888897657,
    -0.49861076029300328, 0.041555057407175613, 1.0569715142428786
    );
    float sub1 = pow(L + 16.0, 3.0) / 1560896.0;
    float sub2 = sub1 > 0.0088564516790356308 ? sub1 : L / 903.2962962962963;
    vec3 top1   = (284517.0 * m2[0] - 94839.0  * m2[2]) * sub2;
    vec3 bottom = (632260.0 * m2[2] - 126452.0 * m2[1]) * sub2;
    vec3 top2   = (838422.0 * m2[2] + 769860.0 * m2[1] + 731718.0 * m2[0]) * L * sub2;
    vec3 bound0x = top1 / bottom;
    vec3 bound0y = top2 / bottom;
    vec3 bound1x =              top1 / (bottom+126452.0);
    vec3 bound1y = (top2-769860.0*L) / (bottom+126452.0);
    vec3 lengths0 = hsluv_lengthOfRayUntilIntersect(hrad, bound0x, bound0y);
    vec3 lengths1 = hsluv_lengthOfRayUntilIntersect(hrad, bound1x, bound1y);
    return min(lengths0.r,
    min(lengths1.r,
    min(lengths0.g,
    min(lengths1.g,
    min(lengths0.b,
    lengths1.b)))));
}
float hsluv_fromLinear(float c) {
    return c <= 0.0031308 ? 12.92 * c : 1.055 * pow(c, 1.0 / 2.4) - 0.055;
}
vec3 hsluv_fromLinear(vec3 c) {
    return vec3(hsluv_fromLinear(c.r), hsluv_fromLinear(c.g), hsluv_fromLinear(c.b));
}
float hsluv_lToY(float L) {
    return L <= 8.0 ? L / 903.2962962962963 : pow((L + 16.0) / 116.0, 3.0);
}
vec3 xyzToRgb(vec3 tuple) {
    const mat3 m = mat3(
    3.2409699419045214, -1.5373831775700935, -0.49861076029300328,
    -0.96924363628087983, 1.8759675015077207, 0.041555057407175613,
    0.055630079696993609, -0.20397695888897657, 1.0569715142428786);
    return hsluv_fromLinear(tuple*m);
}
vec3 luvToXyz(vec3 tuple) {
    float L = tuple.x;
    float U = tuple.y / (13.0 * L) + 0.19783000664283681;
    float V = tuple.z / (13.0 * L) + 0.468319994938791;
    float Y = hsluv_lToY(L);
    float X = 2.25 * U * Y / V;
    float Z = (3./V - 5.)*Y - (X/3.);
    return vec3(X, Y, Z);
}
vec3 lchToLuv(vec3 tuple) {
    float hrad = radians(tuple.b);
    return vec3(
    tuple.r,
    cos(hrad) * tuple.g,
    sin(hrad) * tuple.g
    );
}
vec3 hsluvToLch(vec3 tuple) {
    tuple.g *= hsluv_maxChromaForLH(tuple.b, tuple.r) * 0.01;
    return tuple.bgr;
}
vec3 lchToRgb(vec3 tuple) {
    return xyzToRgb(luvToXyz(lchToLuv(tuple)));
}
vec3 hsluvToRgb(vec3 tuple) {
    return lchToRgb(hsluvToLch(tuple));
}