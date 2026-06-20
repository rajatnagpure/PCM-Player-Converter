# Design Language & System Guide

Welcome to the **PCM Player & Converter Design System**. This document defines the user interface (UI) principles, layout heuristics, visual tokens, and user experience (UX) guidelines governing the application.

---

## 1. Core Principle: Tactile Neumorphism (Soft UI)

The design language of this app belongs to **Soft Neumorphism**, themed like a hardware synthesizer or a physical audio mixer board. Instead of flat cards floating in empty space with ambient dropshadows, elements in the UI are **extruded from or indented into** the parent surface.

### The Physics of Shadows

Light is simulated as coming from the **top-left corner (135° angle)**.
- **Extruded (Pressed = false):** Creates a "floating/raised" button or card.
  - *Top-Left Shadow:* Light color, reflecting incoming light.
  - *Bottom-Right Shadow:* Dark color, casting a shadow away from the light.
- **Inset (Pressed = true / Inputs):** Creates a "pressed" button or a container designed to hold something (like text inputs, icons, or progress sliders).
  - *Inner shadows* simulate depth sinking into the screen.

---

## 2. Color Palettes (Theme Configs)

The interface supports four distinct themes. Each theme provides a unique accent and shifts between deep midnight aesthetics and high-contrast daylight surfaces.

| Theme | Type | Background | Light Shadow | Dark Shadow | Accent / Primary |
|---|---|---|---|---|---|
| **Azure** | Dark | `#2E3239` | `#454B55` | `#1A1C20` | `#00D2FF` (Azure Blue) |
| **Midnight** | Dark | `#2E3239` | `#454B55` | `#1A1C20` | `#9b5de5` (Violet) |
| **Daylight** | Light | `#E0E4EC` | `#FFFFFF` | `#C2C7D1` | `#0082A8` (Deep Blue) |
| **Sunrise** | Light | `#E0E4EC` | `#FFFFFF` | `#C2C7D1` | `#FF6B4A` (Orange) |

---

## 3. Typography System

The typography is built around **Monospace** typefaces, creating a retro-futuristic digital display feel, reminiscent of hardware frequency analyzers and command-line interfaces.

### Hierarchy & Weights

To prevent monospace layouts from looking thin, sparse, or unreadable, we utilize **heavier, denser font weights**:

1. **Titles (`titleLarge`, `titleMedium`, `titleSmall`)**
   - **Weight:** `FontWeight.Bold` (W700)
   - **Usage:** Screen headings, prominent branding titles, main dialog headers.
2. **Body Text (`bodyLarge`, `bodyMedium`, `bodySmall`)**
   - **Weight:** `FontWeight.Medium` (W500)
   - **Usage:** Card contents, descriptive texts, list item details.
3. **Interactive Labels (`labelLarge`, `labelMedium`, `labelSmall`)**
   - **Weight:** `FontWeight.SemiBold` (W600)
   - **Usage:** Button labels, tab selectors, configuration settings, status pills.

---

## 4. Tactile & Haptic Feedback

Neumorphism is a highly visual-tactile interface; therefore, physical interactions must feel organic.

### Press States
Buttons must dynamically toggle their neumorphic state to `isPressed = true` on contact and transition smoothly back to `isPressed = false` when released.

### Haptic Feedback
- **Standard Interactions:** Every click event on buttons, tab items, toggles, selectors, and cards triggers a tactile `HapticFeedbackConstants.KEYBOARD_TAP`.
- **Preference Controls:** Users can enable or disable haptic feedback at any time via the Settings dialog under the **HAPTICS** section. By default, haptics are **ON**.
