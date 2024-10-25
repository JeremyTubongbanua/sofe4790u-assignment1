#!/bin/bash
if [[ "$OSTYPE" != "linux-gnu"* && "$OSTYPE" != "darwin"* ]]; then
    echo "This script can only be run on Linux or macOS."
    exit 1
fi

FULL_PATH_TO_SCRIPT="$(realpath "${BASH_SOURCE[0]}")"
SCRIPT_DIRECTORY="$(dirname "$FULL_PATH_TO_SCRIPT")"
cd "$SCRIPT_DIRECTORY"

rm -rf ../client1/out ../client2/out ../client3/out ../client1/*.jar ../client2/*.jar ../client3/*.jar ../server/out ../server/*.jar