package me.anno.io.utils

import me.anno.Engine
import me.anno.Time.nanoTime
import me.anno.utils.Threads
import me.anno.engine.Events.addEvent
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.utils.OSFeatures

class SaveMaybe {
    private val saveDelay = SECONDS_TO_NANOS
    private var lastSaveTime = nanoTime - saveDelay - 1
    fun saveMaybe(name: String, wasChanged: () -> Boolean, save: () -> Unit) {
        if (wasChanged()) {
            synchronized(this) {
                if (nanoTime - lastSaveTime >= saveDelay || Engine.shutdown) {// only save every 1s
                    if (!OSFeatures.hasMultiThreading) {
                        save()
                        lastSaveTime = nanoTime
                    } else {
                        // delay in case it needs longer
                        lastSaveTime = nanoTime + 60 * SECONDS_TO_NANOS
                        Threads.runTaskThread("Saving $name") {
                            save()
                            lastSaveTime = nanoTime
                        }
                    }
                } else {
                    addEvent(10) {
                        saveMaybe(name, wasChanged, save)
                    }
                }
            }
        }
    }
}