package me.anno.objects

import me.anno.objects.animation.AnimatedProperty

data class InspectableAnimProperty(val value: AnimatedProperty<*>, val title: String, val description: String = "")