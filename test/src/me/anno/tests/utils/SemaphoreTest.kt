package me.anno.tests.utils

import me.anno.tests.LOGGER
import me.anno.utils.Threads
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

fun main() {

    // low cpu usage, and it works:
    // max is 3

    val s = Semaphore(3)
    val c = AtomicInteger()
    var max = 0

    repeat(12) {
        Threads.start("SemaphoreTest") {
            repeat(1000) {
                s.acquire()
                val ci = c.incrementAndGet()
                if (ci > max) {
                    max = ci
                    LOGGER.info("max: $max")
                }
                Thread.sleep(1)
                c.decrementAndGet()
                s.release()
            }
        }
    }
}