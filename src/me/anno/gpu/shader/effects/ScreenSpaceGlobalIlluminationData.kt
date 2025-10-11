package me.anno.gpu.shader.effects

import me.anno.gpu.texture.ITexture2D

/**
 * Used for calculating SSGI instead of SSAO.
 * */
class ScreenSpaceGlobalIlluminationData(
    val illuminated: ITexture2D, val color: ITexture2D,
    val roughness: ITexture2D, val roughnessMask: Int,
)