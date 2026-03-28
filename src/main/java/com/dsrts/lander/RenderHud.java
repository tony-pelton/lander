package com.dsrts.lander;

import org.lwjgl.opengl.GL11;

public class RenderHud {
    // Draws the HUD (fuel, throttle, velocities)
    static void drawHud(LanderState lander) {
        // Save current matrix and set up HUD coordinate space
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        // Set up screen-space coordinates for HUD
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, Lander.SCREEN_WIDTH, Lander.SCREEN_HEIGHT, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        drawDVyIndicator(lander);
        drawVyIndicator(lander);
        drawHudText(lander);

        // Restore previous matrix state
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    private static void drawVerticalIndicator(float x, float y, float width, float height, String label, float value, float maxValue) {
        float halfBar = height / 2f;
        float centerY = y + halfBar;

        // Draw label above the bar
        Render.drawString(label, x - 14, y - 28);

        // Clamp value
        if (value > maxValue) value = maxValue;
        if (value < -maxValue) value = -maxValue;

        // Draw fixed white center line
        GL11.glColor3f(1, 1, 1);
        GL11.glLineWidth(3.0f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x - width / 2 - 2, centerY);
        GL11.glVertex2f(x + width / 2 + 2, centerY);
        GL11.glEnd();
        GL11.glLineWidth(1.0f);

        // Draw white rectangle outline around the bar
        GL11.glColor3f(1, 1, 1);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x - width / 2, y);
        GL11.glVertex2f(x + width / 2, y);
        GL11.glVertex2f(x + width / 2, y + height);
        GL11.glVertex2f(x - width / 2, y + height);
        GL11.glEnd();

        // Draw scale indicators
        float tickX = x + width / 2 + 4;
        GL11.glBegin(GL11.GL_LINES);
        // Top
        GL11.glVertex2f(tickX - 2, y);
        GL11.glVertex2f(tickX + 2, y);
        // Middle-Top
        GL11.glVertex2f(tickX - 2, centerY - 25);
        GL11.glVertex2f(tickX + 2, centerY - 25);
        // Middle-Bottom
        GL11.glVertex2f(tickX - 2, centerY + 25);
        GL11.glVertex2f(tickX + 2, centerY + 25);
        // Bottom
        GL11.glVertex2f(tickX - 2, y + height);
        GL11.glVertex2f(tickX + 2, y + height);
        GL11.glEnd();

        // Draw fill
        if (value != 0.0f) {
            float fillPosition = centerY - (value / maxValue) * halfBar;
            if (value > 0.0f) {
                GL11.glColor3f(0, 1, 0); // Green
            } else {
                GL11.glColor3f(1, 0, 0); // Red
            }
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x - width / 2, centerY);
            GL11.glVertex2f(x + width / 2, centerY);
            GL11.glVertex2f(x + width / 2, fillPosition);
            GL11.glVertex2f(x - width / 2, fillPosition);
            GL11.glEnd();
        }
    }

    private static void drawDVyIndicator(LanderState lander) {
        float barX = 50;
        float barY = 50;
        float barWidth = 18;
        float barHeight = 100;
        float maxAccel = 2.0f;
        drawVerticalIndicator(barX, barY, barWidth, barHeight, "dVy", lander.verticalAccel, maxAccel);
    }

    private static void drawVyIndicator(LanderState lander) {
        float barX = 50;
        float barY = 50;
        float barWidth = 18;
        float barHeight = 100;
        float barX_vy = barX + barWidth * 2 + 20;
        float maxVel = 50.0f;
        drawVerticalIndicator(barX_vy, barY, barWidth, barHeight, "Vy", lander.vy, maxVel);
    }

    private static void drawHudText(LanderState lander) {
        // Draw HUD text (velocities, altitudes, fuel, throttle, goal values)
        float textX = Lander.SCREEN_WIDTH - 250;
        float textY = 40;
        float lineHeight = 22;
        Render.drawString(String.format("Vy: %6.2f m/s", lander.vy), textX, textY);
        Render.drawString(String.format("Vx: %6.2f m/s", lander.vx), textX, textY + lineHeight);
        Render.drawString(String.format("Goal Vy: %6.2f m/s", lander.goalVy), textX, textY + 2 * lineHeight);
        Render.drawString(String.format("Goal Vx: %6.2f m/s", lander.goalVx), textX, textY + 3 * lineHeight);
        Render.drawString(String.format("Altitude: %6.2f m", lander.y), textX, textY + 4 * lineHeight);
        Render.drawString(String.format("Fuel: %6.1f kg", lander.fuelMass), textX, textY + 5 * lineHeight);
        Render.drawString(String.format("Throttle: %3d%%", (int)(lander.throttle * 100)), textX, textY + 6 * lineHeight);
    }
}
