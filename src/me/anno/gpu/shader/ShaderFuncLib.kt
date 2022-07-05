package me.anno.gpu.shader

object ShaderFuncLib {

    val noiseFunc = "" +
            "float random(vec2 co){\n" +
            "    return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453);\n" +
            "}\n"

    val reinhardToneMapping = "" +
            "vec3 reinhard(vec3 color){\n" +
            "   return color / (color + 1.0);\n" +
            "}\n"

    val reinhard2ToneMapping = "" +
            "vec3 reinhard(vec3 color){\n" +
            "   const float invWhiteSq = ${1.0 / 16.0};\n" +
            "   return (color * (1.0 + color * invWhiteSq)) / (color + 1.0);\n" +
            "}\n"

    val uchimuraToneMapping = "" +
            // Uchimura 2017, "HDR theory and practice"
            // Math: https://www.desmos.com/calculator/gslcdxvipg
            // Source: https://www.slideshare.net/nikuque/hdr-theory-and-practicce-jp
            // source^2: https://github.com/dmnsgn/glsl-tone-map/blob/master/uchimura.glsl
            // todo could be simplified a lot...
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
            "}"

    // academy color encoding system; e.g., used by UE4
    // says it shall be standard for the film industry
    val acesToneMapping = "" +
            // Narkowicz 2015, "ACES Filmic Tone Mapping Curve"
            // source^2: https://github.com/dmnsgn/glsl-tone-map/blob/master/aces.glsl
            "vec3 aces(vec3 x) {\n" +
            "   return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);\n" +
            "}"


}