import React, { useEffect, useRef } from 'react';
// @ts-ignore - plain JS module
import { initGame } from './zs-game';
import heroImg from './photos/hero.png';
import heroIdleImg from './photos/hero-idle.png';
import heroFireImg from './photos/hero-fire.png';
import zombieImg from './photos/zombie.png';
import zombieAttackImg from './photos/zombie-attack.png';
import zombieDeathImg from './photos/zombie-death.png';
import wreckImg from './photos/wreck.png';
import flashImg from './photos/flash.png';
import splatImg from './photos/splat.png';
import portraitImg from './photos/portrait.png';
import mapImg from './photos/map.jpg';
import mapGraveyardImg from './photos/map-graveyard.png';
import mapHighwayImg from './photos/map-highway.png';
import runnerImg from './photos/runner.png';
import bruteImg from './photos/brute.png';
import spitterImg from './photos/spitter.png';
import kingGoliathImg from './photos/king-goliath.png';
import kingBroodImg from './photos/king-brood.png';
import wPistolImg from './photos/w-pistol.png';
import wRifleImg from './photos/w-rifle.png';
import wShotgunImg from './photos/w-shotgun.png';
import wSniperImg from './photos/w-sniper.png';
import wFlameImg from './photos/w-flame.png';
import wRpgImg from './photos/w-rpg.png';
import pickupImg from './photos/pickup.png';

const MARKUP = `
<div class="zs-topbar">
  <div class="zs-brand">ZOMBIE SURVIVOR<small>Vexolabs prototype</small></div>
</div>
<div class="zs-stage" id="zs-stage">
  <canvas id="zs-cv"></canvas>
  <div class="zs-loading" id="zs-loading">Loading...</div>
</div>
<div class="zs-hint">Pick a map and hit PLAY. Move with the stick (3 control modes in SETTINGS). Weapons fire by themselves - tap the icons (top) to switch. Kills earn coins: spend them in the SHOP on upgrades and new weapons. Kings every 5th wave.</div>
`;

export function App() {
    const ref = useRef<HTMLDivElement>(null);
    useEffect(() => {
        if (!ref.current) return;
        initGame(ref.current, {
            hero: heroImg, heroIdle: heroIdleImg, heroFire: heroFireImg, zombie: zombieImg, zombieAttack: zombieAttackImg, zombieDeath: zombieDeathImg, wreck: wreckImg,
            flash: flashImg, splat: splatImg, portrait: portraitImg, map: mapImg,
            mapGraveyard: mapGraveyardImg, mapHighway: mapHighwayImg,
            runner: runnerImg, brute: bruteImg, spitter: spitterImg, kingGoliath: kingGoliathImg, kingBrood: kingBroodImg,
            wPistol: wPistolImg, wRifle: wRifleImg, wShotgun: wShotgunImg, wSniper: wSniperImg, wFlame: wFlameImg, wRpg: wRpgImg,
            pickup: pickupImg,
        });
    }, []);
    return <div className="zs-root" ref={ref} dangerouslySetInnerHTML={{ __html: MARKUP }} />;
}
