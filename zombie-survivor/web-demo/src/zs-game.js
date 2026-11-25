// Zombie Survivor - web port of the libGDX build (same gameplay, same sprites).
// v5: full game shell - main menu with 3 maps, pause, settings (audio + control
// modes), coin economy + shop with upgrades and weapon unlocks, procedural audio.

export function initGame(root, urls) {
  const cv = root.querySelector('#zs-cv');
  const loadingEl = root.querySelector('#zs-loading');
  const stage = root.querySelector('#zs-stage');
  const ctx = cv.getContext('2d');

  // ---- constants (mirror the Kotlin build) ----
  const MAP = 1600;
  const HERO_SPEED = 170;
  const AIM_RANGE = 430;
  const HERO_SIZE = 88;
  const WRECK_W = 118, WRECK_H = 65;
  const MUZZLE_DIST = 44;
  const STICK_R = 70;

  const MON = {
    shambler: { size: 80, hp: 80, hpW: 20, speed: 52, speedW: 3, maxSpeed: 95, dps: 14, img: 'zombie', coins: 1 },
    runner: { size: 64, hp: 40, hpW: 10, speed: 110, speedW: 4, maxSpeed: 170, dps: 8, img: 'runner', coins: 2 },
    brute: { size: 120, hp: 260, hpW: 50, speed: 34, speedW: 2, maxSpeed: 60, dps: 30, img: 'brute', coins: 4 },
    spitter: { size: 76, hp: 70, hpW: 15, speed: 46, speedW: 3, maxSpeed: 80, dps: 10, img: 'spitter', coins: 3 },
    goliath: { size: 190, hp: 1500, hpW: 400, speed: 30, speedW: 1, maxSpeed: 45, dps: 45, img: 'kingGoliath', coins: 30, king: true },
    brood: { size: 170, hp: 1100, hpW: 300, speed: 26, speedW: 1, maxSpeed: 40, dps: 25, img: 'kingBrood', coins: 30, king: true },
  };

  const WEAPONS = {
    pistol: { label: 'Pistol', rate: .33, dmg: 34, speed: 700, life: 1.1, spread: .01, pellets: 1, pierce: false, aoe: 0, flame: false, ammo: -1, img: 'wPistol' },
    rifle: { label: 'Machine Gun', rate: .20, dmg: 40, speed: 620, life: 1.1, spread: .045, pellets: 1, pierce: false, aoe: 0, flame: false, ammo: 150, img: 'wRifle' },
    shotgun: { label: 'Shotgun', rate: .70, dmg: 16, speed: 560, life: .5, spread: .16, pellets: 6, pierce: false, aoe: 0, flame: false, ammo: 30, img: 'wShotgun' },
    sniper: { label: 'Sniper', rate: 1.1, dmg: 150, speed: 900, life: 1.4, spread: .005, pellets: 1, pierce: true, aoe: 0, flame: false, ammo: 15, img: 'wSniper' },
    flame: { label: 'Flamethrower', rate: .05, dmg: 6, speed: 240, life: .35, spread: .22, pellets: 1, pierce: false, aoe: 0, flame: true, ammo: 200, img: 'wFlame' },
    rpg: { label: 'RPG Launcher', rate: 1.4, dmg: 90, speed: 380, life: 1.6, spread: .01, pellets: 1, pierce: false, aoe: 110, flame: false, ammo: 6, img: 'wRpg' },
  };

  const MAPS = [
    { label: 'Old Street', img: 'map', spitterBias: 0, runnerBias: 0, wrecks: [[420, 700, 15], [980, 620, -8], [1220, 980, 22], [560, 990, -18], [1350, 700, 5]] },
    { label: 'Foggy Graveyard', img: 'mapGraveyard', spitterBias: 12, runnerBias: 0, wrecks: [[300, 400, 3], [700, 1200, -6], [1100, 500, 10], [1300, 1100, -14], [520, 860, 7], [900, 900, 0]] },
    { label: 'Desert Highway', img: 'mapHighway', spitterBias: 0, runnerBias: 15, wrecks: [[380, 300, 90], [760, 640, 85], [1150, 990, 95], [1450, 1300, 88], [200, 1000, 92]] },
  ];

  const CONTROL_LABELS = ['FLOATING STICK', 'FIXED STICK', 'FOLLOW TOUCH'];

  // ---- persistent profile (localStorage) ----
  const SAVE_DEFAULTS = { coins: 0, bestWave: 0, dmgLvl: 0, spdLvl: 0, hpLvl: 0, musicVol: 6, sfxVol: 8, control: 0, wShotgun: false, wSniper: false, wFlame: false, wRpg: false };
  const SAVE = (() => {
    try { return Object.assign({}, SAVE_DEFAULTS, JSON.parse(localStorage.getItem('zs-save') || '{}')); }
    catch (e) { return Object.assign({}, SAVE_DEFAULTS); }
  })();
  function persist() { try { localStorage.setItem('zs-save', JSON.stringify(SAVE)); } catch (e) { /* private mode */ } }
  const dmgMul = () => 1 + 0.12 * SAVE.dmgLvl;
  const speedMul = () => 1 + 0.10 * SAVE.spdLvl;
  const maxHp = () => 100 + 25 * SAVE.hpLvl;
  const weaponUnlocked = (k) => k === 'pistol' || k === 'rifle' || !!SAVE['w' + k[0].toUpperCase() + k.slice(1)];

  // ---- procedural audio (WebAudio, mirrors Android Sfx.kt) ----
  let actx = null;
  function ac() {
    try {
      if (!actx) actx = new (window.AudioContext || window.webkitAudioContext)();
      if (actx.state === 'suspended') actx.resume();
      return actx;
    } catch (e) { return null; }
  }
  function tone(freq, dur, type, vol) {
    const a = ac(); if (!a || vol <= 0) return;
    try {
      const o = a.createOscillator(), g = a.createGain();
      o.type = type; o.frequency.value = freq;
      g.gain.setValueAtTime(vol, a.currentTime);
      g.gain.exponentialRampToValueAtTime(0.001, a.currentTime + dur);
      o.connect(g); g.connect(a.destination);
      o.start(); o.stop(a.currentTime + dur);
    } catch (e) { /* silent */ }
  }
  function noiseBurst(dur, vol, cutoff) {
    const a = ac(); if (!a || vol <= 0) return;
    try {
      const n = Math.floor(a.sampleRate * dur);
      const buf = a.createBuffer(1, n, a.sampleRate);
      const d = buf.getChannelData(0);
      for (let i = 0; i < n; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / n);
      const src = a.createBufferSource(); src.buffer = buf;
      const f = a.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = cutoff;
      const g = a.createGain(); g.gain.value = vol;
      src.connect(f); f.connect(g); g.connect(a.destination);
      src.start();
    } catch (e) { /* silent */ }
  }
  const sfxVol = () => SAVE.sfxVol / 10;
  function sfxShoot(wk) {
    const cut = { pistol: 1400, rifle: 1100, shotgun: 800, sniper: 600, flame: 2200, rpg: 500 }[wk] || 1000;
    noiseBurst(0.1, 0.16 * sfxVol(), cut);
  }
  function sfxKill() { tone(85 + Math.random() * 30, 0.16, 'sine', 0.22 * sfxVol()); }
  function sfxBoom() { noiseBurst(0.45, 0.4 * sfxVol(), 420); tone(55, 0.4, 'sine', 0.25 * sfxVol()); }
  // chiptune bass loop
  const MUSIC_NOTES = [55, 55, 65.41, 49, 55, 55, 82.41, 65.41];
  let musicStep = 0;
  setInterval(() => {
    if (SAVE.musicVol <= 0) return;
    const n = MUSIC_NOTES[musicStep % MUSIC_NOTES.length];
    tone(n, 0.2, 'square', 0.045 * (SAVE.musicVol / 10));
    tone(n, 0.18, 'sine', 0.03 * (SAVE.musicVol / 10));
    musicStep++;
  }, 220);

  // ---- load sprites via fetch + createImageBitmap (plain <img> loading is blocked in the viewer sandbox) ----
  const img = {};
  const keys = Object.keys(urls);
  const status = {};
  let started = false;

  async function loadAsset(k, attempt) {
    try {
      const res = await fetch(urls[k]);
      if (!res.ok) throw new Error('http ' + res.status);
      const blob = await res.blob();
      img[k] = await createImageBitmap(blob);
      status[k] = 'ok';
      if (k === 'wreck') buildWreckVariants();
    } catch (e) {
      status[k] = 'err x' + attempt + ': ' + String(e).slice(0, 60);
      if (attempt < 6) setTimeout(() => loadAsset(k, attempt + 1), 1500);
    }
  }
  keys.forEach((k) => { status[k] = 'loading'; loadAsset(k, 1); });

  // ---- state ----
  let scene = 'menu'; // menu | play | pause | shop | settings
  let mapIdx = 0;
  const hero = { x: MAP / 2, y: MAP / 2 };
  let heroAngle = 0, hp = 100;
  let monsters = [], bullets = [], globs = [], splats = [], wrecks = [], dying = [], pickups = [], explosions = [];
  let nextMonId = 1;
  let wave = 0, kills = 0, coinsRun = 0, waveDelay = 2, bannerT = 0, bannerText = '', fireTimer = 0, flashT = 0, time = 0;
  let gameOver = false, gameOverT = 0, banked = false;
  let moveId = null;
  const stickOrigin = { x: 0, y: 0 };
  const stickDelta = { x: 0, y: 0 };
  let followId = null;
  const followWorld = { x: 0, y: 0 };
  let followActive = false;
  const keysDown = new Set();
  const ammo = {}; // weaponKey -> rounds (-1 infinite)
  let weapon = 'rifle';

  // screen metrics (CSS pixels)
  let cssW = 0, cssH = 0, scale = 1, vw = 900, vh = 540;
  let camX = hero.x, camY = hero.y;

  const rnd = (a, b) => a + Math.random() * (b - a);

  function placeWrecks() {
    wrecks = MAPS[mapIdx].wrecks.map((w, i) => ({ x: w[0], y: w[1], rot: w[2], tint: i % 3 }));
  }

  function addMonster(kind, x, y) {
    const k = MON[kind];
    const hpMax = k.hp + wave * k.hpW;
    monsters.push({
      id: nextMonId++, kind, k,
      x: Math.min(Math.max(x, 50), MAP - 50),
      y: Math.min(Math.max(y, 50), MAP - 50),
      hp: hpMax, maxHp: hpMax,
      speed: Math.min(k.speed + wave * k.speedW, k.maxSpeed),
      phase: Math.random() * Math.PI * 2,
      contact: false, burnT: 0, spitT: 0, summonT: 0, slamT: 0,
    });
  }

  function spawnRegular() {
    const a = Math.random() * Math.PI * 2;
    const d = rnd(480, 680);
    const roll = Math.floor(Math.random() * 100);
    const m = MAPS[mapIdx];
    let kind = 'shambler';
    if (wave >= 4 && roll < 20 + m.spitterBias) kind = 'spitter';
    else if (wave >= 3 && roll < 35) kind = 'brute';
    else if (wave >= 2 && roll < 65 + m.runnerBias) kind = 'runner';
    addMonster(kind, hero.x + Math.cos(a) * d, hero.y + Math.sin(a) * d);
  }

  function spawnWave() {
    wave++;
    bannerT = 2.2;
    if (wave % 5 === 0) {
      bannerText = 'KING WAVE ' + wave;
      const king = (Math.floor(wave / 5) % 2 === 1) ? 'goliath' : 'brood';
      const a = Math.random() * Math.PI * 2;
      addMonster(king, hero.x + Math.cos(a) * 500, hero.y + Math.sin(a) * 500);
      for (let i = 0; i < 2 + Math.floor(wave / 2); i++) spawnRegular();
    } else {
      bannerText = 'WAVE ' + wave;
      for (let i = 0; i < 3 + wave * 2; i++) spawnRegular();
    }
  }

  function reset() {
    hero.x = MAP / 2; hero.y = MAP / 2; hp = maxHp();
    monsters = []; bullets = []; globs = []; splats = []; dying = []; pickups = []; explosions = [];
    wave = 0; kills = 0; coinsRun = 0; waveDelay = 2; bannerT = 0;
    gameOver = false; gameOverT = 0; banked = false;
    moveId = null; stickDelta.x = 0; stickDelta.y = 0;
    followId = null; followActive = false;
    for (const k of Object.keys(ammo)) delete ammo[k];
    for (const k of Object.keys(WEAPONS)) if (weaponUnlocked(k)) ammo[k] = WEAPONS[k].ammo;
    weapon = ammo.rifle !== undefined ? 'rifle' : 'pistol';
    fireTimer = 0; flashT = 0;
  }

  function bankCoins() {
    if (banked) return;
    banked = true;
    SAVE.coins += coinsRun;
    if (wave > SAVE.bestWave) SAVE.bestWave = wave;
    persist();
  }

  function cycleWeapon() {
    const owned = Object.keys(WEAPONS).filter((k) => ammo[k] !== undefined);
    if (owned.length < 2) return;
    weapon = owned[(owned.indexOf(weapon) + 1) % owned.length];
    fireTimer = 0;
  }

  function dropPickup(x, y) {
    const notOwned = Object.keys(WEAPONS).filter((k) => ammo[k] === undefined);
    let wk, am;
    if (notOwned.length) {
      wk = notOwned[Math.floor(Math.random() * notOwned.length)];
      am = WEAPONS[wk].ammo;
    } else {
      const owned = Object.keys(ammo).filter((k) => k !== 'pistol');
      wk = owned.length ? owned[Math.floor(Math.random() * owned.length)] : 'rifle';
      am = Math.max(Math.floor(WEAPONS[wk].ammo / 2), 1);
    }
    pickups.push({ x: Math.min(Math.max(x, 60), MAP - 60), y: Math.min(Math.max(y, 60), MAP - 60), weapon: wk, ammo: am, t: 0 });
  }

  function explode(x, y, radius, dmg) {
    explosions.push({ x, y, t: 0, radius });
    sfxBoom();
    for (const z of monsters) {
      if ((x - z.x) ** 2 + (y - z.y) ** 2 < radius * radius) {
        z.hp -= dmg;
        if (z.burnT < 1) z.burnT = 1;
      }
    }
  }

  // ---- wreck tinting (multiply tint, keep alpha) ----
  function tinted(source, color) {
    const c = document.createElement('canvas');
    c.width = source.width; c.height = source.height;
    const x = c.getContext('2d');
    x.drawImage(source, 0, 0);
    x.globalCompositeOperation = 'multiply';
    x.fillStyle = color;
    x.fillRect(0, 0, c.width, c.height);
    x.globalCompositeOperation = 'destination-in';
    x.drawImage(source, 0, 0);
    return c;
  }
  let wreckVariants = [null, null, null];
  function buildWreckVariants() {
    if (!spriteReady(img.wreck)) return;
    wreckVariants = [
      tinted(img.wreck, '#ff9e8c'),
      tinted(img.wreck, '#9ec2ff'),
      tinted(img.wreck, '#d9dde6'),
    ];
  }

  // ---- sizing ----
  function resize() {
    cssW = stage.clientWidth;
    const shell = scene !== 'play' && scene !== 'pause';
    cssH = Math.round(Math.min(Math.max(cssW * 0.62, shell ? 470 : 280), shell ? 660 : 560));
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    cv.style.height = cssH + 'px';
    cv.width = Math.round(cssW * dpr);
    cv.height = Math.round(cssH * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    vw = 900;
    vh = vw * cssH / cssW;
    scale = cssW / vw;
  }
  window.addEventListener('resize', resize);

  // ---- UI buttons ----
  function drawButton(b) {
    ctx.fillStyle = 'rgba(41,56,86,0.95)';
    ctx.fillRect(b.x, b.y, b.w, b.h);
    ctx.strokeStyle = '#8ca6e0'; ctx.lineWidth = 1.5;
    ctx.strokeRect(b.x, b.y, b.w, b.h);
    ctx.fillStyle = '#fff';
    ctx.font = '600 ' + (b.fs || 17) + 'px system-ui, sans-serif';
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    ctx.fillText(b.label, b.x + b.w / 2, b.y + b.h / 2);
    ctx.textAlign = 'left';
  }
  function drawButtons(list) { for (const b of list) drawButton(b); }
  const hit = (b, p) => p.x >= b.x && p.x <= b.x + b.w && p.y >= b.y && p.y <= b.y + b.h;

  function menuButtons() {
    const btnW = Math.min(300, cssW * 0.68), bx = cssW / 2 - btnW / 2;
    const pw = Math.min(cssW * 0.52, 280, cssH * 0.34 / 0.66), ph = pw * 0.66;
    const px = cssW / 2 - pw / 2;
    const labelY = cssH * 0.30;
    const py = labelY + 12;
    const btnH = 46, gap = 10;
    const playY = Math.min(py + ph + 16, cssH - 3 * btnH - 2 * gap - 14);
    return {
      pw, ph, px, py, labelY,
      list: [
        { x: bx, y: playY, w: btnW, h: btnH, label: 'PLAY', id: 'play' },
        { x: bx, y: playY + btnH + gap, w: btnW, h: btnH, label: 'SHOP', id: 'shop' },
        { x: bx, y: playY + 2 * (btnH + gap), w: btnW, h: btnH, label: 'SETTINGS', id: 'settings' },
        { x: Math.max(px - 52, 6), y: py + ph / 2 - 20, w: 40, h: 40, label: '<', id: 'prev' },
        { x: Math.min(px + pw + 12, cssW - 46), y: py + ph / 2 - 20, w: 40, h: 40, label: '>', id: 'next' },
      ],
    };
  }

  const UPGRADE_PRICES = { dmg: [50, 120, 260], spd: [40, 100, 220], hp: [60, 130, 280] };
  const SHOP_WEAPONS = ['shotgun', 'sniper', 'flame', 'rpg'];
  const WEAPON_PRICES = { shotgun: 80, sniper: 120, flame: 150, rpg: 200 };

  function shopButtons() {
    const rows = 7, rowH = 40, rowGap = 8;
    const buyW = 96, buyX = cssW - buyW - 14;
    const topY = 128;
    const list = [{ x: 12, y: cssH - 54, w: 120, h: 42, label: '< BACK', id: 'back' }];
    for (let i = 0; i < rows; i++) list.push({ x: buyX, y: topY + i * (rowH + rowGap), w: buyW, h: rowH, label: 'buy', id: 'row' + i, fs: 14 });
    return { list, rows, rowH, rowGap, topY, buyW };
  }

  function settingsButtons() {
    const rowH = 44, gap = 14, topY = cssH * 0.42;
    const minusX = cssW / 2 + 40, plusX = cssW - 80;
    const list = [{ x: 12, y: cssH - 54, w: 120, h: 42, label: '< BACK', id: 'back' }];
    for (let i = 0; i < 3; i++) {
      list.push({ x: minusX, y: topY + i * (rowH + gap), w: 44, h: rowH, label: '-', id: 'minus' + i });
      list.push({ x: plusX, y: topY + i * (rowH + gap), w: 44, h: rowH, label: '+', id: 'plus' + i });
    }
    return { list, topY, rowH, gap };
  }

  function pauseButtons() {
    const bw = Math.min(300, cssW * 0.68), bx = cssW / 2 - bw / 2;
    return [
      { x: bx, y: cssH / 2 - 10, w: bw, h: 48, label: 'RESUME', id: 'resume' },
      { x: bx, y: cssH / 2 + 52, w: bw, h: 48, label: 'RESTART', id: 'restart' },
      { x: bx, y: cssH / 2 + 114, w: bw, h: 48, label: 'QUIT TO MENU', id: 'quit' },
    ];
  }

  // ---- input ----
  function pointPos(e) {
    const r = cv.getBoundingClientRect();
    return { x: e.clientX - r.left, y: e.clientY - r.top };
  }
  cv.addEventListener('pointerdown', (e) => {
    e.preventDefault();
    ac(); // unlock audio on first gesture
    const p = pointPos(e);

    if (scene === 'menu') {
      const m = menuButtons();
      for (const b of m.list) {
        if (!hit(b, p)) continue;
        if (b.id === 'play') { scene = 'play'; reset(); placeWrecks(); return; }
        if (b.id === 'shop') { scene = 'shop'; return; }
        if (b.id === 'settings') { scene = 'settings'; return; }
        if (b.id === 'prev') { mapIdx = (mapIdx + MAPS.length - 1) % MAPS.length; return; }
        if (b.id === 'next') { mapIdx = (mapIdx + 1) % MAPS.length; return; }
      }
      return;
    }
    if (scene === 'shop') {
      const s = shopButtons();
      for (const b of s.list) {
        if (!hit(b, p)) continue;
        if (b.id === 'back') { scene = 'menu'; return; }
        buyRow(parseInt(b.id.slice(3), 10));
        return;
      }
      return;
    }
    if (scene === 'settings') {
      const s = settingsButtons();
      for (const b of s.list) {
        if (!hit(b, p)) continue;
        if (b.id === 'back') { scene = 'menu'; return; }
        adjust(b.id.startsWith('minus') ? parseInt(b.id.slice(5), 10) : parseInt(b.id.slice(4), 10), b.id.startsWith('minus') ? -1 : 1);
        return;
      }
      return;
    }
    if (scene === 'pause') {
      for (const b of pauseButtons()) {
        if (!hit(b, p)) continue;
        if (b.id === 'resume') scene = 'play';
        else if (b.id === 'restart') { bankCoins(); reset(); placeWrecks(); scene = 'play'; }
        else if (b.id === 'quit') { bankCoins(); scene = 'menu'; }
        return;
      }
      return;
    }

    // scene === 'play'
    if (gameOver) {
      bankCoins();
      if (gameOverT > 0.9) { scene = 'menu'; }
      return;
    }
    // pause button (top-right)
    if (p.x > cssW - 60 && p.y < 60) { scene = 'pause'; return; }
    // weapon HUD strip cycles
    if (p.y < 70 && Math.abs(p.x - cssW / 2) < 170 && p.x < cssW - 70) { cycleWeapon(); return; }
    if (SAVE.control === 2) {
      followId = e.pointerId;
      setFollow(p);
      try { cv.setPointerCapture(e.pointerId); } catch (err) { /* synthetic pointers */ }
      return;
    }
    if (moveId === null && (SAVE.control === 1 ? (Math.hypot(p.x - 110, p.y - (cssH - 100)) < 160) : p.x < cssW / 2)) {
      moveId = e.pointerId;
      if (SAVE.control === 1) { stickOrigin.x = 110; stickOrigin.y = cssH - 100; }
      else { stickOrigin.x = p.x; stickOrigin.y = p.y; }
      stickDelta.x = 0; stickDelta.y = 0;
      try { cv.setPointerCapture(e.pointerId); } catch (err) { /* synthetic pointers */ }
    }
  });

  function setFollow(p) {
    followWorld.x = p.x / scale + camX - vw / 2;
    followWorld.y = camY + vh / 2 - p.y / scale;
    followActive = true;
  }

  cv.addEventListener('pointermove', (e) => {
    const p = pointPos(e);
    if (e.pointerId === followId) { setFollow(p); return; }
    if (e.pointerId !== moveId) return;
    let dx = p.x - stickOrigin.x, dy = p.y - stickOrigin.y;
    const len = Math.hypot(dx, dy);
    if (len > STICK_R) { dx = dx / len * STICK_R; dy = dy / len * STICK_R; }
    stickDelta.x = dx; stickDelta.y = dy;
  });
  function release(e) {
    if (e.pointerId === followId) { followId = null; followActive = false; }
    if (e.pointerId !== moveId) return;
    moveId = null; stickDelta.x = 0; stickDelta.y = 0;
  }
  cv.addEventListener('pointerup', release);
  cv.addEventListener('pointercancel', release);

  window.addEventListener('keydown', (e) => {
    keysDown.add(e.key.length === 1 ? e.key.toLowerCase() : e.key);
    if (e.key === 'Escape' || e.key === 'p' || e.key === 'P') {
      if (scene === 'play' && !gameOver) { scene = 'pause'; return; }
      if (scene === 'pause') { scene = 'play'; return; }
      if (scene === 'shop' || scene === 'settings') { scene = 'menu'; return; }
    }
    if (scene !== 'play' || gameOver) return;
    if (e.key === 'q' || e.key === 'Q') { cycleWeapon(); return; }
    const n = parseInt(e.key, 10);
    if (n >= 1 && n <= 6) {
      const owned = Object.keys(WEAPONS).filter((k) => ammo[k] !== undefined);
      if (n <= owned.length) { weapon = owned[n - 1]; fireTimer = 0; }
    }
  });
  window.addEventListener('keyup', (e) => keysDown.delete(e.key.length === 1 ? e.key.toLowerCase() : e.key));

  function buyRow(i) {
    if (i < 3) {
      const key = ['dmg', 'spd', 'hp'][i];
      const lvlKey = key + 'Lvl';
      const lvl = SAVE[lvlKey];
      if (lvl >= 3) return;
      const price = UPGRADE_PRICES[key][lvl];
      if (SAVE.coins < price) return;
      SAVE.coins -= price;
      SAVE[lvlKey] = lvl + 1;
    } else {
      const wk = SHOP_WEAPONS[i - 3];
      const flag = 'w' + wk[0].toUpperCase() + wk.slice(1);
      if (SAVE[flag]) return;
      const price = WEAPON_PRICES[wk];
      if (SAVE.coins < price) return;
      SAVE.coins -= price;
      SAVE[flag] = true;
    }
    persist();
    sfxKill();
  }

  function adjust(row, dir) {
    if (row === 0) SAVE.musicVol = Math.min(Math.max(SAVE.musicVol + dir, 0), 10);
    else if (row === 1) { SAVE.sfxVol = Math.min(Math.max(SAVE.sfxVol + dir, 0), 10); sfxKill(); }
    else SAVE.control = (SAVE.control + dir + 3) % 3;
    persist();
  }

  // ---- world<->screen ----
  const sx = (wx) => (wx - camX + vw / 2) * scale;
  const sy = (wy) => (camY + vh / 2 - wy) * scale;

  function spriteReady(im) {
    if (!im) return false;
    if (im.tagName === 'IMG') return im.complete && im.naturalWidth > 0;
    return im.width > 0 && im.height > 0;
  }

  function drawSprite(im, wx, wy, w, h, angleDeg, alpha) {
    if (!spriteReady(im)) return;
    ctx.save();
    if (alpha !== undefined && alpha < 1) ctx.globalAlpha = Math.max(alpha, 0);
    ctx.translate(sx(wx), sy(wy));
    ctx.rotate(-angleDeg * Math.PI / 180);
    ctx.drawImage(im, -w * scale / 2, -h * scale / 2, w * scale, h * scale);
    ctx.restore();
  }

  function drawSpriteFrame(im, frame, cellW, cellH, wx, wy, w, h, angleDeg, alpha) {
    if (!spriteReady(im)) return;
    ctx.save();
    if (alpha !== undefined && alpha < 1) ctx.globalAlpha = Math.max(alpha, 0);
    ctx.translate(sx(wx), sy(wy));
    ctx.rotate(-angleDeg * Math.PI / 180);
    ctx.drawImage(im, frame * cellW, 0, cellW, cellH, -w * scale / 2, -h * scale / 2, w * scale, h * scale);
    ctx.restore();
  }

  function monAngle(z) { return Math.atan2(hero.y - z.y, hero.x - z.x) * 180 / Math.PI; }

  // ---- update ----
  function update(dt) {
    let mx = 0, my = 0;
    if (SAVE.control === 2) {
      if (followActive) {
        const dx = followWorld.x - hero.x, dy = followWorld.y - hero.y;
        const d = Math.hypot(dx, dy);
        if (d > 14) { mx = dx / d; my = dy / d; } else followActive = false;
      }
    } else if (stickDelta.x * stickDelta.x + stickDelta.y * stickDelta.y > 25) {
      mx = stickDelta.x / STICK_R;
      my = -stickDelta.y / STICK_R;
    }
    if (keysDown.has('a') || keysDown.has('ArrowLeft')) mx -= 1;
    if (keysDown.has('d') || keysDown.has('ArrowRight')) mx += 1;
    if (keysDown.has('w') || keysDown.has('ArrowUp')) my += 1;
    if (keysDown.has('s') || keysDown.has('ArrowDown')) my -= 1;
    const moving = mx !== 0 || my !== 0;
    if (moving) {
      const len = Math.max(Math.hypot(mx, my), 0.001);
      const sp = HERO_SPEED * speedMul();
      hero.x += mx / len * sp * dt * Math.min(len, 1);
      hero.y += my / len * sp * dt * Math.min(len, 1);
      hero.x = Math.min(Math.max(hero.x, 40), MAP - 40);
      hero.y = Math.min(Math.max(hero.y, 40), MAP - 40);
    }
    if (flashT > 0) flashT -= dt;

    // auto-fire at nearest monster
    let target = null, best = AIM_RANGE * AIM_RANGE;
    for (const z of monsters) {
      const d2 = (hero.x - z.x) ** 2 + (hero.y - z.y) ** 2;
      if (d2 < best) { best = d2; target = z }
    }
    if (target) {
      heroAngle = Math.atan2(target.y - hero.y, target.x - hero.x) * 180 / Math.PI;
      fireTimer -= dt;
      const w = WEAPONS[weapon];
      if (fireTimer <= 0 && (ammo[weapon] === -1 || ammo[weapon] > 0)) {
        fireTimer = w.rate;
        const base = Math.atan2(target.y - hero.y, target.x - hero.x);
        const dmg = Math.max(Math.round(w.dmg * dmgMul()), 1);
        for (let i = 0; i < w.pellets; i++) {
          const a = base + rnd(-w.spread, w.spread);
          bullets.push({
            x: hero.x + Math.cos(a) * 40, y: hero.y + Math.sin(a) * 40,
            vx: Math.cos(a) * w.speed, vy: Math.sin(a) * w.speed, life: w.life,
            dmg, pierce: w.pierce, aoe: w.aoe, flame: w.flame, hitIds: new Set(),
          });
        }
        flashT = 0.06;
        sfxShoot(weapon);
        if (ammo[weapon] !== -1) {
          ammo[weapon] -= 1;
          if (ammo[weapon] <= 0) { delete ammo[weapon]; weapon = 'pistol'; }
        }
      }
    } else if (moving) {
      heroAngle = Math.atan2(my, mx) * 180 / Math.PI;
    }

    // bullets
    for (let i = bullets.length - 1; i >= 0; i--) {
      const b = bullets[i];
      b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt;
      if (b.life <= 0) {
        if (b.aoe > 0) explode(b.x, b.y, b.aoe, b.dmg);
        bullets.splice(i, 1);
      }
    }

    // enemy globs
    for (let i = globs.length - 1; i >= 0; i--) {
      const g = globs[i];
      g.x += g.vx * dt; g.y += g.vy * dt; g.life -= dt;
      if ((g.x - hero.x) ** 2 + (g.y - hero.y) ** 2 < 24 * 24) { hp -= g.dmg; globs.splice(i, 1); }
      else if (g.life <= 0) globs.splice(i, 1);
    }

    // monsters: AI + abilities + damage
    for (let i = monsters.length - 1; i >= 0; i--) {
      const z = monsters[i];
      const dx = hero.x - z.x, dy = hero.y - z.y;
      const d = Math.max(Math.hypot(dx, dy), 0.001);

      if (z.burnT > 0) { z.burnT -= dt; z.hp -= 8 * dt; }

      z.contact = d <= 34;
      if (z.kind === 'spitter') {
        if (d > 260) { z.x += dx / d * z.speed * dt; z.y += dy / d * z.speed * dt; }
        else if (d < 180) { z.x -= dx / d * z.speed * 0.7 * dt; z.y -= dy / d * z.speed * 0.7 * dt; }
        z.spitT -= dt;
        if (z.spitT <= 0 && d < 420) {
          z.spitT = 2.2;
          const a = Math.atan2(dy, dx);
          globs.push({ x: z.x + Math.cos(a) * 30, y: z.y + Math.sin(a) * 30, vx: Math.cos(a) * 260, vy: Math.sin(a) * 260, life: 2, dmg: 12 });
        }
      } else if (z.kind === 'goliath') {
        if (d > 34) { z.x += dx / d * z.speed * dt; z.y += dy / d * z.speed * dt; } else hp -= z.k.dps * dt;
        z.slamT -= dt;
        if (z.slamT <= 0 && d < 170) {
          z.slamT = 4;
          explosions.push({ x: z.x, y: z.y, t: 0, radius: 200 });
          sfxBoom();
          if (d < 200) hp -= 30;
        }
        z.summonT -= dt;
        if (z.summonT <= 0) {
          z.summonT = 8;
          if (monsters.filter((m) => !m.k.king).length < 6) {
            for (let s = 0; s < 2; s++) addMonster('shambler', z.x + rnd(-90, 90), z.y + rnd(-90, 90));
          }
        }
      } else if (z.kind === 'brood') {
        if (d > 120) { z.x += dx / d * z.speed * dt; z.y += dy / d * z.speed * dt; } else if (d <= 34) hp -= z.k.dps * dt;
        z.summonT -= dt;
        if (z.summonT <= 0) {
          z.summonT = 3;
          if (monsters.filter((m) => !m.k.king).length < 8) addMonster('runner', z.x + rnd(-70, 70), z.y + rnd(-70, 70));
        }
        z.spitT -= dt;
        if (z.spitT <= 0 && d < 430) {
          z.spitT = 3.5;
          for (let k = -1; k <= 1; k++) {
            const a = Math.atan2(dy, dx) + k * 0.2;
            globs.push({ x: z.x + Math.cos(a) * 60, y: z.y + Math.sin(a) * 60, vx: Math.cos(a) * 240, vy: Math.sin(a) * 240, life: 2.2, dmg: 10 });
          }
        }
      } else {
        if (d > 34) { z.x += dx / d * z.speed * dt; z.y += dy / d * z.speed * dt; }
        else hp -= z.k.dps * dt;
      }

      // bullet hits
      for (let j = bullets.length - 1; j >= 0; j--) {
        const b = bullets[j];
        if (b.hitIds.has(z.id)) continue;
        if ((b.x - z.x) ** 2 + (b.y - z.y) ** 2 < 30 * 30) {
          if (b.aoe > 0) {
            explode(b.x, b.y, b.aoe, b.dmg);
            bullets.splice(j, 1);
          } else {
            z.hp -= b.dmg;
            if (b.flame) z.burnT = 2;
            b.hitIds.add(z.id);
            if (!b.pierce) bullets.splice(j, 1);
          }
        }
      }

      if (z.hp <= 0) {
        splats.push({ x: z.x, y: z.y, rot: Math.random() * 360, t: 0, fr: Math.floor(Math.random() * 3) });
        dying.push({ x: z.x, y: z.y, ang: monAngle(z), t: 0, kind: z.kind, size: z.k.size });
        monsters.splice(i, 1);
        kills++;
        coinsRun += z.k.coins;
        sfxKill();
        if (z.k.king) { dropPickup(z.x + rnd(-50, 50), z.y + rnd(-50, 50)); dropPickup(z.x + rnd(-50, 50), z.y + rnd(-50, 50)); }
        else if (Math.random() < 0.18) dropPickup(z.x, z.y);
      }
    }

    // separation
    for (let a = 0; a < monsters.length; a++) {
      for (let b = a + 1; b < monsters.length; b++) {
        const za = monsters[a], zb = monsters[b];
        const ddx = zb.x - za.x, ddy = zb.y - za.y;
        const dd = ddx * ddx + ddy * ddy;
        if (dd < 36 * 36 && dd > 0.01) {
          const d = Math.sqrt(dd);
          const push = (36 - d) * 0.5;
          const nx = ddx / d, ny = ddy / d;
          za.x -= nx * push; za.y -= ny * push;
          zb.x += nx * push; zb.y += ny * push;
        }
      }
    }

    // pickups
    for (let i = pickups.length - 1; i >= 0; i--) {
      const p = pickups[i];
      p.t += dt;
      if ((p.x - hero.x) ** 2 + (p.y - hero.y) ** 2 < 44 * 44) {
        if (ammo[p.weapon] !== undefined && ammo[p.weapon] !== -1) ammo[p.weapon] += p.ammo;
        else if (ammo[p.weapon] === undefined) ammo[p.weapon] = p.ammo;
        weapon = p.weapon;
        fireTimer = 0;
        pickups.splice(i, 1);
      }
    }

    // aging
    for (let i = splats.length - 1; i >= 0; i--) { splats[i].t += dt; if (splats[i].t > 14) splats.splice(i, 1); }
    for (let i = dying.length - 1; i >= 0; i--) { dying[i].t += dt; if (dying[i].t > 0.6) dying.splice(i, 1); }
    for (let i = explosions.length - 1; i >= 0; i--) { explosions[i].t += dt; if (explosions[i].t > 0.35) explosions.splice(i, 1); }

    if (bannerT > 0) bannerT -= dt;

    if (monsters.length === 0) {
      waveDelay -= dt;
      if (waveDelay <= 0) { spawnWave(); waveDelay = 3; }
    }

    if (hp <= 0) { hp = 0; gameOver = true; gameOverT = 0; }
  }

  // ---- HUD helpers ----
  function capsule(x, y, w, h, fill) {
    const r = h / 2;
    ctx.fillStyle = 'rgba(10,16,12,0.92)';
    ctx.beginPath();
    ctx.arc(x + r, y + r, r, Math.PI / 2, Math.PI * 1.5);
    ctx.arc(x + w - r, y + r, r, -Math.PI / 2, Math.PI / 2);
    ctx.closePath(); ctx.fill();
    const fw = Math.max(w * fill, 0.01);
    ctx.fillStyle = '#59e640';
    ctx.beginPath();
    if (fw <= r) {
      ctx.arc(x + r, y + r, Math.min(fw, r), 0, Math.PI * 2);
    } else {
      const re = fw > w - r ? w - r : fw;
      ctx.arc(x + r, y + r, r, Math.PI / 2, Math.PI * 1.5);
      ctx.rect(x + r, y, re - r, h);
      ctx.arc(x + re, y + r, r, -Math.PI / 2, Math.PI / 2);
    }
    ctx.closePath(); ctx.fill();
  }

  function drawHud() {
    // portrait (skip until decoded)
    if (spriteReady(img.portrait)) {
      ctx.save();
      ctx.beginPath(); ctx.arc(43, 35, 29, 0, Math.PI * 2); ctx.clip();
      ctx.drawImage(img.portrait, 14, 6, 58, 58);
      ctx.restore();
    }
    ctx.strokeStyle = 'rgba(255,255,255,0.35)'; ctx.lineWidth = 2;
    ctx.beginPath(); ctx.arc(43, 35, 29, 0, Math.PI * 2); ctx.stroke();

    capsule(86, 24, 200, 22, hp / maxHp());
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 15px system-ui, sans-serif';
    ctx.textAlign = 'left'; ctx.textBaseline = 'middle';
    ctx.fillText('HP ' + Math.round(hp), 96, 36);

    ctx.textAlign = 'right';
    ctx.font = 'bold 16px system-ui, sans-serif';
    ctx.fillText('WAVE ' + wave, cssW - 76, 24);
    ctx.fillText('KILLS ' + kills, cssW - 76, 46);
    ctx.fillStyle = '#ffd94d';
    ctx.fillText('COINS ' + coinsRun, cssW - 76, 68);
    ctx.fillStyle = '#fff';
    ctx.textAlign = 'left';

    // pause button (top-right)
    ctx.fillStyle = 'rgba(41,56,86,0.9)';
    ctx.fillRect(cssW - 54, 10, 44, 44);
    ctx.strokeStyle = '#8ca6e0'; ctx.lineWidth = 1.5;
    ctx.strokeRect(cssW - 54, 10, 44, 44);
    ctx.fillStyle = '#e8eaf2';
    ctx.fillRect(cssW - 41, 21, 5, 22);
    ctx.fillRect(cssW - 31, 21, 5, 22);

    // weapon HUD: owned icons centered top, current highlighted, label + ammo under
    const owned = Object.keys(WEAPONS).filter((k) => ammo[k] !== undefined);
    const iconSize = 30, gap = 8;
    const totalW = owned.length * (iconSize + gap) - gap;
    let ix = cssW / 2 - totalW / 2;
    for (const k of owned) {
      if (k === weapon) {
        ctx.fillStyle = 'rgba(255,217,77,0.35)';
        ctx.fillRect(ix - 3, 9, iconSize + 6, iconSize + 6);
      }
      if (spriteReady(img[WEAPONS[k].img])) ctx.drawImage(img[WEAPONS[k].img], ix, 12, iconSize, iconSize);
      ix += iconSize + gap;
    }
    const w = WEAPONS[weapon];
    const ammoTxt = ammo[weapon] === -1 ? '∞' : String(ammo[weapon]);
    ctx.textAlign = 'center';
    ctx.font = 'bold 14px system-ui, sans-serif';
    ctx.fillStyle = '#e8eaf2';
    ctx.fillText(w.label + '  ' + ammoTxt, cssW / 2, 56);
    ctx.textAlign = 'left';

    // joystick visuals (stick modes only)
    if (SAVE.control !== 2) {
      ctx.strokeStyle = 'rgba(255,255,255,0.14)'; ctx.lineWidth = 3;
      ctx.beginPath(); ctx.arc(110, cssH - 100, 62, 0, Math.PI * 2); ctx.stroke();
      ctx.fillStyle = 'rgba(255,255,255,0.10)';
      ctx.beginPath(); ctx.arc(110, cssH - 100, 26, 0, Math.PI * 2); ctx.fill();
      if (moveId !== null) {
        ctx.fillStyle = 'rgba(255,255,255,0.12)';
        ctx.beginPath(); ctx.arc(stickOrigin.x, stickOrigin.y, STICK_R, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = 'rgba(255,255,255,0.30)';
        ctx.beginPath(); ctx.arc(stickOrigin.x + stickDelta.x, stickOrigin.y + stickDelta.y, 26, 0, Math.PI * 2); ctx.fill();
      }
    }

    if (bannerT > 0) {
      ctx.globalAlpha = Math.min(Math.max(bannerT / 2.2, 0), 1);
      ctx.fillStyle = '#ffd94d';
      ctx.font = '800 46px system-ui, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(bannerText, cssW / 2, cssH / 2 - 40);
      ctx.textAlign = 'left';
      ctx.globalAlpha = 1;
    }

    if (gameOver) {
      ctx.fillStyle = 'rgba(5,6,12,0.72)';
      ctx.fillRect(0, 0, cssW, cssH);
      ctx.textAlign = 'center';
      ctx.fillStyle = '#f2404d';
      ctx.font = '800 44px system-ui, sans-serif';
      ctx.fillText('GAME OVER', cssW / 2, cssH / 2 - 50);
      ctx.fillStyle = '#e8eaf2';
      ctx.font = '600 18px system-ui, sans-serif';
      ctx.fillText('Waves survived: ' + wave + '   Kills: ' + kills, cssW / 2, cssH / 2 - 6);
      ctx.fillStyle = '#ffd94d';
      ctx.fillText('Coins earned: +' + coinsRun, cssW / 2, cssH / 2 + 24);
      if (gameOverT > 0.9) {
        ctx.fillStyle = '#8a90ad';
        ctx.font = '500 15px system-ui, sans-serif';
        ctx.fillText('Tap anywhere to continue', cssW / 2, cssH / 2 + 58);
      }
      ctx.textAlign = 'left';
    }
  }

  // ---- render ----
  function render() {
    camX = Math.min(Math.max(hero.x, vw / 2 - 100), MAP - vw / 2 + 100);
    camY = Math.min(Math.max(hero.y, vh / 2 - 100), MAP - vh / 2 + 100);

    ctx.fillStyle = '#0d0f1a';
    ctx.fillRect(0, 0, cssW, cssH);

    const mapImg = img[MAPS[mapIdx].img];
    if (spriteReady(mapImg)) {
      ctx.drawImage(mapImg, sx(0), sy(MAP), MAP * scale, MAP * scale);
    }

    for (const s of splats) {
      if (spriteReady(img.splat) && img.splat.width > img.splat.height) {
        const cell = img.splat.height;
        const frames = Math.max(Math.floor(img.splat.width / cell), 1);
        drawSpriteFrame(img.splat, (s.fr || 0) % frames, cell, cell, s.x, s.y, 60, 60, s.rot, 1 - s.t / 14);
      } else {
        drawSprite(img.splat, s.x, s.y, 60, 60, s.rot, 1 - s.t / 14);
      }
    }
    for (const w of wrecks) drawSprite(wreckVariants[w.tint], w.x, w.y, WRECK_W, WRECK_H, w.rot);

    // pickups (crate + floating weapon icon)
    for (const p of pickups) {
      const bobY = Math.sin(time * 3 + p.t) * 5;
      drawSprite(img.pickup, p.x, p.y, 48, 48, 0);
      drawSprite(img[WEAPONS[p.weapon].img], p.x, p.y + 34 + bobY, 36, 36, 0);
    }

    // bullets (look varies by weapon behavior)
    for (const b of bullets) {
      const ang = Math.atan2(b.vy, b.vx);
      ctx.save();
      ctx.translate(sx(b.x), sy(b.y));
      ctx.rotate(-ang);
      if (b.flame) {
        ctx.fillStyle = 'rgba(255,140,30,0.85)';
        ctx.beginPath(); ctx.arc(0, 0, 10 * scale, 0, Math.PI * 2); ctx.fill();
      } else if (b.aoe > 0) {
        ctx.fillStyle = '#a6e680';
        ctx.fillRect(-12 * scale, -6 * scale, 24 * scale, 12 * scale);
      } else if (b.pierce) {
        ctx.fillStyle = '#8ce6ff';
        ctx.fillRect(-16 * scale, -2 * scale, 32 * scale, 4 * scale);
      } else {
        ctx.fillStyle = '#ffb32b';
        ctx.fillRect(-8 * scale, -3 * scale, 16 * scale, 6 * scale);
        ctx.fillStyle = '#fff3d9';
        ctx.fillRect(2 * scale, -1.5 * scale, 6 * scale, 3 * scale);
      }
      ctx.restore();
    }

    // enemy globs
    for (const g of globs) {
      ctx.fillStyle = 'rgba(166,255,77,0.95)';
      ctx.beginPath(); ctx.arc(sx(g.x), sy(g.y), 9 * scale, 0, Math.PI * 2); ctx.fill();
    }

    // explosions (expanding ring)
    for (const e of explosions) {
      const r = e.radius * Math.min(e.t / 0.35, 1);
      ctx.strokeStyle = 'rgba(255,178,51,' + Math.max(1 - e.t / 0.35, 0) + ')';
      ctx.lineWidth = Math.max(3, 8 * scale);
      ctx.beginPath(); ctx.arc(sx(e.x), sy(e.y), r * scale, 0, Math.PI * 2); ctx.stroke();
    }

    // dying monsters (one-shot death anim, then the splat remains)
    for (const d of dying) {
      const sheet = img.zombieDeath;
      if (spriteReady(sheet) && sheet.width > sheet.height) {
        const cell = sheet.height;
        const frames = Math.max(Math.floor(sheet.width / cell), 1);
        const f = Math.min(frames - 1, Math.floor(d.t * 8));
        drawSpriteFrame(sheet, f, cell, cell, d.x, d.y, d.size, d.size, d.ang);
      }
    }

    // monsters
    for (const z of monsters) {
      const ang = monAngle(z);
      const size = z.k.size;
      const burning = z.burnT > 0;
      if (z.kind === 'shambler') {
        const sheet = (z.contact && spriteReady(img.zombieAttack)) ? img.zombieAttack : img.zombie;
        if (spriteReady(sheet) && sheet.width > sheet.height) {
          const cell = sheet.height;
          const frames = Math.max(Math.floor(sheet.width / cell), 1);
          const f = Math.floor(time * (z.contact ? 8 : 10) + z.phase) % frames;
          if (burning) ctx.filter = 'sepia(1) saturate(3) hue-rotate(-30deg)';
          drawSpriteFrame(sheet, f, cell, cell, z.x, z.y, size, size, ang);
          ctx.filter = 'none';
        }
      } else {
        const wob = Math.sin(time * 3.2 + z.phase) * (z.k.king ? 3 : 7);
        if (burning) ctx.filter = 'sepia(1) saturate(3) hue-rotate(-30deg)';
        drawSprite(img[z.k.img], z.x, z.y, size, size, ang + wob);
        ctx.filter = 'none';
      }
      // king HP bar
      if (z.k.king) {
        const bw = size * 0.9;
        ctx.fillStyle = 'rgba(50,10,10,0.9)';
        ctx.fillRect(sx(z.x) - bw * scale / 2, sy(z.y) - (size / 2 + 14) * scale, bw * scale, 6 * scale);
        ctx.fillStyle = '#e62626';
        ctx.fillRect(sx(z.x) - bw * scale / 2, sy(z.y) - (size / 2 + 14) * scale, bw * scale * Math.max(z.hp / z.maxHp, 0), 6 * scale);
      }
    }

    const moving = SAVE.control === 2 ? followActive : (stickDelta.x * stickDelta.x + stickDelta.y * stickDelta.y > 25) || keysDown.size > 0;
    const bob = moving ? Math.sin(time * 12) * 4.5 : 0;
    // pro sheet set: walk while moving, fire while shooting, idle otherwise; static sprite as fallback
    let sheet = img.hero, fps = 10;
    if (!moving && flashT > 0 && spriteReady(img.heroFire)) { sheet = img.heroFire; fps = 12; }
    else if (!moving && spriteReady(img.heroIdle)) { sheet = img.heroIdle; fps = 5; }
    if (spriteReady(sheet) && sheet.width > sheet.height) {
      const cell = sheet.height;
      const frames = Math.max(Math.floor(sheet.width / cell), 1);
      const f = Math.floor(time * fps) % frames;
      drawSpriteFrame(sheet, f, cell, cell, hero.x, hero.y, HERO_SIZE, HERO_SIZE, heroAngle + bob);
    } else {
      drawSprite(sheet, hero.x, hero.y, HERO_SIZE, HERO_SIZE, heroAngle + bob);
    }

    if (flashT > 0 && spriteReady(img.flash)) {
      const a = heroAngle * Math.PI / 180;
      const fs = 52 * (0.6 + 0.4 * flashT / 0.06);
      const alpha = flashT / 0.06;
      const fx = hero.x + Math.cos(a) * MUZZLE_DIST, fy = hero.y + Math.sin(a) * MUZZLE_DIST;
      if (img.flash.width > img.flash.height) {
        const cell = img.flash.height;
        const frames = Math.max(Math.floor(img.flash.width / cell), 1);
        const ff = Math.min(frames - 1, Math.floor((0.06 - flashT) / 0.06 * frames));
        drawSpriteFrame(img.flash, ff, cell, cell, fx, fy, fs, fs, heroAngle, alpha);
      } else {
        drawSprite(img.flash, fx, fy, fs, fs * (img.flash.height / img.flash.width || 1), heroAngle, alpha);
      }
    }

    drawHud();
  }

  // ---- shell screens ----
  function renderMenu() {
    ctx.fillStyle = '#0d0f1a';
    ctx.fillRect(0, 0, cssW, cssH);
    const m = menuButtons();

    ctx.textAlign = 'center';
    ctx.fillStyle = '#f2d13f';
    ctx.font = '800 ' + Math.min(40, cssW * 0.085) + 'px system-ui, sans-serif';
    ctx.fillText('ZOMBIE SURVIVOR', cssW / 2, cssH * 0.16);
    ctx.fillStyle = '#8a90ad';
    ctx.font = '500 14px system-ui, sans-serif';
    ctx.fillText('Vexolabs', cssW / 2, cssH * 0.16 + 22);

    ctx.fillStyle = '#e8eaf2';
    ctx.font = '600 17px system-ui, sans-serif';
    ctx.fillText(MAPS[mapIdx].label, cssW / 2, m.labelY);
    const mi = img[MAPS[mapIdx].img];
    if (spriteReady(mi)) {
      ctx.save();
      ctx.strokeStyle = '#8ca6e0'; ctx.lineWidth = 1.5;
      ctx.strokeRect(m.px - 1, m.py - 1, m.pw + 2, m.ph + 2);
      ctx.drawImage(mi, m.px, m.py, m.pw, m.ph);
      ctx.restore();
    }
    ctx.textAlign = 'left';
    ctx.fillStyle = '#ffd94d';
    ctx.font = '600 14px system-ui, sans-serif';
    ctx.fillText('COINS: ' + SAVE.coins + '    BEST WAVE: ' + SAVE.bestWave, 12, 24);
    drawButtons(m.list);
  }

  function renderShop() {
    ctx.fillStyle = '#0d0f1a';
    ctx.fillRect(0, 0, cssW, cssH);
    const s = shopButtons();

    ctx.textAlign = 'center';
    ctx.fillStyle = '#f2d13f';
    ctx.font = '800 30px system-ui, sans-serif';
    ctx.fillText('SHOP', cssW / 2, 48);
    ctx.fillStyle = '#ffd94d';
    ctx.font = '600 16px system-ui, sans-serif';
    ctx.fillText('COINS: ' + SAVE.coins, cssW / 2, 76);
    ctx.textAlign = 'left';

    const labels = ['Damage +12%', 'Move speed +10%', 'Max HP +25'];
    const lvls = [SAVE.dmgLvl, SAVE.spdLvl, SAVE.hpLvl];
    for (let i = 0; i < s.rows; i++) {
      const y = s.topY + i * (s.rowH + s.rowGap);
      if (i < 3) {
        ctx.fillStyle = '#e8eaf2';
        ctx.font = '600 14px system-ui, sans-serif';
        ctx.fillText(labels[i], 16, y + 14);
        let pips = '';
        for (let p = 0; p < 3; p++) pips += p < lvls[i] ? '[#]' : '[ ]';
        ctx.fillStyle = '#8a90ad';
        ctx.font = '500 12px system-ui, sans-serif';
        ctx.fillText(pips, 16, y + 32);
      } else {
        const wk = SHOP_WEAPONS[i - 3];
        if (spriteReady(img[WEAPONS[wk].img])) ctx.drawImage(img[WEAPONS[wk].img], 14, y + 4, 32, 32);
        ctx.fillStyle = '#e8eaf2';
        ctx.font = '600 13px system-ui, sans-serif';
        ctx.fillText(WEAPONS[wk].label + ' (start loadout)', 54, y + 22);
      }
      // relabel the buy button for this row
      const b = s.list[i + 1];
      if (i < 3) {
        const lvl = lvls[i];
        b.label = lvl >= 3 ? 'MAX' : UPGRADE_PRICES[['dmg', 'spd', 'hp'][i]][lvl] + ' c';
      } else {
        const wk2 = SHOP_WEAPONS[i - 3];
        b.label = weaponUnlocked(wk2) ? 'OWNED' : WEAPON_PRICES[wk2] + ' c';
      }
    }
    drawButtons(s.list);
  }

  function renderSettings() {
    ctx.fillStyle = '#0d0f1a';
    ctx.fillRect(0, 0, cssW, cssH);
    const s = settingsButtons();

    ctx.textAlign = 'center';
    ctx.fillStyle = '#f2d13f';
    ctx.font = '800 30px system-ui, sans-serif';
    ctx.fillText('SETTINGS', cssW / 2, 48);
    ctx.textAlign = 'left';

    const names = ['MUSIC', 'SFX', 'CONTROLS'];
    const vals = [SAVE.musicVol + '/10', SAVE.sfxVol + '/10', CONTROL_LABELS[SAVE.control]];
    ctx.textBaseline = 'middle';
    for (let i = 0; i < 3; i++) {
      const y = s.topY + i * (s.rowH + s.gap) + s.rowH / 2;
      ctx.fillStyle = '#e8eaf2';
      ctx.font = '600 16px system-ui, sans-serif';
      ctx.fillText(names[i], 16, y);
      ctx.fillStyle = '#8a90ad';
      ctx.font = '600 13px system-ui, sans-serif';
      ctx.textAlign = 'right';
      ctx.fillText(vals[i], s.list[1 + i * 2].x - 12, y);
      ctx.textAlign = 'left';
    }
    ctx.textBaseline = 'alphabetic';
    drawButtons(s.list);
  }

  function renderPause() {
    ctx.fillStyle = 'rgba(5,6,12,0.72)';
    ctx.fillRect(0, 0, cssW, cssH);
    ctx.textAlign = 'center';
    ctx.fillStyle = '#f2d13f';
    ctx.font = '800 38px system-ui, sans-serif';
    ctx.fillText('PAUSED', cssW / 2, cssH / 2 - 40);
    ctx.textAlign = 'left';
    drawButtons(pauseButtons());
  }

  // ---- main loop ----
  let last = 0;
  let lastScene = scene;
  function frame(ts) {
    const dt = Math.min((ts - last) / 1000 || 0.016, 0.05);
    last = ts;
    if (scene !== lastScene) { lastScene = scene; resize(); }
    if (scene === 'play') {
      time += dt;
      if (gameOver) { gameOverT += dt; bankCoins(); }
      else update(dt);
      render();
    } else if (scene === 'pause') {
      render();
      renderPause();
    } else if (scene === 'menu') {
      renderMenu();
    } else if (scene === 'shop') {
      renderShop();
    } else if (scene === 'settings') {
      renderSettings();
    }
    requestAnimationFrame(frame);
  }

  // debug/verification hook
  root.__zs = {
    get wave() { return wave; }, get kills() { return kills; },
    get hp() { return hp; }, get x() { return hero.x; }, get y() { return hero.y; },
    get over() { return gameOver; }, get scene() { return scene; },
    get weapon() { return weapon; }, get monsterCount() { return monsters.length; },
    get coinsRun() { return coinsRun; }, get bank() { return SAVE.coins; },
    buttonCenter(id) {
      let list = [];
      if (scene === 'menu') list = menuButtons().list;
      else if (scene === 'shop') list = shopButtons().list;
      else if (scene === 'settings') list = settingsButtons().list;
      else if (scene === 'pause') list = pauseButtons();
      const b = list.find((x) => x.id === id);
      return b ? JSON.stringify({ x: Math.round(b.x + b.w / 2), y: Math.round(b.y + b.h / 2) }) : 'none';
    },
    get assets() { return JSON.stringify(status) + ' | nw: ' + keys.map(k => k + '=' + (img[k] ? img[k].naturalWidth : 'null')).join(','); },
  };

  function begin() {
    if (started) return;
    started = true;
    placeWrecks();
    resize();
    loadingEl.style.display = 'none';
    requestAnimationFrame((t) => { last = t; requestAnimationFrame(frame); });
  }

  // start at the menu; sprites fade in as they finish loading
  begin();
}
