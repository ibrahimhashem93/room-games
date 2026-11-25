package com.vexolabs.zombiesurvivor

import com.badlogic.gdx.Game

class ZombieSurvivorGame : Game() {
    override fun create() {
        setScreen(GameScreen())
    }
}
