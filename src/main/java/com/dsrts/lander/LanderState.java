package com.dsrts.lander;

public class LanderState {
    public boolean up = false;
    public boolean left = false;
    public boolean right = false;
    public boolean space = false;
    public float x, y; // meters
    public float vx = 0, vy = 0;  // m/s
    public float angle = 0;       // degrees
    public float fuel = 1000;     // arbitrary units
    public boolean alive = true;
    public boolean landed = false;

    public LanderState(float[] terrainHeights, float WORLD_WIDTH_M, float WORLD_HEIGHT_M, float LANDER_WIDTH_M, float LANDER_HEIGHT_M, float LANDER_HALF_W, float LANDER_HALF_H) {
        // Random horizontal position
        x = (float)(Math.random() * (WORLD_WIDTH_M - LANDER_WIDTH_M)) + LANDER_HALF_W;
        // Terrain sample index for x
        float terrainSample = x / (WORLD_WIDTH_M / terrainHeights.length);
        int tx = Math.round(terrainSample);
        float terrainY = 0;
        if (tx >= 0 && tx < terrainHeights.length) {
            terrainY = terrainHeights[tx]; // meters above bottom
        }
        // Set Y position 10 meters from top of world
        y = WORLD_HEIGHT_M - 10;
        // Make sure we're not too close to terrain
        float minSafeY = terrainY + LANDER_HEIGHT_M + 2; // 2m safety margin
        if (y < minSafeY) {
            y = minSafeY;
        }
        // Clamp to stay within world bounds
        if (y > WORLD_HEIGHT_M - LANDER_HALF_H) {
            y = WORLD_HEIGHT_M - LANDER_HALF_H;
        }
    }
}
