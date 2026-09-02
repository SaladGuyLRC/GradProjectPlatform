# Dissertation Review and Completion Execution Plan

**Project:** ProjectHelper  
**Dissertation file:** `/Users/liruochen/Desktop/Learn/Uni/S3/Essay/Word/3075301L.docx`  
**Plan status:** Substantive execution complete; author-only completion and final Word pagination remain. No source-code changes are authorised by this file alone.  
**Last updated:** 2026-09-02

## 1. Objective

Complete a source-grounded, evidence-bounded review and improvement cycle for the MSc dissertation. The cycle will align the Introduction, Abstract, Research Questions, Requirements, Design, Implementation, Testing, Discussion, Conclusion and Figures, while preserving the scope of a local graduation-project MVP.

The final manuscript must use English interface and project terminology, British English, and one consistent citation style. It must not claim production readiness, comprehensive security, institution-scale performance, reliable knowledge-answer grounding or user acceptance unless new evidence directly supports those claims.

## 2. Skills and order of use

1. `nature-writing` for argument structure, section roles, claim-evidence alignment and cross-section consistency.
2. `nature-polishing` for sentence-level academic English, British spelling, terminology and concise prose after the argument is stable.
3. `archify` for evidence-based architecture and controlled-AI workflow diagrams.
4. `nature-reviewer` for three independent simulated reviewer reports followed by a separate synthesis.
5. The Word document skill for safe `.docx` editing and render-and-verify checks when the text and figures are ready.

The reviewer stage is a simulated external review, not a real journal decision or a substitute for supervisor feedback.

## 3. Working boundaries

- Do not invent observations, interviews, user feedback, experiments, statistics, references or implementation behaviour.
- Do not expand local MVP evidence into production, comprehensive security, scalability or institutional-readiness claims.
- Test only ProjectHelper dependencies and endpoints. Do not stop, delete or reconfigure another project's Redis, MongoDB or containers.
- Keep ProjectHelper's isolated local ports separate from other projects. Current recorded configuration uses Redis host port `16381` and backend port `18080`; verify before running commands.
- Do not commit or push changes unless separately requested.
- Do not modify unrelated application code. If a test exposes an implementation defect, first classify it as documentation-only, an in-scope minimal fix, or an out-of-scope enhancement.
- Preserve the evidence distinction between implemented, tested, partially tested and untested behaviour.
- Preserve the current claim ceiling: evaluated local MVP for the target teaching workflow.

## 4. Phase 0: establish a baseline

### Actions

- Extract and archive the current Word text for comparison.
- Record word count, chapter structure, figure/table placeholders, references and administrative placeholders.
- Read the evidence ledger, literature-search report and existing test records.
- Inspect the current repository and test configuration without changing files.

### Outputs

- Baseline document inventory.
- Current terminology ledger.
- Claim inventory with evidence status.
- List of unresolved or ambiguous claims.

## 5. Phase 1: cross-section argument audit

### Introduction funnel

Check that the Introduction follows:

> target teaching context -> local workflow problem -> domain integration gap -> authorisation and AI challenges -> bounded solution -> contributions -> research questions -> evaluation route -> scope boundary

### Required audits

- Check that every important Introduction promise has a downstream section and evidence.
- Check that every major Results/Testing claim is motivated by a question or requirement in the Introduction.
- Define the operational meanings of `core workflows`, `prevent` and `useful support`.
- Check the Admin single-weekly-report read exception against all security claims.
- Check that `coordination effort` and `access visibility` are treated as intended benefits unless measured.
- Check that public knowledge retrieval is not confused with private Student-document retrieval.
- Check that `prepareTaskDraft` is presented as a proposal/draft, not an autonomous write operation.
- Check that Chapter 2 analyses the background and integration gap rather than repeating Chapter 1.
- Check that Chapters 3–6 provide a traceable path from requirements to tests.
- Check that Chapters 7–8 do not make stronger claims than Chapter 6 evidence.

### Outputs

- Introduction argument map.
- Introduction-to-later-sections dependency matrix.
- RQ -> requirement -> test -> result -> conclusion traceability table.
- P0/P1/P2 revision list.
- Separate list of Introduction changes, later-chapter changes and missing evidence.

## 6. Phase 2: evidence-strengthening tests

Run only tests that can support claims already in scope. Record exact date, environment, command, expected result, actual result and qualification.

