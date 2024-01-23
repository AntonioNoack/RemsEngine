package me.anno.tests.bench

import me.anno.Time
import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.texture.Texture2D.Companion.setReadAlignment
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Floats.f2
import org.lwjgl.opengl.GL46C
import kotlin.math.roundToInt

fun main() {

    fun createShader(code: String) = Shader(
        "", coordsList, coordsUVVertexShader,
        uvList, listOf(), "void main(){$code}"
    )

    fun repeat(code: String, times: Int): String {
        return Array(times) { code }.joinToString("\n")
    }

    val size = 512

    val warmup = 50
    val benchmark = 1000

    HiddenOpenGLContext.setSize(size, size)
    HiddenOpenGLContext.createOpenGL()

    val buffer = Framebuffer("pow", size, size, 1, 1, true, DepthBufferType.NONE)

    println("Power,Multiplications,GFlops-multiplication,GFlops-floats,GFlops-ints,GFlops-power,Speedup")

    useFrame(buffer, Renderer.copyRenderer) {

        GFXState.blendMode.use(me.anno.gpu.blending.BlendMode.ADD) {

            for (power in 2 until 100) {

                // to reduce the overhead of other stuff
                val repeats = 100
                val init = "float x1 = me.anno.tests.dot(uv, vec2(1.0)),x2,x4,x8,x16,x32,x64;\n"
                val end = "gl_FragColor = vec4(x1,x1,x1,x1);\n"
                val manualCode = StringBuilder()
                for (bit in 1 until 32) {
                    val p = 1.shl(bit)
                    val h = 1.shl(bit - 1)
                    if (power == p) {
                        manualCode.append("x1=x$h*x$h;")
                        break
                    } else if (power > p) {
                        manualCode.append("x$p=x$h*x$h;")
                    } else break
                }

                if (power.and(power - 1) != 0) {
                    // not a power of two, so the result isn't finished yet
                    manualCode.append("x1=")
                    var first = true
                    for (bit in 0 until 32) {
                        val p = 1.shl(bit)
                        if (power.and(p) != 0) {
                            if (!first) {
                                manualCode.append('*')
                            } else first = false
                            manualCode.append("x$p")
                        }
                    }
                    manualCode.append(";\n")
                }

                val multiplications = manualCode.count { it == '*' }

                // LOGGER.info("$power: $manualCode")

                val shaders = listOf(
                    // manually optimized
                    createShader(init + repeat(manualCode.toString(), repeats) + end),
                    // can be optimized
                    createShader(init + repeat("x1=pow(x1,$power.0);", repeats) + end),
                    // can be optimized, int as power
                    createShader(init + repeat("x1=pow(x1,$power);", repeats) + end),
                    // slightly different, so it can't be optimized
                    createShader(init + repeat("x1=pow(x1,${power}.01);", repeats) + end),
                )

                for (shader in shaders) {
                    shader.use()
                }

                val pixels = ByteBufferPool.allocateDirect(4)

                buffer.clearColor(0f, 0f, 0f, 1f, depth = true)

                for (i in 0 until warmup) {
                    for (shader in shaders) {
                        shader.use()
                        flat01.draw(shader)
                    }
                }

                val flops = DoubleArray(shaders.size)
                val avg = 10 // for more stability between runs
                for (j in 0 until avg) {
                    for (index in shaders.indices) {
                        val shader = shaders[index]
                        GFX.check()
                        val t0 = Time.nanoTime
                        for (i in 0 until benchmark) {
                            shader.use()
                            flat01.draw(shader)
                        }
                        // synchronize
                        setReadAlignment(4)
                        GL46C.glReadPixels(0, 0, 1, 1, GL46C.GL_RGBA, GL46C.GL_UNSIGNED_BYTE, pixels)
                        GFX.check()
                        val t1 = Time.nanoTime
                        // the first one may be an outlier
                        if (j > 0) flops[index] += multiplications * repeats.toDouble() * benchmark.toDouble() * size * size / (t1 - t0)
                        GFX.check()
                    }
                }

                for (i in flops.indices) {
                    flops[i] /= (avg - 1.0)
                }

                println(
                    "" +
                            "$power,$multiplications," +
                            "${flops[0].roundToInt()}," +
                            "${flops[1].roundToInt()}," +
                            "${flops[2].roundToInt()}," +
                            "${flops[3].roundToInt()}," +
                            (flops[0] / flops[3]).f2()
                )

            }
        }
    }


}

