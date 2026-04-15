package me.anno.mesh.usd

/**
 * USDA primitive
 * */
data class USDPrim(
    val type: String,
    val name: String,
    val properties: HashMap<String, Any?> = HashMap(),
    val children: ArrayList<USDPrim> = ArrayList(),
    val relationships: HashMap<String, String?> = HashMap(),
    var references: ArrayList<USDReference> = ArrayList(),
    var isInstance: Boolean = false
) {
    var typeName: String? = null
}