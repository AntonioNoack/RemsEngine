package me.anno.tests.engine

import me.anno.Build
import me.anno.engine.RemsEngine

/**
 * This start-the-engine function is located in the test project on purpose:
 * here, all extensions are available, while the main project turns into a "base" project.
 * */
fun main() {
    // todo tabs need to show that their scenes were modified
    if (false) Build.isDebug = false
    RemsEngine().run()
}