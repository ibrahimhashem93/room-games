package com.vexolabs.zombiesurvivor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import java.util.EnumMap

class MenuScreen(private val game: ZombieSurvivorGame) : ScreenAdapter() {

    private lateinit var batch: SpriteBatch
    private lateinit var shapes: ShapeRenderer
    private lateinit var cam: OrthographicCamera
    private lateinit var font: BitmapFont
    private lateinit var midFont: BitmapFont
    private lateinit var bigFont: BitmapFont
    private val layout = GlyphLayout()
    private val mapTexs = EnumMap<MapDef, Texture>(MapDef::class.java)
    private var mapIdx = 0

    override fun show() {
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        cam = OrthographicCamera()
        font = BitmapFont().apply { data.setScale(1.5f) }
        midFont = BitmapFont().apply { data.setScale(2.0f) }
        bigFont = BitmapFont().apply { data.setScale(4.2f) }
        for (m in MapDef.values()) mapTexs[m] = Assets.load(m.textureFile)
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

        val map = MapDef.values()[mapIdx]
        val pw = minOf(w * 0.55f, 320f, h * 0.34f / 0.66f)
        val ph = pw * 0.66f
        val px = w / 2 - pw / 2
        val labelY = h * 0.80f
        val py = labelY - 12f - ph
        val btnH = 54f
        val gap = 12f
        val playY = py - 18f - btnH
        val shopY = playY - gap - btnH
        val setY = shopY - gap - btnH

        val btnW = minOf(320f, w * 0.72f)
        val bx = w / 2 - btnW / 2
        val buttons = listOf(
            Btn(bx, playY, btnW, btnH, "PLAY"),
            Btn(bx, shopY, btnW, btnH, "SHOP"),
            Btn(bx, setY, btnW, btnH, "SETTINGS"),
            Btn(px - 60f, py + ph / 2 - 24f, 48f, 48f, "<"),
            Btn(px + pw + 12f, py + ph / 2 - 24f, 48f, 48f, ">")
        )

        if (Gdx.input.justTouched()) {
            val tx = Gdx.input.x.toFloat()
            val tyUp = h - Gdx.input.y.toFloat()
            when {
                Ui.hit(buttons[0], tx, tyUp) -> { game.go(GameScreen(game, map)); return }
                Ui.hit(buttons[1], tx, tyUp) -> { game.go(ShopScreen(game)); return }
                Ui.hit(buttons[2], tx, tyUp) -> { game.go(SettingsScreen(game)); return }
                Ui.hit(buttons[3], tx, tyUp) -> mapIdx = (mapIdx + MapDef.values().size - 1) % MapDef.values().size
                Ui.hit(buttons[4], tx, tyUp) -> mapIdx = (mapIdx + 1) % MapDef.values().size
            }
        }

        batch.begin()
        val titleY = h * 0.92f
        layout.setText(bigFont, "ZOMBIE SURVIVOR")
        bigFont.setColor(0.95f, 0.82f, 0.25f, 1f)
        bigFont.draw(batch, "ZOMBIE SURVIVOR", (w - layout.width) / 2, titleY)
        bigFont.setColor(Color.WHITE)
        layout.setText(font, "Vexolabs")
        font.draw(batch, "Vexolabs", (w - layout.width) / 2, titleY - 40f)

        // selected map preview + name
        layout.setText(midFont, map.label)
        midFont.draw(batch, map.label, (w - layout.width) / 2, labelY)
        batch.draw(mapTexs.getValue(map), px, py, pw, ph)

        val stats = "COINS: ${Save.coins}    BEST WAVE: ${Save.bestWave}"
        font.draw(batch, stats, 16f, h - 14f)
        batch.end()

        Ui.draw(shapes, batch, font, layout, buttons)
    }

    override fun dispose() {
        batch.dispose(); shapes.dispose(); font.dispose(); midFont.dispose(); bigFont.dispose()
        mapTexs.values.forEach { it.dispose() }
    }
}
