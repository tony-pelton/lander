package com.dsrts.lander;

public class Collision {
    static void collision(LanderState lander) {
        // Collision
        float terrainSample = lander.x / (Lander.WORLD_WIDTH_M / Render.terrainHeights.length);
        int tx = Math.round(terrainSample);
        if (tx >= 0 && tx < Render.terrainHeights.length) {
            float terrainY = Render.terrainHeights[tx];
            if (lander.y - Lander.LANDER_HALF_H < terrainY) {
                lander.y = terrainY + Lander.LANDER_HALF_H;
                lander.vx = lander.vy = 0;
                // Check landing zone
                if (tx >= 600 && tx < 700 && Math.abs(lander.angle) < 10 && Math.abs(lander.vx) < 2
                        && Math.abs(lander.vy) < 2) {
                    lander.landed = true;
                } else {
                    lander.alive = false;
                }
            }
        }
    }
}
