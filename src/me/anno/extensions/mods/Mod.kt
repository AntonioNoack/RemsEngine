package me.anno.extensions.mods

import me.anno.extensions.Extension

/**
 * Mods are extensions, which are loaded at startup, and cannot be deactivated at runtime
 * they can implement very basic functionalities and override core aspects of the program
 * */
abstract class Mod : Extension() {

    open fun onPreInit() {}
    open fun onInit() {}
    open fun onPostInit() {}

    /**
     * may be called, when the program exists
     * if the program is killed, which will not be executed
     * async
     * */
    open fun onExit() {}

}