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
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array

class GameScreen : ScreenAdapter() {

    private val MAP = 1600f
    private val HERO_SPEED = 170f
    private val BULLET_SPEED = 620f
    private val FIRE_RATE = 0.20f
    private val BULLET_DMG = 40
    private val AIM_RANGE = 430f
    private val ZOMBIE_DPS = 14f

    private val HERO_SIZE = 88f
    private val ZOMBIE_SIZE = 80f
    private val WRECK_W = 118f
    private val WRECK_H = 65f
    private val MUZZLE_DIST = 44f

    private lateinit var batch: SpriteBatch
    private lateinit var shapes: ShapeRenderer
    private lateinit var cam: OrthographicCamera
    private lateinit var hudCam: OrthographicCamera
    private lateinit var font: BitmapFont
    private lateinit var bigFont: BitmapFont
    private val layout = GlyphLayout()

    private lateinit var heroTex: Texture
    private lateinit var heroIdleTex: Texture
    private lateinit var heroFireTex: Texture
    private lateinit var zombieTex: Texture
    private lateinit var zombieAttackTex: Texture
    private lateinit var zombieDeathTex: Texture
    private lateinit var wreckTex: Texture
    private lateinit var flashTex: Texture
    private lateinit var splatTex: Texture
    private lateinit var mapTex: Texture
    private lateinit var portraitTex: Texture
    private lateinit var bulletTex: Texture

    private data class Zombie(var x: Float, var y: Float, var hp: Int, val speed: Float, val phase: Float, var contact: Boolean = false)
    private data class Bullet(var x: Float, var y: Float, val vx: Float, val vy: Float, var life: Float)
    private data class Splat(val x: Float, val y: Float, val rot: Float, var t: Float, val fr: Int)
    private data class Wreck(val x: Float, val y: Float, val rot: Float, val tint: Int)
    private data class Dying(val x: Float, val y: Float, val ang: Float, var t: Float)

    private val hero = Vector2(MAP / 2, MAP / 2)
    private var heroAngle = 0f
    private var hp = 100f
    private val zombies = Array<Zombie>()
    private val bullets = Array<Bullet>()
    private val splats = Array<Splat>()
    private val wrecks = Array<Wreck>()
    private val dying = Array<Dying>()

    private var wave = 0
    private var kills = 0
    private var waveDelay = 2f
    private var bannerT = 0f
    private var fireTimer = 0f
    private var flashT = 0f
    private var time = 0f
    private var gameOver = false

    // floating left joystick (screen space)
    private var movePointer = -1
    private val stickOrigin = Vector2()
    private val stickDelta = Vector2()
    private val STICK_R = 70f

