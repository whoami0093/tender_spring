#!/bin/sh
current=`cat spec/.current-spec 2>/dev/null`
echo "Current spec: $current"
if [ -f "spec/$current/.requirements-approved" ]; then
    echo "Requirements approved: Yes"
else
    echo "Requirements approved: No"
fi
ls -la "spec/$current/" 2>/dev/null
