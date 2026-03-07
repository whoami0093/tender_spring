---
allowed-tools: Bash(sh:*), Write
description: Start implementation from approved tasks
argument-hint: [phase-number]
---

## Context

!`sh spec/.scripts/tasks-approved-context.sh`

## Current Tasks

!`sh spec/.scripts/implement-context.sh`

## Your Task

1. Verify tasks are approved — if not, stop and tell user to run `/spec:approve tasks`
2. **Branch management**: check "Branch status" from context above
   - If "NEED TO SWITCH": run `git checkout -b feature/<spec-name>` (or `git checkout feature/<spec-name>` if branch already exists)
   - If "OK": continue
3. If phase number provided ($ARGUMENTS), focus on that phase
4. Display current incomplete tasks
5. Implement tasks sequentially, updating task checkboxes in tasks.md as you go
6. After all tasks done, remind user to open a PR: `gh pr create`