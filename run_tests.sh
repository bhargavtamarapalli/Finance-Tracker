#!/usr/bin/env bash
# =============================================================================
# Finance Manager — E2E & Unit Test Runner
# =============================================================================
# Usage:
#   ./run_tests.sh                  → Run ALL tests
#   ./run_tests.sh auth             → Auth E2E tests (AuthScreenUserFlowTest)
#   ./run_tests.sh dashboard        → Dashboard E2E tests
#   ./run_tests.sh transaction      → Transaction E2E tests
#   ./run_tests.sh category         → Category Management E2E tests
#   ./run_tests.sh guest            → Guest User Flow E2E tests
#   ./run_tests.sh settings         → Settings Extended E2E tests
#   ./run_tests.sh e2e              → ALL new e2e/* tests
#   ./run_tests.sh navigation       → Existing FinanceAppNavigationTest
#   ./run_tests.sh ledger           → Existing FinanceLedgerUserFlowTest
#   ./run_tests.sh analytics        → Existing AnalyticsScreenTest
#   ./run_tests.sh history          → Existing TransactionHistoryScreenTest
#   ./run_tests.sh unit             → Existing FinanceViewModelTest
#   ./run_tests.sh existing         → ALL legacy tests (non-e2e)
#   ./run_tests.sh all              → Run ALL tests
#   ./run_tests.sh --list           → List available suites
# =============================================================================

set -e

JAVA_HOME_PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log_info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
log_success() { echo -e "${GREEN}[PASS]${NC}  $*"; }
log_error()   { echo -e "${RED}[FAIL]${NC}  $*"; }

print_header() {
  echo ""
  echo -e "${CYAN}╔══════════════════════════════════════════════════╗${NC}"
  echo -e "${CYAN}║    Finance Manager — E2E Test Runner             ║${NC}"
  echo -e "${CYAN}╚══════════════════════════════════════════════════╝${NC}"
  echo ""
}

list_suites() {
  echo ""
  echo -e "${YELLOW}New E2E Suites (com.example.e2e.*):${NC}"
  echo "  auth         → AuthScreenUserFlowTest          (15 tests)"
  echo "  dashboard    → DashboardUserFlowTest           (7 tests)"
  echo "  transaction  → TransactionUserFlowTest         (4 tests)"
  echo "  category     → CategoryManagementE2ETest       (4 tests)"
  echo "  guest        → GuestUserFlowTest               (7 tests)"
  echo "  settings     → SettingsExtendedUserFlowTest    (6 tests)"
  echo "  e2e          → ALL of the above"
  echo ""
  echo -e "${YELLOW}Existing Suites:${NC}"
  echo "  navigation   → FinanceAppNavigationTest"
  echo "  ledger       → FinanceLedgerUserFlowTest"
  echo "  analytics    → AnalyticsScreenTest"
  echo "  history      → TransactionHistoryScreenTest"
  echo "  unit         → FinanceViewModelTest"
  echo "  existing     → ALL legacy tests"
  echo ""
  echo -e "${YELLOW}Combined:${NC}"
  echo "  all          → Run every test in the project"
  echo ""
}

run_tests() {
  local filter="$1"
  local label="$2"
  log_info "Suite: $label"
  echo ""
  if [ -n "$filter" ]; then
    JAVA_HOME="$JAVA_HOME_PATH" ./gradlew testDebugUnitTest --tests "$filter"
  else
    JAVA_HOME="$JAVA_HOME_PATH" ./gradlew testDebugUnitTest
  fi
}

print_header
cd "$PROJECT_DIR"

SUITE="${1:-all}"

case "$SUITE" in
  --list|list)   list_suites ; exit 0 ;;

  # ── New E2E ------------------------------------------------------------------
  auth)          run_tests "com.example.e2e.auth.*"         "Auth E2E Tests" ;;
  dashboard)     run_tests "com.example.e2e.dashboard.*"    "Dashboard E2E Tests" ;;
  transaction)   run_tests "com.example.e2e.transaction.*"  "Transaction E2E Tests" ;;
  category)      run_tests "com.example.e2e.category.*"     "Category E2E Tests" ;;
  guest)         run_tests "com.example.e2e.guest.*"        "Guest Flow E2E Tests" ;;
  settings)      run_tests "com.example.e2e.settings.*"     "Settings E2E Tests" ;;
  e2e)           run_tests "com.example.e2e.*"              "All New E2E Tests" ;;

  # ── Existing ----------------------------------------------------------------
  navigation)    run_tests "com.example.ui.FinanceAppNavigationTest"        "Navigation Tests" ;;
  ledger)        run_tests "com.example.ui.FinanceLedgerUserFlowTest"       "Ledger Flow Tests" ;;
  analytics)     run_tests "com.example.AnalyticsScreenTest"                "Analytics Tests" ;;
  history)       run_tests "com.example.TransactionHistoryScreenTest"       "History Tests" ;;
  unit)          run_tests "com.example.ui.viewmodel.FinanceViewModelTest"  "ViewModel Unit Tests" ;;
  existing)
    JAVA_HOME="$JAVA_HOME_PATH" ./gradlew testDebugUnitTest \
      --tests "com.example.ui.FinanceAppNavigationTest" \
      --tests "com.example.ui.FinanceLedgerUserFlowTest" \
      --tests "com.example.ui.CategoryCustomizationUserFlowTest" \
      --tests "com.example.ui.CurrencyCustomizationUserFlowTest" \
      --tests "com.example.ui.AdminConsoleUserFlowTest" \
      --tests "com.example.AnalyticsScreenTest" \
      --tests "com.example.TransactionHistoryScreenTest" \
      --tests "com.example.SettingsScreenTest" \
      --tests "com.example.ui.viewmodel.FinanceViewModelTest"
    ;;

  # ── All ---------------------------------------------------------------------
  all|"")        run_tests "" "ALL Tests" ;;

  *)
    log_error "Unknown suite: '$SUITE'"
    list_suites
    exit 1
    ;;
esac

echo ""
log_info "HTML report: $(pwd)/app/build/reports/tests/testDebugUnitTest/index.html"
echo ""
