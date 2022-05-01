package me.anno.ui.input

import me.anno.ui.Panel

interface InputPanel<V> {

    var isEnabled: Boolean

    var isInputAllowed: Boolean

    val lastValue: V

    fun setValue(value: V, notify: Boolean): Panel

}