package me.anno.ui.input

import me.anno.ui.Panel

interface InputPanel<V> {

    var isEnabled: Boolean

    val lastValue: V

    fun setValue(value: V, notify: Boolean): Panel

}