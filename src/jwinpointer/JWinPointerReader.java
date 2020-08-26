//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package jwinpointer;

/*import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import cswinpointer.IWinPointerReader;
import cswinpointer.WinPointerPlugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import javax.swing.JFrame;
import net.sf.jni4net.Bridge;

public class JWinPointerReader implements IWinPointerReader {

    private final String APP_DIR;
    private final ArrayList<JWinPointerReader.PointerEventListener> _pointerEventListeners;

    public void addPointerEventListener(JWinPointerReader.PointerEventListener listener) {
        this._pointerEventListeners.add(listener);
    }

    public JWinPointerReader(JFrame window) {
        this(window.getTitle());
    }

    public JWinPointerReader(String windowName) {
        this.APP_DIR = "JWinPointer\\";
        this._pointerEventListeners = new ArrayList<>();
        this.initializeBridge();
        HWND hWnd = User32.INSTANCE.FindWindow(null, windowName);
        long nativeHandle = Pointer.nativeValue(hWnd.getPointer());
        WinPointerPlugin plugin = new WinPointerPlugin();
        plugin.Initialize(this, nativeHandle);
    }

    private void initializeBridge() {
        try {
            int model = Integer.parseInt(System.getProperty("sun.arch.data.model"));
            String jni4netLib = "jni4net.n.w" + model + ".v40-0.8.8.0.dll";
            String appData = System.getProperty("java.io.tmpdir") + "JWinPointer\\";
            this.extractDependencies(appData);
            Bridge.setVerbose(true);
            String jni4netPath = appData + jni4netLib;
            Bridge.init(new File(jni4netPath));
            Bridge.LoadAndRegisterAssemblyFrom(new File(appData + "CsWinPointer.j4n.dll"));
        } catch (IOException var5) {
            var5.printStackTrace();
        }

    }

    private void extractDependencies(String appDataDir) throws IOException {
        File directory = new File(appDataDir);
        if (!directory.exists()) { directory.mkdirs(); }

        try {
            for(URL url: Resources.getResourceURLs(
                    url -> { String url2 = url.toString(); return url2.endsWith(".jar") || url2.endsWith(".dll"); })
            ){
                String urlStr = url.toString();
                String fileName = urlStr.substring(urlStr.lastIndexOf('/') + 1);
                InputStream in = url.openStream();
                File fileOut = new File(appDataDir + fileName);
                Files.copy(in, fileOut.toPath(), StandardCopyOption.REPLACE_EXISTING);
                in.close();
            }
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }

    }

    public void PointerXYEvent(int deviceType, int pointerID, int eventType, boolean inverted, int x, int y, int pressure) {
        for(JWinPointerReader.PointerEventListener listener: _pointerEventListeners){
            listener.pointerXYEvent(deviceType, pointerID, eventType, inverted, x, y, pressure);
        }
    }

    public void PointerButtonEvent(int deviceType, int pointerID, int eventType, boolean inverted, int buttonIndex) {
        for(JWinPointerReader.PointerEventListener listener: _pointerEventListeners){
            listener.pointerButtonEvent(deviceType, pointerID, eventType, inverted, buttonIndex);
        }

    }

    public void PointerEvent(int deviceType, int pointerID, int eventType, boolean inverted) {
        for(JWinPointerReader.PointerEventListener listener: _pointerEventListeners){
            listener.pointerEvent(deviceType, pointerID, eventType, inverted);
        }
    }

    public interface PointerEventListener {
        void pointerXYEvent(int var1, int var2, int var3, boolean var4, int var5, int var6, int var7);
        void pointerButtonEvent(int var1, int var2, int var3, boolean var4, int var5);
        void pointerEvent(int var1, int var2, int var3, boolean var4);
    }

}*/
