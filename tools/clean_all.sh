#!/bin/bash
FULL_PATH_TO_SCRIPT="$(realpath "${BASH_SOURCE[0]}")"
SCRIPT_DIRECTORY="$(dirname "$FULL_PATH_TO_SCRIPT")"
cd "$SCRIPT_DIRECTORY"

rm -rf ../client1/out ../client2/out ../client3/out ../client1/*.jar ../client2/*.jar ../client3/*.jar ../server/out ../server/*.jar