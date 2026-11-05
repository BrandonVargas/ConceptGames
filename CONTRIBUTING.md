# Contributing to ConceptGames

Thanks for helping! Bug reports, translations, new minigames and better prompts are all welcome.

## Setup

```bash
cp local.properties.example local.properties   # point sdk.dir at your Android SDK
./gradlew :engine:test                          # fast, pure-Kotlin tests
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

You don't need an API key to work on games: the bundled sample packs (`app/src/main/assets/sample_packs/`) unlock every game.

### Develop the AI flow without a key

`tools/mock-gemini/mock_gemini.py` is a stand-in for the Gemini API (Python 3, no dependencies). It streams a pack slowly with thought summaries, refuses Google Search the way free keys do, and can simulate a rate limit, so you can work on the generation screen without spending quota:

```bash
python3 tools/mock-gemini/mock_gemini.py          # MOCK_GROUNDING=ok|fallback|nofree, MOCK_RATE_LIMIT_ONCE=1
adb reverse tcp:8787 tcp:8787
echo "gemini.baseUrl=http://localhost:8787/v1beta" >> local.properties   # debug builds only
```

Enter any key in the app (`bad` simulates an invalid one). Remove the `gemini.baseUrl` line to go back to the real API.

## How to add a minigame

Every game is built from the same `StudyPack`, so a new game never needs a new AI call.

1. **Engine: the rules.** Add a file in `engine/src/main/kotlin/.../games/builders/`:

   ```kotlin
   data class MyGameRound(val items: List<Item>) : GameRound {
       override val gameId = GameId.MyGame
   }

   object MyGameBuilder : GameBuilder<MyGameRound> {
       override val id = GameId.MyGame
       override fun isAvailable(pack: StudyPack) = pack.concepts.size >= 4   // enough material?
       override fun build(pack: StudyPack, settings: BuildSettings): MyGameRound {
           // Use settings.random (never Random.Default) so tests are reproducible,
           // settings.byDifficulty(easy, medium, hard) for sizes,
           // and pickWeighted(...) so frequently missed concepts show up more.
       }
   }
   ```

   Then add `MyGame` to the end of the `GameId` enum. Ids are stored in the database, so never rename existing ones. Register the builder in `GameCatalog.builders`, and add a branch to `GameRound.conceptIds()` if your round practises concepts.

2. **App: the screen.** Add `app/src/main/java/.../ui/games/MyGame.kt` with
   `@Composable fun MyGame(round: MyGameRound, onFinish: (GameResult) -> Unit)` and register it in `GameScreenRegistry.kt`. Reuse `GameColumn`, `GameProgress`, `OptionButton`, `FeedbackPanel` and `rememberGameFeedback()` for a consistent look. Call `onFinish` exactly once.

3. **Strings and metadata.** Add a name, a description and an icon in `ui/components/Common.kt` (`GameId.meta()`), with strings in both `values/strings.xml` and `values-es/strings.xml`.

`GameCatalogTest` automatically builds every available game for every sample pack at every difficulty. Add a branch to its `check()` with your game's invariants.

### Needs more data from the AI?

If your game needs a new kind of content, add an optional field to `RawPack` (engine), the cleaned field to `StudyPack`, validation in `PackValidator`, and the matching instructions and schema in `PackPrompt`. Keep it optional so older saved packs still load.

## Guidelines

* Games must work in **any language and script**. Use `Graphemes.split` instead of iterating chars, and the pack's alphabet (`Alphabets.forPack`) instead of A–Z.
* Offer a **tap alternative to every drag** gesture, for accessibility.
* No new network services, analytics or proprietary SDKs. The app stays F-Droid friendly.
* Never commit API keys. `local.properties` is git-ignored.

## Translations

UI strings live in `app/src/main/res/values*/strings.xml`. To add a language, copy `values/strings.xml` into `values-xx/`, translate it, and add the locale to `res/xml/locales_config.xml` and `localeFilters` in `app/build.gradle.kts`.