### Automated checks

- Backend Maven tests.
- Frontend TypeScript check and production build.
- AI actor-resolution and configuration tests.
- Task permission and task-draft tests.
- Deadline parser tests.
- Redis vector-store and knowledge-indexing tests.
- Seed-data initialisation tests.

### Functional/system checks

- Student, Mentor and Admin authentication.
- Student project creation and maintenance.
- Student task viewing and status update.
- Mentor task assignment for an assigned Student.
- Student weekly-report draft creation, edit, delete and submission.
- Mentor visibility of submitted reports and exclusion of drafts.
- Student PDF upload and version history.
- Mentor access to assigned Student submissions.
- Admin user and organisation administration.
- User self-service password change.
- Public knowledge-document administration.

### Authorisation checks

- Unauthenticated access to protected routes.
- Student access to another Student's project, task, report and submission.
- Mentor access to a non-assigned Student.
- Cross-Mentor and cross-organisation access.
- Student access to Mentor-only operations.
- Admin access to Student-only operations.
- Resource-ID tampering.
- Disabled-account login and invalidation of old authentication context.
- The documented Admin single-report exception.
- AI tools attempting to access out-of-scope Student data.

### Knowledge-base/RAG checks

- Public PDF upload.
- PDF parsing and chunk creation.
- Embedding and Redis Stack indexing.
- Vector search.
- Source document and chunk metadata in the response.
- Whether the cited content actually supports the answer.
- Separation of public knowledge documents from private Student submissions.

### AI task-draft checks

- Clear deadline.
- Relative deadline.
- Missing deadline and 24-hour default.
- Unsupported or ambiguous deadline.
- Past deadline.
- Missing or invalid task fields.
- Unauthorised target Student or project.
- Cancel.
- Confirm.
- Repeated Confirm and duplicate prevention.
- Draft expiry.
- Cross-user draft access.

### Performance boundary

No performance claim will be added by default. A small local sanity check may be recorded only as environment evidence; it must not be described as scalability or production performance.

### Failure handling

- Infrastructure collision: repair only ProjectHelper's isolated configuration.
- Documentation mismatch: correct the manuscript or test record.
- In-scope minimal implementation defect: record the root cause and make only the minimal relevant fix if needed.
- Out-of-scope enhancement: record as limitation/future work.
- Any failed or unexecuted test remains explicitly marked as failed or not tested.

## 7. Phase 3: decide claim strength from evidence

Classify every major claim as one of:

- **Supported:** implementation and repeatable evidence are present.
- **Partially supported:** implementation and selected evidence are present, but coverage is limited.
- **Implemented only:** code or design exists without sufficient runtime evidence.
- **Not established:** evidence is absent or contradictory.

Use the classification to control wording:

- `selected`, `tested scenarios`, `local seed environment`, `initial MVP` for partial evidence;
- `operational support` rather than broad user-perceived usefulness when no UAT exists;
- `risk reduction` rather than security guarantee;
- `retrieval with source indicators` rather than verified factual grounding.

## 8. Phase 4: generate necessary figures with archify

### Figure 3.1: Role-aware application sitemap

Show the implemented route visibility by role:

- Sign in and Dashboard entry points.
- Student/Mentor Tasks and Weekly Reports routes.
- Student `My Project` and Mentor `Student Projects` routes.
- Shared Members, Knowledge Base and AI Assistant routes.
- A clear note that navigation visibility is not a substitute for backend authorisation.

### Figure 4.1: Overall ProjectHelper architecture

Show only implemented or explicitly bounded components:

- Vue frontend.
- Spring Boot REST backend.
- JWT security.
- Controller, service and repository layers.
- MongoDB durable business data.
- Redis Stack transient state and vector retrieval.
- Local PDF storage.
- DashScope/Qwen chat and embedding services.
- Public knowledge boundary.

Do not depict production gateway, high availability, managed secret storage, SSO or other unimplemented infrastructure.

### Figure 4.2: Controlled AI task-draft workflow

Show:

- User request.
- Model extraction.
- `prepareTaskDraft`.
- Backend field, deadline, target and authorisation validation.
- Redis temporary draft.
- Frontend confirmation card.
- Confirm or Cancel.
- Confirmation revalidation.
- TaskService and MongoDB persistence.
- Invalid/expired draft creates no task.
- Repeated confirmation does not create a duplicate task.

