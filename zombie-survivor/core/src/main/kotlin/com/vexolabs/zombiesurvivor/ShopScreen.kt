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

class ShopScreen(private val game: ZombieSurvivorGame) : ScreenAdapter() {

    private lateinit var batch: SpriteBatch
    private lateinit var shapes: ShapeRenderer
    private lateinit var cam: OrthographicCamera
    private lateinit var font: BitmapFont
    private lateinit var midFont: BitmapFont
    private lateinit var bigFont: BitmapFont
    private val layout = GlyphLayout()
    private val weaponTex = EnumMap<Weapon, Texture>(Weapon::class.java)

    private val upgradePrices = intArrayOf(50, 120, 260)
    private val speedPrices = intArrayOf(40, 100, 220)
    private val hpPrices = intArrayOf(60, 130, 280)
    private val shopWeapons = listOf(Weapon.SHOTGUN, Weapon.SNIPER, Weapon.FLAME, Weapon.RPG)
    private val weaponPrices = mapOf(Weapon.SHOTGUN to 80, Weapon.SNIPER to 120, Weapon.FLAME to 150, Weapon.RPG to 200)

    override fun show() {
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        cam = OrthographicCamera()
        font = BitmapFont().apply { data.setScale(1.4f) }
        midFont = BitmapFont().apply { data.setScale(1.8f) }
        bigFont = BitmapFont().apply { data.setScale(3f) }
        for (wp in shopWeapons) weaponTex[wp] = Assets.load(wp.icon)
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

        val rowH = 46f
        val rowGap = 8f
        val buyW = 110f
        val buyX = w - buyW - 24f
        val buttons = mutableListOf<Btn>()
        buttons.add(Btn(24f, 24f, 150f, 48f, "< BACK"))

        // 3 upgrade rows then 4 weapon rows, top-down
        val rows = 7
        val topY = h - 160f
        for (i in 0 until rows) {
            buttons.add(Btn(buyX, topY - i * (rowH + rowGap), buyW, rowH, "buy$i"))
        }

        if (Gdx.input.justTouched()) {
            val tx = Gdx.input.x.toFloat()
            val tyUp = h - Gdx.input.y.toFloat()
            if (Ui.hit(buttons[0], tx, tyUp)) { game.go(MenuScreen(game)); return }
            for (i in 0 until rows) {
                if (Ui.hit(buttons[i + 1], tx, tyUp)) buyRow(i)
            }
        }

        batch.begin()
        layout.setText(bigFont, "SHOP")
        bigFont.setColor(0.95f, 0.82f, 0.25f, 1f)
        bigFont.draw(batch, "SHOP", (w - layout.width) / 2, h - 60f)
        bigFont.setColor(Color.WHITE)
        layout.setText(midFont, "COINS: ${Save.coins}")
        midFont.draw(batch, "COINS: ${Save.coins}", (w - layout.width) / 2, h - 110f)

        val labels = arrayOf(
            "Damage +12%", "Move speed +10%", "Max HP +25",
            Weapon.SHOTGUN.label, Weapon.SNIPER.label, Weapon.FLAME.label, Weapon.RPG.label
        )
        for (i in 0 until rows) {
            val y = topY - i * (rowH + rowGap)
            if (i < 3) {
                font.draw(batch, labels[i], 30f, y + rowH / 2 + 12f)
                val lvl = when (i) { 0 -> Save.dmgLvl; 1 -> Save.spdLvl; else -> Save.hpLvl }
                var pips = ""
                repeat(3) { pips += if (it < lvl) "[#]" else "[ ]" }
                font.draw(batch, pips, 30f, y + rowH / 2 - 12f)
            } else {
                val wp = shopWeapons[i - 3]
                batch.draw(weaponTex.getValue(wp), 30f, y + 5f, 36f, 36f)
                font.draw(batch, wp.label + "  (start loadout)", 80f, y + rowH / 2 + 8f)
            }
        }
        batch.end()

        // price buttons: relabel per state
        for (i in 0 until rows) {
            val b = buttons[i + 1]
            val price: Int?
            val ownedOrMax: Boolean
            if (i < 3) {
                val lvl = when (i) { 0 -> Save.dmgLvl; 1 -> Save.spdLvl; else -> Save.hpLvl }
                val table = when (i) { 0 -> upgradePrices; 1 -> speedPrices; else -> hpPrices }
                price = if (lvl < 3) table[lvl] else null
                ownedOrMax = lvl >= 3
            } else {
                val wp = shopWeapons[i - 3]
                price = weaponPrices.getValue(wp)
                ownedOrMax = Save.weaponUnlocked(wp)
            }
            buttons[i + 1] = Btn(b.x, b.y, b.w, b.h, if (ownedOrMax) (if (i < 3) "MAX" else "OWNED") else "$price c")
        }
        Ui.draw(shapes, batch, font, layout, listOf(buttons[0]) + buttons.subList(1, buttons.size))
    }

    private fun buyRow(i: Int) {
        if (i < 3) {
            val lvl = when (i) { 0 -> Save.dmgLvl; 1 -> Save.spdLvl; else -> Save.hpLvl }
            if (lvl >= 3) return
            val price = when (i) { 0 -> upgradePrices; 1 -> speedPrices; else -> hpPrices }[lvl]
            if (Save.coins < price) return
            Save.coins = Save.coins - price
            when (i) { 0 -> Save.dmgLvl = lvl + 1; 1 -> Save.spdLvl = lvl + 1; else -> Save.hpLvl = lvl + 1 }
        } else {
            val wp = shopWeapons[i - 3]
            if (Save.weaponUnlocked(wp)) return
            val price = weaponPrices.getValue(wp)
            if (Save.coins < price) return
            Save.coins = Save.coins - price
            Save.unlockWeapon(wp)
        }
        Sfx.kill()
    }

    override fun dispose() {
        batch.dispose(); shapes.dispose(); font.dispose(); midFont.dispose(); bigFont.dispose()
        weaponTex.values.forEach { it.dispose() }
    }
}
