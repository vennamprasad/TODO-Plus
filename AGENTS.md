# AWS AI-DLC 2.0 Workflow Rules (TODO++)

> Official AWS AI-DLC (AI-Driven Development Life Cycle) 2.0 Adaptive Workflow Steering Rules for AI Agents.  
> Core Rules: [`aidlc-rules/core-workflow.md`](file:///Users/prasadvennam/MY%20WORK/IDEA-PROJECTS/TODO-plus/aidlc-rules/core-workflow.md)  
> Rule Details: [`aidlc-rule-details/`](file:///Users/prasadvennam/MY%20WORK/IDEA-PROJECTS/TODO-plus/aidlc-rule-details/) (and `.aidlc-rule-details/`)  
> Reference: [awslabs/aidlc-workflows](https://github.com/awslabs/aidlc-workflows)

## 📌 Core Tenets
1. **Verifiable & Self-Correcting Execution**: Never declare completion without executing automated build/test verification commands.
2. **Phase-Gated Life Cycle**: Progress sequentially through Inception -> Architecture & Design -> Execution -> Verification -> Documentation.
3. **Traceable Artifacts**: Store all phase artifacts under `aidlc-docs/` (`requirements/`, `architecture/`, `design/`, `tasks/`, `verification/`).
4. **IntelliJ Platform SDK Integrity**: Maintain 100% thread safety (`runReadAction`), non-blocking EDT, and zero deprecated API usage.

---

## 🔄 Three-Phase Adaptive Workflow

### Phase 1: Inception & Alignment
- Understand project context, user requirements, and constraints.
- Document business rules, functional requirements, and non-functional goals in `aidlc-docs/requirements/REQUIREMENTS.md`.

### Phase 2: Architecture & Detailed Design
- Formulate high-level architecture in `aidlc-docs/architecture/SYSTEM_ARCHITECTURE.md`.
- Detail component specifications, data contracts, and settings persistence models in `aidlc-docs/design/COMPONENT_DESIGN.md`.

### Phase 3: Execution, Verification & Documentation
- Break implementation into atomic tasks in `aidlc-docs/tasks/IMPLEMENTATION_TASKS.md`.
- Execute implementation following Test-Driven Development (TDD) or atomic iterations.
- Run test suites (`./gradlew test`) and plugin packaging (`./gradlew buildPlugin`).
- Record empirical test evidence in `aidlc-docs/verification/VERIFICATION_REPORT.md`.
- Launch sandbox testing (`./gradlew runIde`) for UI validation.
- Update `CHANGELOG.md`, `README.md`, `USAGE.md`, and `docs/AIDLC_GUIDE.md`.

---

## 🛠️ Project Guidelines
- **Language**: Kotlin 1.9+ / Java 17 / IntelliJ Platform SDK (IC-2024.1+).
- **Threading Rules**: Never perform blocking operations or I/O inside `runReadAction` or on the Event Dispatch Thread (EDT).
- **Scanning Rules**: Use `ProjectFileIndex` to skip excluded files, enforce `maxFileSizeMb` checks, and stream `ProgressIndicator` metrics.
