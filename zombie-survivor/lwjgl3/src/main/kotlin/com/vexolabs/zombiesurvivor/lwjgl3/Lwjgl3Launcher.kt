package com.vexolabs.zombiesurvivor.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.vexolabs.zombiesurvivor.ZombieSurvivorGame

fun main() {
    val cfg = Lwjgl3ApplicationConfiguration()
    cfg.setTitle("Zombie Survivor")
    cfg.setWindowedMode(960, 540)
    cfg.setWindowSizeLimits(480, 270, -1, -1)
    Lwjgl3Application(ZombieSurvivorGame(), cfg)
}
