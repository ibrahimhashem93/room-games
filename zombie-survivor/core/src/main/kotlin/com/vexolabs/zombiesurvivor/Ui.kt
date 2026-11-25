package com.vexolabs.zombiesurvivor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

// ---- minimal touch UI: buttons drawn with shapes + text ----
class Btn(val x: Float, val y: Float, val w: Float, val h: Float, val label: String)

object Ui {
    fun hit(b: Btn, tx: Float, tyUp: Float): Boolean =
        tx >= b.x && tx <= b.x + b.w && tyUp >= b.y && tyUp <= b.y + b.h

    fun draw(shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, layout: GlyphLayout, buttons: List<Btn>) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (b in buttons) {
            shapes.color = Color(0.16f, 0.22f, 0.34f, 0.95f)
            shapes.rect(b.x, b.y, b.w, b.h)
        }
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(0.55f, 0.65f, 0.9f, 1f)
        for (b in buttons) shapes.rect(b.x, b.y, b.w, b.h)
        shapes.end()
        batch.begin()
        for (b in buttons) {
            layout.setText(font, b.label)
            font.draw(batch, b.label, b.x + (b.w - layout.width) / 2, b.y + (b.h + layout.height) / 2)
        }
        batch.end()
    }
}