### Figure 4.3: Conceptual project-centred data model

Show the logical relationships among Organisation, Student, Mentor, Graduation Project,
Tasks, Weekly Reports and Submission Versions, together with the separate public
knowledge-document scope. This is a conceptual model, not a physical MongoDB ERD.

### Figure 5.1: Public knowledge indexing and retrieval pipeline

Show the Admin-managed public PDF path through extraction, embedding, Redis Stack vector
indexing, query-time retrieval and source-indicator response. Keep Student submissions and
weekly reports outside this public retrieval path and do not imply that source indicators
prove answer correctness.

### Figure QA

- Author a fresh Archify specification from repository evidence.
- Validate with `archify validate`.
- Deliver with `archify deliver`.
- Perform bounded browser/visual checks.
- Export SVG/PNG and insert figure captions and in-text references.
- Do not expose credentials, tokens, local paths or private identifiers.

## 9. Phase 5: revise with nature-writing

### Introduction

- Keep the local observation appropriately scoped.
- State the domain integration gap explicitly.
- Distinguish RBAC from resource-level authorisation.
- Keep controlled AI, public-only retrieval and draft confirmation as bounded contributions.
- Align the three Research Questions with observable tests.
- Avoid detailed Redis TTL, parser and lock mechanics in the Introduction unless needed for the argument.

### Chapters 2–3

- Make Chapter 2 analytical and non-repetitive.
- Keep tool comparisons factual and first-party-source based.
- Define R1–R4, FR/NFR and acceptance conditions.
- Add a clear requirements-observation method and limitation.

### Chapters 4–6

- Align design and implementation descriptions with actual code.
- Separate system prompt guidance from backend authorisation.
- Separate retrieval success, citation metadata and answer correctness.
- Map each RQ to concrete tests and report untested scenarios.

### Chapters 7–8

- Interpret evidence without repeating all test details.
- Make limitations resolve named evidence gaps.
- Keep conclusions at the local-MVP claim level.

## 10. Phase 6: revise with nature-polishing

After structural revision is stable:

- Apply British English consistently.
- Use one canonical spelling for `organisation`, `authorisation`, `modelling` and `programme`.
- Standardise `Student–Mentor relationship`, `resource-level authorisation`, `public knowledge base` and `task draft`.
- Remove promotional wording and unsupported superlatives.
- Keep sentences concise and paragraphs single-purpose.
- Preserve citation intent and evidence boundaries.
- Do not add claims merely to improve rhetorical flow.

## 11. Phase 7: integrate and verify the Word document

- Preserve a pre-edit copy.
- Apply only approved text and figure changes.
- Insert Figures 3.1, 4.1, 4.2, 4.3 and 5.1 with captions and accessible alternative text.
- Update contents, List of Figures and List of Tables.
- Remove or resolve placeholders that are not allowed in a final submission.
- Check references and citation numbering.
- Render and inspect the document where the environment permits.
- Record that final pagination must be confirmed in Microsoft Word if automated rendering is unavailable.

Do not fill the following without author confirmation:

- Author name.
- AI Declaration.
- Acknowledgements.
- Factual details of the local observation.
- User-study results or user feedback.
- Permission to use screenshots.

## 12. Phase 8: independent simulated external review

Use `nature-reviewer` only after the manuscript and evidence packet are stable.

Generate three mutually blind reviewer reports from the same immutable manuscript/source packet:

1. Contribution, originality and significance.
2. Technical soundness, authorisation and reproducibility.
3. Evaluation completeness, writing, figures and claim boundaries.

Freeze all three reports before synthesis. Then produce a separate cross-review synthesis containing:

- Consensus strengths.
- Consensus blocking concerns.
- Other major concerns.
- Differences in emphasis.
- Minor revision checklist.
- Unsupported or not-assessable claims.

Do not describe this simulation as a real editorial decision.

## 13. Phase 9: final pre-submission audit

### Content

- Introduction, Abstract, RQs and Conclusion agree.
- Every strong claim has a downstream evidence pointer.
- Results distinguish tested, implemented-only and untested behaviour.
- Discussion and Conclusion do not exceed the evidence ceiling.

### References

- Verify Lewis et al. RAG metadata [5].
- Verify that the OneDrive source [9] supports the exact capability wording.
- Use one citation style throughout.

### Privacy and security

