package me.anno.remsstudio.objects.inspectable

import me.anno.animation.AnimatedProperty

data class InspectableAnimProperty(val value: AnimatedProperty<*>, val title: String, val description: String = "")