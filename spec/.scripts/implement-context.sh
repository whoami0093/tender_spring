#!/bin/sh
current=`cat spec/.current-spec 2>/dev/null`
tasks_file="spec/$current/tasks.md"
if [ -f "$tasks_file" ]; then
    echo "=== Phase Overview ==="
    grep "^## Phase" "$tasks_file"
    echo ""
    echo "=== Incomplete Tasks ==="
    grep "^- \[ \]" "$tasks_file" | head -20
fi
