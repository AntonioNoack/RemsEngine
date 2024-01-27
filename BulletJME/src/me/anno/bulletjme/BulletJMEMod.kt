package me.anno.bulletjme

import com.jme3.system.NativeLibraryLoader.loadLibbulletjme
import me.anno.extensions.mods.Mod
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.utils.OS.downloads
import java.io.File

@Suppress("unused")
class BulletJMEMod : Mod() {
    companion object {
        init {
            loadLibbulletjme(
                true, File(downloads.getChild("lib/java/bullet-jme").absolutePath),
                "Release", "Dp"
            )
        }
    }

    override fun onPreInit() {
        super.onPreInit()
        registerCustomClass(BulletPhysics())
        registerCustomClass(Rigidbody())
    }
}
