package me.anno.maths.bvh.shader

abstract class RTShaderLib {
    abstract fun glslBLASIntersection(telemetry: Boolean): String
    abstract fun glslTLASIntersection(telemetry: Boolean): String
}