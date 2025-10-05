package me.anno.ecs.systems

/**
 * Component, that can be looked up in GlobalSettings.
 * This only can be reasonably used, if the configuration is indeed global for a certain point in time.
 *
 * This only works if the setting class is final! (todo fix?)
 * */
interface GlobalSetting {

    /**
     * The setting with the highest priority value is chosen.
     * The default priority should be 0.
     * */
    var priority: Double
}