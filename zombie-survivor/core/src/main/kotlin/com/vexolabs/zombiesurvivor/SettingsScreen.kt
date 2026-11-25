package com.vexolabs.zombiesurvivor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class SettingsScreen(private val game: ZombieSurvivorGame) : ScreenAdapter() {

    private lateinit var batch: SpriteBatch
    private lateinit var shapes: ShapeRenderer
    private lateinit var cam: OrthographicCamera
    private lateinit var font: BitmapFont
    private lateinit var midFont: BitmapFont
    private lateinit var bigFont: BitmapFont
    private val layout = GlyphLayout()

    private val controlLabels = arrayOf("FLOATING STICK", "FIXED STICK", "FOLLOW TOUCH")

    override fun show() {
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        cam = OrthographicCamera()
        font = BitmapFont().apply { data.setScale(1.5f) }
        midFont = BitmapFont().apply { data.setScale(1.9f) }
        bigFont = BitmapFont().apply { data.setScale(3f) }
    }

    override fun render(delta: Float) {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        cam.setToOrtho(false, w, h)
        cam.update()
        shapes.projectionMatrix = cam.combined
        batch.projectionMatrix = cam.combined

        Gdx.gl.glClearColor(0.05f, 0.06f, 0.10f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val rowH = 60f
        val topY = h - 220f
        val minusX = w / 2 + 30f
        val plusX = w - 100f
        val buttons = mutableListOf<Btn>()
        buttons.add(Btn(24f, 24f, 150f, 52f, "< BACK"))
        for (i in 0..2) {
            buttons.add(Btn(minusX, topY - i * (rowH + 18f), 56f, rowH, "-$i"))
            buttons.add(Btn(plusX, topY - i * (rowH + 18f), 56f, rowH, "+$i"))
        }

        if (Gdx.input.justTouched()) {
            val tx = Gdx.input.x.toFloat()
            val tyUp = h - Gdx.input.y.toFloat()
            if (Ui.hit(buttons[0], tx, tyUp)) { game.go(MenuScreen(game)); return }
            for (i in 0..2) {
                if (Ui.hit(buttons[1 + i * 2], tx, tyUp)) adjust(i, -1)
                if (Ui.hit(buttons[2 + i * 2], tx, tyUp)) adjust(i, 1)
            }
        }

        batch.begin()
        layout.setText(bigFont, "SETTINGS")
        bigFont.setColor(0.95f, 0.82f, 0.25f, 1f)
        bigFont.draw(batch, "SETTINGS", (w - layout.width) / 2, h - 70f)
        bigFont.setColor(Color.WHITE)

        val values = arrayOf(
            "MUSIC", "SFX", "CONTROLS"
        )
        val shown = arrayOf(
            "${Save.musicVol}/10", "${Save.sfxVol}/10", controlLabels[Save.control]
        )
        for (i in 0..2) {
            val y = topY - i * (rowH + 18f)
            midFont.draw(batch, values[i], 30f, y + rowH / 2 + 10f)
            layout.setText(font, shown[i])
            font.draw(batch, shown[i], plusX - 20f - layout.width, y + rowH / 2 + 8f)
        }
        batch.end()

        val btns = buttons.mapIndexed { idx, b ->
            if (idx == 0) b else Btn(b.x, b.y, b.w, b.h, if (idx % 2 == 1) "-" else "+")
        }
        Ui.draw(shapes, batch, midFont, layout, btns)
    }

    private fun adjust(row: Int, dir: Int) {
        when (row) {
            0 -> { Save.musicVol = (Save.musicVol + dir).coerceIn(0, 10); Sfx.musicVol = Save.musicVol / 10f * 0.55f }
            1 -> { Save.sfxVol = (Save.sfxVol + dir).coerceIn(0, 10); Sfx.sfxVol = Save.sfxVol / 10f; Sfx.kill() }
            2 -> Save.control = (Save.control + dir + 3) % 3
        }
    }

    override fun dispose() {
        batch.dispose(); shapes.dispose(); font.dispose(); midFont.dispose(); bigFont.dispose()
    }
}
