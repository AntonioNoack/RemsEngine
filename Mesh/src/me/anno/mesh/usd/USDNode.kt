package me.anno.mesh.usd

class USDNode(val parent: USDNode?, val name: String, val isProperty: Boolean) {
    val separator get() = if (isProperty) '.' else '/'
    val absolutePath: String =
        if (parent != null) {
            if (parent.parent == null) "${parent.name}$separator$name"
            else "${parent.absolutePath}$separator$name"
        } else "/"

    override fun toString(): String {
        return absolutePath
    }
}