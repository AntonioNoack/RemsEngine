/*
recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.recast

import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class Telemetry {
    companion object {
        private val LOGGER = LogManager.getLogger(Telemetry::class)
    }

    private val startTimes = ThreadLocal.withInitial<HashMap<TelemetryType, Long>> { HashMap() }
    private val accumulator = ConcurrentHashMap<TelemetryType, AtomicLong>()

    fun startTimer(type: TelemetryType) {
        startTimes.get()[type] = System.nanoTime()
    }

    fun stopTimer(type: TelemetryType) {
        accumulator.computeIfAbsent(type) { AtomicLong() }
            .addAndGet(System.nanoTime() - startTimes.get()[type]!!)
    }

    fun warn(string: String?) {
        System.err.println(string)
    }

    fun print() {
        accumulator.forEach { (n: TelemetryType, v: AtomicLong) ->
            val time = v.get() / 100_000
            LOGGER.info("${n.name}: ${time / 10}.${time % 10} ms")
        }
    }
}