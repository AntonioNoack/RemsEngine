package me.anno.ecs.annotations

/**
 * elements with smaller index come first
 * the default value is 0, so use negative numbers to come first, and positive values to come last
 * */
annotation class Order(val index: Int)
