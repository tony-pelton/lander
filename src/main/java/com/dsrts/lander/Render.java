package com.dsrts.lander;

import org.lwjgl.opengl.GL11;

public class Render {
    // Terrain: 1600 samples, 1 sample per 0.15625m (250m/1600)
    public static float[] terrainHeights = new float[1600];
    public static void generateTerrain() {
        double base = 30; // meters above bottom
        int n = terrainHeights.length;
        int padWidth = getPadWidth();
        int[] padCenters = getPadCenters();
        int padCount = padCenters.length;
        double mountainHeight = 40; // peak above base

        // 1. Left edge to left pad (flat)
        int leftPadStart = padCenters[0] - padWidth / 2;
        for (int i = 0; i < leftPadStart; i++) {
            terrainHeights[i] = (float) base;
        }

        // 2. Between pads: mountain (parabola)
        for (int p = 0; p < padCount - 1; p++) {
            int padAEnd = padCenters[p] + padWidth / 2;
            int padBStart = padCenters[p+1] - padWidth / 2;
            int regionLen = padBStart - padAEnd;
            int mid = regionLen / 2;
            double peak = base + mountainHeight;
            // Left half: base to peak
            for (int i = 0; i <= mid; i++) {
                int idx = padAEnd + i;
                double t = (double)i / mid; // 0 to 1
                double y = base + t * (peak - base);
                terrainHeights[idx] = (float)y;
            }
            // Right half: peak to base
            for (int i = mid + 1; i < regionLen; i++) {
                int idx = padAEnd + i;
                double t = (double)(i - mid) / (regionLen - 1 - mid); // 0 to 1
                double y = peak + t * (base - peak);
                terrainHeights[idx] = (float)y;
            }
        }

        // 3. Right pad to right edge (flat)
        int rightPadEnd = padCenters[padCount-1] + padWidth / 2;
        for (int i = rightPadEnd; i < n; i++) {
            terrainHeights[i] = (float) base;
        }

        // 4. Flatten pads
        for (int c = 0; c < padCenters.length; c++) {
            int start = padCenters[c] - padWidth / 2;
            int end = padCenters[c] + padWidth / 2;
            if (start < 0) start = 0;
            if (end > n - 1) end = n - 1;
            for (int i = start; i < end; i++) {
                terrainHeights[i] = (float) base;
            }
        }
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

        // Draw distant objects (e.g., Earth)
        Render.drawObjects();

        // Draw terrain and landing pad
        Render.renderTerrain();

        // Draw HUD
        Render.drawHud(lander);

        // Draw lander (all parts, effects)
        Render.renderLander(lander);
    }

    // Draws the terrain and landing pad
    public static int getPadWidth() {
        return 100;
    }
    public static int[] getPadCenters() {
        int n = terrainHeights.length;
        return new int[] {
            n / 6,        // left third center
            n / 2,        // center
            n * 5 / 6     // right third center
        };
    }

    public static void renderTerrain() {
        // Draw terrain (white)
        GL11.glColor3f(1, 1, 1);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < terrainHeights.length; i++) {
            GL11.glVertex2f(i * Lander.WORLD_WIDTH_M / terrainHeights.length * Lander.PIXELS_PER_METER_X,
                    Lander.SCREEN_HEIGHT - terrainHeights[i] * Lander.PIXELS_PER_METER_Y);
        }
        GL11.glEnd();

        // Draw landing pads (red)
        GL11.glColor3f(1, 0, 0);
        int padWidth = 100; // same as original pad (700 - 600)
        int n = terrainHeights.length;

        // Define centers for left, center, right pads
        int[] padCenters = new int[] {
            n / 6,        // left third center
            n / 2,        // center
            n * 5 / 6     // right third center
        };
        for (int c = 0; c < padCenters.length; c++) {
            int start = padCenters[c] - padWidth / 2;
            int end = padCenters[c] + padWidth / 2;
            // Clamp to valid range
            if (start < 0) start = 0;
            if (end > n - 1) end = n - 1;
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = start; i < end; i++) {
                GL11.glVertex2f(i * Lander.WORLD_WIDTH_M / n * Lander.PIXELS_PER_METER_X,
                        Lander.SCREEN_HEIGHT - terrainHeights[i] * Lander.PIXELS_PER_METER_Y);
            }
            GL11.glEnd();
        }
    }

    // Draws distant objects in the sky (e.g., Earth)
    public static void drawObjects() {
        // Draw distant Earth (high, right of center)
        float earthRadius = 22f;
        float earthX = Lander.SCREEN_WIDTH * 0.60f;
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

        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glColor3f(1, 1, 1); // White text
        // Format text with 1 decimal place
        String hudText = String.format("Fuel: %.0f kg\nThrottle: %d%%\nVx: %.1f m/s\nVy: %.1f m/s",
                lander.fuelMass,
                Math.round(lander.throttle * 100),
                lander.vx,
                lander.vy);
        // Draw each line of text
        float x = Lander.SCREEN_WIDTH - 200; // 200 pixels from right edge
        float y = 30; // 30 pixels from top
        float lineHeight = 20;
        for (String line : hudText.split("\n")) {
            Lander.drawString(line, x, y);
            y += lineHeight;
        }
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
        if (lander.fuelMass > 0 && lander.alive && !lander.landed && lander.space) {
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

