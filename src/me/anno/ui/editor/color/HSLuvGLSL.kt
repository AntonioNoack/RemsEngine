package me.anno.ui.editor.color

object HSLuvGLSL {

    val GLSL =
            "vec3 hsluv_intersectLineLine(vec3 line1x, vec3 line1y, vec3 line2x, vec3 line2y) {\n" +
            "    return (line1y - line2y) / (line2x - line1x);\n" +
            "}\n" +
            "vec3 hsluv_distanceFromPole(vec3 pointx,vec3 pointy) {\n" +
            "    return sqrt(pointx*pointx + pointy*pointy);\n" +
            "}\n" +
            "vec3 hsluv_lengthOfRayUntilIntersect(float theta, vec3 x, vec3 y) {\n" +
            "    vec3 len = y / (sin(theta) - x * cos(theta));\n" +
            "    if (len.r < 0.0) {len.r=1000.0;}\n" +
            "    if (len.g < 0.0) {len.g=1000.0;}\n" +
            "    if (len.b < 0.0) {len.b=1000.0;}\n" +
            "    return len;\n" +
            "}\n" +
            "float hsluv_maxSafeChromaForL(float L){\n" +
            "    mat3 m2 = mat3(\n" +
            "         3.2409699419045214  ,-0.96924363628087983 , 0.055630079696993609,\n" +
            "        -1.5373831775700935  , 1.8759675015077207  ,-0.20397695888897657 ,\n" +
            "        -0.49861076029300328 , 0.041555057407175613, 1.0569715142428786\n" +
            "    );\n" +
            "    float sub0 = L + 16.0;\n" +
            "    float sub1 = sub0 * sub0 * sub0 * .000000641;\n" +
            "    float sub2 = sub1 > 0.0088564516790356308 ? sub1 : L / 903.2962962962963;\n" +
            "\n" +
            "    vec3 top1   = (284517.0 * m2[0] - 94839.0  * m2[2]) * sub2;\n" +
            "    vec3 bottom = (632260.0 * m2[2] - 126452.0 * m2[1]) * sub2;\n" +
            "    vec3 top2   = (838422.0 * m2[2] + 769860.0 * m2[1] + 731718.0 * m2[0]) * L * sub2;\n" +
            "\n" +
            "    vec3 bounds0x = top1 / bottom;\n" +
            "    vec3 bounds0y = top2 / bottom;\n" +
            "\n" +
            "    vec3 bounds1x =              top1 / (bottom+126452.0);\n" +
            "    vec3 bounds1y = (top2-769860.0*L) / (bottom+126452.0);\n" +
            "\n" +
            "    vec3 xs0 = hsluv_intersectLineLine(bounds0x, bounds0y, -1.0/bounds0x, vec3(0.0) );\n" +
            "    vec3 xs1 = hsluv_intersectLineLine(bounds1x, bounds1y, -1.0/bounds1x, vec3(0.0) );\n" +
            "\n" +
            "    vec3 lengths0 = hsluv_distanceFromPole( xs0, bounds0y + xs0 * bounds0x );\n" +
            "    vec3 lengths1 = hsluv_distanceFromPole( xs1, bounds1y + xs1 * bounds1x );\n" +
            "\n" +
            "    return  min(lengths0.r,\n" +
            "            min(lengths1.r,\n" +
            "            min(lengths0.g,\n" +
            "            min(lengths1.g,\n" +
            "            min(lengths0.b,\n" +
            "                lengths1.b)))));\n" +
            "}\n" +
            "\n" +
            "float hsluv_maxChromaForLH(float L, float H) {\n" +
            "\n" +
            "    float hrad = radians(H);\n" +
            "\n" +
            "    mat3 m2 = mat3(\n" +
            "         3.2409699419045214  ,-0.96924363628087983 , 0.055630079696993609,\n" +
            "        -1.5373831775700935  , 1.8759675015077207  ,-0.20397695888897657 ,\n" +
            "        -0.49861076029300328 , 0.041555057407175613, 1.0569715142428786\n" +
            "    );\n" +
            "    float sub1 = pow(L + 16.0, 3.0) / 1560896.0;\n" +
            "    float sub2 = sub1 > 0.0088564516790356308 ? sub1 : L / 903.2962962962963;\n" +
            "\n" +
            "    vec3 top1   = (284517.0 * m2[0] - 94839.0  * m2[2]) * sub2;\n" +
            "    vec3 bottom = (632260.0 * m2[2] - 126452.0 * m2[1]) * sub2;\n" +
            "    vec3 top2   = (838422.0 * m2[2] + 769860.0 * m2[1] + 731718.0 * m2[0]) * L * sub2;\n" +
            "\n" +
            "    vec3 bound0x = top1 / bottom;\n" +
            "    vec3 bound0y = top2 / bottom;\n" +
            "\n" +
            "    vec3 bound1x =              top1 / (bottom+126452.0);\n" +
            "    vec3 bound1y = (top2-769860.0*L) / (bottom+126452.0);\n" +
            "\n" +
            "    vec3 lengths0 = hsluv_lengthOfRayUntilIntersect(hrad, bound0x, bound0y );\n" +
            "    vec3 lengths1 = hsluv_lengthOfRayUntilIntersect(hrad, bound1x, bound1y );\n" +
            "\n" +
            "    return  min(lengths0.r,\n" +
            "            min(lengths1.r,\n" +
            "            min(lengths0.g,\n" +
            "            min(lengths1.g,\n" +
            "            min(lengths0.b,\n" +
            "                lengths1.b)))));\n" +
            "}\n" +
            "float hsluv_fromLinear(float c) {\n" +
            "    return c <= 0.0031308 ? 12.92 * c : 1.055 * pow(c, 1.0 / 2.4) - 0.055;\n" +
            "}\n" +
            "vec3 hsluv_fromLinear(vec3 c) {\n" +
            "    return vec3( hsluv_fromLinear(c.r), hsluv_fromLinear(c.g), hsluv_fromLinear(c.b) );\n" +
            "}\n" +
            "float hsluv_toLinear(float c) {\n" +
            "    return c > 0.04045 ? pow((c + 0.055) / (1.0 + 0.055), 2.4) : c / 12.92;\n" +
            "}\n" +
            "vec3 hsluv_toLinear(vec3 c) {\n" +
            "    return vec3( hsluv_toLinear(c.r), hsluv_toLinear(c.g), hsluv_toLinear(c.b) );\n" +
            "}\n" +
            "float hsluv_yToL(float Y){\n" +
            "    return Y <= 0.0088564516790356308 ? Y * 903.2962962962963 : 116.0 * pow(Y, 1.0 / 3.0) - 16.0;\n" +
            "}\n" +
            "float hsluv_lToY(float L) {\n" +
            "    return L <= 8.0 ? L / 903.2962962962963 : pow((L + 16.0) / 116.0, 3.0);\n" +
            "}\n" +
            "vec3 xyzToRgb(vec3 tuple) {\n" +
            "    const mat3 m = mat3(\n" +
            "        3.2409699419045214  ,-1.5373831775700935 ,-0.49861076029300328 ,\n" +
            "       -0.96924363628087983 , 1.8759675015077207 , 0.041555057407175613,\n" +
            "        0.055630079696993609,-0.20397695888897657, 1.0569715142428786  );\n" +
            "\n" +
            "    return hsluv_fromLinear(tuple*m);\n" +
            "}\n" +
            "vec3 rgbToXyz(vec3 tuple) {\n" +
            "    const mat3 m = mat3(\n" +
            "        0.41239079926595948 , 0.35758433938387796, 0.18048078840183429 ,\n" +
            "        0.21263900587151036 , 0.71516867876775593, 0.072192315360733715,\n" +
            "        0.019330818715591851, 0.11919477979462599, 0.95053215224966058\n" +
            "    );\n" +
            "    return hsluv_toLinear(tuple) * m;\n" +
            "}\n" +
            "vec3 xyzToLuv(vec3 tuple){\n" +
            "    float X = tuple.x;\n" +
            "    float Y = tuple.y;\n" +
            "    float Z = tuple.z;\n" +
            "\n" +
            "    float L = hsluv_yToL(Y);\n" +
            "\n" +
            "    float div = 1./dot(tuple,vec3(1,15,3));\n" +
            "\n" +
            "    return vec3(\n" +
            "        1.,\n" +
            "        (52. * (X*div) - 2.57179),\n" +
            "        (117.* (Y*div) - 6.08816)\n" +
            "    ) * L;\n" +
            "}\n" +
            "vec3 luvToXyz(vec3 tuple) {\n" +
            "    float L = tuple.x;\n" +
            "\n" +
            "    float U = tuple.y / (13.0 * L) + 0.19783000664283681;\n" +
            "    float V = tuple.z / (13.0 * L) + 0.468319994938791;\n" +
            "\n" +
            "    float Y = hsluv_lToY(L);\n" +
            "    float X = 2.25 * U * Y / V;\n" +
            "    float Z = (3./V - 5.)*Y - (X/3.);\n" +
            "\n" +
            "    return vec3(X, Y, Z);\n" +
            "}\n" +
            "vec3 luvToLch(vec3 tuple) {\n" +
            "    float L = tuple.x;\n" +
            "    float U = tuple.y;\n" +
            "    float V = tuple.z;\n" +
            "\n" +
            "    float C = length(tuple.yz);\n" +
            "    float H = degrees(atan(V,U));\n" +
            "    if (H < 0.0) {\n" +
            "        H = 360.0 + H;\n" +
            "    }\n" +
            "\n" +
            "    return vec3(L, C, H);\n" +
            "}\n" +
            "vec3 lchToLuv(vec3 tuple) {\n" +
            "    float hrad = radians(tuple.b);\n" +
            "    return vec3(\n" +
            "        tuple.r,\n" +
            "        cos(hrad) * tuple.g,\n" +
            "        sin(hrad) * tuple.g\n" +
            "    );\n" +
            "}\n" +
            "vec3 hsluvToLch(vec3 tuple) {\n" +
            "    tuple.g *= hsluv_maxChromaForLH(tuple.b, tuple.r) * .01;\n" +
            "    return tuple.bgr;\n" +
            "}\n" +
            "vec3 lchToHsluv(vec3 tuple) {\n" +
            "    tuple.g /= hsluv_maxChromaForLH(tuple.r, tuple.b) * .01;\n" +
            "    return tuple.bgr;\n" +
            "}\n" +
            "vec3 hpluvToLch(vec3 tuple) {\n" +
            "    tuple.g *= hsluv_maxSafeChromaForL(tuple.b) * .01;\n" +
            "    return tuple.bgr;\n" +
            "}\n" +
            "vec3 lchToHpluv(vec3 tuple) {\n" +
            "    tuple.g /= hsluv_maxSafeChromaForL(tuple.r) * .01;\n" +
            "    return tuple.bgr;\n" +
            "}\n" +
            "vec3 lchToRgb(vec3 tuple) {\n" +
            "    return xyzToRgb(luvToXyz(lchToLuv(tuple)));\n" +
            "}\n" +
            "vec3 rgbToLch(vec3 tuple) {\n" +
            "    return luvToLch(xyzToLuv(rgbToXyz(tuple)));\n" +
            "}\n" +
            "\n" +
            "vec3 hsluvToRgb(vec3 tuple) {\n" +
            "    return lchToRgb(hsluvToLch(tuple));\n" +
            "}\n" +
            "vec3 rgbToHsluv(vec3 tuple) {\n" +
            "    return lchToHsluv(rgbToLch(tuple));\n" +
            "}\n" +
            "vec3 hpluvToRgb(vec3 tuple) {\n" +
            "    return lchToRgb(hpluvToLch(tuple));\n" +
            "}\n" +
            "vec3 rgbToHpluv(vec3 tuple) {\n" +
            "    return lchToHpluv(rgbToLch(tuple));\n" +
            "}\n" +
            "vec3 luvToRgb(vec3 tuple){\n" +
            "    return xyzToRgb(luvToXyz(tuple));\n" +
            "}\n"

}