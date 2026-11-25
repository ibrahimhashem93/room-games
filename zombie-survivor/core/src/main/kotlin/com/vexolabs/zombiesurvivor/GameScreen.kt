package com.vexolabs.zombiesurvivor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
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
import java.util.EnumMap

class GameScreen : ScreenAdapter() {

    private val MAP = 1600f
    private val HERO_SPEED = 170f
    private val AIM_RANGE = 430f

    private val HERO_SIZE = 88f
    private val WRECK_W = 118f
    private val WRECK_H = 65f
    private val MUZZLE_DIST = 44f

    // ---- monsters ----
    enum class MonKind(
        val size: Float, val baseHp: Int, val hpPerWave: Int,
        val baseSpeed: Float, val speedPerWave: Float, val maxSpeed: Float,
        val contactDps: Float, val tex: String, val king: Boolean = false
    ) {
        SHAMBLER(80f, 80, 20, 52f, 3f, 95f, 14f, "zombie.png"),
        RUNNER(64f, 40, 10, 110f, 4f, 170f, 8f, "runner.png"),
        BRUTE(120f, 260, 50, 34f, 2f, 60f, 30f, "brute.png"),
        SPITTER(76f, 70, 15, 46f, 3f, 80f, 10f, "spitter.png"),
        KING_GOLIATH(190f, 1500, 400, 30f, 1f, 45f, 45f, "king-goliath.png", true),
        KING_BROOD(170f, 1100, 300, 26f, 1f, 40f, 25f, "king-brood.png", true)
    }

    // ---- weapons ----
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

    private data class Monster(
        val id: Int, var x: Float, var y: Float, var hp: Int, val maxHp: Int,
        val kind: MonKind, val speed: Float, val phase: Float,
        var contact: Boolean = false, var burnT: Float = 0f,
        var spitT: Float = 0f, var summonT: Float = 0f, var slamT: Float = 0f
    )
    private data class Bullet(
        var x: Float, var y: Float, val vx: Float, val vy: Float, var life: Float,
        val dmg: Int, val pierce: Boolean, val aoe: Float, val flame: Boolean,
        val hitIds: MutableSet<Int> = mutableSetOf()
    )
    private data class Glob(var x: Float, var y: Float, val vx: Float, val vy: Float, var life: Float, val dmg: Float)
    private data class Splat(val x: Float, val y: Float, val rot: Float, var t: Float, val fr: Int)
    private data class Wreck(val x: Float, val y: Float, val rot: Float, val tint: Int)
    private data class Dying(val x: Float, val y: Float, val ang: Float, var t: Float, val kind: MonKind)
    private data class Pickup(val x: Float, val y: Float, val weapon: Weapon, val ammo: Int, var t: Float = 0f)
    private data class Explosion(val x: Float, val y: Float, var t: Float, val radius: Float)

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
    private lateinit var wreckTex: Texture
    private lateinit var flashTex: Texture
    private lateinit var splatTex: Texture
    private lateinit var mapTex: Texture
    private lateinit var portraitTex: Texture
    private lateinit var bulletTex: Texture
    private lateinit var pickupTex: Texture
    private lateinit var zombieTex: Texture
    private lateinit var zombieAttackTex: Texture
    private lateinit var zombieDeathTex: Texture
    private val monTex = EnumMap<MonKind, Texture>(MonKind::class.java)
    private val weaponTex = EnumMap<Weapon, Texture>(Weapon::class.java)

    private val hero = Vector2(MAP / 2, MAP / 2)
    private var heroAngle = 0f
    private var hp = 100f
    private val monsters = Array<Monster>()
    private val bullets = Array<Bullet>()
    private val globs = Array<Glob>()
    private val splats = Array<Splat>()
    private val wrecks = Array<Wreck>()
    private val dying = Array<Dying>()
    private val pickups = Array<Pickup>()
    private val explosions = Array<Explosion>()

    private var nextMonId = 1
    private var wave = 0
    private var kills = 0
    private var waveDelay = 2f
    private var bannerT = 0f
    private var bannerText = ""
    private var fireTimer = 0f
    private var flashT = 0f
    private var time = 0f
    private var gameOver = false

    // weapons: ammo per owned weapon (-1 = infinite); start with pistol + rifle
    private val ammo = EnumMap<Weapon, Int>(Weapon::class.java)
    private var weapon = Weapon.RIFLE

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
        pickupTex = Assets.load("pickup.png")
        for (k in MonKind.values()) monTex[k] = Assets.load(k.tex)
        for (w in Weapon.values()) weaponTex[w] = Assets.load(w.icon)
        reset()
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

