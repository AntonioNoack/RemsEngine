package me.anno.extensions.mods

import me.anno.extensions.Extension

/**
 * Mods are extensions, which are loaded at startup, and cannot be deactivated at runtime
 * they can implement very basic functionalities and override core aspects of the program
 * */
abstract class Mod: Extension() {

    abstract fun onPreInit()
    abstract fun onInit()
    abstract fun onPostInit()

    /**
     * may be called, when the program exists
     * if the program is killed, which will not be executed
     * async
     * */
    abstract fun onExit()

}