package com.dsrts.lander;

public class LanderState {
    public boolean up = false;
    public boolean left = false;
    public boolean right = false;
    public boolean space = false;
    public float x, y; // meters
    public float vx = 0, vy = 0;  // m/s
    public float angle = 0;       // degrees
    // --- Tunable start state ---
    public static final float INITIAL_DESCENT_RATE = -4.0f; // m/s (downward)
    // Approximate hover throttle: gravity * mass / thrust
    public static final float INITIAL_THROTTLE = (1.62f * 16400f) / 44000f; // ~0.60
    // For HUD: previous vertical speed and vertical acceleration (rate of change of vy)
    public float prevVy = 0.0f;
    public float verticalAccel = 0.0f;
    // Realistic mass/fuel
    public final float dryMass = 8200f; // kg
    public float fuelMass = 8200f;      // kg (full)
    public float getTotalMass() { return dryMass + fuelMass; }
    public float throttle = 0.0f; // 0.0 = off, 1.0 = full thrust
    public boolean alive = true;
    public boolean landed = false;

    public LanderState(float[] terrainHeights, float WORLD_WIDTH_M, float WORLD_HEIGHT_M, float LANDER_WIDTH_M, float LANDER_HEIGHT_M, float LANDER_HALF_W, float LANDER_HALF_H) {
        this.fuelMass = 8200f; // Reset to full at start
        this.vy = INITIAL_DESCENT_RATE;
        this.throttle = INITIAL_THROTTLE;

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
