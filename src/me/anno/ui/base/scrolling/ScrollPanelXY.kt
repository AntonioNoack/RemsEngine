package me.anno.ui.base.scrolling

import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

open class ScrollPanelXY(child: Panel, padding: Padding,
                         style: Style,
                         alignX: AxisAlignment,
                         alignY: AxisAlignment):
    ScrollPanelX(
        ScrollPanelY(
            child,
            Padding(), style, alignY
        ),
        padding, style, alignX){

    constructor(padding: Padding, style: Style): this(
        PanelListY(style), padding, style,
        AxisAlignment.MIN,
        AxisAlignment.MIN)

    val content = (this.child as ScrollPanelY).child

    override fun getClassName(): String = "ScrollPanelXY"

}