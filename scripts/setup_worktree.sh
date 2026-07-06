#!/usr/bin/env bash
set -euo pipefail
# Create isolated git worktree for testing
repo_root=$(git rev-parse --show-toplevel)
worktree_dir="${repo_root}/../FinanceManager-tests"
if [ -d "$worktree_dir" ]; then
  echo "Worktree already exists at $worktree_dir"
else
  git worktree add "$worktree_dir" HEAD
  echo "Worktree created at $worktree_dir"
fi
