package com.karaokedev.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KaraokeEngine(context: Context, private val preset: Preset) {
    private val appContext = context.applicationContext
    private val sampleRate = 44100
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile private var running = false
    private val captured = ArrayList<FloatArray>()

    fun start(): Boolean {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return false
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        val rec = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuf * 4)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuf * 4)
            .build()
        if (rec.state != AudioRecord.STATE_INITIALIZED || track.state != AudioTrack.STATE_INITIALIZED) {
            rec.release()
            track.release()
            return false
        }
        audioRecord = rec
        audioTrack = track
        synchronized(captured) { captured.clear() }
        running = true
        thread = Thread { loop(rec, track) }.apply { start() }
        return true
    }

    private fun loop(rec: AudioRecord, track: AudioTrack) {
        val chain = EffectsChain(sampleRate).apply { configure(preset) }
        val shortBuf = ShortArray(1024)
        val floatBuf = FloatArray(1024)
        rec.startRecording()
        track.play()
        while (running) {
            val n = rec.read(shortBuf, 0, shortBuf.size)
            if (n <= 0) continue
            for (i in 0 until n) floatBuf[i] = shortBuf[i] / 32768f
            chain.process(floatBuf, n)
            synchronized(captured) { captured.add(floatBuf.copyOf(n)) }
            for (i in 0 until n) {
                shortBuf[i] = (floatBuf[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            }
            track.write(shortBuf, 0, n)
        }
        rec.stop()
        track.stop()
    }

    fun stop(): File? {
        running = false
        thread?.join(2000)
        thread = null
        audioRecord?.release()
        audioRecord = null
        audioTrack?.release()
        audioTrack = null
        return writeWav()
    }

    private fun writeWav(): File? {
        val blocks = synchronized(captured) {
            val copy = ArrayList(captured)
            captured.clear()
            copy
        }
        if (blocks.isEmpty()) return null
        val totalSamples = blocks.sumOf { it.size }
        val dir = File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "gravacoes")
            .apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "karaoke_$stamp.wav")
        FileOutputStream(file).use { out ->
            val dataSize = totalSamples * 2
            out.write("RIFF".toByteArray())
            writeLe32(out, 36 + dataSize)
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            writeLe32(out, 16)
            writeLe16(out, 1)
            writeLe16(out, 1)
            writeLe32(out, sampleRate)
            writeLe32(out, sampleRate * 2)
            writeLe16(out, 2)
            writeLe16(out, 16)
            out.write("data".toByteArray())
            writeLe32(out, dataSize)
            for (block in blocks) {
                for (sample in block) {
                    val v = (sample.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                    out.write(v.toInt() and 0xff)
                    out.write((v.toInt() shr 8) and 0xff)
                }
            }
        }
        return file
    }

    private fun writeLe16(out: FileOutputStream, value: Int) {
        out.write(value and 0xff)
        out.write((value shr 8) and 0xff)
    }

    private fun writeLe32(out: FileOutputStream, value: Int) {
        out.write(value and 0xff)
        out.write((value shr 8) and 0xff)
        out.write((value shr 16) and 0xff)
        out.write((value shr 24) and 0xff)
    }
}
