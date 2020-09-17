package me.karl.dataStructures;

/**
 * Contains the extracted data for an animated model, which includes the mesh data, and skeleton (joints heirarchy) data.
 *
 * @author Karl
 */
public class AnimatedModelData {

    private final SkeletonData joints;
    private final MeshData mesh;
    private final TextureData textures;

    public AnimatedModelData(MeshData mesh, SkeletonData joints, TextureData textures) {
        this.joints = joints;
        this.mesh = mesh;
        this.textures = textures;
    }

    public SkeletonData getJointsData() {
        return joints;
    }

    public MeshData getMeshData() {
        return mesh;
    }

    public TextureData getTextureData() {
        return textures;
    }

}
