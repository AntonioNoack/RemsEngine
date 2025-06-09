package me.anno.tests.network

import java.util.concurrent.atomic.AtomicInteger

object NetworkTests {
    private val port = AtomicInteger(3000)
    fun nextPort(): Int = port.incrementAndGet()
}