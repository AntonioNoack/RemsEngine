package me.karl.main;



import org.joml.Vector3f;
import me.karl.utils.URI;

/**
 * Just some configs. File locations mostly.
 * 
 * @author Karl
 *
 */
public class GeneralSettings {
	
	public static final URI RES_FOLDER = new URI("res");
	public static final String MODEL_FILE = "model.dae";
	public static final String ANIM_FILE = "model.dae";
	public static final String DIFFUSE_FILE = "diffuse.png";
	
	public static final int MAX_WEIGHTS = 3;
	
	public static final Vector3f LIGHT_DIR = new Vector3f(0.2f, -0.3f, -0.8f);
	
}
