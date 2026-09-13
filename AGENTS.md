# AGENTS.md

RootBoot is a Fabric mod for Minecraft that reunites many quality-of-life functionalities.

## Technical details

- Mod id / namespace: `rootboot`
- Package base: `br.com.bobwizley.rootboot`
- Minecraft version: `26.2`
- Fabric Loader: `0.19.3`
- Fabric API `0.155.2+26.2`
- Fabric Loom `1.17.17`
- Gradle `9.6.1`
- Java `25`

Minecraft `26.2` ships a deofuscated, unmapped JAR (Mojang stopped publishing mappings) — the build uses Loom's no-remap plugin with no mappings layer, so mod dependencies are plain `implementation`/`api`, not `modImplementation`/`modApi`.

Sources are split across two Loom source sets: `src/main` (common) and `src/client` (client-only).

## Commands

```bash
./gradlew build        # compile + test + jar
./gradlew test         # run the JUnit test suite
./gradlew runClient    # launch a dev client
./gradlew runServer    # launch a dev server
./gradlew runDatagen   # regenerate src/main/generated
```

`src/main/generated` is produced by `runDatagen` and committed into the repo (ADR-0003) — never hand-edit the JSON there; change the datagen providers and rerun the task instead.

## Releases

Pushing a `vMAJOR.MINOR.PATCH` tag is the release: the pipeline creates the Gitea release when it is missing, with the tag name as the title and the commit subjects since the previous tag as the body. Creating the release by hand in the Gitea UI does the same thing, and a body written there is never overwritten. The jar is only attached after a green build, so a failed build leaves no release behind.

## Project state

The project is being built incrementally, one feature at a time, tracked under the epic **issue #1**. The full scope decisions live in `docs/FEATURES.md` — read it before picking up any feature-shaped issue, since it's the source of truth for what each feature does and what's explicitly out of scope.

## Docs & agents

- Domain: `CONTEXT.md` + `docs/adr/` (see `docs/agents/domain.md`).
- Issues: Gitea `vctrtvfrrr/rootboot` via the `tea` CLI (`docs/agents/issue-tracker.md`).
- Triage labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix` (`docs/agents/triage-labels.md`).
