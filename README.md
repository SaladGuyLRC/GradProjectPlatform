# ProjectHelper

ProjectHelper is a graduation-project collaboration platform for students, mentors and administrators. It is a modular Spring Boot application with a Vue 3 frontend, MongoDB for business records, Redis Stack for document vectors and short-lived AI conversations, and Spring AI Alibaba for Qwen tool calling.

## Local startup

Requirements: Java 21, Maven, Node.js 18+, Docker and Docker Compose.

1. Start MongoDB and Redis Stack:

   ```bash
   docker compose up -d
   ```

2. Configure the backend environment. Copy `.env.example` to your shell environment or export the values directly. `DASHSCOPE_API_KEY` is only required for live AI chat and PDF embedding. The server starts without it and returns `AI_NOT_CONFIGURED` from `/api/ai/chat`.

3. Start the backend:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

4. Start the frontend in a second terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

The frontend is served at `http://localhost:5173` and proxies `/api` to `http://127.0.0.1:8080`. Set `VITE_API_BASE_URL` and `VITE_API_PROXY_TARGET` when using a different deployment.

## Demo data

Set `SEED_ENABLED=true` before starting the backend. The idempotent demo accounts use the password `ProjectHelper@123`:

| Username | Role | Purpose |
| --- | --- | --- |
| `admin` | ADMIN | Organization and knowledge-base administration |
| `mentor1` | MENTOR | Mentor workflow and student review |
| `student1` | STUDENT | Primary student workflow |
| `student5` | STUDENT | Student without a project |
| `student7` | STUDENT | Disabled-account login boundary |

## API walkthrough

Login and use the returned JWT in the `Authorization: Bearer <token>` header:

```bash
curl -s http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"student1","password":"ProjectHelper@123"}'

curl -s 'http://localhost:8080/api/dashboard/student' -H "Authorization: Bearer $TOKEN"
curl -s 'http://localhost:8080/api/tasks?page=0&size=20' -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/ai/chat \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"message":"What are my recent tasks?","conversationId":"student1-demo"}'
```

Student data is object-scoped. A student cannot request another student's tasks, a mentor can only request tasks and reports for assigned students, and administrators cannot use AI to read student business data. AI tools do not expose weekly-report submission, report review or destructive deletion operations. AI conversations are isolated by authenticated user and conversation ID, retain the latest 20 messages, and expire after two hours in Redis.

## Verification

```bash
cd backend
mvn test

cd ../frontend
npm run build
```

Do not commit `node_modules`, `target`, uploaded PDFs, logs or API keys.
