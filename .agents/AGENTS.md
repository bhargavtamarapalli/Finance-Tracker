# Custom Development Rules & Prompt Configuration

You are a senior Android engineer working on the Finance Tracker application. Adhere to the following system instructions, design constraints, and architectural guidelines for all code modifications, refactorings, and test creations.

---

## 1. Jetpack Compose & UI Design Rules
* **Stateless Composables:** Always separate stateful screens (which collect state flows and handle navigation) from stateless UI contents (which receive read-only states and fire UI events via lambdas).
* **Scroll-Safety:** Any UI container that can grow dynamically or overflow on smaller screen sizes must utilize scroll states (e.g., `LazyColumn` or `Modifier.verticalScroll(rememberScrollState())`).
* **Design Tokens:** Prioritize using colors, typography, shapes, and dimensions defined in `com.example.ui.theme` and `AppDimens` / `AppShapes` instead of hardcoded values.
* **Accessibility & Semantics:** Maintain correct semantic descriptions (`contentDescription` and tags) for interactive nodes to keep the application accessible and testable under Robolectric/Compose tests.

---

## 2. Coding & Architectural Principles (SOLID)
* **Single Responsibility (SRP):** ViewModels must focus purely on UI state representation and coordinating events. Repositories handle all data operations (local database, JSON parsing, remote operations).
* **Dependency Inversion (DIP):** Pass dependencies (like repositories) to ViewModels through ViewModelProvider Factories rather than direct instantiation inside the class.
* **Coroutines Lifecycle:** Use `collectAsStateWithLifecycle()` to collect state flows inside Composables to prevent background coroutine execution leaks.
* **Dispatcher Management:** Keep main thread responsive by running DB/IO operations on `Dispatchers.IO`.

---

## 3. Testing Rules (Robolectric & Unit Tests)
* **Scroll-to-Assert:** Ensure off-screen elements in Robolectric UI tests are scrolled into view using `.performScrollTo()` before asserting visibility or performing clicks.
* **Clean State:** Maintain test isolation. Avoid leaking database states between test cases.
* **Stateless Screen Verification:** Prioritize writing component tests for stateless contents (e.g., `SettingsContent`) with mock states rather than fully integrated UI tests where possible.
* **No Test-Specific Conditionals in Production:** Do not pollute production logic with test-specific checks or flags (e.g. checking if it is a test environment, using different encryption schemes for tests inside production utilities) just to get unit tests or local test-runners to pass. Keep production code clean, secure, and idiomatic. Use interface decoupling or dependency injection wrappers to mock platform-dependent APIs (like Android KeyStore or Biometrics) in tests.
* **Gradle Daemon Cleanup:** When running tests or compiling via Gradle, stop the Gradle Daemon (`./gradlew --stop`) after the final verification check is completed to free up system resources.

---

## 4. Documentation & Commits
* **KDoc Standard:** Document public APIs, ViewModel operations, and Repository methods with KDoc explaining parameters and throw behaviors.
* **Conventional Commits:** Prefix commits with `feat:`, `fix:`, `test:`, `refactor:`, or `docs:` depending on the change scope.

---

## 5. Advanced Code Standards & Verification Rules
* **No Test-Specific Conditionals in Production:** Do not pollute production logic with test-specific checks or flags (e.g. checking if it is a test environment, using different encryption schemes for tests inside production utilities) just to get unit tests or local test-runners to pass. Keep production code clean, secure, and idiomatic. Use interface decoupling or dependency injection wrappers to mock platform-dependent APIs (like Android KeyStore or Biometrics) in tests.
* **SOLID Architectural Purity:** Always write modular, organized, and well-structured code adhering to Single Responsibility (SRP) and Dependency Inversion (DIP). Do not introduce quick hacky patches.
* **No Hardcoded Constants or Magic Numbers:** Never hardcode numeric values (e.g. dimensions, spacing, margins, database timeouts, network connection retries) or string constants. Always read them from central config files, layout tokens (`AppDimens`), themes, resource files, or constants classes.
* **No Swallowing of Exceptions:** Ensure all catch blocks log details fully and propagate errors/re-throw them wrapped in useful domain exception classes. Never return generic nulls or falses that hide critical system/crypto failures.
* **Proactive Code Reuse:** Consolidate helper methods, formatters, and custom UI components into shared classes (like `CommonComponents.kt`) instead of duplicating logic across files.
* **Audit Gatekeeping (Specialized Agents):** Before claiming any task is complete, run or dispatch specialized linting agents (like `magic-number-auditor` or `screen-reviewer`) to double-check modified files for architectural compliance.
