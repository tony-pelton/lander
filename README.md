# 🚀 Lunar Lander (LWJGL Edition)

Welcome to **Lunar Lander** — a retro-inspired, side-scrolling moon landing game, built with pure Java and LWJGL! This project is the result of some serious vibe coding between you (the pilot) and Cascade (your AI copilot). Together, we ditched old dependencies, leveled up our text rendering, and landed on a clean, modern, and fun codebase.

---

## ✨ Features
- **Smooth OpenGL graphics** using LWJGL
- **TrueType font HUD** with crispy `stb_truetype` rendering
- **Realistic physics** (gravity, thrust, rotation, and fuel)
- **Procedurally generated terrain** with a flat landing pad
- **Keyboard controls** for classic lunar lander gameplay
- **Fly-by-wire mode**: toggle between manual and automatic control for a modern piloting experience
- **No JOGL, no GLUT, no nonsense**

---

## 🧑‍🚀 Apollo-Inspired Engine Physics

Hold onto your visors! The lander’s engine and fuel system are now modeled after the legendary Apollo Lunar Module, using real-world data for a spicy boost in realism:

- **Lander mass (fully fueled):** 16,400 kg
- **Fuel mass:** 8,200 kg (that’s half the weight!)
- **Engine thrust:** 44,000 N (44 kN — enough to leap off the Moon)
- **Fuel burn rate:** 14.5 kg/s (watch that tank drop!)

We now calculate acceleration and velocity using actual thrust, mass, and lunar gravity. As you burn fuel, your lander gets lighter, just like the real Apollo missions. This means:
- **Heavier at launch, lighter at touchdown** — your handling changes as you descend!
- **Fuel is precious** — throttle wisely, or you’ll relive the tension of Armstrong and Aldrin’s final descent.

> _"Houston, Tranquility Base here. The Eagle has landed."_

All physics numbers were sourced and cross-checked with NASA’s Apollo documentation and public records. This is as close as you’ll get to flying the real thing — without leaving your chair!

---

## 🎮 How to Play
- **Arrow Up:** Increase throttle (main engine power)
- **Arrow Down:** Decrease throttle (main engine power)
- **Throttle:** Ranges from 0% (off) to 100% (full thrust) — adjust smoothly for precise control!
- **Arrow Left/Right:** Rotate lander (unless Space is held)
- **Space + Left/Right:** Fire side jets for horizontal movement (no rotation)
- **ESC:** Quit the game

Land softly on the red pad in the middle of the screen. If you land too hard, or at a bad angle, you’ll crash! Your fuel is limited, so plan your descent and throttle carefully!

---

## 🛰️ Fly-By-Wire Mode

Fly-by-wire mode lets you pilot the lander like a modern spacecraft:

- **Toggle Fly-By-Wire**: Press the `A` key to switch between manual and fly-by-wire mode at any time.
- **Automatic Throttle & Angle**: In fly-by-wire mode, the lander automatically adjusts its throttle and angle to reach your target vertical and horizontal velocities using a smart controller.
- **Set Target Velocities**: Use the arrow keys to increment your goal vertical (`goalVy`) and horizontal (`goalVx`) velocities:
    - **Up/Down**: Increase/decrease target vertical speed
    - **Left/Right**: Increase/decrease target horizontal speed
- **Manual Override**: Switch back to manual mode (press `A` again) to regain full control over throttle and rotation. Your goal velocities will sync to your current velocities.

Fly-by-wire is ideal for smooth, precise landings or for those who want to focus on strategy instead of fine motor control. Try switching modes mid-flight!

---

## 🛠️ Getting Started

### Prerequisites
- Java 17+
- Maven
- OpenGL-capable system

### Setup & Run
1. **Clone this repo** (or copy the codebase).
2. Make sure you have `Roboto-Regular.ttf` in `src/main/resources/fonts/` (download from [Google Fonts](https://fonts.google.com/specimen/Roboto)).
3. In the project root, run:
    ```sh
    mvn clean compile
    mvn exec:java -Dexec.mainClass="com.dsrts.lander.Lander"
    ```
4. Land that ship!

---

## 🤝 Vibe-Coded by Human + AI
This project was built in a collaborative, iterative, and fun way — with you providing the vision and Cascade helping with the code, refactors, and fixes. We made decisions together, solved rendering puzzles, and kept the codebase clean and modern.

If you want to extend the game, add new features, or just keep the vibes going, let’s keep coding!

---

Happy landings! 🌙
