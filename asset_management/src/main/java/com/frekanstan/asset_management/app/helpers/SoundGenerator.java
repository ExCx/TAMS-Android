package com.frekanstan.asset_management.app.helpers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;

import com.frekanstan.asset_management.R;

import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;

public class SoundGenerator {
    private static final int SAMPLE_RATE_PER_MS = 8;
    @Getter
    private boolean isPlaying = false;
    private boolean isBusy = false;
    @Getter @Setter
    private int hotness;

    private SoundPool pool;
    private int sampleId, streamId;

    public SoundGenerator(Context context) {
        val audioAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder()
                .setAudioAttributes(audioAttrs)
                .setMaxStreams(1)
                .build();
        pool.load(context, R.raw.radar_beep, 1);
        pool.setOnLoadCompleteListener((soundPool, sampleId, status) ->
                this.sampleId = sampleId);
    }

    public void success() {
        if (!isBusy) {
            isBusy = true;
            playTone(100, 880);
            new Handler(Looper.getMainLooper()).postDelayed(() -> isBusy = false, 300);
        }
    }

    public void fail() {
        playTone(300, 300);
    }

    public void playLoop() {
        streamId = pool.play(sampleId, 1, 1, 1, -1, 0.5F);
        //isPlaying = true;
        //loopTone();
    }

    public void stopLoop() {
        pool.stop(streamId);
        //isPlaying = false;
    }

    public void setRate(int percentage) {
        val rate = 0.5F + (0.015F * percentage);
        pool.setRate(streamId, rate);
    }

    private AudioTrack audioTrack;
    public void playTone(int durationMs, double frequency) {
        byte[] generatedSound = generateTone(durationMs, frequency);
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE_PER_MS * 1000,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSound.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSound, 0, generatedSound.length);
        audioTrack.play();
    }

    private byte[] generateTone(int durationMs, double frequency) {
        int numSamples = durationMs * SAMPLE_RATE_PER_MS;
        var sample = new double[numSamples];
        for (int i = 0; i < numSamples; ++i)
            sample[i] = Math.sin(2 * Math.PI * i / (SAMPLE_RATE_PER_MS * 1000 / frequency));

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        val generatedSnd = new byte[2 * numSamples];
        int idx = 0;
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767)); // scale to maximum amplitude
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        return generatedSnd;
    }
}