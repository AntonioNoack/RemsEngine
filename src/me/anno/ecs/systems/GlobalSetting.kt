package me.anno.ecs.systems

/**
 * Component, that can be looked up in GlobalSettings.
 * This only works if the setting class is final! (todo fix?)
 * */
interface GlobalSetting {

    /**
     * The setting with the highest priority value is chosen.
     * */
    var priority: Double
}