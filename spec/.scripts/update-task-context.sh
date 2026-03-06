#!/bin/sh
current=`cat spec/.current-spec 2>/dev/null`
tasks_file="spec/$current/tasks.md"
if [ -f "$tasks_file" ]; then
    grep -n "^- \[" "$tasks_file" | head -20
else
    echo "No tasks.md found"
fi
