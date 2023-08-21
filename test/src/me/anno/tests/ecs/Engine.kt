package me.anno.tests.ecs

import me.anno.engine.RemsEngine

/**
 * This start-the-engine function is located in the test project on purpose:
 * here, all extensions are available, while the main project turns into a "base" project.
 * */
fun main() {
    RemsEngine().run()
}