/*
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hmdm.launcher.util;

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
    // Lower octave
    private static final double C3 = 130.81;
    private static final double D3 = 146.83;
    private static final double E3 = 164.81;
    private static final double F3 = 174.61;
    private static final double G3 = 196.00;
    private static final double A3 = 220.00;
    private static final double B3 = 246.94;

    // Middle octave
    private static final double C4 = 261.63;
    private static final double D4 = 293.66;
    private static final double E4 = 329.63;
    private static final double F4 = 349.23;
    private static final double G4 = 392.00;
    private static final double A4 = 440.00;
    private static final double B4 = 493.88;

    // Higher octave
    private static final double C5 = 523.25;
    private static final double D5 = 587.33;
    private static final double E5 = 659.25;
    private static final double F5 = 698.46;
    private static final double G5 = 783.99;
    private static final double A5 = 880.00;
    private static final double B5 = 987.77;

    // Even higher octave
    private static final double C6 = 1046.50;
    private static final double D6 = 1174.66;
    private static final double E6 = 1318.51;
    private static final double F6 = 1396.91;
    private static final double G6 = 1567.98;
    private static final double A6 = 1760.00;
    private static final double B6 = 1975.53;

    // --- Main melody ---
private static final double[][] MELODY = { {FS4, 400}, {A4, 400}, {A4, 400}, {A4, 400}, {GS4, 400}, {FS4, 400}, {E4, 400}, {D4, 600}, {D4, 400}, {E4, 400}, {FS4, 400}, {FS4, 400}, {FS4, 600}, {E4, 400}, {D4, 400}, {B3, 400}, {A3, 600}, {A3, 400}, {B3, 400}, {D4, 400}, {D4, 400}, {D4, 400}, {CS4, 600}, {B3, 400}, {A3, 400}, {FS3, 400}, {GS3, 400}, {A3, 400}, {A3, 800} };

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
