# SyncProbe

SyncProbe is an AI-powered documentation drift scanner.

It analyzes a repository, compares docs and code semantically, and returns suggested fixes for stale documentation. The project includes:

- A Spring Boot backend for ingestion, chunking, embedding, vector search, and suggestion generation
- A React + Tailwind dashboard for submitting scans and reviewing proposed fixes

## What It Does

- Clones or ingests a repository
- Parses documentation and source code
- Chunks content and generates embeddings
- Stores vectors in Chroma
- Detects potentially stale documentation
- Generates suggested documentation fixes (LLM-assisted)
- Exposes async scan APIs (`start scan` + `poll status`)

## Project Structure

```text
sync-probe/
├── backend/    # Spring Boot API + scan pipeline
├── frontend/   # React dashboard (Vite + Tailwind)
└── mvnw        # Maven wrapper
```

## Tech Stack

- Backend: Java 21, Spring Boot 3.2, Maven
- AI/Vector: LangChain4j, Gemini (chat + embeddings), Chroma
- Persistence: PostgreSQL (scan/job state + mapping records)
- Frontend: React, TypeScript, Vite, Tailwind CSS

## Prerequisites

- Java 21+
- Node.js 20+
- Docker (for Chroma)
- PostgreSQL running locally or remotely
- Gemini API key

## Configuration

Backend config is in:

- `backend/src/main/resources/application.yaml`

Key values to verify:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `chroma.base-url`
- `GEMINI_API_KEY` environment variable

Example env setup:

```bash
export GEMINI_API_KEY=your_key_here
```

## Running Dependencies

### 1) Start Chroma

Use this image version to stay compatible with the currently used Chroma API behavior:

```bash
docker run -d --name chroma \
  -p 8000:8000 \
  -v chroma-data:/data \
  ghcr.io/chroma-core/chroma:0.4.24
```

Sanity check:

```bash
curl http://127.0.0.1:8000/api/v1/collections
```

### 2) Start PostgreSQL

Make sure the database in `application.yaml` exists (for example `syncprobe`) and credentials are correct.

## Run the Backend

From repo root:

```bash
./mvnw -f backend/pom.xml spring-boot:run
```

Default Spring Boot port is `8080` unless overridden.

Health check:

```bash
curl http://localhost:8080/health
```

## Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

The UI defaults to calling:

- `http://localhost:8080`

Override with:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

## API Endpoints

### Async scan flow (recommended)

1) Start a scan:

```http
POST /api/scans
Content-Type: application/json

{
  "repoUrl": "https://github.com/owner/repo.git"
}
```

Response:

```json
{
  "scanId": "uuid"
}
```

2) Poll status:

```http
GET /api/scans/{scanId}
```

Response:

```json
{
  "status": "RUNNING | SUCCEEDED | FAILED",
  "result": {
    "proposedFixes": [
      { "text": "..." }
    ]
  },
  "error": "..."
}
```

### Other endpoints

- `POST /api/scan` (direct scan path)
- `POST /api/scan/query`
- `GET /api/scan/hello`
- `GET /health`

## Dashboard UX

The frontend dashboard provides:

- Repo URL input + **Scan Repository** button
- File matrix with:
  - File Name
  - Status (`HEALTHY`, `STALE`, `SYNCING`)
  - Score (`--%` when not available)
  - Actions (`VIEW DOCS`, `HEAL DOCS`, `Please Wait`)
- Proposed Fix section with:
  - `REJECT`
  - `COMMIT FIX`

## Tests

Run backend tests:

```bash
./mvnw -f backend/pom.xml test
```

## Known Notes

- Current suggestions are mostly text-based and not yet a fully structured file-diff contract.
- `COMMIT FIX` / `REJECT` are UI actions today; end-to-end fix application workflow is a next step.
- CORS config currently allows `http://localhost:3000`; Vite commonly runs on `5173`, so update backend CORS settings if needed.

## Roadmap

- Structured suggestion DTOs (`filePath`, `proposedPatch`, etc.)
- Real commit/PR workflow for accepted fixes
- Scan history and trend analytics
- Authentication

