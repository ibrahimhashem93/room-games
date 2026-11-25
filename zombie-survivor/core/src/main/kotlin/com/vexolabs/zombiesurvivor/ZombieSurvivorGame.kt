package com.vexolabs.zombiesurvivor

import com.badlogic.gdx.Game
import com.badlogic.gdx.Screen

class ZombieSurvivorGame : Game() {
    override fun create() {
        Sfx.init()
        setScreen(MenuScreen(this))
    }

    fun go(s: Screen) {
        val old = screen
        setScreen(s)
        old?.dispose()
    }

    override fun dispose() {
        super.dispose()
        Sfx.dispose()
    }
}
