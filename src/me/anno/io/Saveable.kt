package me.anno.io

import me.anno.engine.EngineBase
import me.anno.io.json.saveable.JsonStringWriter

/**
 * base implementation for everything that should be saveable
 * */
open class Saveable : ISaveable {
    override fun toString(): String {
        return JsonStringWriter.toText(this, EngineBase.workspace)
    }
}