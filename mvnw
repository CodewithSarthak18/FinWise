#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$DIR/tools/maven/bin/mvn" ]; then
    MAVEN_CMD="$DIR/tools/maven/bin/mvn"
else
    MAVEN_CMD="mvn"
fi
exec "$MAVEN_CMD" "$@"
