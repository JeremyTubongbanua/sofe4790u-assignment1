#!/bin/bash
if [[ "$OSTYPE" != "linux-gnu"* && "$OSTYPE" != "darwin"* ]]; then
    echo "This script can only be run on Linux or macOS."
    exit 1
fi

FULL_PATH_TO_SCRIPT="$(realpath "${BASH_SOURCE[0]}")"
SCRIPT_DIRECTORY="$(dirname "$FULL_PATH_TO_SCRIPT")"
cd "$SCRIPT_DIRECTORY"

javac -d out ../sofe4790u/a1/server/Server.java && jar cfe server.jar sofe4790u.a1.server.Server -C out sofe4790u && java -jar server.jar 5555 5556