package me.anno.tests.io.files

import me.anno.io.json.generic.JsonFormatter
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class JsonFormatterTest {
    @Test
    fun transformTwiceShouldBeSame() {
        val src = """
         [{"class":"Prefab","i:*ptr":1,"S:className":"Entity","CAdd[]:adds":[7,{"c:type":"c","S:name":"BoxCollider",
         "S:className":"BoxCollider"},{"c:type":"c","S:name":"BulletPhysics","S:className":"BulletPhysics"},{"c:type":"c",
         "S:name":"DynamicBody","S:className":"DynamicBody"},{"c:type":"c","S:name":"SliderConstraint","S:className":"SliderConstraint"},
         {"c:type":"e","S:name":"Entity","S:className":"Entity"},{"Path:path":{"S:v":"e0,Entity"},"c:type":"c","S:name":"BoxCollider",
         "S:className":"BoxCollider"},{"Path:path":{"S:v":"e0,Entity"},"c:type":"c","S:name":"DynamicBody","S:className":"DynamicBody"}],
         "CSet[]:sets":[18,{"i:*ptr":11,"Path:path":{"i:*ptr":12,"S:v":"c2,DynamicBody"},"b:overrideGravity":true},{"Path:path":{"S:v":"e0,Entity"},
         "v3d:position":[-2.4744907802625904,0.018504960234546175,-1.7895318679968293]},{"Path:path":{"S:v":"c3,SliderConstraint"},"Path:other":{"S:v":
         "e0,Entity/c1,DynamicBody"}},{"Path:path":{"S:v":"e0,Entity/c1,DynamicBody"},"b:overrideGravity":true},{"Path:path":{"S:v":"e0,Entity"},"b:isCollapsed":false},
         {"v3d:gravity":[10,0,0]},{"b:isEnabled":true},{"q4d:rotation":[0,0,0,1]},{"v3d:scale":[1]},{"S:name":"Colliders"},{"b:isCollapsed":false},
         {"v3d:position":[0.9965685024139616,-4.998401793503934,3.8464554916186247]},{"Path:path":{"S:v":"c1,BulletPhysics"},"d:automaticDeathHeight":-5},
         {"Path:path":{"S:v":"c2,DynamicBody"},"d:mass":1},{"Path:path":32,"v3d:gravity":[0,1,0]},{"Path:path":{"S:v":"e0,Entity"},"v3d:position":
         [-1.8121681560740845,-0.022380086565996216,-2.405396522738568]},{"Path:path":35,"q4d:rotation":[0,0,0,1]},{"Path:path":35,"v3d:scale":[1]}]}]
    """.replace('"', '"')

        val f1 = JsonFormatter.format(src)
        val f2 = JsonFormatter.format(f1)

        assertEquals(f1, f2)
    }
}