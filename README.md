# DocBrain — AI Document Q&A Platform

DocBrain is an end-to-end AI-powered document intelligence platform. It allows users to upload documents (PDF, DOCX, TXT), processes them into vector embeddings using RAG (Retrieval-Augmented Generation), and lets users ask natural language questions against their documents with cited answers.

## 🚀 Features

*   **Document Upload & Processing:** Upload PDF, DOCX, and TXT files. The platform automatically extracts text, chunks it intelligently, and generates vector embeddings.
*   **Intelligent Q&A (RAG):** Ask natural language questions about your uploaded documents. The AI provides accurate answers based *only* on the provided context, complete with source citations.
*   **Workspace Management:** Organize documents into custom collections/workspaces.
*   **Chat History:** Full conversational history with contextual awareness.
*   **Rate Limiting & Caching:** Redis-backed sliding window rate limiting and query/embedding caching for performance.
*   **Secure:** JWT-based stateless authentication and user-scoped data access.

## 🛠️ Tech Stack

### Backend
*   **Java 21 / Spring Boot 3.4**
*   **Gradle (Kotlin DSL)**
*   **PostgreSQL 16 + pgvector** (Relational & Vector database)
*   **Redis** (Caching & Rate Limiting)
*   **Google Gemini API:** `gemini-2.0-flash` (LLM) & `text-embedding-004` (Embeddings)
*   **Apache Tika** (Document parsing)
*   **Spring Security + JWT**

### Frontend
*   **React 18 + TypeScript** (Vite)
*   **Tailwind CSS**
*   **React Router v6**
*   **Axios + React Query**

### Infrastructure
*   **Docker & Docker Compose**
*   **GitHub Actions** (CI/CD)
*   **Railway** (Backend Deployment)
*   **Vercel** (Frontend Deployment)

## 🏗️ Architecture

1.  **Ingestion:** Documents are uploaded -> Parsed by Apache Tika -> Split into overlapping chunks (~500 tokens) -> Embedded via Gemini -> Stored in pgvector.
2.  **Retrieval:** User asks a question -> Question is embedded -> Cosine similarity search against pgvector retrieves top-K relevant chunks.
3.  **Generation:** System prompt + retrieved chunks + conversation history + user question are sent to Gemini 2.0 Flash -> Answer with citations is returned.

## 💻 Running Locally

### Prerequisites
*   Java 21 JDK
*   Node.js (v18+)
*   Docker & Docker Compose
*   Google Gemini API Key (Get one free at Google AI Studio)

### 1. Start Database & Cache
```bash
cd docbrain
docker compose up -d
```
This starts PostgreSQL (with pgvector) on port `5432` and Redis on port `6379`.

### 2. Configure Backend
Copy `.env.example` to `.env` or set the following environment variables:
```properties
GEMINI_API_KEY=your_gemini_api_key_here
JWT_SECRET=a_very_long_secure_random_string_for_jwt_signing
```

### 3. Run Backend
```bash
cd docbrain
./gradlew bootRun
```
The API will be available at `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

### 4. Run Frontend
```bash
cd docbrain-ui
npm install
npm run dev
```
The UI will be available at `http://localhost:5173`.

## 🚢 Deployment

### Backend (Railway)
1. Link your GitHub repository to Railway.
2. Add PostgreSQL and Redis plugins in Railway.
3. Ensure the `pgvector` extension is enabled on the Railway Postgres instance.
4. Set the environment variables (`DATABASE_URL`, `REDIS_HOST`, `GEMINI_API_KEY`, `JWT_SECRET`).
5. Railway will automatically build and deploy using the provided `Dockerfile` and `railway.toml`.

### Frontend (Vercel)
1. Import the `docbrain-ui` folder into Vercel.
2. The `vercel.json` file handles routing for the React SPA.
3. Update the API proxy destination in `vercel.json` to point to your deployed Railway backend URL.

## 📄 License
MIT License