- No API keys, passwords, JWTs, local filesystem paths, private URLs or personal identifiers.
- No unapproved Student or Mentor information in figures or screenshots.

### Administrative and layout items

- Replace `Author: Your Name`.
- Complete the School-required AI Declaration.
- Decide whether Acknowledgements are included.
- Update Word fields.
- Confirm final body-page count in Word.
- Ensure references and appendices are excluded from the body-page limit as required.

## 14. Information that must come from the author

The following cannot be inferred safely:

1. The factual scope and date of the local workflow observation.
2. Whether the observation involved only document/workflow review or any discussion with users.
3. Whether a formal AI usefulness or user-acceptance evaluation will be conducted.
4. Author name, AI Declaration, acknowledgements and final submission details.
5. Which screenshots may be included in the dissertation.
6. Final institutional citation and formatting requirements.
7. Final confirmation of Word pagination and visual layout.

If no formal AI or user evaluation is conducted, the manuscript will explicitly state that operational AI behaviour was tested but user-perceived usefulness and systematic answer quality were not established.

## 15. Final deliverables

- Revised Word dissertation.
- Introduction-to-results traceability matrix.
- Test evidence record and untested-scenario list.
- Figures 3.1, 4.1, 4.2, 4.3 and 5.1 in editable/source and rendered formats.
- Updated captions, contents and figure/table lists.
- Citation and terminology audit.
- Three independent simulated reviewer reports.
- Post-review synthesis.
- P0/P1/P2 final action list.
- Author-only completion checklist.

## 16. Execution status (2 September 2026)

| Workstream | Status | Evidence / hand-off |
|---|---|---|
| Evidence and cross-section audit | Completed | `introduction-later-audit-2026-09-02.md`, `citation-terminology-audit-2026-09-02.md`, `evidence-ledger.md` |
| Safe automated and runtime checks | Completed for selected in-scope scenarios | `execution-evidence-2026-09-02.md`; 28 backend tests passed and frontend build passed |
| Archify architecture figure | Completed | `.artifacts/figures/projecthelper-architecture.json` and rendered exports; validation/delivery passed |
| Archify task-draft workflow figure | Completed with bounded visual QA | `.artifacts/figures/projecthelper-task-draft-workflow.json`; validation/delivery passed, manual screenshot inspection completed; the final DOCX uses a clean cropped raster from the visible capture because the direct clean PNG export rendered incorrectly; automated visual-check timed out |
| Archify conceptual data model figure (Figure 4.3) | Completed with bounded visual QA | `.artifacts/figures/projecthelper-conceptual-data-model.json`; validation/delivery and automated visual-check passed; clean cropped paper image inserted |
| Archify public knowledge retrieval figure (Figure 5.1) | Completed with bounded visual QA | `.artifacts/figures/projecthelper-knowledge-retrieval.json`; validation/delivery and automated visual-check passed; clean cropped paper image inserted |
| Archify role-aware sitemap (Figure 3.1) | Completed with bounded visual QA | `.artifacts/figures/projecthelper-role-aware-sitemap-tree.json`; hierarchical role-aware layout, validation/delivery and automated visual-check passed; clean cropped paper image inserted |
| Word manuscript structural revision | Completed for evidence-bound corrections | `Essay/Word/3075301L.docx`; backup copies retained; Appendix A now includes evidence-status column |
| British-English polishing and terminology alignment | Completed for the revised passages | See citation/terminology audit and revised DOCX |
| Accessibility/structure audit | Completed | `.artifacts/dissertation/a11y-audit.json`; no high, medium or low issues reported |
| Three independent simulated reviewer reports | Completed and frozen against the immutable packet | `nature-reviewer-technical-2026-09-02.md`, `nature-reviewer-significance-2026-09-02.md`, `nature-reviewer-readability-2026-09-02.md`; packet hash recorded in `review-packet-sha256.txt` |
| Cross-review synthesis | Completed | `nature-reviewer-synthesis-2026-09-02.md` |
| Automated DOCX render and final pagination | Partially complete / blocked by environment | `pdf2image` and LibreOffice/`soffice` are unavailable; final visual pagination must be checked in Microsoft Word |
| Author-only metadata and submission decisions | Pending author | `author-completion-checklist.md` |

The review stage is now closed. Any additional experiments, user studies, screenshots, Git operations or source-code changes require a separate decision and are not implied by this plan.
