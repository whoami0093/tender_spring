#!/bin/sh
current=`cat spec/.current-spec 2>/dev/null`
if [ -z "$current" ]; then
    echo "No active spec"
    exit 0
fi
echo "Current spec: $current"
ls -la "spec/$current/" 2>/dev/null || echo "Spec not found"
