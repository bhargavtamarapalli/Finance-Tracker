---
name: Obsidian Analytics
colors:
  surface: '#131315'
  surface-dim: '#131315'
  surface-bright: '#39393b'
  surface-container-lowest: '#0e0e10'
  surface-container-low: '#1b1b1d'
  surface-container: '#201f21'
  surface-container-high: '#2a2a2c'
  surface-container-highest: '#353437'
  on-surface: '#e5e1e4'
  on-surface-variant: '#cfc2d6'
  inverse-surface: '#e5e1e4'
  inverse-on-surface: '#303032'
  outline: '#988d9f'
  outline-variant: '#4d4354'
  surface-tint: '#ddb7ff'
  primary: '#ddb7ff'
  on-primary: '#490080'
  primary-container: '#b76dff'
  on-primary-container: '#400071'
  inverse-primary: '#842bd2'
  secondary: '#4edea3'
  on-secondary: '#003824'
  secondary-container: '#00a572'
  on-secondary-container: '#00311f'
  tertiary: '#ffb3b0'
  on-tertiary: '#670211'
  tertiary-container: '#ea6767'
  on-tertiary-container: '#5b000d'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#f0dbff'
  primary-fixed-dim: '#ddb7ff'
  on-primary-fixed: '#2c0051'
  on-primary-fixed-variant: '#6900b3'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffdad8'
  tertiary-fixed-dim: '#ffb3b0'
  on-tertiary-fixed: '#410006'
  on-tertiary-fixed-variant: '#881d24'
  background: '#131315'
  on-background: '#e5e1e4'
  surface-variant: '#353437'
typography:
  headline-lg:
    fontFamily: Manrope
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Manrope
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Manrope
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  headline-lg-mobile:
    fontFamily: Manrope
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 32px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-padding: 1.25rem
  gutter: 1rem
  stack-sm: 0.5rem
  stack-md: 1rem
  stack-lg: 1.5rem
---

## Brand & Style

This design system embodies a **Premium Matte** aesthetic, specifically tailored for high-end financial analytics and data visualization. It targets a sophisticated audience that values precision, discretion, and a focused work environment.

The visual direction combines **Minimalism** with subtle **Glassmorphism**. By using deep charcoal surfaces instead of pure black, the UI achieves a "soft-touch" matte feel that reduces eye strain while maintaining high perceived value. Interactive elements leverage vibrant glows to provide feedback, creating a sense of depth and luminosity within a dark, structured environment.

## Colors

The palette is anchored by a **Deep Charcoal** neutral base, which provides a more premium feel than absolute black. 

- **Primary & Secondary:** A sophisticated pairing of Electric Purple and Emerald Green represents growth and technological precision.
- **Data Visualization:** A curated set of chart colors (Emerald, Purple, and Soft Reds) ensures distinct categorization while maintaining a harmonious, jewel-toned saturation level.
- **Interactive States:** Instead of traditional hover states, elements utilize a subtle primary-tinted glow (`interactive_glow`) to signify focus and interactivity.

## Typography

The system utilizes **Manrope** for its modern, balanced proportions that maintain legibility at small sizes—essential for data-heavy dashboards.

**JetBrains Mono** is used selectively for labels, data points, and currency values to evoke a sense of technical accuracy and "fintech" utility. Headlines use a tighter letter-spacing to feel more compact and architectural.

## Layout & Spacing

This design system uses a **Fluid Grid** model with a specific focus on vertical rhythm. Content is housed within 12-column layouts on desktop and a single-column stack on mobile.

The spacing rhythm is built on a 4px baseline, with 20px (1.25rem) standard side margins for mobile devices to give the "frosted" cards room to breathe. Components should use generous internal padding to maintain the "premium" feel and avoid data density fatigue.

## Elevation & Depth

Depth is achieved through **Glassmorphism** and **Tonal Layering** rather than traditional drop shadows.

1.  **Background:** The lowest layer is the `#121214` charcoal base.
2.  **Cards:** Surface containers use `#1A1A1E` with a 1px low-opacity border (`rgba(255, 255, 255, 0.08)`) to create a "frosted" edge.
3.  **Active Elements:** Highlights use a subtle backdrop blur (12px to 20px) and a soft inner glow to appear as if they are illuminated from within.

## Shapes

The design system uses a **Rounded** shape language to soften the industrial charcoal palette.

- **Cards/Containers:** 1rem (`rounded-lg`) corner radius.
- **Buttons/Inputs:** 0.5rem corner radius.
- **Data Points:** Perfect circles for chart nodes to contrast against the linear geometry of the grid.

## Components

### Cards
Cards are the primary container. They feature a subtle gradient fill from top-left to bottom-right, transitioning between slightly different shades of charcoal to mimic a matte finish. Borders are 1px solid with 10% white opacity.

### Buttons
Primary buttons use a solid primary fill with a soft 12px outer glow of the same color. Secondary buttons use the "ghost" style with the frosted border treatment.

### Charts & Data
Lines in trend charts use a 3px stroke with a vertical gradient that fades into the background. Data nodes (dots) are white with a 4px outer glow to indicate the "active" or "current" point.

### Lists & Insights
List items are separated by subtle 1px dividers. Icons within lists (like "Insights") are contained within small, rounded-square backgrounds with 10% opacity of their respective functional color (e.g., a red icon on 10% red background).

### Navigation
The bottom navigation bar uses a heavy backdrop blur (20px) and 85% opacity to allow chart colors to bleed through as the user scrolls, reinforcing the glassmorphic theme.