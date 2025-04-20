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
        String hudText = String.format("Fuel: %.0f\nVx: %.1f m/s\nVy: %.1f m/s",
                lander.fuel,
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
        // Draw triangle: base at bottom, tip at top (isosceles)
        GL11.glBegin(GL11.GL_LINE_LOOP);
        // Top vertex (tip)
        GL11.glVertex2f(0, -Lander.LANDER_HALF_H * Lander.PIXELS_PER_METER_Y);
        // Bottom right
        GL11.glVertex2f(Lander.LANDER_HALF_W * Lander.PIXELS_PER_METER_X, Lander.LANDER_HALF_H * Lander.PIXELS_PER_METER_Y);
        // Bottom left
        GL11.glVertex2f(-Lander.LANDER_HALF_W * Lander.PIXELS_PER_METER_X, Lander.LANDER_HALF_H * Lander.PIXELS_PER_METER_Y);
        GL11.glEnd();

        // Draw thrust flame (if thrusting)
        if (lander.fuel > 0 && lander.alive && !lander.landed && lander.up) {
            GL11.glColor3f(1, 0.6f, 0);
            GL11.glBegin(GL11.GL_TRIANGLES);
            GL11.glVertex2f(-Lander.LANDER_HALF_W * 0.4f * Lander.PIXELS_PER_METER_X,
                    Lander.LANDER_HALF_H * Lander.PIXELS_PER_METER_Y);
            GL11.glVertex2f(Lander.LANDER_HALF_W * 0.4f * Lander.PIXELS_PER_METER_X,
                    Lander.LANDER_HALF_H * Lander.PIXELS_PER_METER_Y);
            GL11.glVertex2f(0,
                    (Lander.LANDER_HALF_H + 2 + (float) Math.random() * 2) * Lander.PIXELS_PER_METER_Y);
            GL11.glEnd();
        }
        GL11.glPopMatrix();
    }
}
