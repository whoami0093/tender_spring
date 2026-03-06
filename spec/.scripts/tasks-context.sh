#!/bin/sh
current=`cat spec/.current-spec 2>/dev/null`
echo "Current spec: $current"
if [ -f "spec/$current/.design-approved" ]; then
    echo "Design approved: Yes"
else
    echo "Design approved: No"
fi
