# Green UI Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task by task.

**Goal:** Apply the approved green, light surface visual direction to the existing Android app without changing feature behavior or the bottom tab structure.

**Architecture:** Keep the existing XML View layouts, view IDs, fragments, and navigation. Update semantic color resources and shared backgrounds first, then revise the home layout's visual hierarchy. Existing secondary pages inherit shared tokens.

**Tech Stack:** Android XML layouts, drawable resources, Kotlin fragments, Gradle.

**Spec:** [approved green visual preview](../../../design/ui_preview_v3/overview.png).

## Global Constraints

- Keep the four existing bottom tabs and their destinations.
- Keep fragment IDs and click targets used by Kotlin code.
- Do not change scan, deletion, compression, permission, ad, or analytics behavior.
- Preserve existing uncommitted user work.

## Tasks

- [x] Update semantic colors and shared button/hero drawables to the green palette.
- [x] Restyle the home scan card, widget prompt, storage summary, and feature cards using existing view IDs.
- [x] Align secondary page card surfaces through shared colors and drawable resources.
- [x] Compile the app, inspect the diff, and correct any resource or layout errors.
