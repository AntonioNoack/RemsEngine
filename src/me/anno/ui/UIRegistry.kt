package me.anno.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.components.ui.CanvasComponent
import me.anno.fonts.Font
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.ui.anim.AnimContainer
import me.anno.ui.anim.MoveAnimation
import me.anno.ui.anim.ScaleAnimation
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.ImageButton
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.NineTilePanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.image.IconPanel
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
import kotlin.reflect.KClass
import kotlin.reflect.javaType

object UIRegistry {

    fun init() {

        // ui base
        registerCustomClass(CanvasComponent())

        // ui containers
        registerCustomClass(Font())
        registerCustomClass(Padding())
        registerPanelClass(Panel::class)
        registerPanelClass(PanelListX::class)
        registerPanelClass(PanelListY::class)
        registerPanelClass(PanelStack::class)
        registerPanelClass(ScrollPanelX::class)
        registerPanelClass(ScrollPanelY::class)
        registerPanelClass(ScrollPanelXY::class)
        registerPanelClass(NineTilePanel::class)
        registerPanelClass(TitledListY::class)

        // ui animations
        registerPanelClass(AnimContainer::class)
        registerCustomClass(MoveAnimation())
        registerCustomClass(ScaleAnimation())

        // ui content
        registerPanelClass(TextPanel::class)
        registerPanelClass(LinkPanel::class)
        registerPanelClass(SimpleTextPanel::class)
        registerPanelClass(IconPanel::class)
        registerPanelClass(TextButton::class)
        registerPanelClass(ImageButton::class)
        registerPanelClass(SpacerPanel::class)
        registerPanelClass(ColorChooser::class)
        registerPanelClass(ColorInput::class)
        registerPanelClass(BooleanInput::class)
        registerPanelClass(FloatInput::class)
        registerPanelClass(FloatVectorInput::class)
        registerPanelClass(IntInput::class)
        registerPanelClass(IntVectorInput::class)
        registerPanelClass(TextInput::class)
        registerPanelClass(TextInputML::class)
        registerPanelClass(PureTextInputML::class)
        // not finished:
        // registerPanelClass(ConsoleInput::class)
    }

    fun <V : Panel> registerPanelClass(clazz: KClass<V>) {
        val constructor = clazz.constructors.first {
            it.parameters.size == 1 &&
                    it.parameters.first().type.classifier == Style::class
        }
        registerCustomClass { constructor.call(DefaultConfig.style) }
    }
}