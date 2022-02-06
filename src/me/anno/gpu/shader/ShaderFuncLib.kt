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
            "vec3 uchimura(vec3 x, float P, float a, float m, float l, float c, float b) {\n" +
            "  float l0 = ((P - m) * l) / a;\n" +
            "  float L0 = m - m / a;\n" +
            "  float L1 = m + (1.0 - m) / a;\n" +
            "  float S0 = m + l0;\n" +
            "  float S1 = m + a * l0;\n" +
            "  float C2 = (a * P) / (P - S1);\n" +
            "  float CP = -C2 / P;\n" +
            "\n" +
            "  vec3 w0 = vec3(1.0 - smoothstep(0.0, m, x));\n" +
            "  vec3 w2 = vec3(step(m + l0, x));\n" +
            "  vec3 w1 = vec3(1.0 - w0 - w2);\n" +
            "\n" +
            "  vec3 T = vec3(m * pow(x / m, vec3(c)) + b);\n" +
            "  vec3 S = vec3(P - (P - S1) * exp(CP * (x - S0)));\n" +
            "  vec3 L = vec3(m + a * (x - m));\n" +
            "\n" +
            "  return T * w0 + L * w1 + S * w2;\n" +
            "}\n" +
            "\n" +
            "vec3 uchimura(vec3 x) {\n" +
            "  const float P = 1.0;  // max display brightness\n" +
            "  const float a = 1.0;  // contrast\n" +
            "  const float m = 0.22; // linear section start\n" +
            "  const float l = 0.4;  // linear section length\n" +
            "  const float c = 1.33; // black\n" +
            "  const float b = 0.0;  // pedestal\n" +
            "\n" +
            "  return uchimura(x, P, a, m, l, c, b);\n" +
            "}"

    // academy color encoding system; e.g. used by UE4
    // says it shall be standard for the film industry
    val acesToneMapping = "" +
            // Narkowicz 2015, "ACES Filmic Tone Mapping Curve"
            // source^2: https://github.com/dmnsgn/glsl-tone-map/blob/master/aces.glsl
            "vec3 aces(vec3 x) {\n" +
            "   const float a = 2.51;\n" +
            "   const float b = 0.03;\n" +
            "   const float c = 2.43;\n" +
            "   const float d = 0.59;\n" +
            "   const float e = 0.14;\n" +
            "   return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);\n" +
            "}"


}