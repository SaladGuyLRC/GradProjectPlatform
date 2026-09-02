# ProjectHelper MVP Mentor Meeting Script

This document is a speaking script for reporting the minimum viable product (MVP) to a supervisor. The wording is intentionally conversational. It can be read directly or shortened during the meeting.

## Opening

> This week I focused on defining and stabilising the minimum viable product for ProjectHelper. The MVP is a graduation-project collaboration platform for students, mentors and administrators. It covers the main collaboration workflow: managing a project, assigning and completing tasks, submitting weekly reports, sharing project files, and using an AI assistant for project-related questions.

> I have kept the scope deliberately small. The goal is to demonstrate a complete and testable workflow rather than to build every possible university-management feature.

## 1. MVP Goal and Problem

> The problem is that project information is often spread across messages, documents and separate tools. ProjectHelper gives students and mentors one place to manage project information, deadlines, weekly progress and submissions. The MVP is designed to prove that this collaboration workflow is technically feasible and useful.

## 2. Target Users and Roles

> The system has three roles. Students manage their own project, tasks, weekly reports and paper submissions. Mentors view their assigned students, assign tasks, inspect project submissions and review weekly reports. Administrators manage users, organizations and the shared knowledge base. Access is controlled by the authenticated role and by the user-to-project relationship.

## 3. MVP Scope

> The MVP includes authentication, role-based access, organization and member management, student projects, PDF submissions, tasks, weekly reports, the AI assistant and a public knowledge base. It does not currently aim to provide email notifications, a full university registry integration, production-grade object storage, advanced analytics or a complete deployment platform.

## 4. Student Workflow

> A student logs in and sees a dashboard with project information, upcoming tasks and progress information. The student can create or edit a project summary, set the project dates, upload paper PDFs as versions, update task status, create weekly-report drafts and submit them to the mentor. The student can also ask the AI assistant questions about their own project data and the public knowledge base.

## 5. Mentor Workflow

> A mentor sees only assigned students. The mentor can select a student, view that student's project and uploaded submissions, create and assign tasks, and review submitted weekly reports. The mentor cannot access unrelated students. This gives the mentor a focused supervision workflow without exposing all student data.

## 6. Admin Workflow

> An administrator manages members and the organization tree. The administrator can create users with a default initial password, edit permitted profile fields, and disable accounts. Disabled users are filtered from the active organization tree. The administrator also uploads and manages public knowledge-base PDFs and can inspect indexing diagnostics.

## 7. Project and Paper Submission Flow

> A student creates a project with a title, summary, technology stack, repository URL and project dates. The student can upload a PDF submission. Each new upload creates a new version, while the mentor can see and download submissions from assigned students. The backend validates the file type and size and stores the file separately from the business record.

## 8. Task Flow

> Tasks have a title, description, type, priority, status, deadline and creator. A mentor can assign a task to a student. The student can update the task status, while the creator can edit or delete the task according to the current rules. The task list also shows who assigned the task and marks incomplete tasks as overdue when their deadline has passed.

## 9. Weekly Report Flow

> A student selects a project week and writes completed work, current problems and the next-week plan. The report is first saved as a draft. The student can edit or delete the draft and submit it when ready. A submitted report cannot be edited by the student. The mentor can view the student name and student number, read the report and save review comments.

## 10. Ordinary AI Conversation

> The frontend sends a synchronous HTTP POST request to the Spring Boot backend. The backend authenticates the user, loads that user's recent conversation history from Redis and calls Qwen through the DashScope HTTPS API. The response is returned as one complete answer. The current implementation does not use WebSocket, Server-Sent Events or streaming responses.

## 11. AI Task Draft and Confirmation

> The AI is not trusted to write a task directly. When the user asks to create a task, the model extracts the original information and calls `prepareTaskDraft`. The backend parses the deadline, applies defaults, validates fields and permissions, and stores a pending draft. The frontend displays the structured draft. Only after the user clicks Confirm does the backend revalidate the draft and call `TaskService` to create the real task. Cancel leaves the task uncreated.

## 12. Knowledge Base Flow

> An administrator uploads a PDF. The backend validates the file, extracts text, splits it into chunks, requests embeddings from DashScope and stores the vectors in Redis Stack. When a user asks a knowledge-base question, the AI can call the knowledge tool. The backend embeds the question, performs vector search and returns the relevant chunks and source information to Qwen. The final answer can therefore include citations to the indexed document.

> The knowledge-base code path is implemented. The remaining validation work is to run a real upload, indexing and question-answering flow with a real PDF and configured embedding API, because the current automated Redis test uses mocked RediSearch output.

## 13. System Architecture

> The frontend is a Vue 3 application. The backend is a Spring Boot application. MongoDB stores business records such as users, projects, tasks and reports. Redis stores short-lived conversation and task-draft data and provides the vector index for knowledge search. DashScope provides Qwen chat and embedding services. The browser communicates with the backend through HTTP, and the backend communicates with DashScope through HTTPS.

## 14. Backend Layering

> The backend is organised primarily by business feature, such as `task`, `project`, `progress`, `knowledge`, `organization` and `ai`. Inside those features there are controllers, services, repositories and domain models. This keeps the files for one business capability together. Controllers handle HTTP, services handle application rules, repositories handle persistence and AI tools call domain services instead of accessing repositories directly.

