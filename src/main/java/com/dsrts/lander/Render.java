package com.dsrts.lander;

import org.lwjgl.opengl.GL11;

public class Render {

    // Static galaxy star positions to prevent flickering
    private static final int GALAXY_STAR_COUNT = 30;
    private static final float[] galaxyStarX = new float[GALAXY_STAR_COUNT];
    private static final float[] galaxyStarY = new float[GALAXY_STAR_COUNT];
    private static final int galaxyThird = (int)(Math.random() * 3); // Choose galaxy third at startup
    private static float galaxyRotation = 0f; // Current rotation angle
    private static final float GALAXY_ROTATION_SPEED = 10f; // Degrees per second
    
    // Background stars configuration
    private static final int SMALL_STARS_COUNT = 75;
    private static final int LARGE_STARS_COUNT = 25; // Roughly 3:1 ratio
    private static final float[] smallStarX = new float[SMALL_STARS_COUNT];
    private static final float[] smallStarY = new float[SMALL_STARS_COUNT];
    private static final float[] largeStarX = new float[LARGE_STARS_COUNT];
    private static final float[] largeStarY = new float[LARGE_STARS_COUNT];
    private static final float LARGE_STAR_SIZE = 3.0f;
    private static final float LARGE_STAR_INNER_SIZE = 1.5f;
    
    static {
        // Initialize galaxy star positions once
        for (int i = 0; i < GALAXY_STAR_COUNT; i++) {
            double angle = Math.random() * 2.0 * Math.PI;
            double rad = (0.5 + Math.random() * 0.5);
            galaxyStarX[i] = (float)Math.cos(angle) * (float)rad;
            galaxyStarY[i] = (float)Math.sin(angle) * (float)rad;
        }

        // Initialize background stars
        float minClearance = 10.0f; // minimum meters above terrain
        for (int i = 0; i < SMALL_STARS_COUNT; i++) {
            smallStarX[i] = (float)(Math.random() * Lander.WORLD_WIDTH_M);
            // Find terrain height at this x position
            int terrainIndex = (int)(smallStarX[i] * Terrain.terrainHeights.length / Lander.WORLD_WIDTH_M);
            float terrainHeight = Terrain.terrainHeights[terrainIndex];
            // Place star between minClearance above terrain and world height
            float availableHeight = Lander.WORLD_HEIGHT_M - (terrainHeight + minClearance);
            smallStarY[i] = terrainHeight + minClearance + (float)(Math.random() * availableHeight);
        }

        for (int i = 0; i < LARGE_STARS_COUNT; i++) {
            largeStarX[i] = (float)(Math.random() * Lander.WORLD_WIDTH_M);
            // Find terrain height at this x position
            int terrainIndex = (int)(largeStarX[i] * Terrain.terrainHeights.length / Lander.WORLD_WIDTH_M);
            float terrainHeight = Terrain.terrainHeights[terrainIndex];
            // Place star between minClearance above terrain and world height
            float availableHeight = Lander.WORLD_HEIGHT_M - (terrainHeight + minClearance);
            largeStarY[i] = terrainHeight + minClearance + (float)(Math.random() * availableHeight);
        }
    }

    private static void drawLargeStar(float x, float y) {
        // Draw the outer cross
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x - LARGE_STAR_SIZE, y);
        GL11.glVertex2f(x + LARGE_STAR_SIZE, y);
        GL11.glVertex2f(x, y - LARGE_STAR_SIZE);
        GL11.glVertex2f(x, y + LARGE_STAR_SIZE);
        GL11.glEnd();
        
