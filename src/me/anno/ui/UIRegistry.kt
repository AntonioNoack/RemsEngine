package me.anno.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.components.ui.CanvasComponent
import me.anno.fonts.Font
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.ui.anim.AnimContainer
import me.anno.ui.anim.MoveAnimation
import me.anno.ui.anim.ScaleAnimation
import me.anno.ui.base.image.IconPanel
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.ImageButton
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.NineTilePanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.LinkPanel
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.color.ColorChooser
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.ColorInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.FloatVectorInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.IntVectorInput
import me.anno.ui.input.TextInput
import me.anno.ui.input.TextInputML
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.input.components.PureTextInputML

object UIRegistry {

    fun init() {

        // ui base
        registerCustomClass(CanvasComponent())

        // ui containers
        val style = DefaultConfig.style
        registerCustomClass(Font())
        registerCustomClass(Padding())
        registerCustomClass { Panel(style) }
        registerCustomClass { PanelListX(style) }
        registerCustomClass { PanelListY(style) }
        registerCustomClass { PanelStack(style) }
        registerCustomClass { ScrollPanelX(style) }
        registerCustomClass { ScrollPanelY(style) }
        registerCustomClass { ScrollPanelXY(style) }
        registerCustomClass { NineTilePanel(style) }
        registerCustomClass { TitledListY(style) }

        // ui animations
        registerCustomClass { AnimContainer(style) }
        registerCustomClass(MoveAnimation())
        registerCustomClass(ScaleAnimation())

        // ui content
        registerCustomClass { TextPanel(style) }
        registerCustomClass { LinkPanel(style) }
        registerCustomClass { SimpleTextPanel(style) }
        registerCustomClass { IconPanel(style) }
        registerCustomClass { TextButton(style) }
        registerCustomClass { ImageButton(style) }
        registerCustomClass { SpacerPanel(style) }
        registerCustomClass { ColorChooser(style) }
        registerCustomClass { ColorInput(style) }
        registerCustomClass { BooleanInput(style) }
        registerCustomClass { FloatInput(style) }
        registerCustomClass { FloatVectorInput(style) }
        registerCustomClass { IntInput(style) }
        registerCustomClass { IntVectorInput(style) }
        registerCustomClass { TextInput(style) }
        registerCustomClass { TextInputML(style) }
        registerCustomClass { PureTextInput(style) }
        registerCustomClass { PureTextInputML(style) }
        // not finished:
        // registerCustomClass { ConsoleInput(style) }
    }
}