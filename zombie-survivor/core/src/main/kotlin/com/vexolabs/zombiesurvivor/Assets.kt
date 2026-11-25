package com.vexolabs.zombiesurvivor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture

/** Game textures: real sprites loaded from assets/, plus a tiny code-drawn bullet tracer. */
object Assets {

    fun load(name: String): Texture {
        val t = Texture(Gdx.files.internal(name))
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        return t
    }

    /** Small bright tracer bullet, drawn in code (looks like the concept's tracer lines). */
    fun bullet(): Texture {
        val p = Pixmap(16, 6, Pixmap.Format.RGBA8888)
        p.setColor(1f, 0.72f, 0.15f, 1f); p.fillRectangle(0, 1, 13, 4)
        p.setColor(1f, 0.95f, 0.70f, 1f); p.fillRectangle(2, 2, 12, 2)
        p.setColor(1f, 1f, 1f, 1f); p.fillRectangle(13, 1, 3, 4)
        val t = Texture(p)
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        p.dispose()
        return t
    }

    val WRECK_TINTS = arrayOf(
        Color(1.00f, 0.62f, 0.55f, 1f),  // rusty red
        Color(0.62f, 0.76f, 1.00f, 1f),  // dusty blue
        Color(0.85f, 0.87f, 0.90f, 1f)   // gray
    )
}
