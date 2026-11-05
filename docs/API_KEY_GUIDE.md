# Get your free Gemini API key

*[Leer en español](API_KEY_GUIDE.es.md)*

ConceptGames uses Google's **Gemini** AI to turn a topic into study games. Gemini has a **free tier**, so each player brings their own key. That means there's no server to run, no shared quota to run out, and no account to create in the app.

It takes about two minutes, and you don't need a credit card.

## Steps

1. **Open Google AI Studio.** Go to **<https://aistudio.google.com/apikey>** and sign in with any Google account. In the app, the *Open Google AI Studio* button takes you straight there.
2. **Create an API key.** Tap **Create API key**. If you're asked to choose a Google Cloud project, the default one is fine, or you can create a new one. AI Studio turns on the Gemini API for that project automatically.
3. **Copy the key.** It's a long code that usually starts with `AIza`. Use the copy button next to it.
4. **Paste it in ConceptGames.** Go back to the app, tap **Paste**, then **Test and save**. The app sends one free request (the model list) to check that Google accepts the key, and only then saves it.

   ✅ *"Your key works!"* means you're ready to create games.

You can reopen the guide any time from **Settings → AI → Manage**.

## Good to know

| | |
|---|---|
| 🔒 **Keep it private** | Anyone with your key can use your quota. Don't share it, commit it to git, or post it online. |
| 💸 **Stay free** | Don't enable billing on the project unless you want to pay. Without billing, Google simply rejects requests once you reach the free limit; you are never charged. |
| 🛡️ **What gets sent** | The topic, the Wikipedia text you picked and the prompt are sent to the Gemini API. On the free tier Google may use this content to improve its products ([Gemini API terms](https://ai.google.dev/gemini-api/terms)). Don't type personal information. |
| 📱 **Where it's stored** | The key is encrypted with an AES key that lives in your phone's Android Keystore. It is excluded from backups and only ever sent to `generativelanguage.googleapis.com`. |
| ♻️ **Revoke or rotate** | Delete the key in AI Studio whenever you like, create a new one and paste it in the app. *Settings → Manage → Remove saved key* deletes it from the phone. |
| 🔎 **Web search** | ConceptGames can add a Google Search step ("grounding") on top of Wikipedia. Google doesn't include grounding in the free tier of its newest models, so with a free key this step is usually skipped and the screen says why. Your games are still made from the Wikipedia articles you picked. You can turn the step off in *Settings*. |
| ⏳ **Limits** | Free keys have per-minute and per-day limits that Google changes from time to time. Check your current limits in [AI Studio](https://aistudio.google.com/usage). Creating a topic costs 1 request, or 2 with web search on. Replaying games costs nothing. |

## Troubleshooting

| Message in the app | What it means | What to do |
|---|---|---|
| **That key doesn't work** (HTTP 400 `API_KEY_INVALID` or 401/403) | The key is mistyped, incomplete or deleted. | Copy it again from AI Studio. Make sure there are no spaces or missing characters. |
| **Gemini API is turned off** (403 `SERVICE_DISABLED`) | The key belongs to a Cloud project where the Generative Language API isn't enabled. | Create the key from AI Studio, which enables the API, or enable *Generative Language API* in the Google Cloud console. |
| **Not available in your region** (400 `FAILED_PRECONDITION`) | The Gemini API isn't offered where you are. | See the [available regions](https://ai.google.dev/gemini-api/docs/available-regions). |
| **Free quota reached** (429 `RESOURCE_EXHAUSTED`) | You hit a per-minute or daily free limit. | Wait a minute, or try again tomorrow. You can also pick a lighter model (e.g. `gemini-flash-lite-latest`) in *Settings*. |
| **Model not available** (404) | The model selected in *Settings* doesn't exist for your key. | Choose `gemini-flash-latest` or another model from the list. |
| **Gemini is busy** (500/503) | Google's servers are overloaded. | The app retries automatically. If it still fails, try again shortly. |

## For developers

Debug builds can prefill a key from the git-ignored `local.properties`:

```properties
gemini.apiKey=AIza...
```

Never commit a real key. CI builds don't need one, because the tests use mocked responses.
