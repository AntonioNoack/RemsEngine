package me.anno.extensions.plugins

import me.anno.extensions.Extension

abstract class Plugin : Extension() {
    /**
     * is called, when the plugin is started
     * loads async
     * everything, that is not 100% always the same, should be initialized here
     * */
    open fun onEnable() {}

    /**
     * is called at the end of plugin life,
     * should clean up and save everything
     * asnyc as well
     * will not be executed, if the program is killed
     * */
    open fun onDisable() {}
}