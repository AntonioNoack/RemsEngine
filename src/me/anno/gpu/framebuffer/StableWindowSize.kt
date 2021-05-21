package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.studio.rems.RemsStudio

class StableWindowSize {

    var dx = 0
    var dy = 0

    private var lastW = 0
    private var lastH = 0

    var stableWidth = 0
    var stableHeight = 0

    private var lastSizeUpdate = 0L

    fun updateSize(width: Int, height: Int, onlyShowTarget: Boolean) {

        var stableWidth = 0
        var stableHeight = 0

        // check if the size stayed the same;
        // because resizing all framebuffers is expensive (causes lag)
        val matchesSize = lastW == width && lastH == height
        val wasNotRecentlyUpdated = lastSizeUpdate + 1e8 < GFX.gameTime
        if (matchesSize) {
            if (wasNotRecentlyUpdated) {
                stableWidth = width
                stableHeight = height
            }
        } else {
            lastSizeUpdate = GFX.gameTime
            lastW = width
            lastH = height
        }

        if (stableWidth == 0 || stableHeight == 0) {
            stableWidth = width
            stableHeight = height
        }

        // must be first for the frame to be centered
        if (onlyShowTarget) {
            if (stableWidth * RemsStudio.targetHeight > RemsStudio.targetWidth * stableHeight) {
                stableWidth = stableHeight * RemsStudio.targetWidth / RemsStudio.targetHeight
                dx = (width - stableWidth) / 2
                dy = 0
            } else {
                stableHeight = stableWidth * RemsStudio.targetHeight / RemsStudio.targetWidth
                dy = (height - stableHeight) / 2
                dx = 0
            }
        } else {
            dx = 0
            dy = 0
        }

        this.stableWidth = stableWidth
        this.stableHeight = stableHeight

    }

}