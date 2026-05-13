# MuPDF Android Viewer — v1.28.0-beta.1

> **This is a beta release.** Features are functional but not fully hardened.
> Feedback and bug reports are welcome via [GitHub Issues](../../issues).

---

## What's New

### E-ink Screen Support

The viewer now has a dedicated settings menu (wrench icon in the top bar) with two toggles aimed at e-ink devices:

- **Disable Animations** — removes all scroll and page-turn animations. Page transitions become instant, which prevents ghosting on e-ink panels. Fling gestures jump directly to the next or previous page instead of smooth-scrolling.
- **E-ink Refresh** — triggers a full-screen redraw after each page settles, improving contrast and clearing residual images on devices that support a hardware refresh cycle.

Both settings persist across app restarts.

### AI-Assisted Reading (Beta)

A new AI reading assistant is layered on top of the PDF viewer. To use it:

1. Tap the **AI** button in the bottom-right corner to enter AI mode.
2. **Draw a circle or gesture** over any area of the page.
3. The app extracts the page text and sends it to OpenAI. A **bottom panel** slides up and streams the response in real time.
4. The response is structured as:
   - **AI Explanation** — 2–4 plain-language sentences on the core ideas.
   - **Atomic Note** — a compact note with Core Concept, Key Insight, and related fields.
5. Tap **Save Atomic Note** to export the result as a `.md` file to `Documents/ObsidianVault/Atoms/` for use in Obsidian.

> **Requires an OpenAI API key.** Set it once via Settings → "Set OpenAI API Key". The key is encrypted on-device using AES256-GCM and never stored in plain text.

### Obsidian Export

Generated atomic notes are saved as Markdown files with `#atom #atomread` tags, ready to drop into an Obsidian vault.

---

## Installation

1. Download the APK for your device architecture below.
2. Enable **Install from unknown sources** in Android Settings if prompted.
3. Install and open the app.

| APK | Architecture |
|-----|-------------|
| `mupdf-viewer-1.28.0-beta.1-arm64-v8a.apk` | Most modern phones (64-bit ARM) |
| `mupdf-viewer-1.28.0-beta.1-armeabi-v7a.apk` | Older / 32-bit ARM devices |
| `mupdf-viewer-1.28.0-beta.1-x86_64.apk` | Emulators / x86 devices |

**Minimum Android version: 7.0 (API 24)**

---

## Known Limitations

- The AI gesture layer (`IonizationOverlay`) detects any completed drag gesture, not specifically a circle. A circle-detection algorithm is planned for the next release.
- `AtomReadActivity` (the alternate PDF opener in the system "Open with" sheet) currently uses a placeholder AI response; real API calls are only wired through `AiDocumentActivity`.
- `BondingView` (visual concept-linking UI) is included in the build but not yet accessible from the main flow.
- E-ink refresh relies on a generic `invalidate()` call. Devices with a native full-refresh API (e.g., ONYX BOOX) may need device-specific integration for optimal results.
- `WRITE_EXTERNAL_STORAGE` is required for Obsidian export on Android < 10. On Android 10+, the app writes to the shared `Documents` folder without this permission.

---

## Changes Since 1.27.1a

- Add e-ink animation toggle and full-screen refresh trigger (`ReaderView`)
- Add Settings popup menu with persistent preferences (`DocumentActivity`)
- Add `AiDocumentActivity` with Jetpack Compose AI overlay
- Add `IonizationOverlay` gesture layer with vibration feedback
- Add `OpenAiClient` with SSE streaming support
- Add `AiPanel` bottom sheet for real-time response rendering
- Add `MuPDFCore.getPageText()` for structured text extraction
- Add `SecurePreferences` for encrypted API key storage
- Add `ObsidianExporter` for Markdown note export
- Add `AtomReadActivity` as alternate PDF intent handler
- Add `BondingView` prototype (not yet in main flow)
- Introduce Kotlin, Jetpack Compose (Material3), OkHttp, Coroutines
- Bump `compileSdkVersion` 33 → 34, `minSdkVersion` 21 → 24, Java 17
