# 🚀 Lunar Lander (LWJGL Edition)

Welcome to **Lunar Lander** — a retro-inspired, side-scrolling moon landing game, built with pure Java and LWJGL! This project is the result of some serious vibe coding between you (the pilot) and Cascade (your AI copilot). Together, we ditched old dependencies, leveled up our text rendering, and landed on a clean, modern, and fun codebase.

---

## ✨ Features
- **Smooth OpenGL graphics** using LWJGL
- **TrueType font HUD** with crispy `stb_truetype` rendering
- **Realistic physics** (gravity, thrust, rotation, and fuel)
- **Procedurally generated terrain** with a flat landing pad
- **Keyboard controls** for classic lunar lander gameplay
- **No JOGL, no GLUT, no nonsense**

---

## 🎮 How to Play
- **Arrow Up:** Main engine (thrust)
- **Arrow Left/Right:** Rotate lander
- **Space:** Side jets (for fine control)
- **ESC:** Quit the game

Land softly on the red pad in the middle of the screen. If you land too hard, or at a bad angle, you’ll crash! Your fuel is limited, so plan your descent!

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

## 📄 License
MIT License (or your choice — just keep the vibes positive!)

---

Happy landings! 🌙
