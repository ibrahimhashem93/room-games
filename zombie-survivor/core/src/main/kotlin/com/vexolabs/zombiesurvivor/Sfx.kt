package com.vexolabs.zombiesurvivor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.AudioDevice
import com.badlogic.gdx.audio.Sound
import java.io.ByteArrayOutputStream

// ---- procedural audio: generated WAV SFX + a looping chiptune bass line ----
// Everything is defensive: any audio failure just leaves the game silent.
object Sfx {
    private var shot: Sound? = null
    private var squish: Sound? = null
    private var boom: Sound? = null
    @Volatile var musicVol = 0.5f
    @Volatile var sfxVol = 0.8f
    private var running = false
    private var musicThread: Thread? = null
    private const val SR = 22050

    fun init() {
        musicVol = Save.musicVol / 10f * 0.55f
        sfxVol = Save.sfxVol / 10f
        try {
            if (shot == null) {
                shot = loadWav("zs-shot.wav", synth(0.12f) { t ->
                    ((Math.random().toFloat() * 2 - 1) * 0.55f + sin2(170f, t) * 0.45f) * expd(t, 26f)
                })
                squish = loadWav("zs-squish.wav", synth(0.18f) { t ->
                    ((Math.random().toFloat() * 2 - 1) * 0.35f + sin2(85f, t) * 0.55f) * expd(t, 11f)
                })
                boom = loadWav("zs-boom.wav", synth(0.5f) { t ->
                    ((Math.random().toFloat() * 2 - 1) * 0.7f + sin2(55f, t) * 0.5f) * expd(t, 6f)
                })
            }
            startMusic()
        } catch (t: Throwable) { /* silent game */ }
    }

    fun shoot(w: Weapon) {
        val pitch = when (w) {
            Weapon.PISTOL -> 1.2f; Weapon.RIFLE -> 1.0f; Weapon.SHOTGUN -> 0.75f
            Weapon.SNIPER -> 0.6f; Weapon.FLAME -> 1.45f; Weapon.RPG -> 0.5f
        }
        try { shot?.play(sfxVol * 0.25f, pitch, 0f) } catch (t: Throwable) {}
    }

    fun kill() { try { squish?.play(sfxVol * 0.5f, 0.9f + Math.random().toFloat() * 0.3f, 0f) } catch (t: Throwable) {} }
    fun boom() { try { boom?.play(sfxVol * 0.6f, 1f, 0f) } catch (t: Throwable) {} }

    private fun sin2(freq: Float, t: Float) = Math.sin(2.0 * Math.PI * freq * t).toFloat()
    private fun expd(t: Float, k: Float) = Math.exp((-k * t).toDouble()).toFloat()

    private fun synth(dur: Float, f: (Float) -> Float): FloatArray {
        val n = (SR * dur).toInt()
        val out = FloatArray(n)
        for (i in 0 until n) out[i] = f(i.toFloat() / SR)
        return out
    }

    private fun loadWav(name: String, samples: FloatArray): Sound {
        val fh = Gdx.files.local(name)
        if (!fh.exists()) fh.writeBytes(wavBytes(samples), false)
        return Gdx.audio.newSound(fh)
    }

    private fun wavBytes(samples: FloatArray): ByteArray {
        val bos = ByteArrayOutputStream()
        fun w32(v: Int) { bos.write(v and 0xFF); bos.write((v shr 8) and 0xFF); bos.write((v shr 16) and 0xFF); bos.write((v shr 24) and 0xFF) }
        fun w16(v: Int) { bos.write(v and 0xFF); bos.write((v shr 8) and 0xFF) }
        val dataLen = samples.size * 2
        bos.writeBytes("RIFF".toByteArray()); w32(36 + dataLen); bos.writeBytes("WAVE".toByteArray())
        bos.writeBytes("fmt ".toByteArray()); w32(16); w16(1); w16(1); w32(SR); w32(SR * 2); w16(2); w16(16)
        bos.writeBytes("data".toByteArray()); w32(dataLen)
        for (s in samples) {
            val v = (s.coerceIn(-1f, 1f) * 32767).toInt()
            w16(v and 0xFFFF)
        }
        return bos.toByteArray()
    }

    private fun startMusic() {
        if (musicThread != null) return
        running = true
        val t = Thread {
            var dev: AudioDevice? = null
            try {
                dev = Gdx.audio.newAudioDevice(SR, true)
                val notes = floatArrayOf(55f, 55f, 65.41f, 49f, 55f, 55f, 82.41f, 65.41f)
                val step = (SR * 0.22f).toInt()
                val buf = FloatArray(step)
                var pos = 0
                while (running) {
                    val stepIdx = pos / step
                    val n = notes[stepIdx % notes.size]
                    for (i in 0 until step) {
                        val tt = (pos + i).toFloat() / SR
                        val ph = 2.0 * Math.PI * n * tt
                        var s = (if (Math.sin(ph) > 0) 0.20f else -0.20f) + (Math.sin(ph) * 0.16f).toFloat()
                        if (stepIdx % 2 == 1 && (pos + i) % (step * 2) < SR * 0.015f) {
                            s += (Math.random().toFloat() * 2 - 1) * 0.05f
                        }
                        buf[i] = s * musicVol
                    }
                    pos += step
                    dev.writeSamples(buf, 0, buf.size)
                }
            } catch (t: Throwable) {
                running = false
            } finally {
                try { dev?.dispose() } catch (t: Throwable) {}
            }
        }
        t.isDaemon = true
        t.start()
        musicThread = t
    }

    fun dispose() {
        running = false
        try { shot?.dispose(); squish?.dispose(); boom?.dispose() } catch (t: Throwable) {}
        shot = null; squish = null; boom = null; musicThread = null
    }
}
