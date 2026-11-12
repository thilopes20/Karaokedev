package com.karaokedev.app.audio

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class Biquad(sampleRate: Int) {
    private val fs = sampleRate.toDouble()
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun highpass(freq: Double, q: Double) {
        val w0 = 2.0 * Math.PI * freq / fs
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)
        setCoefficients(
            b0 = (1.0 + cosW0) / 2.0,
            b1 = -(1.0 + cosW0),
            b2 = (1.0 + cosW0) / 2.0,
            a0 = 1.0 + alpha,
            a1 = -2.0 * cosW0,
            a2 = 1.0 - alpha
        )
    }

    fun peaking(freq: Double, q: Double, gainDb: Double) {
        val a = pow(10.0, gainDb / 40.0)
        val w0 = 2.0 * Math.PI * freq / fs
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)
        setCoefficients(
            b0 = 1.0 + alpha * a,
            b1 = -2.0 * cosW0,
            b2 = 1.0 - alpha * a,
            a0 = 1.0 + alpha / a,
            a1 = -2.0 * cosW0,
            a2 = 1.0 - alpha / a
        )
    }

    private fun setCoefficients(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double) {
        this.b0 = b0 / a0
        this.b1 = b1 / a0
        this.b2 = b2 / a0
        this.a1 = a1 / a0
        this.a2 = a2 / a0
    }

    fun process(block: FloatArray, count: Int) {
        for (i in 0 until count) {
            val x = block[i].toDouble()
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            block[i] = y.toFloat()
        }
    }
}

class NoiseGate {
    var threshold = 0.01f
    private var gain = 0f

    fun process(block: FloatArray, count: Int) {
        for (i in 0 until count) {
            val target = if (abs(block[i]) > threshold) 1f else 0f
            gain += (target - gain) * 0.002f
            block[i] *= gain
        }
    }
}

class Compressor {
    var threshold = 0.3f
    var ratio = 3f
    private var envelope = 0f

    fun process(block: FloatArray, count: Int) {
        for (i in 0 until count) {
            val x = abs(block[i])
            envelope = if (x > envelope) 0.02f * x + 0.98f * envelope else 0.0005f * x + 0.9995f * envelope
            var gain = 1f
            if (envelope > threshold) {
                gain = (threshold + (envelope - threshold) / ratio) / envelope
            }
            block[i] *= gain
        }
    }
}

class Echo(private val sampleRate: Int) {
    private var buffer = FloatArray(1)
    private var index = 0
    private var feedback = 0f
    private var mix = 0.35f

    fun setDelayMs(ms: Int, feedbackLevel: Float) {
        val size = (sampleRate * ms / 1000).coerceIn(1, sampleRate)
        buffer = FloatArray(size)
        index = 0
        feedback = feedbackLevel
    }

    fun process(block: FloatArray, count: Int) {
        for (i in 0 until count) {
            val delayed = buffer[index]
            buffer[index] = block[i] + delayed * feedback
            block[i] = block[i] * (1 - mix) + delayed * mix
            index = (index + 1) % buffer.size
        }
    }
}

class Reverb(private val sampleRate: Int) {
    var decay = 0.7f
    var mix = 0.25f
    private val scale = sampleRate / 44100.0
    private val combs = intArrayOf(1116, 1188, 1277, 1356).map { Comb((it * scale).toInt().coerceAtLeast(1)) }
    private val allpasses = intArrayOf(556, 441).map { Allpass((it * scale).toInt().coerceAtLeast(1)) }

    fun process(block: FloatArray, count: Int) {
        for (i in 0 until count) {
            val dry = block[i]
            var wet = 0f
            for (comb in combs) wet += comb.process(dry, decay)
            wet /= combs.size
            for (allpass in allpasses) wet = allpass.process(wet)
            block[i] = dry * (1 - mix) + wet * mix
        }
    }
}

private class Comb(size: Int) {
    private val buffer = FloatArray(size)
    private var index = 0
    private var filterStore = 0f

    fun process(input: Float, feedback: Float): Float {
        val output = buffer[index]
        filterStore = output * 0.5f + filterStore * 0.5f
        buffer[index] = input + filterStore * feedback
        index = (index + 1) % buffer.size
        return output
    }
}

private class Allpass(size: Int) {
    private val buffer = FloatArray(size)
    private var index = 0

    fun process(input: Float): Float {
        val bufout = buffer[index]
        val output = -input + bufout
        buffer[index] = input + bufout * 0.5f
        index = (index + 1) % buffer.size
        return output
    }
}
