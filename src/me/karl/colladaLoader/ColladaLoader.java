package me.karl.colladaLoader;

import me.karl.dataStructures.*;
import me.karl.utils.URI;
import me.karl.xmlParser.XmlNode;
import me.karl.xmlParser.XmlParser;

public class ColladaLoader {

	public static AnimatedModelData loadColladaModel(URI colladaFile, int maxWeights) {
		XmlNode node = XmlParser.loadXmlFile(colladaFile);

		SkinLoader skinLoader = new SkinLoader(node.getChild("library_controllers"), maxWeights);
		SkinningData skinningData = skinLoader.extractSkinData();

		SkeletonLoader jointsLoader = new SkeletonLoader(node.getChild("library_visual_scenes"), skinningData.jointOrder);
		SkeletonData jointsData = jointsLoader.extractBoneData();

		GeometryLoader g = new GeometryLoader(node.getChild("library_geometries"), skinningData.verticesSkinData);
		MeshData meshData = g.extractModelData();

		TextureLoader l = new TextureLoader(node.getChild("library_images"));
		TextureData textureData = l.extractTextureData();

		return new AnimatedModelData(meshData, jointsData, textureData);
	}

	public static AnimationData loadColladaAnimation(URI colladaFile) {
		XmlNode node = XmlParser.loadXmlFile(colladaFile);
		XmlNode animNode = node.getChild("library_animations");
		XmlNode jointsNode = node.getChild("library_visual_scenes");
		AnimationLoader loader = new AnimationLoader(animNode, jointsNode);
		AnimationData animData = loader.extractAnimation();
		return animData;
	}

}
