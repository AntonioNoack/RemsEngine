package me.anno.ui.input

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility

object InputVisibility {

    private val visible = HashMap<String,Visibility>()

    operator fun get(title: String) = visible[title] ?: Visibility.GONE

    fun toggle(title: String, panel: Panel){
        visible[title] = if(this[title] != Visibility.VISIBLE) Visibility.VISIBLE else Visibility.GONE
        panel.invalidateLayout()
    }

    fun show(title: String, panel: Panel?){
        visible[title] = Visibility.VISIBLE
        panel?.invalidateLayout()
    }

}