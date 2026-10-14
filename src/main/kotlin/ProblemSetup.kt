package org.sedaiadesigns

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

internal data class SetupResult(val testClass: String, val starter: Path, val test: Path) {
    val command: String get() = "./gradlew test --tests $testClass"
}

internal object ProblemSetup {
    private const val PACKAGE = "org.sedaiadesigns"
    private const val MAIN_MARKER = "// Managed by LeetcodeViewer"

    fun create(project: Project, question: Question): SetupResult {
        val root = Path.of(project.basePath ?: error("Open a Kotlin Gradle project first."))
        require(Files.exists(root.resolve("build.gradle.kts")) || Files.exists(root.resolve("build.gradle"))) {
            "This workflow needs a Gradle project."
        }
        val className = "Problem${question.number}" + question.slug.split('-')
            .joinToString("") { part -> part.replaceFirstChar { it.uppercase() }.filter { it.isLetterOrDigit() } }
        val testName = "${className}Test"
        val packageDir = Path.of("org", "sedaiadesigns")
        val starter = root.resolve("src/main/kotlin").resolve(packageDir).resolve("$className.kt")
        val test = root.resolve("src/test/kotlin").resolve(packageDir).resolve("$testName.kt")
        val runConfig = root.resolve(".run").resolve("$testName.run.xml")
        val main = root.resolve("src/main/kotlin").resolve(packageDir).resolve("Main.kt")

        require(!Files.exists(starter) && !Files.exists(test) && !Files.exists(runConfig)) {
            "This problem already has starter files or a run configuration. Existing work was kept."
        }
        if (Files.exists(main)) require(Files.readString(main).startsWith(MAIN_MARKER)) {
            "Main.kt exists and is not managed by Leetcode Helper. Add the call manually; no files were changed."
        }

        val excerpt = question.content.replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&amp;", "&").replace(Regex("\\s+"), " ").trim().take(500)
        val examples = Regex("<pre[^>]*>(.*?)</pre>", RegexOption.DOT_MATCHES_ALL)
            .findAll(question.content).take(2).map { match ->
                match.groupValues[1].replace(Regex("<[^>]+>"), " ")
                    .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                    .replace("&amp;", "&").trim()
            }.filter { it.isNotBlank() }.toList()
        val brief = ("${question.number}. ${question.title}\n$excerpt\n" +
            examples.mapIndexed { index, example -> "Example ${index + 1}: $example" }.joinToString("\n") +
            "\n${question.url}")
            .lines().joinToString("\n") { " * " + it.replace("*/", "* / ") }
        val starterText = """package $PACKAGE

/**
$brief
 */
class $className {
    fun solve(): Unit = TODO("Implement LeetCode #${question.number}")
}
"""
        val testText = """package $PACKAGE

import org.junit.Test

class $testName {
    @Test
    fun starterIsReadyForImplementation() {
        $className().solve()
    }
}
"""
        val configText = """<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="$testName" type="GradleRunConfiguration" factoryName="Gradle">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="${'$'}PROJECT_DIR${'$'}" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="--tests $PACKAGE.$testName" />
      <option name="taskNames"><list><option value="test" /></list></option>
    </ExternalSystemSettings>
    <method v="2" />
  </configuration>
</component>
"""
        val previous = if (Files.exists(main)) Files.readString(main) else null
        val previousCalls = previous?.lines()?.map { it.trim() }
            ?.filter { it.startsWith("Problem") || it.startsWith("// Problem") }
            ?.map { "        // " + it.removePrefix("// ") } ?: emptyList()
        val mainText = buildString {
            appendLine(MAIN_MARKER)
            appendLine("package $PACKAGE")
            appendLine()
            appendLine("object Main {")
            appendLine("    @JvmStatic fun main(args: Array<String>) {")
            previousCalls.forEach { appendLine(it) }
            appendLine("        $className().solve()")
            appendLine("    }")
            appendLine("}")
        }

        Files.createDirectories(starter.parent)
        Files.createDirectories(test.parent)
        Files.createDirectories(runConfig.parent)
        Files.writeString(starter, starterText, StandardOpenOption.CREATE_NEW)
        Files.writeString(test, testText, StandardOpenOption.CREATE_NEW)
        Files.writeString(runConfig, configText, StandardOpenOption.CREATE_NEW)
        Files.writeString(main, mainText)
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root)
        return SetupResult("$PACKAGE.$testName", starter, test)
    }
}
