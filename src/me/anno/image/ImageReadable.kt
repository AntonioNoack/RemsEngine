package me.anno.image

import me.anno.image.raw.GPUImage

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

    // todo we could add functions for the metadata...

}