    private fun addMonster(kind: MonKind, x: Float, y: Float) {
        val speed = (kind.baseSpeed + wave * kind.speedPerWave).coerceAtMost(kind.maxSpeed)
        val mhp = kind.baseHp + wave * kind.hpPerWave
        monsters.add(Monster(nextMonId++, x.coerceIn(50f, MAP - 50), y.coerceIn(50f, MAP - 50), mhp, mhp, kind, speed, MathUtils.random(MathUtils.PI2)))
    }

    private fun spawnWave() {
        wave++
        bannerT = 2.2f
        val bossWave = wave % 5 == 0
        if (bossWave) {
            bannerText = "KING WAVE $wave"
            val king = if ((wave / 5) % 2 == 1) MonKind.KING_GOLIATH else MonKind.KING_BROOD
            val a = MathUtils.random(MathUtils.PI2)
            addMonster(king, hero.x + MathUtils.cos(a) * 500f, hero.y + MathUtils.sin(a) * 500f)
            repeat(2 + wave / 2) { spawnRegular() }
        } else {
            bannerText = "WAVE $wave"
            repeat(3 + wave * 2) { spawnRegular() }
        }
    }

    private fun spawnRegular() {
        val a = MathUtils.random(MathUtils.PI2)
        val d = MathUtils.random(480f, 680f)
        val zx = hero.x + MathUtils.cos(a) * d
        val zy = hero.y + MathUtils.sin(a) * d
        val roll = MathUtils.random(100)
        val kind = when {
            wave >= 4 && roll < 20 -> MonKind.SPITTER
            wave >= 3 && roll < 35 -> MonKind.BRUTE
            wave >= 2 && roll < 65 -> MonKind.RUNNER
            else -> MonKind.SHAMBLER
        }
        addMonster(kind, zx, zy)
    }

