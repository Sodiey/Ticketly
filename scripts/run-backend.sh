#!/usr/bin/env bash
# Installs dependencies (if needed) and starts the mock REST backend that the
# Android app talks to at http://10.0.2.2:4000 from the emulator.
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
backend_dir="$(cd "$script_dir/../backend" && pwd)"

cd "$backend_dir"

if [ ! -d node_modules ]; then
    echo "Installing backend dependencies..."
    npm install
fi

npm start