/*

[17:07:07,INFO:HiddenOpenGLContext] Using LWJGL Version 3.2.3 build 13
[17:07:08,INFO:Clock] Used 0.079s for error callback
[17:07:08,INFO:Clock] Used 0.013s for GLFW initialization
[17:07:08,INFO:Clock] Used 0.200s for create window
Power,Multiplications,GFlops-multiplication,GFlops-floats,GFlops-ints,GFlops-power,Speedup
[17:07:08,WARN:Warning] Too early access of DefaultConfig[gpu.textureBudget]
2,1,1246,1429,1447,324,3.84
3,2,2663,2692,2708,651,4.09
4,2,2682,2679,2698,650,4.12
5,3,2766,972,974,973,2.84
6,3,2785,978,974,976,2.85
7,4,2830,1295,1303,1299,2.18
8,3,2783,2792,2809,960,2.90
9,4,2836,1298,1301,1302,2.18
10,4,2833,1291,1302,1298,2.18
11,5,2858,1623,1629,1623,1.76
12,4,2824,1302,1295,1303,2.17
13,5,2866,1628,1624,1626,1.76
14,5,2869,1614,1623,1611,1.78
15,6,2886,1945,1943,1953,1.48
16,4,2821,1305,1300,1305,2.16
17,5,2868,1615,1625,1619,1.77
18,5,2858,1620,1625,1624,1.76
19,6,2890,1949,1946,1949,1.48
20,5,2871,1618,1627,1625,1.77
21,6,2879,1945,1947,1943,1.48
22,6,2886,1944,1949,1952,1.48
23,7,2901,2271,2269,2268,1.28
24,5,2872,1621,1628,1624,1.77
25,6,2886,1942,1943,1942,1.49
26,6,2880,1949,1949,1953,1.47
27,7,2891,2273,2263,2266,1.28
28,6,2883,1949,1946,1953,1.48
29,7,2910,2279,2281,2279,1.28
30,7,2899,2272,2276,2277,1.27
31,8,2906,2598,2595,2596,1.12
32,5,2872,1621,1625,1622,1.77
33,6,2901,1953,1942,1949,1.49
34,6,2895,1948,1939,1944,1.49
35,7,2895,2274,2266,2268,1.28
36,6,2881,1937,1944,1948,1.48
37,7,2894,2277,2270,2280,1.27
38,7,2902,2275,2264,2273,1.28
39,8,2910,2602,2594,2603,1.12
40,6,2877,1945,1947,1945,1.48
41,7,2892,2276,2277,2282,1.27
42,7,2887,2271,2272,2273,1.27
43,8,2912,2599,2606,2599,1.12
44,7,2910,2278,2284,2276,1.28
45,8,2920,2597,2601,2600,1.12
46,8,2920,2600,2601,2590,1.13
47,9,2925,2921,2926,2927,1.00
48,6,2885,1935,1955,1956,1.47
49,7,2901,2271,2279,2288,1.27
50,7,2904,2281,2276,2278,1.27
51,8,2919,2608,2594,2607,1.12
52,7,2902,2282,2270,2273,1.28
53,8,2903,2598,2602,2598,1.12
54,8,2918,2602,2602,2604,1.12
55,9,2932,2927,2924,2936,1.00
56,7,2907,2284,2282,2281,1.27
57,8,2920,2606,2604,2610,1.12
58,8,2913,2593,2597,2587,1.13
59,9,2925,2923,2924,2920,1.00
60,8,2930,2614,2606,2613,1.12
61,9,2932,2946,2946,2947,1.00
62,9,2926,2935,2937,2947,0.99
63,10,2958,3258,3192,3266,0.91
64,6,2902,1957,1956,1959,1.48
65,7,2903,2274,2267,2273,1.28
66,7,2909,2277,2276,2286,1.27
67,8,2908,2602,2606,2599,1.12
68,7,2894,2272,2279,2276,1.27
69,8,2923,2597,2606,2606,1.12
70,8,2910,2596,2599,2600,1.12
71,9,2926,2921,2927,2924,1.00
72,7,2909,2283,2273,2273,1.28
73,8,2909,2602,2602,2599,1.12
74,8,2914,2602,2602,2603,1.12
75,9,2924,2925,2927,2933,1.00
76,8,2904,2608,2602,2601,1.12
77,9,2911,2919,2917,2909,1.00
78,9,2927,2921,2917,2935,1.00
79,10,2929,3241,3246,3246,0.90
80,7,2903,2273,2276,2275,1.28
81,8,2916,2596,2592,2589,1.13
82,8,2913,2600,2597,2598,1.12
83,9,2925,2931,2926,2913,1.00
84,8,2917,2598,2606,2597,1.12
85,9,2920,2916,2918,2927,1.00
86,9,2942,2922,2944,2936,1.00
87,10,2961,3254,3259,3268,0.91
88,8,2934,2607,2608,2612,1.12
89,9,2918,2939,2931,2916,1.00
90,9,2927,2928,2920,2924,1.00
91,10,2940,3253,3252,3246,0.91
92,9,2924,2933,2926,2928,1.00
93,10,2940,3259,3237,3251,0.90
94,10,2928,3247,3247,3264,0.90
95,11,2933,3599,3593,3594,0.82
96,7,2883,2282,2268,2269,1.27
97,8,2911,2602,2595,2600,1.12
98,8,2896,2588,2591,2587,1.12
99,9,2924,2939,2936,2938,1.00

Process finished with exit code 0

* */