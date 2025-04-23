package com.dsrts.lander;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;


public class Lander {
    // --- METRIC SCALE CONSTANTS ---
    static final float WORLD_WIDTH_M = 750f; // World is 750 meters wide (3 screens)
    static final float WORLD_HEIGHT_M = 150f; // World is 150 meters high (matches aspect ratio)
    static final int SCREEN_WIDTH = 1280;
    static final int SCREEN_HEIGHT = 768;
    static final float PIXELS_PER_METER_X = (SCREEN_WIDTH * 3) / WORLD_WIDTH_M; // Keep original scale
    static final float PIXELS_PER_METER_Y = SCREEN_HEIGHT / WORLD_HEIGHT_M; // ≈5.12 px/m
    static final float LANDER_WIDTH_M = 5f;
    static final float LANDER_HEIGHT_M = 5f;
    static final float LANDER_HALF_W = LANDER_WIDTH_M / 2;
    static final float LANDER_HALF_H = LANDER_HEIGHT_M / 2;

    private static STBFontRenderer fontRenderer;

    static void drawString(String text, float x, float y) {
        if (fontRenderer == null) {
            fontRenderer = new STBFontRenderer("/fonts/Roboto-Regular.ttf", 32f);
        }
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        fontRenderer.drawText(text, 0, 0, 1.0f, 1.0f);
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void main(String[] args) {
        // Setup an error callback
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        int width = SCREEN_WIDTH;
        int height = SCREEN_HEIGHT;
        long window = GLFW.glfwCreateWindow(width, height, "Lander", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            GLFW.glfwTerminate();
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Center the window
        GLFW.glfwSetWindowPos(window, 100, 100); // Simple position, can be improved

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        GLFW.glfwShowWindow(window);

        // Game state
        Terrain.generateTerrain();

        LanderState lander = new LanderState(Terrain.terrainHeights, WORLD_WIDTH_M, WORLD_HEIGHT_M, LANDER_WIDTH_M,
                LANDER_HEIGHT_M, LANDER_HALF_W, LANDER_HALF_H);

        // Start camera in center of terrain
        float camX = WORLD_WIDTH_M / 2f - (SCREEN_WIDTH / PIXELS_PER_METER_X) / 2f; // meters
        long lastTime = System.nanoTime();
        double accumulator = 0.0;
        double dt = 1.0 / 60.0; // 60 FPS physics
        // Track previous A key state for edge detection
        boolean prevAPressed = false; // initialize before loop

        // Main loop
        while (!GLFW.glfwWindowShouldClose(window)) {
            // --- Input ---
            GLFW.glfwPollEvents();
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(window, true);
            }
            lander.up = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
            lander.down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS;
            lander.left = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS;
            lander.right = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS;
            lander.space = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;

            // Toggle fly-by-wire mode with A key (on key press, edge detection)
            boolean aPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
            if (aPressed && !prevAPressed) {
                lander.flyByWireMode = !lander.flyByWireMode;
            }

            // Update goal velocities or sync them
            float GOAL_INCREMENT = 0.1f; // m/s per key press
            if (lander.flyByWireMode) {
                if (lander.up) lander.goalVy += GOAL_INCREMENT;
                if (lander.down) lander.goalVy -= GOAL_INCREMENT;
                if (lander.left) lander.goalVx -= GOAL_INCREMENT;
                if (lander.right) lander.goalVx += GOAL_INCREMENT;
            } else {
                // Manual mode: keep goals in sync with actual
                lander.goalVx = lander.vx;
                lander.goalVy = lander.vy;
            }

            // --- Physics (fixed timestep) ---
            long now = System.nanoTime();
            accumulator += (now - lastTime) / 1e9;
            lastTime = now;
            while (accumulator >= dt) {
                // Update previous A key state at the end of each loop iteration
                prevAPressed = aPressed;
                if (lander.alive) {
                    if (lander.flyByWireMode) {
                        Physics.flybywire(lander, dt);
                    } else {
                        Physics.advance(lander, dt);
                    }
                    // Camera follows lander (in meters)
                    // Smoothly follow lander within world bounds
                    float targetCamX = lander.x - (SCREEN_WIDTH / PIXELS_PER_METER_X) / 2f;
                    // Clamp camera to world bounds
                    float camMin = 0;
                    float camMax = WORLD_WIDTH_M - (SCREEN_WIDTH / PIXELS_PER_METER_X);
                    if (targetCamX < camMin) targetCamX = camMin;
                    if (targetCamX > camMax) targetCamX = camMax;
                    // Move camera towards target with smooth interpolation
                    camX = targetCamX; // Direct follow, no smoothing needed
                    Collision.collision(lander);
                }
                accumulator -= dt;
            }

            // --- Render ---
            Render.renderScene(lander, camX);

            if (lander.landed) {
                String msg = "LANDED! Press ESC to quit.";
                float textWidth = msg.length() * 16; // crude estimate, adjust as needed
                float centerX = (SCREEN_WIDTH - textWidth) / 2f;
                float centerY = SCREEN_HEIGHT / 2f;
                drawString(msg, centerX, centerY);
            } else if (!lander.alive) {
                String msg = "CRASHED! Press ESC to quit.";
                float textWidth = msg.length() * 16; // crude estimate, adjust as needed
                float centerX = (SCREEN_WIDTH - textWidth) / 2f;
                float centerY = SCREEN_HEIGHT / 2f;
                drawString(msg, centerX, centerY);
            }

            // Swap buffers
            GLFW.glfwSwapBuffers(window);
        }

        // Cleanup
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }

}
