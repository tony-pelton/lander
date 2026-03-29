package com.dsrts.lander;

import com.badlogic.gdx.physics.box2d.Body;

public class LanderState {
    public Body body; // Box2D body
    public boolean up = false;
    public boolean down = false;
    public boolean left = false;
    public boolean right = false;
    public boolean space = false;
//    public float camX;
    public float x, y; // meters
    public float vx = 0, vy = 0;  // m/s
    public float goalVx = 0, goalVy = 0; // m/s, for fly-by-wire mode
    public float goalAngle = 0;          // degrees, for FBW attitude hold
    public boolean flyByWireMode = false; // toggled by A key
    public float angle = 0;       // degrees
    public float omega = 0;       // degrees/s (angular velocity)
    
    // --- Tunable start state ---
    public static final float INITIAL_DESCENT_RATE = -4.0f; // m/s (downward)
    // For HUD: previous vertical speed and vertical acceleration (rate of change of vy)
    public float prevVy = 0.0f;
    public float verticalAccel = 0.0f;
    public float integralVyError = 0.0f; // For PID
    public float derivativeVyError = 0.0f; // For PID
    public float filteredVyError = 0.0f; // For PID
    
    // Smoothed values for HUD display
    public float smoothedVerticalAccel = 0.0f;
    public float smoothedThrottle = 0.0f;

    // Realistic mass/fuel (Apollo Lunar Module Descent Stage approx)
    public static final float DRY_MASS = 4214f;      // kg
    public static final float TOTAL_FUEL_MASS = 10867f; // kg
    public float fuelMass;      // kg
    public float cargoMass = 0f;

    // Approximate hover throttle: gravity * (dry + startFuel) / thrust
    public static final float INITIAL_THROTTLE = (1.62f * (DRY_MASS + 1500f)) / 44000f;

    public float throttle = 0.0f; // 0.0 = off, 1.0 = full thrust
    public float throttleLeft = 0.0f;
    public float throttleRight = 0.0f;

    public boolean alive = true;
    public boolean landed = false;

    public LanderState(float[] terrainHeights, float WORLD_WIDTH_M, float WORLD_HEIGHT_M, float LANDER_WIDTH_M, float LANDER_HEIGHT_M, float LANDER_HALF_W, float LANDER_HALF_H) {
        this.fuelMass = 1500f; 
        
        this.vy = INITIAL_DESCENT_RATE;
        this.throttle = INITIAL_THROTTLE;

        // Start in middle of terrain
        x = WORLD_WIDTH_M / 2f;
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

    public float getTotalMass() { return DRY_MASS + fuelMass + cargoMass; }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LanderState{");
        sb.append("up=").append(up);
        sb.append(", down=").append(down);
        sb.append(", left=").append(left);
        sb.append(", right=").append(right);
        sb.append(", space=").append(space);
//        sb.append(", camX=").append(camX);
        sb.append(", x=").append(x);
        sb.append(", y=").append(y);
        sb.append(", vx=").append(vx);
        sb.append(", vy=").append(vy);
        sb.append(", goalVx=").append(goalVx);
        sb.append(", goalVy=").append(goalVy);
        sb.append(", flyByWireMode=").append(flyByWireMode);
        sb.append(", angle=").append(angle);
        sb.append(", prevVy=").append(prevVy);
        sb.append(", verticalAccel=").append(verticalAccel);
        sb.append(", dryMass=").append(DRY_MASS);
        sb.append(", fuelMass=").append(fuelMass);
        sb.append(", throttle=").append(throttle);
        sb.append(", alive=").append(alive);
        sb.append(", landed=").append(landed);
        sb.append('}');
        return sb.toString();
    }
}
