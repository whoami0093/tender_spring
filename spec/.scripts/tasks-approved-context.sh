#!/bin/sh
current=`cat spec/.current-spec 2>/dev/null`
echo "Current spec: $current"
if [ -f "spec/$current/.tasks-approved" ]; then
    echo "Tasks approved: Yes"
else
    echo "Tasks approved: No"
fi
