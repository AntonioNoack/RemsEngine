package me.karl.textures;

import me.anno.gpu.texture.Texture2D;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import me.karl.utils.URI;

public class Texture {

	public final int textureId;
	public final int size;
	private final int type;

	protected Texture(int textureId, int size) {
		this.textureId = textureId;
		this.size = size;
		this.type = GL11.GL_TEXTURE_2D;
	}

	protected Texture(int textureId, int type, int size) {
		this.textureId = textureId;
		this.size = size;
		this.type = type;
	}

	public void bindToUnit(int unit) {
		Texture2D.Companion.activeSlot(unit);
		Texture2D.Companion.bindTexture(type,textureId);
	}

	public void destroy() {
		GL11.glDeleteTextures(textureId);
	}

	public static TextureBuilder newTexture(URI textureFile) {
		return new TextureBuilder(textureFile);
	}

}
