---
allowed-tools: Bash(sh:*)
description: Approve a specification phase
argument-hint: requirements|design|tasks
---

## Context

!`sh spec/.scripts/approve-context.sh`

## Your Task

For the phase "$ARGUMENTS":

1. Verify the phase file exists (requirements.md, design.md, or tasks.md)
2. Create approval marker file: `.${ARGUMENTS}-approved`
3. Inform user about next steps:
   - After requirements → design phase
   - After design → tasks phase
   - After tasks → implementation
4. If invalid phase name, show valid options

Use touch command to create approval markers.