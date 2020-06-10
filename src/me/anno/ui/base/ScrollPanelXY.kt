package me.anno.ui.base

import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

open class ScrollPanelXY(child: Panel, padding: Padding,
                    style: Style,
                    alignX: WrapAlign.AxisAlignment,
                    alignY: WrapAlign.AxisAlignment):
    ScrollPanelX(
        ScrollPanelY(child,
            Padding(0), style, alignY),
        padding, style, alignX){

    constructor(padding: Padding, style: Style): this(
        PanelListY(style), padding, style,
        WrapAlign.AxisAlignment.MIN,
        WrapAlign.AxisAlignment.MIN)

    val content = (this.child as ScrollPanelY).child

    override fun getClassName(): String = "ScrollPanelXY"

}