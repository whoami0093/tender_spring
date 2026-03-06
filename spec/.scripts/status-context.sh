#!/bin/sh
for dir in spec/*/; do
    if [ -d "$dir" ]; then
        echo "=== $dir ==="
        ls -la "$dir" | grep -E "(requirements|design|tasks)\.md|\..*-approved"
        if [ -f "$dir/tasks.md" ]; then
            echo "Task progress:"
            grep "^- \[" "$dir/tasks.md" | head -5
            total=`grep -c "^- \[" "$dir/tasks.md" 2>/dev/null || echo 0`
            completed=`grep -c "^- \[x\]" "$dir/tasks.md" 2>/dev/null || echo 0`
            echo "Total tasks: $total"
            echo "Completed: $completed"
        fi
        echo ""
    fi
done
