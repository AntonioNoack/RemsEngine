package me.anno.tests.ui

import me.anno.engine.RemsEngine

fun main() {
    // done file explorer's scrolling is lagging every once in a while;
    //  - fps stays perfectly stable
    //  - panel reacts to hover events, responsive
    //  - children just are not layout correctly
    // done project list can get stuck the same way
    RemsEngine().run()
}