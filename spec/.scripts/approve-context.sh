#!/bin/sh
current=`cat spec/.current-spec 2>/dev/null`
echo "Current spec: $current"
ls -la "spec/$current/" 2>/dev/null
