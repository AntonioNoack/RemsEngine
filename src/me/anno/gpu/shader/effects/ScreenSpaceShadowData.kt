package me.anno.gpu.shader.effects

import org.joml.Vector3f

/**
 * SSR, Used for adding surface-shadows to SSAO.
 * Not compatible with SSGI (yet)?
 * */
class ScreenSpaceShadowData(
    val direction: Vector3f,
    val strength: Float
)