    private fun reset() {
        hero.set(MAP / 2, MAP / 2)
        hp = 100f
        monsters.clear(); bullets.clear(); splats.clear(); dying.clear()
        globs.clear(); pickups.clear(); explosions.clear()
        wave = 0; kills = 0; waveDelay = 2f; bannerT = 0f
        gameOver = false
        movePointer = -1
        ammo.clear()
        ammo[Weapon.PISTOL] = -1
        ammo[Weapon.RIFLE] = Weapon.RIFLE.startAmmo
        weapon = Weapon.RIFLE
        fireTimer = 0f; flashT = 0f
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

    private fun cycleWeapon() {
        val owned = Weapon.values().filter { ammo.containsKey(it) }
        if (owned.size < 2) return
        val idx = owned.indexOf(weapon)
        weapon = owned[(idx + 1) % owned.size]
        fireTimer = 0f
    }

    private fun pollWeaponKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) cycleWeapon()
        val keys = intArrayOf(Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5, Input.Keys.NUM_6)
        val owned = Weapon.values().filter { ammo.containsKey(it) }
        for ((i, k) in keys.withIndex()) {
            if (Gdx.input.isKeyJustPressed(k) && i < owned.size) { weapon = owned[i]; fireTimer = 0f }
        }
        // tap on the weapon HUD zone (top-center) cycles on touch screens
        if (Gdx.input.justTouched() && movePointer == -1) {
            val tx = Gdx.input.x.toFloat()
            val ty = Gdx.input.y.toFloat()
            val w = Gdx.graphics.width.toFloat()
            if (tx > w / 2 - 150 && tx < w / 2 + 150 && ty < 70) cycleWeapon()
        }
    }

    override fun render(delta: Float) {
        val dt = delta.coerceAtMost(0.05f)
        time += dt
        pollJoystick()

        if (gameOver) {
            if (Gdx.input.justTouched()) reset()
        } else {
            pollWeaponKeys()
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

        // pickups (crate + floating weapon icon)
        for (p in pickups) {
            val bobY = MathUtils.sin(time * 3f + p.t) * 5f
            batch.draw(pickupTex, p.x - 24, p.y - 24, 48f, 48f)
            val icon = weaponTex.getValue(p.weapon)
            batch.draw(icon, p.x - 18, p.y - 18 + 34 + bobY, 36f, 36f)
        }

        // bullets (tracer look varies by weapon behavior)
        for (b in bullets) {
            val ang = MathUtils.atan2(b.vy, b.vx) * MathUtils.radiansToDegrees
            when {
                b.flame -> {
                    batch.setColor(1f, 0.55f, 0.12f, 0.85f)
                    batch.draw(bulletTex, b.x - 10, b.y - 10, 20f, 20f)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
                b.aoe > 0f -> {
                    batch.setColor(0.65f, 0.9f, 0.5f, 1f)
                    batch.draw(bulletTex, b.x - 12, b.y - 6, 12f, 6f, 24f, 12f, 1f, 1f, ang, 0, 0, 16, 6, false, false)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
                b.pierce -> {
                    batch.setColor(0.55f, 0.9f, 1f, 1f)
                    batch.draw(bulletTex, b.x - 16, b.y - 2, 16f, 2f, 32f, 4f, 1f, 1f, ang, 0, 0, 16, 6, false, false)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
                else -> batch.draw(bulletTex, b.x - 8, b.y - 3, 8f, 3f, 16f, 6f, 1f, 1f, ang, 0, 0, 16, 6, false, false)
            }
        }

        // enemy globs (spitter / brood projectiles)
        for (g in globs) {
            batch.setColor(0.65f, 1f, 0.3f, 0.95f)
            batch.draw(bulletTex, g.x - 9, g.y - 9, 18f, 18f)
            batch.setColor(1f, 1f, 1f, 1f)
        }

        // explosions (expanding ring)
        for (e in explosions) {
            val r = e.radius * (e.t / 0.35f).coerceIn(0f, 1f)
            batch.setColor(1f, 0.7f, 0.2f, (1f - e.t / 0.35f).coerceIn(0f, 1f))
            batch.draw(bulletTex, e.x - r, e.y - 6, r * 2, 12f)
            batch.setColor(1f, 1f, 1f, 1f)
        }

        // dying monsters (one-shot death anim, then the splat remains)
        for (d in dying) {
            val sheet = zombieDeathTex
            val cell = sheet.height
            val frames = (sheet.width / cell).coerceAtLeast(1)
            val f = (d.t * 8f).toInt().coerceAtMost(frames - 1)
            val size = d.kind.size
            batch.draw(sheet, d.x - size / 2, d.y - size / 2, size / 2, size / 2, size, size, 1f, 1f, d.ang, f * cell, 0, cell, cell, false, false)
        }

        // monsters: shambler uses the animated sheets; others use their sprite with code motion
        for (z in monsters) {
            val ang = monsterAngle(z)
            val size = z.kind.size
            if (z.burnT > 0f) batch.setColor(1f, 0.62f, 0.32f, 1f)
            if (z.kind == MonKind.SHAMBLER) {
                val sheet = if (z.contact) zombieAttackTex else zombieTex
                val fps = if (z.contact) 8f else 10f
                val cell = sheet.height
                val frames = (sheet.width / cell).coerceAtLeast(1)
                val f = MathUtils.floor(time * fps + z.phase) % frames
                batch.draw(sheet, z.x - size / 2, z.y - size / 2, size / 2, size / 2, size, size, 1f, 1f, ang, f * cell, 0, cell, cell, false, false)
            } else {
                val tex = monTex.getValue(z.kind)
                val wob = MathUtils.sin(time * 3.2f + z.phase) * (if (z.kind.king) 3f else 7f)
                batch.draw(tex, z.x - size / 2, z.y - size / 2, size / 2, size / 2, size, size, 1f, 1f, ang + wob, 0, 0, tex.width, tex.height, false, false)
            }
            batch.setColor(1f, 1f, 1f, 1f)
            // king HP bar above the sprite
            if (z.kind.king) {
                val bw = size * 0.9f
                batch.setColor(0.2f, 0.05f, 0.05f, 0.9f)
                batch.draw(bulletTex, z.x - bw / 2, z.y + size / 2 + 8, bw, 6f)
                batch.setColor(0.9f, 0.15f, 0.15f, 1f)
                batch.draw(bulletTex, z.x - bw / 2, z.y + size / 2 + 8, bw * (z.hp.toFloat() / z.maxHp).coerceIn(0f, 1f), 6f)
                batch.setColor(1f, 1f, 1f, 1f)
            }
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

    private fun monsterAngle(z: Monster): Float =
        MathUtils.atan2(hero.y - z.y, hero.x - z.x) * MathUtils.radiansToDegrees

    private fun fireOnce(baseAngle: Float) {
        val w = weapon
        repeat(w.pellets) {
            val a = baseAngle + MathUtils.random(-w.spread, w.spread)
            bullets.add(Bullet(
                hero.x + MathUtils.cos(a) * 40f, hero.y + MathUtils.sin(a) * 40f,
                MathUtils.cos(a) * w.speed, MathUtils.sin(a) * w.speed, w.life,
                w.dmg, w.pierce, w.aoe, w.flame
            ))
        }
        flashT = 0.06f
        if (ammo[weapon] != -1) {
            val left = (ammo[weapon] ?: 0) - 1
            ammo[weapon] = left
            if (left <= 0) {
                ammo.remove(weapon)
                weapon = Weapon.PISTOL
            }
        }
    }

    private fun explode(x: Float, y: Float, radius: Float, dmg: Int) {
        explosions.add(Explosion(x, y, 0f, radius))
        for (z in monsters) {
            if (Vector2.dst2(x, y, z.x, z.y) < radius * radius) {
                z.hp -= dmg
                if (z.burnT < 1f) z.burnT = 1f
            }
        }
    }

    private fun update(dt: Float) {
        // movement (screen y is down, world y is up)
        if (stickDelta.len2() > 25f) {
            hero.x += stickDelta.x / STICK_R * HERO_SPEED * dt
            hero.y -= stickDelta.y / STICK_R * HERO_SPEED * dt
            hero.x = hero.x.coerceIn(40f, MAP - 40)
            hero.y = hero.y.coerceIn(40f, MAP - 40)
        }
        if (flashT > 0f) flashT -= dt

        // auto-fire at nearest monster
        var target: Monster? = null
        var best = AIM_RANGE * AIM_RANGE
        for (z in monsters) {
            val d2 = Vector2.dst2(hero.x, hero.y, z.x, z.y)
            if (d2 < best) { best = d2; target = z }
        }
        if (target != null) {
            heroAngle = MathUtils.atan2(target.y - hero.y, target.x - hero.x) * MathUtils.radiansToDegrees
            fireTimer -= dt
            if (fireTimer <= 0f && (ammo[weapon] == -1 || (ammo[weapon] ?: 0) > 0)) {
                fireTimer = weapon.rate
                val a = MathUtils.atan2(target.y - hero.y, target.x - hero.x)
                fireOnce(a)
            }
        } else if (stickDelta.len2() > 25f) {
            heroAngle = MathUtils.atan2(-stickDelta.y, stickDelta.x) * MathUtils.radiansToDegrees
        }

        // bullets
        var i = bullets.size - 1
        while (i >= 0) {
            val b = bullets[i]
            b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt
            if (b.life <= 0f) {
                if (b.aoe > 0f) explode(b.x, b.y, b.aoe, b.dmg)
                bullets.removeIndex(i)
            }
            i--
        }

        // enemy globs
        i = globs.size - 1
        while (i >= 0) {
            val g = globs[i]
            g.x += g.vx * dt; g.y += g.vy * dt; g.life -= dt
            if (Vector2.dst2(g.x, g.y, hero.x, hero.y) < 24f * 24f) {
                hp -= g.dmg
                globs.removeIndex(i)
            } else if (g.life <= 0f) globs.removeIndex(i)
            i--
        }

        // monsters: AI + abilities + damage
        i = monsters.size - 1
        while (i >= 0) {
            val z = monsters[i]
            val dx = hero.x - z.x; val dy = hero.y - z.y
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat().coerceAtLeast(0.001f)

            // burn damage-over-time
            if (z.burnT > 0f) {
                z.burnT -= dt
                z.hp -= (8f * dt).toInt().coerceAtLeast(if (MathUtils.random() < 8f * dt) 1 else 0)
            }

            z.contact = d <= 34f
            when (z.kind) {
                MonKind.SPITTER -> {
                    // keeps distance, spits globs
                    if (d > 260f) { z.x += dx / d * z.speed * dt; z.y += dy / d * z.speed * dt }
                    else if (d < 180f) { z.x -= dx / d * z.speed * 0.7f * dt; z.y -= dy / d * z.speed * 0.7f * dt }
                    z.spitT -= dt
                    if (z.spitT <= 0f && d < 420f) {
                        z.spitT = 2.2f
                        val a = MathUtils.atan2(dy, dx)
                        globs.add(Glob(z.x + MathUtils.cos(a) * 30f, z.y + MathUtils.sin(a) * 30f, MathUtils.cos(a) * 260f, MathUtils.sin(a) * 260f, 2f, 12f))
                    }
                }
                MonKind.KING_GOLIATH -> {
                    if (d > 34f) { z.x += dx / d * z.speed * dt; z.y += dy / d * z.speed * dt } else hp -= z.kind.contactDps * dt
                    z.slamT -= dt
                    if (z.slamT <= 0f && d < 170f) {
                        z.slamT = 4f
                        explosions.add(Explosion(z.x, z.y, 0f, 200f))
                        if (d < 200f) hp -= 30f
                    }
                    z.summonT -= dt
                    if (z.summonT <= 0f) {
                        z.summonT = 8f
                        val minions = monsters.count { !it.kind.king }
                        if (minions < 6) repeat(2) {
                            addMonster(MonKind.SHAMBLER, z.x + MathUtils.random(-90f, 90f), z.y + MathUtils.random(-90f, 90f))
                        }
                    }
                }
                MonKind.KING_BROOD -> {
                    if (d > 120f) { z.x += dx / d * z.speed * dt; z.y += dy / d * z.speed * dt } else if (d <= 34f) hp -= z.kind.contactDps * dt
                    z.summonT -= dt
                    if (z.summonT <= 0f) {
                        z.summonT = 3f
                        val minions = monsters.count { !it.kind.king }
                        if (minions < 8) addMonster(MonKind.RUNNER, z.x + MathUtils.random(-70f, 70f), z.y + MathUtils.random(-70f, 70f))
                    }
                    z.spitT -= dt
                    if (z.spitT <= 0f && d < 430f) {
                        z.spitT = 3.5f
                        for (k in -1..1) {
                            val a = MathUtils.atan2(dy, dx) + k * 0.2f
                            globs.add(Glob(z.x + MathUtils.cos(a) * 60f, z.y + MathUtils.sin(a) * 60f, MathUtils.cos(a) * 240f, MathUtils.sin(a) * 240f, 2.2f, 10f))
                        }
                    }
                }
                else -> {
                    if (d > 34f) { z.x += dx / d * z.speed * dt; z.y += dy / d * z.speed * dt }
                    else hp -= z.kind.contactDps * dt
                }
            }

            // bullet hits
            var j = bullets.size - 1
            while (j >= 0) {
                val b = bullets[j]
                if (b.hitIds.contains(z.id)) { j--; continue }
                if (Vector2.dst2(b.x, b.y, z.x, z.y) < 30f * 30f) {
                    if (b.aoe > 0f) {
                        explode(b.x, b.y, b.aoe, b.dmg)
                        bullets.removeIndex(j)
                    } else {
                        z.hp -= b.dmg
                        if (b.flame) z.burnT = 2f
                        b.hitIds.add(z.id)
                        if (!b.pierce) bullets.removeIndex(j)
                    }
                }
                j--
            }

            if (z.hp <= 0) {
                splats.add(Splat(z.x, z.y, MathUtils.random(360f), 0f, MathUtils.random(2)))
                dying.add(Dying(z.x, z.y, monsterAngle(z), 0f, z.kind))
                monsters.removeIndex(i)
                kills++
                // drops: kings always drop 2 weapon crates; regulars 18%
                if (z.kind.king) {
                    repeat(2) { dropPickup(z.x + MathUtils.random(-50f, 50f), z.y + MathUtils.random(-50f, 50f)) }
                } else if (MathUtils.random(100) < 18) {
                    dropPickup(z.x, z.y)
                }
            }
            i--
        }
        // simple separation so monsters do not stack
        var a = 0
        while (a < monsters.size) {
            var b = a + 1
            while (b < monsters.size) {
                val za = monsters[a]; val zb = monsters[b]
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

        // pickups: attract + collect
        i = pickups.size - 1
        while (i >= 0) {
            val p = pickups[i]
            p.t += dt
            if (Vector2.dst2(p.x, p.y, hero.x, hero.y) < 44f * 44f) {
                val has = ammo.containsKey(p.weapon)
                if (has && ammo[p.weapon] != -1) ammo[p.weapon] = (ammo[p.weapon] ?: 0) + p.ammo
                else if (!has) ammo[p.weapon] = p.ammo
                weapon = p.weapon
                fireTimer = 0f
                pickups.removeIndex(i)
            }
            i--
        }

        // aging
        i = splats.size - 1
        while (i >= 0) { splats[i].t += dt; if (splats[i].t > 14f) splats.removeIndex(i); i-- }
        i = dying.size - 1
        while (i >= 0) { dying[i].t += dt; if (dying[i].t > 0.6f) dying.removeIndex(i); i-- }
        i = explosions.size - 1
        while (i >= 0) { explosions[i].t += dt; if (explosions[i].t > 0.35f) explosions.removeIndex(i); i-- }

        if (bannerT > 0f) bannerT -= dt

        // wave flow
        if (monsters.isEmpty) {
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

    private fun dropPickup(x: Float, y: Float) {
        // drop a weapon the player does not own yet, otherwise ammo for a random owned non-pistol weapon
        val notOwned = Weapon.values().filter { !ammo.containsKey(it) }
        if (notOwned.isNotEmpty()) {
            val w = notOwned.random()
            pickups.add(Pickup(x.coerceIn(60f, MAP - 60), y.coerceIn(60f, MAP - 60), w, w.startAmmo))
        } else {
            val owned = ammo.keys.filter { it != Weapon.PISTOL }
            val w = if (owned.isNotEmpty()) owned.random() else Weapon.RIFLE
            pickups.add(Pickup(x.coerceIn(60f, MAP - 60), y.coerceIn(60f, MAP - 60), w, (w.startAmmo / 2).coerceAtLeast(1)))
        }
    }

    private fun capsule(x: Float, y: Float, w: Float, h: Float, fill: Float) {
        val r = h / 2
        shapes.color = Color(0.10f, 0.14f, 0.12f, 0.92f)
        shapes.rect(x + r, y, w - h, h)
        shapes.circle(x + r, y + r, r)
        shapes.circle(x + w - r, y + r, r)
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

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = hudCam.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        capsule(86f, h - 46f, 200f, 22f, hp / 100f)
        shapes.color = Color(1f, 1f, 1f, 0.10f)
        shapes.circle(110f, 100f, 62f)
        shapes.color = Color(1f, 1f, 1f, 0.22f)
        shapes.circle(110f, 100f, 26f)
        if (movePointer != -1) {
            shapes.color = Color(1f, 1f, 1f, 0.12f)
            shapes.circle(stickOrigin.x, h - stickOrigin.y, STICK_R)
            shapes.color = Color(1f, 1f, 1f, 0.30f)
            shapes.circle(stickOrigin.x + stickDelta.x, h - stickOrigin.y - stickDelta.y, 26f)
        }
        shapes.end()

        batch.projectionMatrix = hudCam.combined
        batch.begin()
        batch.draw(portraitTex, 14f, h - 66f, 58f, 58f)
        font.draw(batch, "HP ${hp.toInt()}", 96f, h - 30f)
        font.draw(batch, "WAVE $wave", w - 130f, h - 22f)
        font.draw(batch, "KILLS $kills", w - 130f, h - 46f)

        // weapon HUD: icons of owned weapons, current highlighted, ammo under the current
        val owned = Weapon.values().filter { ammo.containsKey(it) }
        val iconSize = 34f
        val totalW = owned.size * (iconSize + 8f)
        var ix = w / 2 - totalW / 2
        for (ow in owned) {
            if (ow == weapon) {
                batch.setColor(1f, 0.85f, 0.3f, 0.35f)
                batch.draw(bulletTex, ix - 3, h - 46f, iconSize + 6, iconSize + 6)
                batch.setColor(1f, 1f, 1f, 1f)
            }
            batch.draw(weaponTex.getValue(ow), ix, h - 43f, iconSize, iconSize)
            ix += iconSize + 8f
        }
        val ammoTxt = if (ammo[weapon] == -1) "∞" else "${ammo[weapon]}"
        layout.setText(font, "${weapon.label}  $ammoTxt")
        font.draw(batch, "${weapon.label}  $ammoTxt", (w - layout.width) / 2, h - 52f)

        if (bannerT > 0f) {
            layout.setText(bigFont, bannerText)
            bigFont.setColor(1f, 0.85f, 0.3f, (bannerT / 2.2f).coerceIn(0f, 1f))
            bigFont.draw(batch, bannerText, (w - layout.width) / 2, h / 2 + layout.height + 60f)
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
        wreckTex.dispose(); flashTex.dispose(); pickupTex.dispose()
        splatTex.dispose(); mapTex.dispose(); portraitTex.dispose(); bulletTex.dispose()
        monTex.values.forEach { it.dispose() }
        weaponTex.values.forEach { it.dispose() }
    }
}
