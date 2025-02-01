package me.anno.tests.engine

import me.anno.engine.RemsEngine

/**
 * This start-the-engine function is located in the test project on purpose:
 * here, all extensions are available, while the main project turns into a "base" project.
 * */
fun main() {
    // todo tag-manager UI:
    //  - shows tags of current prefab
    //  - you can add tags, separate them by comma
    // todo assign colors to tags??
    // todo super tags, which combine multiple tags??
    // todo tag rules, e.g., pirate -> human
    // todo file explorer mode that uses tags instead of folders
    // todo collection-projects, which can be added to regular projects, and then are indexed, too?
    RemsEngine().run()
}