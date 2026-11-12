package com.karaokedev.app.audio

class EffectsChain(private val sampleRate: Int) {
    var pitchRatio = 1.0f
        private set

    private val gate = NoiseGate()
    private val highpass = Biquad(sampleRate)
    private val presence = Biquad(sampleRate)
    private val compressor = Compressor()
    private val reverb = Reverb(sampleRate)
    private val echo = Echo(sampleRate)
    private var echoEnabled = false

    fun configure(preset: Preset) {
        pitchRatio = preset.pitchRatio
        gate.threshold = preset.gateThreshold
        highpass.highpass(preset.highpassHz, 0.7)
        presence.peaking(3000.0, 1.0, preset.presenceGainDb)
        compressor.threshold = preset.compressThreshold
        reverb.decay = preset.reverbDecay
        reverb.mix = preset.reverbMix
        echoEnabled = preset.echoTimeMs > 0
        if (echoEnabled) echo.setDelayMs(preset.echoTimeMs, preset.echoFeedback)
    }

    fun process(block: FloatArray, count: Int) {
        gate.process(block, count)
        highpass.process(block, count)
        presence.process(block, count)
        compressor.process(block, count)
        if (pitchRatio != 1f) pitchShift(block, count, pitchRatio)
        reverb.process(block, count)
        if (echoEnabled) echo.process(block, count)
    }

    private fun pitchShift(block: FloatArray, count: Int, ratio: Float) {
        val out = FloatArray(count)
        var pos = 0f
        for (i in 0 until count) {
            val i0 = pos.toInt()
            val frac = pos - i0
            val a = if (i0 < count) block[i0] else 0f
            val b = if (i0 + 1 < count) block[i0 + 1] else 0f
            out[i] = a + (b - a) * frac
            pos += ratio
        }
        System.arraycopy(out, 0, block, 0, count)
    }
}
