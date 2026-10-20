# agent-ready-java

A small hexagonal Java project that gives a coding agent (or a human) as much **deterministic
feedback** as possible. To add it to an existing repository, paste the prompt in
[Adopting this in an existing repository](#adopting-this-in-an-existing-repository) into your agent.

```
./verify                 # fast gates, run after every change
./verify --fix           # auto-format first, then the fast gates
./verify --full          # + Docker integration tests, mutation testing, copy-paste detection
./verify --snapshots     # re-record Selfie snapshots after an intentional behaviour change
./verify --install-hooks # format on commit, fast gates on push
./verify --baseline      # record today's violations (adopting this in an existing repo)
```

`./verify` prints `VERIFY OK` or a short list of problems with hints. Full log: `target/verify.log`.
First run on a fresh checkout: use `./verify --fix` so Spotless normalises formatting.

## When each gate runs

| Where | What | Blocks? |
|---|---|---|
| pre-commit (`.githooks/pre-commit`) | Formats the staged Java files and re-stages them | Never: committing to save work comes first. A file with unstaged changes is left alone (formatting it would pull those changes into the commit) and pre-push catches it. |
| pre-push (`.githooks/pre-push`) | `./verify`: every fast gate plus the ratchet | Yes. Size, structure and design are judged on the whole change, not on each commit. |
| CI (`.github/workflows/verify.yml`, `ci/agent-ready.gitlab-ci.yml`) | `./verify`, then `./verify --full` and coverage of the changed lines | Yes. Hooks can be skipped with `--no-verify`, CI cannot. |

Hooks are opt-in per clone: `./verify --install-hooks` sets `core.hooksPath`, and `./verify` prints
a tip until it is done. GitLab projects reuse the template with `include:` (see the header of
`ci/agent-ready.gitlab-ci.yml`); `.gitlab-ci.yml` shows the minimal setup.

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
| Nothing got looser: baselines only shrink, minimums only rise, limits only fall, no new `@SuppressWarnings`/`NOPMD`/`@Disabled` | `tools/ratchet/` | `config/baseline/`, `pom.xml` properties | fast |
| Shell scripts (`verify`, git hooks) | shellcheck, when installed (it is on GitHub runners) | `verify` | fast |
| Changed lines are covered (≥ 95%) | diff-cover | CI files | CI, PRs only |

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
- Gates are symptoms, not targets. A size, coupling, complexity or architecture failure usually
  reveals a design problem whose answer can be much broader than the flagged line: a missing
  concept, a misplaced responsibility, a boundary to rethink. Satisfying a rule to the letter
  (splitting a method mechanically, moving code around to fit a number) is not a fix. When the only
  available fix brings no real design benefit, stop and ask a human or a design-focused agent.
  `./verify` repeats this whenever a design gate fails.
- Fix the code, not the gate: never loosen a threshold or add an ignore to get green. The ratchet
  enforces this (see below).

## Adopting this in an existing repository

Paste this prompt into a coding agent opened at the root of your repository:

````text
Install the deterministic feedback setup of https://github.com/martinsson/agent-ready-java in this
repository and prove that every check works. Clone it to a temporary directory and read its README
first: it is the specification. Do not decide what to do about the violations the checks find in
the existing code: report them and let me choose.

1. Survey: build tool (Maven required; for Gradle, stop and tell me), JDK, base package(s), how the
   code is layered today, existing static analysis, test frameworks, CI system. Show me the survey
   before changing anything.
2. Install: copy config/, .mvn/jvm.config, verify, .githooks/, tools/ratchet/, the CI file that
   matches this repository's CI, and the plugin blocks and properties of its pom.xml. Replace
   com.example everywhere (NullAway AnnotatedPackages, JaCoCo/PIT filters, ArchitectureTest). Merge
   with existing plugins instead of duplicating them. In ArchitectureTest, keep the generic rules
   and replace the hexagonal layering rules with rules that describe the layering this code has;
   mark the ones you guessed.
3. Measure: empty config/baseline/ and run ./verify --baseline, which records the existing
   violations without failing. Collect the Error Prone and NullAway errors from the compilation
   (they cannot be baselined) and today's coverage and mutation score.
4. Prove that each check fires. For each row of the "What gives feedback" table, add the smallest
   change that should break it, in a new file so no baseline covers it (an over-long method, an
   unused private field, a Mockito import, a forbidden dependency between layers, a test without
   assertion that survives PIT, an added @SuppressWarnings, a lowered minimum...). Run ./verify (or
   --full) and check that it fails with that check's message. A check that does not fail is not
   working: fix the installation and probe again. Remove every probe afterwards.
5. Report, as tables:
   - per tool: version, where it runs (fast/full/CI), probe result (fired / did not fire / skipped
     and why), number of active rules;
   - ArchUnit: each rule in one line, marked "generic" or "guessed from the code";
   - per rule with existing violations: count, files touched, the size of a fix, and one suggestion:
     * fix: few occurrences and an easy fix. Keep the rule, fix them in a separate commit after
       this one;
     * loosen: so many occurrences that complying means reworking much of the code. Relax the
       threshold (give the value that passes today) or switch the rule off;
     * ratchet: a big change but few occurrences. Keep the rule, keep the occurrences in the
       baseline, new code may not add any;
   - coverage and mutation: today's values, suggested as the minimums.
6. Ask me to choose for each rule, then apply what I chose. Do not commit: I will review the diff.
````

Read the probe column before anything else: a check that fired on its probe is one you know works,
the others are configuration you are trusting. The triage of existing violations is your call, rule
by rule; the agent only suggests. Loosening a rule here does not contradict "fix the code, not the
gate": it is a human decision taken before the ratchet starts, and from then on the ratchet holds
the line.

### After the install: what the prompt cannot do for you

- **Architecture rules need you.** Generic rules work anywhere, but rules that catch design drift
  encode decisions only the team knows: which modules may depend on which, where I/O is allowed,
  what a domain object must not see. Follow up with: *"Interview me about this codebase's
  architecture, one question at a time, then turn each answer into an ArchUnit rule with a
  `.because(...)` that states the reason, and show me the violations it finds."*
- **Gates do not make the agent test first.** Coverage of changed lines is enforced in CI (95%),
  but nothing forces the agent to pin behaviour with a characterization test *before* it touches
  legacy code, or to read PIT survivors. That comes from the harness (see *Not included*): a
  `CLAUDE.md` and a bug-fix/feature workflow saying "characterization test first, mutation score
  on the touched classes after". To find out whether it works, try it: ask the agent for a small
  bug fix in untested code and look at its first commits. If the first diff is in `src/main`, the
  workflow is not doing its job.

### Doing it by hand

1. Copy `config/`, `.mvn/jvm.config`, the `verify` script, and the plugin blocks from `pom.xml`.
2. Change `com.example` in the `AnnotatedPackages` option (pom), `ArchitectureTest` and the JaCoCo/PIT
   package filters.
3. Copy `.githooks/`, `tools/ratchet/`, `config/baseline/` (empty it) and one of the CI files.
4. Make the build compile with Error Prone and NullAway, then run `./verify --baseline` and commit
   `config/baseline/`. That is the ratchet (details below).
5. Set `coverage.*.minimum` and `mutation.score.minimum` in `pom.xml` to today's values (or a bit
   below). They may only go up from there; changed lines are held to 95% by diff-cover in CI.
