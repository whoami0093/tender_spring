#!/bin/sh
current=`cat spec/.current-spec 2>/dev/null`
if [ -z "$current" ]; then
    echo "No active spec. Use 'spec switch <spec-name>' to select one."
    exit 0
fi

echo "Active spec: $current"
echo "---"

req_approved=false
design_approved=false
tasks_approved=false

if [ -f "spec/$current/.requirements-approved" ]; then req_approved=true; fi
if [ -f "spec/$current/.design-approved" ]; then design_approved=true; fi
if [ -f "spec/$current/.tasks-approved" ]; then tasks_approved=true; fi

if $req_approved && $design_approved && $tasks_approved; then
    echo "## Implementation Review"
    echo ""
    echo "All specification documents are approved. It's time to review the implementation against the spec."
    echo ""
    echo "**Your Task:**"
    echo ""
    echo "1.  **Analyze the Specification:**"
    echo "    - Read and understand the full specification:"
    echo "      - spec/$current/requirements.md"
    echo "      - spec/$current/design.md"
    echo "      - spec/$current/tasks.md"
    echo ""
    echo "2.  **Inspect the Code:**"
    echo "    - Identify and review the code changes that implement this specification."
    echo ""
    echo "3.  **Verify Compliance:**"
    echo "    - Requirements, Design, Tasks — check each is implemented correctly."
    echo ""
    echo "4.  **Deliver Your Review:**"
    echo "    - Summarize findings, highlight deviations or bugs."
else
    echo "## Specification Review"
    echo ""
    echo "The following specification documents are present:"
    ls -1 "spec/$current/" | grep -E "(requirements|design|tasks)\.md"
    echo ""
    echo "Approval status:"
    if $req_approved; then echo "- [x] requirements.md"; else echo "- [ ] requirements.md"; fi
    if $design_approved; then echo "- [x] design.md"; else echo "- [ ] design.md"; fi
    if $tasks_approved; then echo "- [x] tasks.md"; else echo "- [ ] tasks.md"; fi
    echo ""
    echo "**Your Task:** Review the first unapproved document and approve it when ready."
fi
