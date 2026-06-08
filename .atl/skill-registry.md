# Skill Registry — Point of Sale

## Project Conventions

- **AGENTS.md**: Not found
- **Conventions file**: Not found
- **Branch strategy**: `main` stable, `sprint/N` feature branches, deleted on merge

## User Skills (global)

| Skill | Description | Trigger |
|-------|-------------|---------|
| branch-pr | PR creation workflow for Agent Teams Lite | Creating a pull request, opening a PR, or preparing changes for review |
| go-testing | Go testing patterns for Gentleman.Dots | Writing Go tests, using teatest, or adding test coverage |
| issue-creation | Issue creation workflow for Agent Teams Lite | Creating a GitHub issue, reporting a bug, or requesting a feature |
| judgment-day | Parallel adversarial review protocol | User says "judgment day", "review adversarial", "doble review", etc. |
| skill-creator | Creates new AI agent skills | User asks to create a new skill or add agent instructions |

## SDD Skills (project workflow)

| Skill | Phase | Description |
|-------|-------|-------------|
| sdd-init | Bootstrap | Initialize SDD context in project |
| sdd-explore | Discovery | Explore and investigate ideas |
| sdd-propose | Planning | Create change proposal |
| sdd-spec | Specification | Write requirements and scenarios |
| sdd-design | Architecture | Technical design document |
| sdd-tasks | Breakdown | Implementation task checklist |
| sdd-apply | Implementation | Write code following specs/design |
| sdd-verify | Validation | Validate implementation against specs |
| sdd-archive | Closure | Archive completed change |
| sdd-onboard | Onboarding | Guided SDD walkthrough |

## Testing

- **Backend**: JUnit 5 + Mockito + MockMvc (unit + integration), H2 in-memory
- **Frontend**: None installed
- **Coverage**: Not configured
- **Strict TDD**: enabled
