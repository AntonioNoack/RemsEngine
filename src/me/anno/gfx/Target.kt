package me.anno.gfx

interface Target {
    val width: Int
    val height: Int
    val depth: Int
    val samples: Int
    fun destroy()
}

interface Texture : Target
interface Texture2D : Texture
interface Texture2DArray : Texture
interface Texture3D : Texture
interface TextureCube : Texture