## 15. Data and Storage Responsibilities

> MongoDB is the source of truth for durable business data. Redis is used for temporary conversations, pending AI task drafts and vector search data. Uploaded PDFs are stored on the configured local filesystem for the MVP. This is suitable for a local demonstration, but a production deployment would use managed object storage and a backup strategy.

## 16. Permissions and Data Isolation

> Authentication uses JWT. The backend checks both the user's role and the user's relationship to the requested data. A student can access only their own project, tasks and reports. A mentor can access only assigned students. Administrators have administrative functions but are deliberately prevented from using AI tools to read student business data. These rules are enforced in backend services rather than relying only on frontend route visibility or AI instructions.

## 17. Time and AI Output Validation

> The database stores task deadlines as UTC instants. The task interface displays and edits them as `Europe/London` time, so the browser's local timezone does not change the business meaning. AI-generated task fields are treated as untrusted input. The backend parses the original deadline wording, supplies a default when necessary, validates required fields and checks permissions before creating a draft or task.

## 18. Completed Work

> The main MVP modules are now implemented: authentication, role-based access, organizations and members, projects, PDF submissions, tasks, weekly reports, ordinary AI chat, validated AI task drafts and the knowledge-base management and retrieval flow. The frontend build succeeds, the backend test suite currently has 25 passing tests, and the application can run locally with MongoDB, Redis, Spring Boot and Vite.

## 19. Current Testing Coverage

> Unit testing is partially covered, mainly for backend business logic, AI permissions, deadline parsing, task drafts, knowledge tools and Redis response parsing. Environment testing has been performed locally with Java 21, MongoDB, Redis Stack, the backend and the frontend. System integration testing is not yet complete because there is no full automated test covering the browser, backend, real databases, Redis vector search and DashScope together. Security testing currently covers application permission logic, but not a formal penetration test or OWASP scan. Performance testing has not yet been performed.

## 20. Local Demonstration Environment

> The local environment requires Java 21, Maven, Node.js, Docker and Docker Compose. MongoDB and Redis Stack run in Docker. The backend runs on port 8080 and the frontend runs on port 5173. Live AI features require `DASHSCOPE_API_KEY`. The demonstration uses separate Admin, Mentor and Student accounts, and credentials are kept in the local environment rather than committed to the repository.

## 21. Suggested MVP Demonstration Order

> I would demonstrate the system in this order: first log in as a student and show the project and task dashboard; then create or update a project and upload a PDF; next show a task assigned by the mentor and update its status; then create, submit and review a weekly report; finally show an AI question, a knowledge-base answer and an AI-generated task draft that requires confirmation. I would finish by showing the Admin knowledge-base and member-management screens.

## 22. Known Limitations

> The MVP depends on an external DashScope API key for live AI and embeddings. Uploaded files use local storage, so this is not yet a production deployment architecture. There is no automated frontend end-to-end suite, no formal performance benchmark and no complete security scan. Natural-language time interpretation is validated by the backend but cannot guarantee every possible wording. These are known MVP limitations rather than hidden requirements.

## 23. Next Development Steps

> The next priority is a real knowledge-base integration test using an actual PDF and the live Redis Stack and embedding service. After that I would add a small end-to-end regression suite for authentication and the main Student, Mentor and Admin workflows. The next stages would be security scanning, performance testing, production file storage, deployment configuration and optional notification features.

## 24. Documentation and Delivery Materials

> For final delivery I will provide the startup instructions, architecture explanation, role and permission matrix, feature test sheet, screenshots or a recorded demonstration, known limitations and future-work discussion. The code, test evidence and documentation will describe the current MVP honestly, including which tests are automated and which are manual.

## Closing Summary

> In summary, ProjectHelper has reached a demonstrable minimum viable product. The central student-mentor collaboration workflow is implemented, the Admin and Knowledge Base functions are available, and the AI assistant is integrated with permission-aware backend tools. The remaining work is mainly real integration validation, broader testing and delivery documentation rather than a missing core user workflow.

## Short Answers to Likely Follow-up Questions

### Why is the task created only after confirmation?

> The language model is probabilistic, so it should not be trusted to write business data directly. The draft-confirmation step creates a human checkpoint and lets the backend validate the deadline, fields and permissions twice.

### Why store UTC instead of UK local time?

> UTC gives the database one unambiguous instant. The frontend converts that instant to `Europe/London` for the user interface, including British Summer Time and Greenwich Mean Time changes.

### Why use both MongoDB and Redis?

> MongoDB stores durable business records. Redis is better suited to short-lived conversations, temporary task drafts and fast vector-search data. Separating these responsibilities keeps the storage model simple for the MVP.

### Is the AI allowed to access all student data?

> No. The backend resolves the authenticated actor and applies role and relationship checks inside the domain services. The AI prompt describes these rules, but the backend is the final enforcement point.

### Is this production-ready?

> It is ready as a local MVP demonstration and a foundation for the dissertation. It is not yet production-ready because it still needs managed file storage, deployment configuration, complete integration and security testing, and performance validation.
