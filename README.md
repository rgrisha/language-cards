# Language Cards

Personal language-flashcard app: a word is shown with its English translation,
an AI-generated sample sentence, and TTS audio of that sentence. The backend
keeps a small buffer of pre-generated sentence+audio pairs per word so the
"next" card is always ready instantly.

- **Backend**: Java 21 / Spring Boot, Postgres, self-hosted Piper (TTS), and a
  choice of sentence-generation provider: Claude API or a self-hosted
  CroissantLLM (via Ollama)
- **Frontend**: React + TypeScript (Vite), served by the backend
- **Deployment**: Docker Compose; images built in CI and published to GHCR

## Prerequisites

- Docker + Docker Compose
- If using Claude (the default): an Anthropic API key
  ([console.anthropic.com](https://console.anthropic.com))
- If using CroissantLLM instead: no API key needed, just more disk/RAM for
  Ollama's model (see below)

## Local setup

```bash
cp .env.example .env
# edit .env: set ANTHROPIC_API_KEY and POSTGRES_PASSWORD
docker compose up --build
```

The app is served at `http://localhost:8080`.

### Using CroissantLLM instead of Claude

Set `SENTENCE_PROVIDER=croissant` in `.env` (no `ANTHROPIC_API_KEY` needed) and
start the stack as usual:

```bash
docker compose up -d
```

The `ollama` service pulls the model automatically on first startup when
`SENTENCE_PROVIDER=croissant` (~870MB, cached in the `ollama-data` volume —
later restarts just do a quick local check, not a re-download). Claude-only
users pay no cost for this: the pull is skipped entirely when
`SENTENCE_PROVIDER` isn't `croissant`.

CroissantLLM is a small (1.3B parameter) open bilingual French/English model —
expect noticeably lower sentence quality than Claude, and slower generation on
CPU (fine here since generation happens in the background, not on the user's
click path).

## Importing words

Words are bulk-imported from a CSV file — there's no add-word UI. Translations
are supplied at import time (e.g. filled in a spreadsheet beforehand), not
generated, so the CSV **requires** both a `word` column and a `translation`
column; rows missing either are skipped.

```csv
word,translation
maison,house
chat,cat
```

Import via:

```bash
curl -X POST "http://localhost:8080/api/words/import?language=fr" \
  -F "file=@words.csv"
```

`language` is an arbitrary short code (e.g. `fr`, `es`, `de`) — it's just used
to filter which deck a word belongs to and to select the matching Piper voice
model. To add another language, add its voice model to `piper/Dockerfile` and
set `PIPER_MODEL` accordingly (see below).

## How content generation works

A background job (`CardBufferService`) keeps ~10 sample sentences (with
audio) generated ahead of demand for every imported word, calling:

- `SentenceGenerationService` for the sentence text — either
  `ClaudeSentenceGenerationService` or `CroissantSentenceGenerationService`,
  selected at runtime by the `SENTENCE_PROVIDER` env var (`claude` or
  `croissant`, via `@ConditionalOnProperty` so only one bean is ever active)
- `TextToSpeechService` (Piper sidecar) for the audio

Both are interfaces specifically so a provider can be swapped without
touching the rest of the app — add a new `@Service` implementation, gate it
with `@ConditionalOnProperty`, and add the config it needs.

## API

- `GET /api/cards/next?language=fr` — next card `{ word, translationEn,
  sentenceText, audioUrl }`
- `GET /api/audio/{id}` — streams the wav file for a card
- `POST /api/words/import?language=fr` — multipart CSV import

## Environment variables

| Variable | Description |
| --- | --- |
| `SENTENCE_PROVIDER` | `claude` (default) or `croissant` |
| `ANTHROPIC_API_KEY` | Claude API key (only when `SENTENCE_PROVIDER=claude`) |
| `ANTHROPIC_MODEL` | Model id (default `claude-opus-4-8`) |
| `OLLAMA_MODEL` | Ollama model tag (only when `SENTENCE_PROVIDER=croissant`) |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Postgres credentials |
| `BUFFER_SIZE` | Target sentences generated ahead per word (default 10) |
| `BUFFER_BATCH` | Words processed per scheduler tick (default 3) |
| `CARD_COOLDOWN` | Minimum cards between repeats of the same word (default 50) |
| `IMAGE_NAMESPACE` | GHCR namespace images are tagged/pulled under |

## Deployment (local machine pulling prebuilt images)

On push to `main`, GitHub Actions builds the `backend` and `piper` images and
publishes them to `ghcr.io/<you>/language-cards-{backend,piper}:latest`. On
the machine that runs the app:

```bash
cp .env.example .env   # first time only
docker compose pull
docker compose up -d
```

`postgres` and `ollama` always come from their official Docker Hub images;
only `backend`/`piper` are pulled from GHCR.
# language-cards