        // Draw the inner cross (45 degrees rotated)
        GL11.glBegin(GL11.GL_LINES);
        float diag = LARGE_STAR_INNER_SIZE * 0.707107f; // cos(45°)
        GL11.glVertex2f(x - diag, y - diag);
        GL11.glVertex2f(x + diag, y + diag);
        GL11.glVertex2f(x - diag, y + diag);
        GL11.glVertex2f(x + diag, y - diag);
        GL11.glEnd();
    }

    public static void renderScene(LanderState lander, float camX) {
        // --- Render ---
        GL11.glClearColor(0.05f, 0.05f, 0.08f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        // Camera: left=camX (meters), right=camX+window (meters)
        GL11.glOrtho(camX * Lander.PIXELS_PER_METER_X,
                (camX + Lander.SCREEN_WIDTH / Lander.PIXELS_PER_METER_X) * Lander.PIXELS_PER_METER_X, Lander.SCREEN_HEIGHT, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        drawStars();

        // Draw distant objects (e.g., Earth)
        drawObjects();

        // Draw terrain and landing pad
        renderTerrain();

        // Draw HUD
        drawHud(lander);

        // Draw lander (all parts, effects)
        renderLander(lander);
    }

    public static void renderTerrain() {
        // Draw terrain (white)
        GL11.glColor3f(1, 1, 1);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < Terrain.terrainHeights.length; i++) {
            GL11.glVertex2f(i * Lander.WORLD_WIDTH_M / Terrain.terrainHeights.length * Lander.PIXELS_PER_METER_X,
                    Lander.SCREEN_HEIGHT - Terrain.terrainHeights[i] * Lander.PIXELS_PER_METER_Y);
        }
        GL11.glEnd();

        // Draw landing pads (red)
        GL11.glColor3f(1, 0, 0);

        int n = Terrain.terrainHeights.length;

        for (int c = 0; c < Terrain.padCenters.length; c++) {
            int start = Terrain.padCenters[c] - Terrain.padWidth / 2;
            int end = Terrain.padCenters[c] + Terrain.padWidth / 2;
            // Clamp to valid range
            if (start < 0) start = 0;
            if (end > n - 1) end = n - 1;
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = start; i < end; i++) {
                GL11.glVertex2f(i * Lander.WORLD_WIDTH_M / n * Lander.PIXELS_PER_METER_X,
                        Lander.SCREEN_HEIGHT - Terrain.terrainHeights[i] * Lander.PIXELS_PER_METER_Y);
            }
            GL11.glEnd();
        }
    }

    public static void drawStars() {
        // Draw background stars
        GL11.glColor3f(0.8f, 0.8f, 0.9f);
        // Small stars
        GL11.glPointSize(1.0f);
        GL11.glBegin(GL11.GL_POINTS);
        for (int i = 0; i < SMALL_STARS_COUNT; i++) {
            float x = smallStarX[i] * Lander.PIXELS_PER_METER_X;
            float y = Lander.SCREEN_HEIGHT - smallStarY[i] * Lander.PIXELS_PER_METER_Y;
            GL11.glVertex2f(x, y);
        }
        GL11.glEnd();

        // Large stars
        GL11.glLineWidth(1.0f);
        for (int i = 0; i < LARGE_STARS_COUNT; i++) {
            float x = largeStarX[i] * Lander.PIXELS_PER_METER_X;
            float y = Lander.SCREEN_HEIGHT - largeStarY[i] * Lander.PIXELS_PER_METER_Y;
            drawLargeStar(x, y);
        }
    }
    // Draws distant objects in the sky (e.g., Earth)
    public static void drawObjects() {
        // Calculate terrain thirds for object placement
        float thirdWidth = Lander.WORLD_WIDTH_M / 3f;
        
        // Position galaxy in center-left of chosen third
        float galaxyX = (galaxyThird * thirdWidth + thirdWidth * 0.3f) * Lander.PIXELS_PER_METER_X;
        float galaxyRadius = 44f;
        float galaxyY = 75f;
        int galaxySegments = 40;

        // Update galaxy rotation
        float deltaTime = 1.0f / 60.0f; // Assuming 60 FPS, could be made more precise
        galaxyRotation += GALAXY_ROTATION_SPEED * deltaTime;
        if (galaxyRotation > 360f) galaxyRotation -= 360f;

        // Save current matrix
        GL11.glPushMatrix();
        // Translate to galaxy center, rotate, then draw
        GL11.glTranslatef(galaxyX, galaxyY, 0);
        GL11.glRotatef(galaxyRotation, 0, 0, 1);
        
        GL11.glColor3f(0.8f, 0.6f, 1.0f); // galaxy color
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= galaxySegments; i++) {
            double theta = 2.0 * Math.PI * i / galaxySegments;
            float dx = (float)Math.cos(theta) * galaxyRadius;
            float dy = (float)Math.sin(theta) * galaxyRadius;
            GL11.glVertex2f(dx, dy);
        }
        GL11.glEnd();
        GL11.glPointSize(2.0f);
        GL11.glBegin(GL11.GL_POINTS);
        for (int i = 0; i < GALAXY_STAR_COUNT; i++) {
            float sx = galaxyStarX[i] * galaxyRadius;
            float sy = galaxyStarY[i] * galaxyRadius;
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            GL11.glVertex2f(sx, sy);
        }
        GL11.glEnd();
        
        // Restore matrix
        GL11.glPopMatrix();

        // Draw distant Earth (centered in middle-upper right of center third)
        float earthRadius = 22f;
        float earthX = (thirdWidth + thirdWidth * 0.7f) * Lander.PIXELS_PER_METER_X; // Position in center third
        float earthY = 54f;
        int numSegments = 40;
        // Draw Earth circle
        GL11.glColor3f(0.2f, 0.4f, 1.0f); // Earth blue
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(earthX, earthY);
        for (int i = 0; i <= numSegments; i++) {
            double theta = 2.0 * Math.PI * i / numSegments;
            float dx = (float)Math.cos(theta) * earthRadius;
            float dy = (float)Math.sin(theta) * earthRadius;
            GL11.glVertex2f(earthX + dx, earthY + dy);
        }
        GL11.glEnd();
        // Draw simple "continents" as green shapes
        GL11.glColor3f(0.1f, 0.7f, 0.2f);
        GL11.glBegin(GL11.GL_POLYGON);
        GL11.glVertex2f(earthX + earthRadius * 0.2f, earthY - earthRadius * 0.1f);
        GL11.glVertex2f(earthX + earthRadius * 0.4f, earthY - earthRadius * 0.4f);
        GL11.glVertex2f(earthX + earthRadius * 0.1f, earthY - earthRadius * 0.5f);
        GL11.glVertex2f(earthX - earthRadius * 0.1f, earthY - earthRadius * 0.3f);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_POLYGON);
        GL11.glVertex2f(earthX - earthRadius * 0.3f, earthY + earthRadius * 0.1f);
        GL11.glVertex2f(earthX - earthRadius * 0.2f, earthY + earthRadius * 0.3f);
        GL11.glVertex2f(earthX, earthY + earthRadius * 0.2f);
        GL11.glVertex2f(earthX - earthRadius * 0.1f, earthY);
        GL11.glEnd();
    }

    // Draws the HUD (fuel, throttle, velocities)
    public static void drawHud(LanderState lander) {
        // Save current matrix and set up HUD coordinate space
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        // Set up screen-space coordinates for HUD
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, Lander.SCREEN_WIDTH, Lander.SCREEN_HEIGHT, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // Draw vertical acceleration bar (left side, label above)
        float barX = 50;
        float barY = 50;
        // Draw label above the bar
        Lander.drawString("dVy", barX - 14, barY - 28);
        float barWidth = 18;
        float barHeight = 100;
        float halfBar = barHeight / 2f;
        // Compute vertical acceleration (rate of change of vy)
        float verticalAccel = lander.verticalAccel;
        // Clamp to [-5, 5] m/s^2
        if (verticalAccel > 5) verticalAccel = 5;
        if (verticalAccel < -5) verticalAccel = -5;
        // Draw fixed white center line
        float centerY = barY + halfBar;
        GL11.glColor3f(1, 1, 1);
        GL11.glLineWidth(3.0f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(barX - barWidth / 2 - 2, centerY);
        GL11.glVertex2f(barX + barWidth / 2 + 2, centerY);
        GL11.glEnd();
        GL11.glLineWidth(1.0f);
        // Draw white rectangle outline around the bar
        GL11.glColor3f(1, 1, 1);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(barX - barWidth / 2, barY);
        GL11.glVertex2f(barX + barWidth / 2, barY);
        GL11.glVertex2f(barX + barWidth / 2, barY + barHeight);
        GL11.glVertex2f(barX - barWidth / 2, barY + barHeight);
        GL11.glEnd();
        // Draw green fill for positive acceleration (up from center)
        if (verticalAccel > 0.0f) {
            float fillTop = centerY - (verticalAccel / 5.0f) * halfBar; // +5 m/s²: fillTop == barY
            GL11.glColor3f(0, 1, 0);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(barX - barWidth / 2, fillTop);
            GL11.glVertex2f(barX + barWidth / 2, fillTop);
            GL11.glVertex2f(barX + barWidth / 2, centerY);
            GL11.glVertex2f(barX - barWidth / 2, centerY);
            GL11.glEnd();
        }
        // Draw red fill for negative acceleration (down from center)
        if (verticalAccel < 0.0f) {
            float fillBot = centerY - (verticalAccel / 5.0f) * halfBar; // -5 m/s²: fillBot == barY + barHeight
            GL11.glColor3f(1, 0, 0);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(barX - barWidth / 2, centerY);
            GL11.glVertex2f(barX + barWidth / 2, centerY);
            GL11.glVertex2f(barX + barWidth / 2, fillBot);
            GL11.glVertex2f(barX - barWidth / 2, fillBot);
            GL11.glEnd();
        }

        // Draw HUD text (velocities, altitudes, fuel, throttle, goal values)
        float textX = Lander.SCREEN_WIDTH - 250;
        float textY = 40;
        float lineHeight = 22;
        Lander.drawString(String.format("Vy: %6.2f m/s", lander.vy), textX, textY);
        Lander.drawString(String.format("Vx: %6.2f m/s", lander.vx), textX, textY + lineHeight);
        Lander.drawString(String.format("Goal Vy: %6.2f m/s", lander.goalVy), textX, textY + 2 * lineHeight);
        Lander.drawString(String.format("Goal Vx: %6.2f m/s", lander.goalVx), textX, textY + 3 * lineHeight);
        Lander.drawString(String.format("Altitude: %6.2f m", lander.y), textX, textY + 4 * lineHeight);
        Lander.drawString(String.format("Fuel: %6.1f kg", lander.fuelMass), textX, textY + 5 * lineHeight);
        Lander.drawString(String.format("Throttle: %3d%%", (int)(lander.throttle * 100)), textX, textY + 6 * lineHeight);

        // Restore previous matrix state
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    // Renders the lander and its effects at its current position
    public static void renderLander(LanderState lander) {
        GL11.glPushMatrix();
        float px = lander.x * Lander.PIXELS_PER_METER_X;
        float py = Lander.SCREEN_HEIGHT - lander.y * Lander.PIXELS_PER_METER_Y;
        GL11.glTranslatef(px, py, 0);
        GL11.glRotatef(lander.angle, 0, 0, 1);
        // Lander color: always white (except for landed/crashed)
        if (!lander.alive)
            GL11.glColor3f(1, 0, 0);
        else if (lander.landed)
            GL11.glColor3f(0, 1, 0);
        else
            GL11.glColor3f(1, 1, 1);
        // Draw main lander body (rectangle)
        float bodyW = Lander.LANDER_HALF_W * 1.2f * Lander.PIXELS_PER_METER_X;
        float bodyH = Lander.LANDER_HALF_H * 0.9f * Lander.PIXELS_PER_METER_Y;
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(-bodyW, -bodyH * 0.5f);
        GL11.glVertex2f(bodyW, -bodyH * 0.5f);
        GL11.glVertex2f(bodyW, bodyH * 0.5f);
        GL11.glVertex2f(-bodyW, bodyH * 0.5f);
        GL11.glEnd();

        // Draw capsule/cab (octagon on top), flat bottom edge aligns with body top
        float cabRadius = bodyW * 0.5f;
        float bodyTopY = -bodyH * 0.5f;
        int cabSegments = 8;
        float[] octX = new float[cabSegments];
        float[] octY = new float[cabSegments];
        // The angle for the first point (flat bottom) is -PI/8, so the flat edge is parallel to X axis
        double theta0 = -Math.PI / 8;
        for (int i = 0; i < cabSegments; i++) {
            double theta = theta0 + 2.0 * Math.PI * i / cabSegments;
            octX[i] = (float) (cabRadius * Math.cos(theta));
            octY[i] = (float) (cabRadius * Math.sin(theta));
        }
        // The cab's center should be above the body's top edge by cabRadius * cos(PI/8)
        float cabCenterY = bodyTopY - cabRadius * (float)Math.cos(Math.PI/8);
        // Draw octagon
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < cabSegments; i++) {
            GL11.glVertex2f(octX[i], octY[i] + cabCenterY);
        }
        GL11.glEnd();

        // Draw four legs (lines from corners)
        float legLen = bodyH * 1.2f;
        float legSpread = bodyW * 0.8f;
        float baseY = bodyH * 0.5f;
        float legAngle = (float) Math.toRadians(30);
        // Left leg
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(-legSpread, baseY);
        GL11.glVertex2f(-legSpread - legLen * (float)Math.sin(legAngle), baseY + legLen * (float)Math.cos(legAngle));
        // Right leg
        GL11.glVertex2f(legSpread, baseY);
        GL11.glVertex2f(legSpread + legLen * (float)Math.sin(legAngle), baseY + legLen * (float)Math.cos(legAngle));
        // Back left leg
        GL11.glVertex2f(-legSpread * 0.6f, baseY);
        GL11.glVertex2f(-legSpread * 0.6f - legLen * 0.5f * (float)Math.sin(legAngle), baseY + legLen * 0.7f * (float)Math.cos(legAngle));
        // Back right leg
        GL11.glVertex2f(legSpread * 0.6f, baseY);
        GL11.glVertex2f(legSpread * 0.6f + legLen * 0.5f * (float)Math.sin(legAngle), baseY + legLen * 0.7f * (float)Math.cos(legAngle));
        GL11.glEnd();

        // Draw thrust flame (if throttle > 0)
        if (lander.fuelMass > 0 && lander.alive && !lander.landed && lander.throttle > 0.0f) {
            GL11.glColor3f(1, 0.6f, 0);
            GL11.glBegin(GL11.GL_TRIANGLES);
            GL11.glVertex2f(-bodyW * 0.4f, baseY);
            GL11.glVertex2f(bodyW * 0.4f, baseY);
            // Scale flame height with throttle, add some flicker
            float flameHeight = (legLen * 0.2f + lander.throttle * legLen * 0.6f) + (float) Math.random() * 8 * lander.throttle;
            GL11.glVertex2f(0, baseY + flameHeight);
            GL11.glEnd();
        }

        // Draw side thruster gas (white, semi-transparent, flickering)
        if (lander.fuelMass > 0 && lander.alive && !lander.landed) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            float gasLength = bodyW * 1.3f + (float)Math.random() * bodyW * 0.4f;
            float gasWidth = bodyH * 0.3f + (float)Math.random() * bodyH * 0.2f;
            float alpha = 0.55f + 0.25f * (float)Math.random();
            // Left command (thrusts right): draw jet on right
            if (lander.left) {
                GL11.glColor4f(1f, 1f, 1f, alpha);
                GL11.glBegin(GL11.GL_TRIANGLES);
                GL11.glVertex2f(bodyW, 0);
                GL11.glVertex2f(bodyW + gasLength, -gasWidth);
                GL11.glVertex2f(bodyW + gasLength, gasWidth);
                GL11.glEnd();
            }
            // Right command (thrusts left): draw jet on left
            if (lander.right) {
                GL11.glColor4f(1f, 1f, 1f, alpha);
                GL11.glBegin(GL11.GL_TRIANGLES);
                GL11.glVertex2f(-bodyW, 0);
                GL11.glVertex2f(-bodyW - gasLength, -gasWidth);
                GL11.glVertex2f(-bodyW - gasLength, gasWidth);
                GL11.glEnd();
            }
            GL11.glDisable(GL11.GL_BLEND);
        }

        GL11.glPopMatrix();
    }
}

