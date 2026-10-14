# Leetcode Helper

An IntelliJ IDEA tool window for reading LeetCode problems by their displayed question number.

This project is 100% AI generated and intended for personal use. Others are welcome to build and use it.

## Use

Open **View → Tool Windows → Leetcode Helper**, enter a positive question number (for example, `1`), and press **Load** or Enter. The panel displays the title, difficulty, statement, examples, and constraints when LeetCode provides them. **Open on LeetCode** opens the problem in a browser. Premium-only statements require viewing on LeetCode.

## Build and run

The project targets IntelliJ IDEA 2026.2.3. Use JDK 25 and run `./gradlew runIde` to open a sandbox IDE, or `./gradlew buildPlugin` to create an installable ZIP in `build/distributions`.

The plugin uses LeetCode's public GraphQL endpoint. An internet connection is required, and lookup may fail if LeetCode changes or restricts that endpoint.

## Problem setup workflow

The artifact generation workflow currently supports Kotlin only. Support for other languages is not planned, but may be implemented by forks or contributions. For other programming languages, use a different setup workflow, or use the Leetcode Helper tool window solely as a problem viewer.

After loading a question, click **Set up Kotlin starter and test** in the Leetcode Helper tool window. For a Kotlin Gradle project, the plugin creates a separate `Problem<number><Title>` starter under `src/main/kotlin/org/sedaiadesigns`, a matching JUnit 4 test under `src/test/kotlin/org/sedaiadesigns`, and a `.run` Gradle configuration. It updates the plugin-managed `Main.main()` to invoke the current problem and leaves earlier calls commented out. Existing problem files are never overwritten. If an unmanaged `Main.kt` already exists at that path, setup stops before making changes.

The generated `solve()` is intentionally unfinished. Run the command shown in the tool window; its test is expected to fail with `NotImplementedError` until you implement the problem. The starter includes a short prompt and up to two examples from LeetCode. The target project must already have JUnit 4 available to its test source set.

For advice or debugging, **Copy instructor prompt** places the learner guidance on the clipboard. Paste it into your preferred AI assistant along with your question and relevant code. The plugin does not send project code to an AI service.
