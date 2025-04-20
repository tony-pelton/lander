package com.dsrts.lander;

import org.lwjgl.opengl.GL11;

public class Render {
    // Terrain: 1600 samples, 1 sample per 0.15625m (250m/1600)
    public static float[] terrainHeights = new float[1600];
    public static void generateTerrain() {
        double base = 30; // meters above bottom
        for (int i = 0; i < terrainHeights.length; i++) {
            double x = (i / (float) terrainHeights.length) * Lander.WORLD_WIDTH_M;
            terrainHeights[i] = (float) (base + 8 * Math.sin(x * 0.25) + 4 * Math.sin(x * 1.2));
        }
        // Flat landing zone in the middle
        int flatStart = 600, flatEnd = 700;
        for (int i = flatStart; i < flatEnd; i++)
            terrainHeights[i] = (float) base;
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

        // Draw terrain (white)
        GL11.glColor3f(1, 1, 1);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < terrainHeights.length; i++) {
            GL11.glVertex2f(i * Lander.WORLD_WIDTH_M / terrainHeights.length * Lander.PIXELS_PER_METER_X,
                    Lander.SCREEN_HEIGHT - terrainHeights[i] * Lander.PIXELS_PER_METER_Y);
        }
        GL11.glEnd();

        // Draw landing pad (red)
        GL11.glColor3f(1, 0, 0);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 600; i < 700; i++) {
            GL11.glVertex2f(i * Lander.WORLD_WIDTH_M / terrainHeights.length * Lander.PIXELS_PER_METER_X,
                    Lander.SCREEN_HEIGHT - terrainHeights[i] * Lander.PIXELS_PER_METER_Y);
        }
        GL11.glEnd();

        // Draw HUD
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glColor3f(1, 1, 1); // White text
        // Format text with 1 decimal place
        String hudText = String.format("Fuel: %.0f kg\nVx: %.1f m/s\nVy: %.1f m/s",
                lander.fuelMass,
                lander.vx,
                lander.vy);
        // Draw each line of text
        float x = Lander.SCREEN_WIDTH - 150; // 150 pixels from right edge
        float y = 30; // 30 pixels from top
        float lineHeight = 20;
        for (String line : hudText.split("\n")) {
            Lander.drawString(line, x, y);
            y += lineHeight;
        }
        GL11.glPopMatrix();

        // Draw lander (5x5 meters)
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

        // Draw thrust flame (if thrusting)
        if (lander.fuelMass > 0 && lander.alive && !lander.landed && lander.up) {
            GL11.glColor3f(1, 0.6f, 0);
            GL11.glBegin(GL11.GL_TRIANGLES);
            GL11.glVertex2f(-bodyW * 0.4f, baseY);
            GL11.glVertex2f(bodyW * 0.4f, baseY);
            GL11.glVertex2f(0, baseY + (legLen * 0.4f) + (float) Math.random() * 10);
            GL11.glEnd();
        }
        GL11.glPopMatrix();
    }
}
