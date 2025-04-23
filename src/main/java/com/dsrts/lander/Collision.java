package com.dsrts.lander;

public class Collision {
    static void collision(LanderState lander) {
        // Collision
        float terrainSample = lander.x / (Lander.WORLD_WIDTH_M / Terrain.terrainHeights.length);
        int tx = Math.round(terrainSample);
        if (tx >= 0 && tx < Terrain.terrainHeights.length) {
            float terrainY = Terrain.terrainHeights[tx];
            if (lander.y - Lander.LANDER_HALF_H < terrainY) {
                lander.y = terrainY + Lander.LANDER_HALF_H;
                lander.vx = lander.vy = 0;
                // Check landing zone (on any pad)
                boolean onPad = false;
                int[] padCenters = Terrain.padCenters;
                int padWidth = Terrain.padWidth;
                for (int c = 0; c < padCenters.length; c++) {
                    int start = padCenters[c] - padWidth / 2;
                    int end = padCenters[c] + padWidth / 2;
                    if (tx >= start && tx < end) {
                        onPad = true;
                        break;
                    }
                }
                if (onPad && Math.abs(lander.angle) < 10 && Math.abs(lander.vx) < 2 && Math.abs(lander.vy) < 2) {
                    lander.landed = true;
                    lander.angle = 0.0f;
                    lander.goalVx = 0.0f;
                    lander.goalVy = 0.0f;
                    lander.throttle = 0.0f;
                } else {
                    lander.alive = false;
                }
            } else {
                if(lander.landed && lander.alive) {
                    lander.landed = false;
                }
            }
        }
    }
}
