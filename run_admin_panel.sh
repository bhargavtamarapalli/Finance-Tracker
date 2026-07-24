#!/bin/bash
# Helper script to launch the Obsidian Admin Panel dev server

echo "============================================="
echo "  Obsidian Web & Mobile Admin Panel Server"
echo "============================================="
echo ""
echo "Starting local Python HTTP server on port 8000..."
echo "👉 Open: http://localhost:8000/admin-panel/ in your browser"
echo ""
echo "Press Ctrl+C to stop the server."
echo "============================================="
echo ""

# Check if Python is installed
if ! command -v python3 &> /dev/null
then
    echo "Python3 could not be found. Please open 'admin-panel/index.html' directly in your browser."
    exit 1
fi

python3 -m http.server 8000
