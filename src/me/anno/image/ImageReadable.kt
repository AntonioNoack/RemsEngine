package me.anno.image

import me.anno.image.raw.GPUImage
import org.joml.Vector2i

/**
 * class, where reading an image is cheap
 * */
interface ImageReadable {

    /**
     * this check should be fast, so if you're a lazy class,
     * just return false
     * */
    fun hasInstantGPUImage(): Boolean {
        return readGPUImage() is GPUImage
    }

    /**
     * this check should be fast, so if you're a lazy class,
     * just return false
     * */
    fun hasInstantCPUImage(): Boolean {
        return true
    }

    /**
     * gets the image; CPUImage if possible
     * */
    fun readCPUImage(): Image

    /**
     * gets the image; GPUImage if possible
     * */
    fun readGPUImage(): Image

    /**
     * gets image size
     * */
    fun readSize(): Vector2i

}