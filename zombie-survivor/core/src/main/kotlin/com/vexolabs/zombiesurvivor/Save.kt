package com.vexolabs.zombiesurvivor

import com.badlogic.gdx.Gdx

// ---- persistent profile: coins, upgrades, weapon unlocks, settings ----
object Save {
    private val p get() = Gdx.app.getPreferences("zombie-survivor")

    var coins: Int
        get() = p.getInteger("coins", 0)
        set(v) { p.putInteger("coins", v).flush() }

    var bestWave: Int
        get() = p.getInteger("bestWave", 0)
        set(v) { p.putInteger("bestWave", v).flush() }

    var dmgLvl: Int
        get() = p.getInteger("dmgLvl", 0)
        set(v) { p.putInteger("dmgLvl", v.coerceIn(0, 3)).flush() }

    var spdLvl: Int
        get() = p.getInteger("spdLvl", 0)
        set(v) { p.putInteger("spdLvl", v.coerceIn(0, 3)).flush() }

    var hpLvl: Int
        get() = p.getInteger("hpLvl", 0)
        set(v) { p.putInteger("hpLvl", v.coerceIn(0, 3)).flush() }

    // volumes stored 0..10
    var musicVol: Int
        get() = p.getInteger("musicVol", 6)
        set(v) { p.putInteger("musicVol", v.coerceIn(0, 10)).flush() }

    var sfxVol: Int
        get() = p.getInteger("sfxVol", 8)
        set(v) { p.putInteger("sfxVol", v.coerceIn(0, 10)).flush() }

    // 0 = floating stick, 1 = fixed stick, 2 = follow touch
    var control: Int
        get() = p.getInteger("control", 0)
        set(v) { p.putInteger("control", v.coerceIn(0, 2)).flush() }

    fun weaponUnlocked(w: Weapon): Boolean =
        w == Weapon.PISTOL || w == Weapon.RIFLE || p.getBoolean("w_" + w.name, false)

    fun unlockWeapon(w: Weapon) { p.putBoolean("w_" + w.name, true).flush() }

    val dmgMul: Float get() = 1f + 0.12f * dmgLvl
    val speedMul: Float get() = 1f + 0.10f * spdLvl
    val maxHp: Float get() = 100f + 25f * hpLvl
}
