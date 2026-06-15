# Mathematical Concept Dependency Mapper

Mathematical Concept Dependency Mapper is a capstone project that turns mathematical text into an interactive prerequisite graph. The backend uses Spring Boot and Spring AI to extract concepts such as definitions, lemmas, theorems, propositions, and corollaries. It stores graph data in Neo4j and exposes it through a REST API. The frontend is a React + Vite + TypeScript app that renders the dependency graph with React Flow. The goal is to make abstract mathematical dependencies easier to inspect during learning, review, and demos.

## Architecture

- `frontend/`: React, Vite, TypeScript, React Flow, KaTeX rendering, and an Axios API client.
- `backend/`: Spring Boot 3, Java 17, Spring AI, Spring Data Neo4j, REST controllers, graph services, and layout logic.
- `neo4j`: graph database used by the backend over Bolt at `bolt://localhost:7687`.
- OpenAI: the backend reads `OPENAI_API_KEY` and uses Spring AI to extract structured graph data from pasted mathematical text.

Request flow:

```text
Browser -> React frontend -> Spring Boot REST API -> Spring AI/OpenAI
                                      |
                                      v
                                  Neo4j graph
```

## Prerequisites

- Java 17
- Maven 3.9 or newer
- Node.js 20 or newer
- Docker Desktop or Docker Engine with Docker Compose
- OpenAI API key

## Local Setup

Create a local environment file from the example:

```bash
cp .env.example .env
```

Edit `.env` and set your OpenAI key:

```bash
OPENAI_API_KEY=sk-your-key
NEO4J_PASSWORD=password
VITE_API_URL=http://localhost:8080/api
MOCK_AI=false
```

Start Neo4j:

```bash
make neo4j
```

Start the backend in a second terminal:

```bash
cd backend
export OPENAI_API_KEY=sk-your-key
export NEO4J_PASSWORD=password
mvn spring-boot:run
```

For an API-free demo, set `MOCK_AI=true` before starting the backend. The backend will return a deterministic continuity/Intermediate Value Theorem graph without calling OpenAI.

On Windows PowerShell, use:

```powershell
cd backend
$env:OPENAI_API_KEY = "sk-your-key"
$env:NEO4J_PASSWORD = "password"
mvn spring-boot:run
```

Start the frontend in a third terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the app at:

```text
http://localhost:5173
```

Useful Make targets:

```bash
make neo4j
make backend
make frontend
make test-backend
make test-frontend
make clean
```

Capstone docs:

- `DEMO.md`
- `docs/ARCHITECTURE.md`
- `docs/LIMITATIONS.md`
- `docs/FUTURE_WORK.md`

Curated examples and evaluation:

```bash
python scripts/evaluate_examples.py --mode mock
python scripts/evaluate_examples.py --mode backend --backend-url http://localhost:8080/api
```

## Demo Flow

1. Start Neo4j:

   ```bash
   make neo4j
   ```

   Neo4j Browser is available at `http://localhost:7474` with username `neo4j` and password `password`.

2. Start the backend:

   ```bash
   cd backend
   export OPENAI_API_KEY=sk-your-key
   export NEO4J_PASSWORD=password
   mvn spring-boot:run
   ```

   Confirm the backend is reachable:

   ```bash
   curl http://localhost:8080/api/graphs/health
   ```

3. Start the frontend:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

4. Paste text demo:

   Select `Text` in the sidebar and paste this sample mathematical text:

   ```text
   Definition (Metric Space). A metric space is a set X together with a distance function d that assigns a nonnegative real number d(x, y) to each pair of points and satisfies positivity, symmetry, and the triangle inequality.

   Definition (Open Ball). In a metric space, the open ball B(x, r) is the set of all points y such that d(x, y) < r.

   Definition (Open Set). A subset U of a metric space is open if for every point x in U there exists r > 0 such that B(x, r) is contained in U.

   Lemma (Open Balls Are Open). Every open ball in a metric space is an open set.

   Theorem (Union of Open Sets). Any union of open sets in a metric space is open.
   ```

5. File upload demo:

   Select `File` in the sidebar and upload a `.txt`, `.md`, or `.pdf` document containing mathematical definitions, lemmas, and theorems. PDF upload uses real PDF text extraction and includes page markers in the text sent to the backend analysis pipeline.

6. Submit the text or file and inspect the dependency graph. Click graph nodes to review the extracted statement/proof details and use the canvas controls to pan and zoom through dependencies.

## REST API

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/graphs/analyze` | Analyze pasted text and create a graph |
| `POST` | `/api/graphs/analyze/file` | Analyze uploaded `.txt`, `.md`, or `.pdf` content |
| `POST` | `/api/graphs/analyze/pdf` | Backward-compatible PDF upload wrapper |
| `GET` | `/api/graphs/{graphId}` | Return graph data for React Flow |
| `GET` | `/api/graphs/{graphId}/export` | Export graph JSON with concepts and dependency reasons |
| `GET` | `/api/graphs` | List previous analyses |
| `GET` | `/api/graphs/health` | Backend health check |

Example:

```bash
curl -X POST http://localhost:8080/api/graphs/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Metric Space Demo",
    "text": "Definition (Metric Space). A metric space is a set X with a distance function d. Definition (Open Ball). An open ball uses the metric distance. Lemma (Open Balls Are Open). Every open ball is an open set."
  }'
```

File upload example:

```bash
curl -X POST http://localhost:8080/api/graphs/analyze/file \
  -F "title=Uploaded Notes" \
  -F "file=@notes.pdf"
```

## Verification

Backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm install
npm run build
```

## Troubleshooting

### Missing `OPENAI_API_KEY`

If the backend fails during startup or analysis with an OpenAI configuration error, set the key before running the backend:

```bash
export OPENAI_API_KEY=sk-your-key
```

PowerShell:

```powershell
$env:OPENAI_API_KEY = "sk-your-key"
```

For local demos without an API key, use:

```bash
export MOCK_AI=true
```

PowerShell:

```powershell
$env:MOCK_AI = "true"
```

### Neo4j Not Running

If the backend cannot connect to Neo4j, start the database and verify the container is healthy:

```bash
make neo4j
docker compose ps
```

The backend expects:

```text
bolt://localhost:7687
username: neo4j
password: password
```

### Frontend Cannot Reach Backend

If the frontend shows network errors, confirm the backend is running:

```bash
curl http://localhost:8080/api/graphs/health
```

Then confirm `VITE_API_URL` is set to:

```text
http://localhost:8080/api
```

Restart the Vite dev server after changing frontend environment variables.

## Repository Hygiene

Generated files and local-only artifacts are ignored by `.gitignore`, including:

- `.idea/`
- `.vscode/`
- `target/`
- `node_modules/`
- `dist/`
- `build/`
- `.env`
- `.DS_Store`
- `__MACOSX/`
- `*.log`
