package com.vexolabs.zombiesurvivor

import com.badlogic.gdx.graphics.Color

// ---- weapons (top-level so Save and Shop can reference them) ----
enum class Weapon(
    val label: String, val rate: Float, val dmg: Int, val speed: Float,
    val life: Float, val spread: Float, val pellets: Int,
    val pierce: Boolean, val aoe: Float, val flame: Boolean, val startAmmo: Int,
    val icon: String, val tint: Color
) {
    PISTOL("Pistol", .33f, 34, 700f, 1.1f, .01f, 1, false, 0f, false, -1, "w-pistol.png", Color(1f, .72f, .15f, 1f)),
    RIFLE("Machine Gun", .20f, 40, 620f, 1.1f, .045f, 1, false, 0f, false, 150, "w-rifle.png", Color(1f, .72f, .15f, 1f)),
    SHOTGUN("Shotgun", .70f, 16, 560f, .5f, .16f, 6, false, 0f, false, 30, "w-shotgun.png", Color(1f, .55f, .2f, 1f)),
    SNIPER("Sniper", 1.1f, 150, 900f, 1.4f, .005f, 1, true, 0f, false, 15, "w-sniper.png", Color(.5f, .9f, 1f, 1f)),
    FLAME("Flamethrower", .05f, 6, 240f, .35f, .22f, 1, false, 0f, true, 200, "w-flame.png", Color(1f, .45f, .1f, 1f)),
    RPG("RPG Launcher", 1.4f, 90, 380f, 1.6f, .01f, 1, false, 110f, false, 6, "w-rpg.png", Color(.7f, 1f, .5f, 1f))
}
