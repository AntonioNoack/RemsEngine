package me.anno.ui.input

import me.anno.ui.Panel

interface InputPanel<V> {

    var isEnabled: Boolean

    var isInputAllowed: Boolean

    val value: V

    fun setValue(newValue: V, notify: Boolean): Panel

}