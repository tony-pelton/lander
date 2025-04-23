package com.dsrts.lander;

public class Terrain {
    // Terrain: 4800 samples, 1 sample per 0.15625m (750m/4800)
    public static float[] terrainHeights = new float[4800];

    public static int[] padCenters = new int[] {
        terrainHeights.length / 7,        // First pad
        terrainHeights.length * 2 / 7,    // Second pad
        terrainHeights.length * 3 / 7,    // Third pad
        terrainHeights.length * 4 / 7,    // Fourth pad
        terrainHeights.length * 5 / 7,    // Fifth pad
        terrainHeights.length * 6 / 7     // Sixth pad
    };

    public static int padWidth = 100; // Original pad width to maintain scale

    public static void generateTerrain() {
        double base = 10; // meters above bottom
        int n = terrainHeights.length;
        int padCount = padCenters.length;
        // double mountainHeight = Math.random() * 100 + 20;

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
            // double peak = base + mountainHeight;
            double peak = base + Math.random() * 50 + 20;
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
}
