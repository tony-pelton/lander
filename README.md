# 🚀 Lunar Lander (LibGDX + Box2D Edition)

Welcome to the evolved **Lunar Lander** — a high-fidelity, physics-driven moon landing simulation. We've migrated from raw LWJGL to the powerful **LibGDX** framework and integrated the **Box2D** rigid-body physics engine to deliver a modern, precise, and visually striking piloting experience.

---

## ✨ Key Features
- **LibGDX Framework**: Modern lifecycle management and robust rendering pipeline.
- **Box2D Physics**: Real-world rigid-body dynamics, including mass-center calculations, torque, and collision manifold detection.
- **Drone Aesthetic**: A dual-nacelle craft design with bidirectional (orange/blue) thruster flames for superior control.
- **Dedicated Dashboard**: A professional 20/80 screen split HUD featuring:
    - **Circular VVI**: Vertical Velocity Indicator with 0.0 at the 9 o'clock position.
    - **Trend Ribbon**: Predictive acceleration bar showing where your velocity will be in 1 second.
    - **Goal Carets**: Visual target markers for Fly-By-Wire (FBW) setpoints.
    - **Bar Instruments**: High-visibility vertical gauges for Throttle and Fuel.
- **Procedural Terrain**: Dynamically generated lunar landscapes with identified landing zones.

---

## 🧑‍🚀 Apollo-Inspired Engine Physics

The physics model has been refined using historical Apollo Lunar Module data, now simulated through Box2D's high-precision integration:

- **Dry Mass:** 4,214 kg
- **Total Fuel Capacity:** 10,867 kg
- **Max Engine Thrust:** 44,000 N (44 kN)
- **Fuel Burn Rate:** 14.5 kg/s

### Dynamic Handling
As you burn fuel, the craft's total mass decreases, significantly altering its TWR (Thrust-to-Weight Ratio) and rotational inertia. A lighter lander is more responsive but harder to settle—simulating the real-world challenge of the final "bingo fuel" phase of a moon landing.

---

## 🛰️ Advanced Flight Control (FBW)

The **Fly-By-Wire (FBW)** system has been upgraded to a high-precision **PID (Proportional-Integral-Derivative)** controller:

- **Vertical Lock**: Automatically maintains target vertical velocity (`goalVy`), accounting for craft tilt and mass changes.
- **Attitude Hold**: Automatically stabilizes the craft's angle (`goalAngle`), allowing the pilot to focus on horizontal translation.
- **Smoothing & Filtering**: Implements non-linear error responses and low-pass signal filtering to ensure rock-solid instrumentation stability even during aggressive maneuvers.

---

## 🎮 How to Play
- **A**: Toggle Fly-By-Wire (FBW) Mode.
- **W / S**: (Manual) Adjust Throttle | (FBW) Adjust Target Vertical Velocity.
- **A / D**: (Manual) Rotate | (FBW) Adjust Target Angle for horizontal translation.
- **ESC**: Quit to desktop.

### Landing Criteria
To achieve a successful landing (indicated by a **GREEN** craft), you must:
1. Contact a designated **RED** landing pad.
2. Maintain a vertical/horizontal velocity below **2.0 m/s**.
3. Keep your tilt angle within **±10 degrees** of vertical.

---

## 🛠️ Getting Started

### Prerequisites
- **Java 17+**
- **Maven 3.8+**
- A system supporting OpenGL 2.0+

### Build & Run
1. In the project root, compile the source:
    ```sh
    mvn clean compile
    ```
2. Launch the simulation:
    ```sh
    mvn exec:java -Dexec.mainClass="com.dsrts.lander.Lander"
    ```

---

## 🤝 Vibe-Coded by Human + AI
This project represents a collaborative journey into modern game engineering. We've moved from simple Euler integration to complex rigid-body solvers, transitioned frameworks, and polished the user experience through iterative telemetry and control-theory tuning.

Happy landings! 🌙