6. Before refactoring untested legacy code, pin its behaviour with Selfie characterization tests.

### The ratchet: existing problems may stay, new ones may not

`./verify --baseline` runs every analysis with failures switched off and records what it finds:

| Gate | Baseline | Granularity |
|---|---|---|
| Checkstyle | `config/baseline/checkstyle-suppressions.xml` | file + check. A second over-long method in a file that already has one gets through. |
| PMD | `config/baseline/pmd-exclusions.properties` (`excludeFromFailureFile`) | class + rule |
| CPD | `config/baseline/cpd-exclusions.txt` | the set of classes sharing a duplicate |
| ArchUnit | `config/baseline/archunit/` (every rule is wrapped in `FreezingArchRule.freeze`) | each violation. A fixed violation drops out of the store on the next test run; commit that. |
| Spotless | none: set `spotless.ratchetFrom` to `origin/main` in `pom.xml` to format only changed files | file |
| Error Prone / NullAway | cannot be baselined. Narrow `NullAway:AnnotatedPackages` to a few packages, or set single checks to `WARN`, and widen over time | |
| JaCoCo / PIT | global minimums in `pom.xml` + changed-lines coverage in CI. For PIT on changed code only, see `scmMutationCoverage` | |

Every `./verify` then compares the working tree with a base commit (`RATCHET_BASE`; locally the
merge-base with `origin`, in CI the PR/MR base) and fails when:
- a baseline gains an entry (a new violation was recorded instead of fixed),
- a `*.minimum` property in `pom.xml` drops, a Checkstyle `max*` limit or a `MAX_*` constant in a
  test rises, a Checkstyle module or PMD rule disappears, an Error Prone check is set to `OFF`/`WARN`,
- `@SuppressWarnings`, `NOPMD`, `CHECKSTYLE:OFF`, `@Disabled` or `@ArchIgnore` appear more often
  under `src/` than before.

When a human decides a gate should be looser, a commit in the range carries a
`Ratchet-Override: <reason>` line: the failures become warnings and the decision stays in history.
Protect `config/` and `tools/` with CODEOWNERS so such a change needs review.

The ratchet eats its own cooking: Maven compiles `tools/` as test sources (build-helper), so
Spotless, Checkstyle, Error Prone, NullAway and PMD check it and its unit tests run with the others.
`./verify` still runs it with plain `java tools/ratchet/Ratchet.java` (multi-file source launcher,
JDK 22+), using the JDK Maven runs on when `java` on the PATH is older.

Targets JDK 25 (`maven.compiler.release` 25, the enforcer requires 25+). Verified on JDK 25 and 26
(Docker 29). Tools that hook into javac or read class files (google-java-format, Error Prone,
NullAway, PMD, JaCoCo, PIT, ArchUnit, Testcontainers) are the ones that break on a new JDK: bump
those first. PMD is the sneaky one: a PMD older than the JDK logs `Unsupported class file major
version`, switches type resolution off and *passes*, so type-based rules stop working without a
failure. `./verify` turns that log line into a failure. Upgrading PMD means bumping `pmd.version`
(the `pmd-java` and `pmd-core` plugin dependencies), not just the plugin: the plugin's bundled PMD
is older (maven-pmd-plugin 3.24.0 ships PMD 7.3.0, for instance). `mvn versions:display-dependency-updates
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
  Also deny `Bash(git commit --no-verify:*)` and `Bash(git push --no-verify:*)` so the agent cannot
  skip the git hooks.

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

## License

[MIT No Attribution](LICENSE) (MIT-0): copy any part of this into your own repository without
keeping a notice.
