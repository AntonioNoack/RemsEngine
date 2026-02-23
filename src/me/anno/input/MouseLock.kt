package me.anno.input

import me.anno.gpu.OSWindow
import me.anno.ui.Panel

/**
 * captures/holds/freezes mouse cursor:
 *   cursor becomes invisible
 *   absolute mouse position unknown, only dx,dy known
 *   mouse can no longer collide with window border,
 *   mouse can no longer switch windows
 * */
object MouseLock {

    var mouseLockWindow: OSWindow? = null
    var mouseLockPanel: Panel? = null

    val isMouseLocked: Boolean
        get() = mouseLockWindow?.isInFocus == true && mouseLockPanel != null

    fun unlockMouse() {
        mouseLockWindow = null
        mouseLockPanel = null
    }

}