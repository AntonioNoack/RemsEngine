package me.karl.colladaLoader;

import me.karl.dataStructures.TextureData;
import me.karl.xmlParser.XmlNode;

import java.util.ArrayList;

public class TextureLoader {

    XmlNode imagesNode;

    public TextureLoader(XmlNode imagesNode) {
        this.imagesNode = imagesNode;
    }

    /**
     * <image id="character_Texture_png" name="character_Texture_png">
     *       <init_from>diffuse.png</init_from>
     *     </image>
     * */

    public TextureData extractTextureData(){
        TextureData data = new TextureData();
        for(XmlNode image: imagesNode.getChildren("image")){
            String src = image.getChild("init_from").getData();
            data.textures.add(src);
        }
        return data;
    }

}
