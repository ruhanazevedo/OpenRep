# OpenRep

> Training your body shouldn't depend on hidden systems you don't understand or expensive apps you can't control.

OpenRep is an open-source Android workout generator built on a simple belief: good training knowledge shouldn't be locked away. Most fitness apps today are closed, generic, and designed around subscriptions — not people. OpenRep is different. Every workout it generates, every exercise it recommends, every line of logic behind it is open for anyone to read, question, and improve.

---

## What it does

- **Generates personalized workouts** based on your goals, available equipment, fitness level, and time
- **Browse and filter exercises** across muscle groups, difficulty, and equipment type
- **Exercise detail view** with images, instructions, and linked video demos
- **Save and manage workout plans** for future sessions
- **Add custom exercises** with your own instructions and media
- **Remote exercise media config** — images and videos can be updated without a new app release

---

## Tech stack

- **Kotlin** + **Jetpack Compose**
- **Room** for local persistence
- **Hilt** for dependency injection
- **Retrofit + Moshi** for network requests
- **Coil** for image loading
- **MVVM** architecture with StateFlow

---

## Getting started

### Requirements
- Android Studio Hedgehog or newer
- Android SDK 26+
- A YouTube Data API v3 key (optional — only needed for video search)

### Setup

```bash
git clone https://github.com/ruhanazevedo/workout-generator.git
cd workout-generator
```

Add your YouTube API key to `local.properties`:

```
YOUTUBE_API_KEY=your_key_here
```

Then open the project in Android Studio and run on a device or emulator (API 26+).

---

## Exercise media

Exercise images are served from a remote config file hosted on GitHub. To add or update images for an exercise without a new release, edit `exercise-media-config.json` directly on the main branch.

---

## Contributing

OpenRep is open source and contributions are welcome. Whether it's fixing a bug, adding an exercise, improving the workout generation logic, or translating the app — all pull requests are appreciated.

1. Fork the repository
2. Create a feature branch (`git checkout -b feat/your-feature`)
3. Commit your changes
4. Open a pull request

---

## License

MIT — see [LICENSE](LICENSE) for details.
