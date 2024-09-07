package me.anno.tests.map

import me.anno.config.DefaultConfig.style
import me.anno.ui.debug.TestEngine.Companion.testUI3

/**
 * todo hierarchical database is broken!!!
 * */
fun main() {
    testUI3("Open Street Maps", OSMPanelV2(style))
    // https://wiki.openstreetmap.org/wiki/Overpass_API#The_Programmatic_Query_Language_(OverpassQL)
    // 51.249,7.148,51.251,7.152) is minimum latitude, minimum longitude, maximum latitude, maximum longitude (or South-West-North-East)
    /*var result = await fetch(
    "https://overpass-api.de/api/interpreter",
    {
        method: "POST",
        body: "data="+ encodeURIComponent(`
            [bbox:30.618338,-96.323712,30.591028,-96.330826]
            [out:json]
            [timeout:90]
            ;
            (
                way
                    (
                         30.626917110746,
                         -96.348809105664,
                         30.634468750236,
                         -96.339893442898
                     );
            );
            out geom;
        `)
    },
).then(
    (data)=>data.json()
)
console.log(JSON.stringify(result , null, 2))*/
}
