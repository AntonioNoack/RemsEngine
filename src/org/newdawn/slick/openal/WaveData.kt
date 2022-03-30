package org.newdawn.slick.openal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import me.anno.utils.pooling.ByteBufferPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLUtil;

// still used for loading wav files
// (when not working with ffmpeg)
public class WaveData {

    private final static Logger LOGGER = LogManager.getLogger(WaveData.class);

    public final ByteBuffer data;
    public final int format;
    public final int samplerate;
    // public -> can't change the naming

    private WaveData(ByteBuffer data, int format, int sampleRate) {
        this.data = data;
        this.format = format;
        this.samplerate = sampleRate;
    }

    public void dispose() {
        this.data.clear();
    }

    public static WaveData create(URL path) {
        try {
            return create(AudioSystem.getAudioInputStream(new BufferedInputStream(path.openStream())));
        } catch (Exception var2) {
            LWJGLUtil.log("Unable to create from: " + path);
            var2.printStackTrace();
            return null;
        }
    }

    public static WaveData create(String path) {
        return create(WaveData.class.getClassLoader().getResource(path));
    }

    private static AudioInputStream getAudioInputStream(InputStream is) throws IOException, UnsupportedAudioFileException {
        /*try {
            return new WaveFloatFileReader().getAudioInputStream(is);// we know the format
        } catch (UnsupportedAudioFileException e){
            return AudioSystem.getAudioInputStream(is);
        }*/
        return AudioSystem.getAudioInputStream(is);
    }

    /*public static WaveData create(InputStream is, int frameCount) {
        try {
            // new WaveReader(is, frameCount);
            WaveReader.INSTANCE.readWAV(is, frameCount);
            // return create(getAudioInputStream(is), frameCount);
        } catch (Exception e) {
            LOGGER.warn("Unable to create from inputstream", e);
            e.printStackTrace();
            return null;
        }
    }*/

    public static WaveData create(InputStream is) {
        try {
            return create(getAudioInputStream(is), -1);
        } catch (Exception e) {
            LOGGER.warn("Unable to create from inputstream", e);
            e.printStackTrace();
            return null;
        }
    }

    public static WaveData create(byte[] buffer) {
        try {
            return create(AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(buffer))));
        } catch (Exception e) {
            LOGGER.warn("Unable to create from byte[]", e);
            e.printStackTrace();
            return null;
        }
    }

    public static WaveData create(ByteBuffer buffer) {
        try {
            byte[] bytes;
            if (buffer.hasArray()) {
                bytes = buffer.array();
            } else {
                bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
            }

            return create(bytes);
        } catch (Exception var2) {
            var2.printStackTrace();
            return null;
        }
    }

    public static WaveData create(AudioInputStream ais, int frameCount) {
        AudioFormat audioformat = ais.getFormat();
        int format;
        switch(audioformat.getChannels()){
            case 1:
                switch(audioformat.getSampleSizeInBits()){
                    case 8: format = 4352;break;
                    case 16:format = 4353;break;
                    default:throw new RuntimeException("Illegal sample size");
                }
                break;
            case 2:
                switch(audioformat.getSampleSizeInBits()){
                    case 8: format = 4354;break;
                    case 16:format = 4355;break;
                    default:throw new RuntimeException("Illegal sample size");
                }
                break;
            default:
                throw new RuntimeException("Only mono or stereo is supported");
        }

        // FFMPEG writes the wrong amount of frames into the file
        // we have to correct that; luckily we know the amount of frames
        int frameLength = frameCount > -1 ? frameCount : (int) ais.getFrameLength();
        int byteLength = audioformat.getChannels() * frameLength * audioformat.getSampleSizeInBits() / 8;
        byte[] buf = new byte[byteLength];

        int targetLength = 0;

        try {
            int length;
            while((length = ais.read(buf, targetLength, buf.length - targetLength)) != -1 && targetLength < buf.length) {
                targetLength += length;
            }
        } catch (IOException var10) {
            return null;
        }

        ByteBuffer buffer = convertAudioBytes(buf, audioformat.getSampleSizeInBits() == 16);
        WaveData wavedata = new WaveData(buffer, format, (int)audioformat.getSampleRate());

        try {
            ais.close();
        } catch (IOException ignored) { }

        return wavedata;
    }

    private static ByteBuffer convertAudioBytes(byte[] audio_bytes, boolean two_bytes_data) {
        ByteBuffer dest = ByteBufferPool.Companion.allocateDirect(audio_bytes.length);
        // the source is little endian, correct?
        if(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN || !two_bytes_data){

            // it's fine from the start
            dest.put(audio_bytes);

        } else {

            ByteBuffer src = ByteBuffer.wrap(audio_bytes);
            src.order(ByteOrder.LITTLE_ENDIAN);

            ShortBuffer dest_short = dest.asShortBuffer();
            dest_short.put(src.asShortBuffer());

        }
        dest.flip();
        return dest;

    }
}
