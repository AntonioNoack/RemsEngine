package me.anno.gpu.shader

object ShaderFuncLib {

    val randomGLSL = "" +
            "#ifndef RANDOM_GLSL\n" +
            "#define RANDOM_GLSL\n" +
            "float random(float co){\n" +
            "    return fract(sin(co) * 43758.5453);\n" +
            "}\n" +
            "float random(vec2 co){\n" +
            "    return random(dot(co, vec2(12.9898,78.233)));\n" +
            "}\n" +
            "float random(vec3 co){\n" +
            "    return random(dot(co, vec3(12.9898,78.233,45.164)));\n" +
            "}\n" +
            "#endif\n"

    @Suppress("unused")
    val reinhardToneMapping = "" +
            "#ifndef REINHARD\n" +
            "#define REINHARD\n" +
            "vec3 reinhard(vec3 color){\n" +
            "   return color / (color + 1.0);\n" +
            "}\n" +
            "#endif\n"

    @Suppress("unused")
    val reinhard2ToneMapping = "" +
            "#ifndef REINHARD2\n" +
            "#define REINHARD2\n" +
            "vec3 reinhard(vec3 color){\n" +
            "   const float invWhiteSq = ${1.0 / 16.0};\n" +
            "   return (color * (1.0 + color * invWhiteSq)) / (color + 1.0);\n" +
            "}\n" +
            "#endif\b"

    @Suppress("unused")
    val uchimuraToneMapping = "" +
            // Uchimura 2017, "HDR theory and practice"
            // Math: https://www.desmos.com/calculator/gslcdxvipg
            // Source: https://www.slideshare.net/nikuque/hdr-theory-and-practicce-jp
            // source^2: https://github.com/dmnsgn/glsl-tone-map/blob/master/uchimura.glsl
            "#ifndef UCHIMURA\n" +
            "#define UCHIMURA\n" +
            "vec3 uchimura(vec3 x, float P, float a, float m, float l, float c, float b) {\n" +
            "   float l0 = ((P - m) * l) / a;\n" +
            "   float L0 = m - m / a;\n" +
            "   float L1 = m + (1.0 - m) / a;\n" +
            "   float S0 = m + l0;\n" +
            "   float S1 = m + a * l0;\n" +
            "   float C2 = (a * P) / (P - S1);\n" +
            "   float CP = -C2 / P;\n" +

            "   vec3 w0 = vec3(1.0 - smoothstep(0.0, m, x));\n" +
            "   vec3 w2 = vec3(step(m + l0, x));\n" +
            "   vec3 w1 = vec3(1.0 - w0 - w2);\n" +

            "   vec3 T = vec3(m * pow(x / m, vec3(c)) + b);\n" +
            "   vec3 S = vec3(P - (P - S1) * exp(CP * (x - S0)));\n" +
            "   vec3 L = vec3(m + a * (x - m));\n" +

            "   return T * w0 + L * w1 + S * w2;\n" +
            "}\n" +
            "vec3 uchimura(vec3 x) {\n" +
            "   const float P = 1.0;\n" +// max display brightness
            "   const float a = 1.0;\n" +// contrast
            "   const float m = 0.22;\n" +// linear section start
            "   const float l = 0.4;\n" + // linear section length
            "   const float c = 1.33;\n" + // black
            "   const float b = 0.0;\n" + // pedestal
            "   return uchimura(x, P, a, m, l, c, b);\n" +
            "}\n" +
            "#endif\n"

    // academy color encoding system; e.g., used by UE4
    // says it shall be standard for the film industry
    @Suppress("unused")
    val acesToneMapping = "" +
            // Narkowicz 2015, "ACES Filmic Tone Mapping Curve"
            // source^2: https://github.com/dmnsgn/glsl-tone-map/blob/master/aces.glsl
            "#ifndef ACES\n" +
            "#define ACES\n" +
            "vec3 aces(vec3 x) {\n" +
            "   return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);\n" +
            "}\n" +
            "#endif\n"

    // from https://www.shadertoy.com/view/WdVyDW
    const val costShadingFunc = "" +
            "vec3 costShadingFunc(float t){\n" +
            "   const vec3 a = vec3(97, 130, 234) / vec3(255.0);\n" +
            "   const vec3 b = vec3(220, 94, 75) / vec3(255.0);\n" +
            "   const vec3 c = vec3(221, 220, 219) / vec3(255.0);\n" +
            "   return t < 0.5 ? mix(a, c, 2.0 * t) : mix(c, b, 2.0 * t - 1.0);\n" +
            "}\n"
}