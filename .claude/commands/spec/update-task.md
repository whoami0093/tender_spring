---
allowed-tools: Bash(sh:*), Write
description: Mark a task as complete
argument-hint: <task-description-or-number>
---

## Current Tasks

!`sh spec/.scripts/update-task-context.sh`

## Your Task

Update the task status for: "$ARGUMENTS"

1. Find the matching task in tasks.md
2. Change `- [ ]` to `- [x]` for that task
3. Show updated progress statistics
4. Suggest next task to work on

Use the Write tool to update the file.