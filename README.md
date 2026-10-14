# agent-ready-java

A small hexagonal Java project that gives a coding agent (or a human) as much **deterministic
feedback** as possible. Copy the pieces you want into an existing repository.

```
./verify              # fast gates, run after every change
./verify --fix        # auto-format first, then the fast gates
./verify --full       # + Docker integration tests, mutation testing, copy-paste detection
./verify --snapshots  # re-record Selfie snapshots after an intentional behaviour change
```

`./verify` prints `VERIFY OK` or a short list of problems with hints. Full log: `target/verify.log`.
First run on a fresh checkout: use `./verify --fix` so Spotless normalises formatting.

## What gives feedback, and where it lives

| Layer | Tool | Configured in | Runs in |
|---|---|---|---|
| Java version, banned dependencies (Mockito) | maven-enforcer | `pom.xml` | fast |
| Formatting | Spotless + google-java-format | `pom.xml` | fast (`--fix` applies) |
| Size limits: method ≤ 50 lines / 30 statements, file ≤ 800 lines, ≤ 30 methods per class, ≤ 5 parameters, ≤ 8 record components, one top-level type per file, short lambdas and anonymous classes; coupling (fan-out, data abstraction); complexity (cyclomatic, NPath, boolean expressions); nesting; banned imports | Checkstyle | `config/checkstyle/checkstyle.xml` | fast |
| Bug patterns + null safety at compile time (warnings are errors) | Error Prone + NullAway | `pom.xml` (compiler plugin), `.mvn/jvm.config` | fast |
| Dead code, design smells | PMD | `config/pmd/ruleset.xml` | fast |
| Business rules | JUnit 5 + AssertJ | `src/test/.../domain`, `.../application` | fast |
| Inputs nobody thought of | jqwik (property-based) | `WalletPropertiesTest` | fast |
| Hexagonal layering, no cycles, ≤ 15 classes per package, no Mockito, no `System.out`, no generic exceptions | ArchUnit | `ArchitectureTest` | fast |
| Pinning current output | Selfie (inline snapshots) | `WalletSnapshotTest` | fast |
| Coverage gate on the core (line 90%, branch 85%) | JaCoCo | `pom.xml` | fast |
| Real database | Testcontainers (PostgreSQL) | `JdbcWalletRepositoryIT` | full |
| Do the tests actually assert something? | PIT mutation testing (threshold 85%) | `pom.xml` profile `full` | full |
| Copy-paste | PMD CPD | `pom.xml` profile `full` | full |

Why Selfie rather than Approvals: expected values are inline in the test source, and
`./verify --snapshots` rewrites them in place, so the agent sees a normal source diff instead of
`.approved.txt` files.

## Architecture (hexagonal)

```
com.example.wallet
├── domain                      pure business logic, depends on nothing outward
├── application
│   ├── WalletService           use-case orchestration
│   └── port
│       ├── in                  driving ports (use-case interfaces)
│       └── out                 driven ports (WalletRepository)
└── adapter
    ├── in                      (none yet) controllers, CLI, messaging
    └── out.persistence         JdbcWalletRepository
```

Dependencies point inwards: `adapter -> application -> domain`. `ArchitectureTest` enforces it.
Each rule has a `.because(...)` message: that message is the first thing an agent reads when it
breaks a rule, so keep it specific.

## Testing principles

- No Mockito (banned by enforcer, Checkstyle and ArchUnit). Write a small in-memory fake of the port:
  see `InMemoryWalletRepository`.
- Real infrastructure in integration tests (Testcontainers), never an in-memory look-alike database.
- Coverage is a floor, mutation score is the real measure: an agent can execute code without
  asserting anything, PIT catches that.
- Fix the code, not the gate: never loosen a threshold or add an ignore to get green.

## Adopting this in an existing repository

1. Copy `config/`, `.mvn/jvm.config`, the `verify` script, and the plugin blocks from `pom.xml`.
2. Change `com.example` in the `AnnotatedPackages` option (pom), `ArchitectureTest` and the JaCoCo/PIT
   package filters.
3. Turn things on one at a time. For a legacy codebase:
   - ArchUnit: wrap rules in `FreezingArchRule.freeze(...)` (see `src/test/resources/archunit.properties`)
     so existing violations are recorded and only **new** ones fail.
   - Checkstyle / PMD: generate a baseline, or start with a handful of rules.
   - NullAway: start with `-XepOpt:NullAway:AnnotatedPackages=<one package>` and widen.
   - JaCoCo: gate on changed code only (diff-cover or a Sonar new-code gate), not the global ratio.
   - PIT: use `scmMutationCoverage` so it only mutates what changed. (`withHistory` now needs a
     separate history plugin in PIT 1.2x, so it is off.)
4. Before refactoring untested legacy code, pin its behaviour with Selfie characterization tests.

Verified on JDK 21 and JDK 25 (Docker 29). Tools that hook into javac or read class files
(google-java-format, Error Prone, NullAway, PMD, JaCoCo, PIT, ArchUnit, Testcontainers) are the
ones that break on a new JDK: bump those first. `mvn versions:display-dependency-updates
versions:display-plugin-updates` lists what is available. google-java-format 1.37.0 does not work
with Spotless 3.10.4 yet, so it stays on 1.36.0.

## Not included (by choice): where to add it

These were discussed but deliberately left out of this template.

**The agent harness (instructions, skills, workflows)**
- `CLAUDE.md` / `AGENTS.md`: keep it short and make it point at `./verify`, this README and the
  docs below.
- `docs/`: architecture overview, testing principles, conventions. Anything that can be an
  ArchUnit rule should be one; docs then only explain *why*.
- `.claude/skills/`: TDD, characterization testing, mutation-survivor triage, hexagonal refactoring.
- `.claude/commands/` and `.claude/agents/`: bug-fix workflow, feature workflow, reviewer and
  test-writer agents.
- `.claude/settings.json` hooks, so the feedback loop is not optional. Example: block finishing until
  verify is green (exit code 2 feeds stderr back to the agent):

  ```json
  {
    "hooks": {
      "Stop": [{ "hooks": [{ "type": "command", "command": "./verify >&2 || exit 2" }] }]
    }
  }
  ```

  A `PostToolUse` hook on edits that runs `mvn -q spotless:apply compile` gives faster feedback.

**More deterministic tooling**
- SpotBugs + FindSecBugs: bytecode-level bugs and security patterns.
- OpenRewrite: detect and auto-migrate patterns with recipes (good agent-friendly fixer).
- jMolecules (+ its ArchUnit integration): declare hexagonal/DDD roles in code instead of by package name.
- Spring Modulith `ApplicationModules.verify()`: if the repo uses Spring.
- Multi-module Maven build or JPMS: makes forbidden dependencies impossible, not just detectable.
- jQAssistant: query the codebase as a graph with Cypher rules.
- Pact / Spring Cloud Contract: contract tests at service boundaries.
- OWASP Dependency-Check or Trivy: vulnerable dependencies.
- Sonar quality gate on new code (CI).
