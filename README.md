# Language Cards

Personal language-flashcard app: a word is shown with its English translation,
an AI-generated sample sentence, and TTS audio of that sentence. The backend
keeps a small buffer of pre-generated sentence+audio pairs per word so the
"next" card is always ready instantly.

- **Backend**: Java 21 / Spring Boot, Postgres, Claude API (sentence
  generation), self-hosted Piper (TTS)
- **Frontend**: React + TypeScript (Vite), served by the backend
- **Deployment**: Docker Compose; images built in CI and published to GHCR

## Prerequisites

- Docker + Docker Compose
- An Anthropic API key ([console.anthropic.com](https://console.anthropic.com))

## Local setup

```bash
cp .env.example .env
# edit .env: set ANTHROPIC_API_KEY and POSTGRES_PASSWORD
docker compose up --build
```

The app is served at `http://localhost:8080`.

## Importing words

Words are bulk-imported from a CSV file — there's no add-word UI. The CSV
needs a header row with a `word` column and an optional `translation` column
(rows without a translation get one filled in automatically the first time a
sample sentence is generated for that word).

```csv
word,translation
maison,house
chat,cat
manger
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

- `SentenceGenerationService` (Claude API) for the sentence + missing
  translation
- `TextToSpeechService` (Piper sidecar) for the audio

Both are defined as interfaces specifically so the provider can be swapped
later without touching the rest of the app — add a new `@Service`
implementation and point Spring at it.

## API

- `GET /api/cards/next?language=fr` — next card `{ word, translationEn,
  sentenceText, audioUrl }`
- `GET /api/audio/{id}` — streams the wav file for a card
- `POST /api/words/import?language=fr` — multipart CSV import

## Environment variables

| Variable | Description |
| --- | --- |
| `ANTHROPIC_API_KEY` | Claude API key |
| `ANTHROPIC_MODEL` | Model id (default `claude-opus-4-8`) |
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

`postgres` always comes from the official Docker Hub image; only
`backend`/`piper` are pulled from GHCR.
# language-cards
