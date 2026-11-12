package com.karaokedev.app.audio

data class Preset(
    val name: String,
    val gateThreshold: Float,
    val highpassHz: Double,
    val presenceGainDb: Double,
    val compressThreshold: Float,
    val reverbDecay: Float,
    val reverbMix: Float,
    val echoTimeMs: Int,
    val echoFeedback: Float,
    val pitchRatio: Float
)

val PRESETS = listOf(
    Preset(
        name = "Studio",
        gateThreshold = 0.010f,
        highpassHz = 90.0,
        presenceGainDb = 3.0,
        compressThreshold = 0.25f,
        reverbDecay = 0.72f,
        reverbMix = 0.22f,
        echoTimeMs = 0,
        echoFeedback = 0.0f,
        pitchRatio = 1.00f
    ),
    Preset(
        name = "Echo",
        gateThreshold = 0.010f,
        highpassHz = 90.0,
        presenceGainDb = 2.0,
        compressThreshold = 0.30f,
        reverbDecay = 0.40f,
        reverbMix = 0.15f,
        echoTimeMs = 250,
        echoFeedback = 0.45f,
        pitchRatio = 1.00f
    ),
    Preset(
        name = "Radio",
        gateThreshold = 0.020f,
        highpassHz = 300.0,
        presenceGainDb = 6.0,
        compressThreshold = 0.35f,
        reverbDecay = 0.05f,
        reverbMix = 0.00f,
        echoTimeMs = 0,
        echoFeedback = 0.0f,
        pitchRatio = 1.00f
    ),
    Preset(
        name = "Chipmunk",
        gateThreshold = 0.010f,
        highpassHz = 90.0,
        presenceGainDb = 2.0,
        compressThreshold = 0.30f,
        reverbDecay = 0.30f,
        reverbMix = 0.15f,
        echoTimeMs = 0,
        echoFeedback = 0.0f,
        pitchRatio = 1.35f
    )
)
