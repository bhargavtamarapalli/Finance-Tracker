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
* **Gradle Daemon Cleanup:** When running tests or compiling via Gradle, stop the Gradle Daemon (`./gradlew --stop`) after the final verification check is completed to free up system resources.

---

## 4. Documentation & Commits
* **KDoc Standard:** Document public APIs, ViewModel operations, and Repository methods with KDoc explaining parameters and throw behaviors.
* **Conventional Commits:** Prefix commits with `feat:`, `fix:`, `test:`, `refactor:`, or `docs:` depending on the change scope.
