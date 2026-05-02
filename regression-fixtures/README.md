# Regression Fixtures

Promoted fixtures for repeatable checks belong here.

Use this directory for models and expected markers that can be run by scripts or tests. Manual exploratory proof files can remain in `proof-fixtures/` until they are promoted.

Suggested layout:

```text
regression-fixtures/
  stl/
  expected/
```

Each promoted fixture should include:

- source model
- config or profile input
- expected G-code markers or summary values
- the command/script that verifies it
