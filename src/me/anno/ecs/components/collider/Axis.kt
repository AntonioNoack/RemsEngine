package me.anno.ecs.components.collider

enum class Axis(val id: Int, val secondary: Int, val tertiary: Int) {
    X(0, 1, 2),
    Y(1, 0, 2),
    Z(2, 0, 1);

    val char = 'x' + id
}