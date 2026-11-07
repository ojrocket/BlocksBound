package com.blocksbound.engine.audio;

import java.util.EnumMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

public final class SoundEffects implements AutoCloseable {
    private static final int SAMPLE_RATE = 22_050;
    private static final int VOICE_COUNT = 2;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final Map<Sound, Clip[]> clips = new EnumMap<>(Sound.class);
    private final Map<Sound, Integer> nextVoice = new EnumMap<>(Sound.class);

    public SoundEffects() throws LineUnavailableException {
        try {
            for (Sound sound : Sound.values()) {
                Clip[] voices = new Clip[VOICE_COUNT];
                clips.put(sound, voices);
                byte[] samples = synthesize(sound);
                for (int voice = 0; voice < voices.length; voice++) {
                    Clip clip = AudioSystem.getClip();
                    voices[voice] = clip;
                    clip.open(FORMAT, samples, 0, samples.length);
                }
            }
        } catch (LineUnavailableException | RuntimeException exception) {
            close();
            throw exception;
        }
    }

    public void play(Sound sound) {
        Clip[] voices = clips.get(sound);
        if (voices == null) {
            throw new IllegalStateException("Sound effects are closed");
        }

        int start = nextVoice.getOrDefault(sound, 0);
        int voice = start;
        for (int offset = 0; offset < voices.length; offset++) {
            int candidate = (start + offset) % voices.length;
            if (!voices[candidate].isRunning()) {
                voice = candidate;
                break;
            }
        }

        Clip clip = voices[voice];
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
        nextVoice.put(sound, (voice + 1) % voices.length);
    }

    @Override
    public void close() {
        for (Clip[] voices : clips.values()) {
            for (Clip clip : voices) {
                if (clip != null) {
                    clip.close();
                }
            }
        }
        clips.clear();
        nextVoice.clear();
    }

    private static byte[] synthesize(Sound sound) {
        int sampleCount = (int) (SAMPLE_RATE * sound.durationSeconds);
        byte[] samples = new byte[sampleCount * 2];
        double phase = 0.0;
        long noiseState = 0x4D595DF4D0F33173L;

        for (int index = 0; index < sampleCount; index++) {
            double progress = (double) index / sampleCount;
            double frequency = sound.startFrequency
                    + (sound.endFrequency - sound.startFrequency) * progress;
            phase += 2.0 * Math.PI * frequency / SAMPLE_RATE;

            noiseState ^= noiseState << 13;
            noiseState ^= noiseState >>> 7;
            noiseState ^= noiseState << 17;
            double noise = (noiseState & 0xFFFF) / 32767.5 - 1.0;

            double attack = Math.min(1.0, progress / 0.04);
            double envelope = attack * Math.pow(1.0 - progress, sound.decay);
            double value = (Math.sin(phase) * (1.0 - sound.noiseMix)
                    + noise * sound.noiseMix) * envelope * sound.volume;
            short sample = (short) (Math.max(-1.0, Math.min(1.0, value)) * Short.MAX_VALUE);
            samples[index * 2] = (byte) sample;
            samples[index * 2 + 1] = (byte) (sample >>> 8);
        }
        return samples;
    }

    public enum Sound {
        BLOCK_BREAK(0.14, 190.0, 72.0, 0.72, 0.65, 5.0),
        BLOCK_PLACE(0.11, 150.0, 82.0, 0.35, 0.55, 4.0),
        BLOCK_PICKUP(0.18, 660.0, 990.0, 0.0, 0.35, 2.2),
        JUMP(0.17, 230.0, 490.0, 0.02, 0.32, 2.0);

        private final double durationSeconds;
        private final double startFrequency;
        private final double endFrequency;
        private final double noiseMix;
        private final double volume;
        private final double decay;

        Sound(double durationSeconds, double startFrequency, double endFrequency,
                double noiseMix, double volume, double decay) {
            this.durationSeconds = durationSeconds;
            this.startFrequency = startFrequency;
            this.endFrequency = endFrequency;
            this.noiseMix = noiseMix;
            this.volume = volume;
            this.decay = decay;
        }
    }
}
