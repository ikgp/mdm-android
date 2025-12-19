import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public final class LastChristmasBeepPlayer {

    private static volatile LastChristmasBeepPlayer instance;

    private static final int SAMPLE_RATE = 44100;
    private static final double VOLUME = 0.6;

    private AudioTrack audioTrack;
    private Thread playThread;
    private volatile boolean playing = false;

    // --- Notes (Hz) ---
    private static final double C4 = 261.63;
    private static final double D4 = 293.66;
    private static final double E4 = 329.63;
    private static final double F4 = 349.23;
    private static final double G4 = 392.00;
    private static final double A4 = 440.00;
    private static final double B4 = 493.88;
    private static final double C5 = 523.25;

    // --- Main melody ---
    private static final double[][] MELODY = {
        // Verse
        {G4, 400}, {A4, 400}, {G4, 400}, {E4, 600},
        {G4, 400}, {A4, 400}, {G4, 400}, {E4, 600},
        {D4, 400}, {E4, 400}, {D4, 400}, {A4, 800},

        {B4, 400}, {C5, 400}, {B4, 400}, {G4, 800},
        {G4, 400}, {A4, 400}, {G4, 400}, {E4, 600},
        {D4, 400}, {E4, 400}, {D4, 400}, {A4, 800},

        // Pre-Chorus ("Once bitten and twice shy…")
        {B4, 400}, {C5, 400}, {D5, 600},
        {D5, 400}, {C5, 400}, {B4, 800},
        {A4, 400}, {B4, 400}, {C5, 600},
        {B4, 400}, {A4, 400}, {G4, 800},

        // Chorus ("Last Christmas, I gave you my heart")
        {D5, 600}, {C5, 400}, {B4, 400}, {A4, 800},
        {B4, 400}, {C5, 600}, {B4, 400}, {A4, 800},
        {G4, 400}, {A4, 400}, {B4, 600},
        {A4, 400}, {G4, 1000}
    };

    // --- Singleton boilerplate ---
    private LastChristmasBeepPlayer() {
        // Prevent external instantiation
    }

    public static LastChristmasBeepPlayer getInstance() {
        if (instance == null) {
            synchronized (LastChristmasBeepPlayer.class) {
                if (instance == null) {
                    instance = new LastChristmasBeepPlayer();
                }
            }
        }
        return instance;
    }

    // --- Public API ---
    public synchronized void play() {
        if (playing) return;
        playing = true;

        int minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        audioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                minBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
        );

        audioTrack.play();

        playThread = new Thread(() -> {
            long startTime = System.currentTimeMillis();

            while (playing && System.currentTimeMillis() - startTime < 30_000) {
                for (double[] note : MELODY) {
                    if (!playing) break;
                    playTone(note[0], (int) note[1]);
                }
            }
            stop();
        }, "LastChristmasBeepThread");

        playThread.start();
    }

    public synchronized void stop() {
        playing = false;

        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    // --- Audio synthesis ---
    private void playTone(double freq, int durationMs) {
        int samples = (int) (durationMs / 1000.0 * SAMPLE_RATE);
        short[] buffer = new short[samples];

        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * i * freq / SAMPLE_RATE;
            buffer[i] = (short) (Math.sin(angle) * Short.MAX_VALUE * VOLUME);
        }

        audioTrack.write(buffer, 0, buffer.length);
    }
}
