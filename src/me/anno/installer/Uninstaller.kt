package me.anno.installer

import me.anno.io.config.ConfigBasics

object Uninstaller {

    fun uninstall(){
        ConfigBasics.cacheFolder.deleteRecursively()
    }

}