    override fun show() {
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        cam = OrthographicCamera()
        hudCam = OrthographicCamera()
        font = BitmapFont().apply { data.setScale(1.4f) }
        bigFont = BitmapFont().apply { data.setScale(3f) }
        heroTex = Assets.load("hero.png")
        heroIdleTex = Assets.load("hero-idle.png")
        heroFireTex = Assets.load("hero-fire.png")
        zombieTex = Assets.load("zombie.png")
        zombieAttackTex = Assets.load("zombie-attack.png")
        zombieDeathTex = Assets.load("zombie-death.png")
        wreckTex = Assets.load("wreck.png")
        flashTex = Assets.load("flash.png")
        splatTex = Assets.load("splat.png")
        mapTex = Assets.load("map.jpg")
        portraitTex = Assets.load("portrait.png")
        bulletTex = Assets.bullet()
        placeWrecks()
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    private fun placeWrecks() {
        wrecks.clear()
        wrecks.add(Wreck(420f, 700f, 15f, 0))
        wrecks.add(Wreck(980f, 620f, -8f, 1))
        wrecks.add(Wreck(1220f, 980f, 22f, 2))
        wrecks.add(Wreck(560f, 990f, -18f, 1))
        wrecks.add(Wreck(1350f, 700f, 5f, 0))
    }

    private fun spawnWave() {
        wave++
        bannerT = 2.2f
        val count = 3 + wave * 2
        val zombieHp = 80 + wave * 20
        val speed = (52f + wave * 3f).coerceAtMost(95f)
        repeat(count) {
            val a = MathUtils.random(MathUtils.PI2)
            val d = MathUtils.random(480f, 680f)
            val zx = (hero.x + MathUtils.cos(a) * d).coerceIn(50f, MAP - 50)
            val zy = (hero.y + MathUtils.sin(a) * d).coerceIn(50f, MAP - 50)
            zombies.add(Zombie(zx, zy, zombieHp, speed, MathUtils.random(MathUtils.PI2)))
        }
    }

    private fun reset() {
        hero.set(MAP / 2, MAP / 2)
        hp = 100f
        zombies.clear(); bullets.clear(); splats.clear(); dying.clear()
        wave = 0; kills = 0; waveDelay = 2f; bannerT = 0f
        gameOver = false
        movePointer = -1
    }

    private fun pollJoystick() {
        if (movePointer != -1 && !Gdx.input.isTouched(movePointer)) {
            movePointer = -1
            stickDelta.setZero()
        }
        if (movePointer == -1) {
            for (i in 0..4) {
                if (Gdx.input.isTouched(i) && Gdx.input.getX(i) < Gdx.graphics.width / 2) {
                    movePointer = i
                    stickOrigin.set(Gdx.input.getX(i).toFloat(), Gdx.input.getY(i).toFloat())
                    stickDelta.setZero()
                    break
                }
            }
        }
        if (movePointer != -1) {
            stickDelta.set(
                Gdx.input.getX(movePointer) - stickOrigin.x,
                Gdx.input.getY(movePointer) - stickOrigin.y
            )
            if (stickDelta.len() > STICK_R) stickDelta.setLength(STICK_R)
        }
    }

    override fun render(delta: Float) {
        val dt = delta.coerceAtMost(0.05f)
        time += dt
        pollJoystick()

        if (gameOver) {
            if (Gdx.input.justTouched()) reset()
        } else {
            update(dt)
        }

        Gdx.gl.glClearColor(0.05f, 0.06f, 0.10f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cam.position.set(
            hero.x.coerceIn(cam.viewportWidth / 2 - 100, MAP - cam.viewportWidth / 2 + 100),
            hero.y.coerceIn(cam.viewportHeight / 2 - 100, MAP - cam.viewportHeight / 2 + 100),
            0f
        )
        cam.update()

        batch.projectionMatrix = cam.combined
        batch.begin()
        batch.draw(mapTex, 0f, 0f, MAP, MAP)

        // splats (fade out; random frame from the 3-variant strip)
        for (s in splats) {
            val cell = splatTex.height
            val frames = (splatTex.width / cell).coerceAtLeast(1)
            batch.setColor(1f, 1f, 1f, (1f - s.t / 14f).coerceIn(0f, 1f))
            batch.draw(splatTex, s.x - 30, s.y - 30, 30f, 30f, 60f, 60f, 1f, 1f, s.rot, (s.fr % frames) * cell, 0, cell, cell, false, false)
        }
        batch.setColor(1f, 1f, 1f, 1f)

        // wrecked cars (one sprite, tinted)
        for (w in wrecks) {
            batch.color = Assets.WRECK_TINTS[w.tint]
            batch.draw(wreckTex, w.x - WRECK_W / 2, w.y - WRECK_H / 2, WRECK_W / 2, WRECK_H / 2, WRECK_W, WRECK_H, 1f, 1f, w.rot, 0, 0, wreckTex.width, wreckTex.height, false, false)
        }
        batch.setColor(1f, 1f, 1f, 1f)

        // bullets
        for (b in bullets) {
            val ang = MathUtils.atan2(b.vy, b.vx) * MathUtils.radiansToDegrees
            batch.draw(bulletTex, b.x - 8, b.y - 3, 8f, 3f, 16f, 6f, 1f, 1f, ang, 0, 0, 16, 6, false, false)
        }

        // dying zombies (one-shot death anim, then the splat remains)
        for (d in dying) {
            val cell = zombieDeathTex.height
            val frames = (zombieDeathTex.width / cell).coerceAtLeast(1)
            val f = (d.t * 8f).toInt().coerceAtMost(frames - 1)
            batch.draw(zombieDeathTex, d.x - ZOMBIE_SIZE / 2, d.y - ZOMBIE_SIZE / 2, ZOMBIE_SIZE / 2, ZOMBIE_SIZE / 2, ZOMBIE_SIZE, ZOMBIE_SIZE, 1f, 1f, d.ang, f * cell, 0, cell, cell, false, false)
        }

        // zombies (walk sheet while chasing, attack sheet on contact)
        for (z in zombies) {
            val sheet = if (z.contact) zombieAttackTex else zombieTex
            val fps = if (z.contact) 8f else 10f
            val cell = sheet.height
            val frames = (sheet.width / cell).coerceAtLeast(1)
            val f = MathUtils.floor(time * fps + z.phase) % frames
            batch.draw(sheet, z.x - ZOMBIE_SIZE / 2, z.y - ZOMBIE_SIZE / 2, ZOMBIE_SIZE / 2, ZOMBIE_SIZE / 2, ZOMBIE_SIZE, ZOMBIE_SIZE, 1f, 1f, zombieAngle(z), f * cell, 0, cell, cell, false, false)
        }

        // hero (idle / walk / fire sheets by state, walk bob while moving)
        val moving = stickDelta.len2() > 25f
        val bob = if (moving) MathUtils.sin(time * 12f) * 4.5f else 0f
        var sheet = heroTex
        var fps = 10f
        if (!moving && flashT > 0f) { sheet = heroFireTex; fps = 12f }
        else if (!moving) { sheet = heroIdleTex; fps = 5f }
        val hCell = sheet.height
        val hFrames = (sheet.width / hCell).coerceAtLeast(1)
        val hf = MathUtils.floor(time * fps) % hFrames
        batch.draw(sheet, hero.x - HERO_SIZE / 2, hero.y - HERO_SIZE / 2, HERO_SIZE / 2, HERO_SIZE / 2, HERO_SIZE, HERO_SIZE, 1f, 1f, heroAngle + bob, hf * hCell, 0, hCell, hCell, false, false)

        // muzzle flash right after a shot (3-frame strip)
        if (flashT > 0f) {
            val fx = hero.x + MathUtils.cosDeg(heroAngle) * MUZZLE_DIST
            val fy = hero.y + MathUtils.sinDeg(heroAngle) * MUZZLE_DIST
            val fs = 52f * (0.6f + 0.4f * flashT / 0.06f)
            val cell = flashTex.height
            val frames = (flashTex.width / cell).coerceAtLeast(1)
            val ff = (((0.06f - flashT) / 0.06f) * frames).toInt().coerceIn(0, frames - 1)
            batch.setColor(1f, 1f, 1f, (flashT / 0.06f).coerceIn(0f, 1f))
            batch.draw(flashTex, fx - fs / 2, fy - fs / 2, fs / 2, fs / 2, fs, fs, 1f, 1f, heroAngle, ff * cell, 0, cell, cell, false, false)
            batch.setColor(1f, 1f, 1f, 1f)
        }
        batch.end()

        drawHud()
    }

    private fun zombieAngle(z: Zombie): Float =
        MathUtils.atan2(hero.y - z.y, hero.x - z.x) * MathUtils.radiansToDegrees

    private fun update(dt: Float) {
        // movement (screen y is down, world y is up)
        if (stickDelta.len2() > 25f) {
            hero.x += stickDelta.x / STICK_R * HERO_SPEED * dt
            hero.y -= stickDelta.y / STICK_R * HERO_SPEED * dt
            hero.x = hero.x.coerceIn(40f, MAP - 40)
            hero.y = hero.y.coerceIn(40f, MAP - 40)
        }
        if (flashT > 0f) flashT -= dt

        // auto-fire at nearest zombie
        var target: Zombie? = null
        var best = AIM_RANGE * AIM_RANGE
        for (z in zombies) {
            val d2 = Vector2.dst2(hero.x, hero.y, z.x, z.y)
            if (d2 < best) { best = d2; target = z }
        }
        if (target != null) {
            heroAngle = MathUtils.atan2(target.y - hero.y, target.x - hero.x) * MathUtils.radiansToDegrees
            fireTimer -= dt
            if (fireTimer <= 0f) {
                fireTimer = FIRE_RATE
                flashT = 0.06f
                val a = MathUtils.atan2(target.y - hero.y, target.x - hero.x) + MathUtils.random(-0.045f, 0.045f)
                bullets.add(Bullet(
                    hero.x + MathUtils.cos(a) * 40f, hero.y + MathUtils.sin(a) * 40f,
                    MathUtils.cos(a) * BULLET_SPEED, MathUtils.sin(a) * BULLET_SPEED, 1.1f
                ))
            }
        } else if (stickDelta.len2() > 25f) {
            heroAngle = MathUtils.atan2(-stickDelta.y, stickDelta.x) * MathUtils.radiansToDegrees
        }

        // bullets
        var i = bullets.size - 1
        while (i >= 0) {
            val b = bullets[i]
            b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt
            if (b.life <= 0f) { bullets.removeIndex(i) }
            i--
        }

        // zombies chase + damage
        i = zombies.size - 1
        while (i >= 0) {
            val z = zombies[i]
            val dx = hero.x - z.x; val dy = hero.y - z.y
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat().coerceAtLeast(0.001f)
            z.contact = d <= 34f
            if (d > 34f) {
                z.x += dx / d * z.speed * dt
                z.y += dy / d * z.speed * dt
            } else {
                hp -= ZOMBIE_DPS * dt
            }
            // bullet hits
            var j = bullets.size - 1
            while (j >= 0) {
                val b = bullets[j]
                if (Vector2.dst2(b.x, b.y, z.x, z.y) < 30f * 30f) {
                    z.hp -= BULLET_DMG
                    bullets.removeIndex(j)
                }
                j--
            }
            if (z.hp <= 0) {
                splats.add(Splat(z.x, z.y, MathUtils.random(360f), 0f, MathUtils.random(2)))
                dying.add(Dying(z.x, z.y, zombieAngle(z), 0f))
                zombies.removeIndex(i)
                kills++
            }
            i--
        }
        // simple separation so zombies do not stack
        var a = 0
        while (a < zombies.size) {
            var b = a + 1
            while (b < zombies.size) {
                val za = zombies[a]; val zb = zombies[b]
                val ddx = zb.x - za.x; val ddy = zb.y - za.y
                val dd = ddx * ddx + ddy * ddy
                if (dd < 36f * 36f && dd > 0.01f) {
                    val d = Math.sqrt(dd.toDouble()).toFloat()
                    val push = (36f - d) * 0.5f
                    val nx = ddx / d; val ny = ddy / d
                    za.x -= nx * push; za.y -= ny * push
                    zb.x += nx * push; zb.y += ny * push
                }
                b++
            }
            a++
        }

        // splat + dying aging
        i = splats.size - 1
        while (i >= 0) {
            splats[i].t += dt
            if (splats[i].t > 14f) splats.removeIndex(i)
            i--
        }
        i = dying.size - 1
        while (i >= 0) {
            dying[i].t += dt
            if (dying[i].t > 0.6f) dying.removeIndex(i)
            i--
        }

        if (bannerT > 0f) bannerT -= dt

        // wave flow
        if (zombies.isEmpty) {
            waveDelay -= dt
            if (waveDelay <= 0f) {
                spawnWave()
                waveDelay = 3f
            }
        }

        if (hp <= 0f) {
            hp = 0f
            gameOver = true
        }
    }

    private fun capsule(x: Float, y: Float, w: Float, h: Float, fill: Float) {
        val r = h / 2
        // dark background capsule
        shapes.color = Color(0.10f, 0.14f, 0.12f, 0.92f)
        shapes.rect(x + r, y, w - h, h)
        shapes.circle(x + r, y + r, r)
        shapes.circle(x + w - r, y + r, r)
        // green fill up to fraction
        val fw = (w * fill).coerceAtLeast(0.01f)
        shapes.color = Color(0.35f, 0.90f, 0.25f, 1f)
        if (fw <= r) {
            shapes.circle(x + r, y + r, fw.coerceAtMost(r))
        } else {
            shapes.circle(x + r, y + r, r)
            shapes.rect(x + r, y, fw - r, h)
            if (fw > w - r) shapes.circle(x + w - r, y + r, r) else shapes.circle(x + fw, y + r, r)
        }
    }

    private fun drawHud() {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        hudCam.setToOrtho(false, w, h)
        hudCam.update()

        // ShapeRenderer needs blending re-enabled after SpriteBatch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = hudCam.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        // health capsule bar beside the portrait
        capsule(86f, h - 46f, 200f, 22f, hp / 100f)
        // static joystick ring bottom-left (concept style)
        shapes.color = Color(1f, 1f, 1f, 0.10f)
        shapes.circle(110f, 100f, 62f)
        shapes.color = Color(1f, 1f, 1f, 0.22f)
        shapes.circle(110f, 100f, 26f)
        // active floating joystick
        if (movePointer != -1) {
            shapes.color = Color(1f, 1f, 1f, 0.12f)
            shapes.circle(stickOrigin.x, h - stickOrigin.y, STICK_R)
            shapes.color = Color(1f, 1f, 1f, 0.30f)
            shapes.circle(stickOrigin.x + stickDelta.x, h - stickOrigin.y - stickDelta.y, 26f)
        }
        shapes.end()

        batch.projectionMatrix = hudCam.combined
        batch.begin()
        // portrait top-left
        batch.draw(portraitTex, 14f, h - 66f, 58f, 58f)
        font.draw(batch, "HP ${hp.toInt()}", 96f, h - 30f)
        font.draw(batch, "WAVE $wave", w - 130f, h - 22f)
        font.draw(batch, "KILLS $kills", w - 130f, h - 46f)
        if (bannerT > 0f) {
            val txt = "WAVE $wave"
            layout.setText(bigFont, txt)
            bigFont.setColor(1f, 0.85f, 0.3f, (bannerT / 2.2f).coerceIn(0f, 1f))
            bigFont.draw(batch, txt, (w - layout.width) / 2, h / 2 + layout.height + 60f)
            bigFont.setColor(Color.WHITE)
        }
        if (gameOver) {
            val t1 = "GAME OVER"
            layout.setText(bigFont, t1)
            bigFont.setColor(0.95f, 0.25f, 0.3f, 1f)
            bigFont.draw(batch, t1, (w - layout.width) / 2, h / 2 + 60f)
            val t2 = "Waves survived: $wave   Kills: $kills"
            layout.setText(font, t2)
            font.draw(batch, t2, (w - layout.width) / 2, h / 2)
            val t3 = "Tap anywhere to restart"
            layout.setText(font, t3)
            font.draw(batch, t3, (w - layout.width) / 2, h / 2 - 36f)
        }
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        val vw = 900f
        cam.viewportWidth = vw
        cam.viewportHeight = vw * height / width
        cam.update()
    }

    override fun dispose() {
        batch.dispose(); shapes.dispose(); font.dispose(); bigFont.dispose()
        heroTex.dispose(); heroIdleTex.dispose(); heroFireTex.dispose()
        zombieTex.dispose(); zombieAttackTex.dispose(); zombieDeathTex.dispose()
        wreckTex.dispose(); flashTex.dispose()
        splatTex.dispose(); mapTex.dispose(); portraitTex.dispose(); bulletTex.dispose()
    }
}
