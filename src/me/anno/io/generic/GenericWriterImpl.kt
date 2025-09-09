package me.anno.io.generic

import me.anno.utils.assertions.assertTrue

abstract class GenericWriterImpl : GenericWriter {

    companion object {
        private const val NTH_STATE_OBJECT = 'o'
        private const val NTH_STATE_ARRAY = 'a'
        private const val STATE_PROPERTY = 'p'

        private const val FIRST_STATE_OBJECT = 'O'
        private const val FIRST_STATE_ARRAY = 'A'
    }

    val depth get() = stack.length

    private val stack = StringBuilder()
    private fun pushState(state: Char) {
        stack.append(state)
    }

    private fun popState(firstState: Char, nthState: Char) {
        val last = stack.last()
        assertTrue(firstState == last || nthState == last) {
            "Incorrect state! '$stack' -= '$nthState'"
        }
        stack.setLength(stack.length - 1)
    }

    fun onValue() {
        // object, array -> ok, property -> pop
        if (stack.isEmpty()) return
        val lastState = stack.last()
        val newState = when (lastState) {
            STATE_PROPERTY -> {
                stack.setLength(stack.length - 1)
                return // early exit
            }
            FIRST_STATE_ARRAY -> NTH_STATE_ARRAY
            FIRST_STATE_OBJECT -> NTH_STATE_OBJECT
            else -> lastState
        }
        if (lastState == newState) {
            next()
        } else {
            stack[stack.lastIndex] = newState
        }
    }

    override fun beginObject(tag: CharSequence?): Boolean {
        onValue()
        pushState(FIRST_STATE_OBJECT)
        return true
    }

    override fun endObject() {
        popState(FIRST_STATE_OBJECT, NTH_STATE_OBJECT)
    }

    override fun beginArray(): Boolean {
        onValue()
        pushState(FIRST_STATE_ARRAY)
        return true
    }

    override fun endArray() {
        popState(FIRST_STATE_ARRAY, NTH_STATE_ARRAY)
    }

    override fun attr(tag: CharSequence): Boolean {
        onValue()
        popState(FIRST_STATE_OBJECT, NTH_STATE_OBJECT)
        pushState(NTH_STATE_OBJECT)
        pushState(STATE_PROPERTY)
        return true
    }

    override fun write(value: CharSequence, isString: Boolean) {
        onValue()
    }

    open fun next() {}

    open fun finish() {}
}