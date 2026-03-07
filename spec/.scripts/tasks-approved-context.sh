#!/bin/sh
current=`cat spec/.current-spec 2>/dev/null`
echo "Current spec: $current"
if [ -f "spec/$current/.tasks-approved" ]; then
    echo "Tasks approved: Yes"
else
    echo "Tasks approved: No"
fi
expected_branch="feature/$current"
current_branch=`git branch --show-current 2>/dev/null`
echo "Expected branch: $expected_branch"
echo "Current branch: $current_branch"
if [ "$current_branch" = "$expected_branch" ]; then
    echo "Branch status: OK"
else
    echo "Branch status: NEED TO SWITCH"
fi
