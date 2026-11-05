# Privacy policy

*Last updated: 2026-10-09*

ConceptGames has **no accounts, no ads, no analytics and no servers** of its own.

## What leaves your device

| When | Sent to | What |
|---|---|---|
| You search for a topic | Wikipedia (`*.wikipedia.org`) and, to list languages, `meta.wikimedia.org` | Your search words and the language edition. |
| You create games | Google Gemini API (`generativelanguage.googleapis.com`), using **your own API key** | The topic, the text of the Wikipedia articles you selected, and the instructions for generating games. If *Search the web too* is on, Gemini also runs a Google Search for the topic. |
| You check your key | Google Gemini API | A request listing available models. |
| You open a source link | The website of that link | A normal web visit in your browser. |

On the Gemini free tier, Google may use submitted content to improve its products, according to the [Gemini API terms](https://ai.google.dev/gemini-api/terms). Avoid entering personal information as a topic.

## What stays on your device

* Your Gemini API key, encrypted with a key held in the Android Keystore. It is excluded from backups and device transfers.
* Your generated topics, game scores and per-concept progress, stored in a local database. You can delete any topic from the library.
* Your settings.

Uninstalling the app removes all of this.

## Contact

Open an issue at <https://github.com/BrandonVargas/ConceptGames/issues>.
