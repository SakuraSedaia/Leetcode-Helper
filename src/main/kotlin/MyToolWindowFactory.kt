package org.sedaiadesigns

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.event.HyperlinkEvent

class MyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val view = QuestionView(project)
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(view, "Question", false))
    }

    private class QuestionView(private val project: Project) : JPanel(BorderLayout()) {
        private val index = JTextField(7)
        private val load = JButton("Load")
        private val setup = JButton("Set up starter and test").apply { isEnabled = false }
        private val coach = JButton("Copy instructor prompt").apply { isEnabled = false }
        private val status = JLabel("Enter a LeetCode question number.")
        private var selectedQuestion: Question? = null
        private val statement = JEditorPane("text/html", "").apply {
            isEditable = false
            addHyperlinkListener { event ->
                if (event.eventType == HyperlinkEvent.EventType.ACTIVATED && event.url?.host == "leetcode.com") {
                    BrowserUtil.browse(event.url)
                }
            }
        }
        private var requestNumber = 0

        init {
            val controls = JPanel(FlowLayout(FlowLayout.LEADING)).apply {
                add(JLabel("Question #"))
                add(index)
                add(load)
                add(setup)
                add(coach)
            }
            add(controls, BorderLayout.NORTH)
            add(JScrollPane(statement), BorderLayout.CENTER)
            add(status, BorderLayout.SOUTH)
            load.addActionListener { fetch() }
            index.addActionListener { fetch() }
            setup.addActionListener {
                val question = selectedQuestion ?: return@addActionListener
                setup.isEnabled = false
                ApplicationManager.getApplication().executeOnPooledThread {
                    val result = runCatching { ProblemSetup.create(project, question) }
                    SwingUtilities.invokeLater {
                        if (project.isDisposed) return@invokeLater
                        result.onSuccess { created ->
                            status.text = "Created starter and test. Run: ${created.command} (expected to fail until implemented)."
                        }.onFailure { error ->
                            status.text = "Setup failed: ${error.message ?: "Unknown error"}"
                        }
                    }
                }
            }
            coach.addActionListener {
                val question = selectedQuestion ?: return@addActionListener
                val prompt = """Act as a LeetCode instructor for my Kotlin project. I am working on #${question.number}, ${question.title} (${question.url}). For advice, reviews, or debugging, explain the code without editing it. Do not give the complete solution directly; explain the relevant behavior and guide me toward the next step. Keep all previous problems and tests. Only modify code if I explicitly ask you to set up a new question or provide a solution."""
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(prompt), null)
                status.text = "Instructor prompt copied. Paste it into your AI assistant with your question."
            }
        }

        private fun fetch() {
            val number = index.text.trim().toIntOrNull()
            if (number == null || number <= 0) {
                status.text = "Enter a positive question number."
                return
            }
            val currentRequest = ++requestNumber
            selectedQuestion = null
            setup.isEnabled = false
            coach.isEnabled = false
            load.isEnabled = false
            status.text = "Loading question #$number…"
            ApplicationManager.getApplication().executeOnPooledThread {
                val result = runCatching { LeetCodeClient.fetch(number) }
                SwingUtilities.invokeLater {
                    if (project.isDisposed || currentRequest != requestNumber) return@invokeLater
                    load.isEnabled = true
                    result.onSuccess { question ->
                        if (question == null) {
                            status.text = "Question #$number was not found."
                            statement.text = ""
                        } else {
                            selectedQuestion = question
                            setup.isEnabled = true
                            coach.isEnabled = true
                            status.text = "${question.difficulty} · Question #${question.number}"
                            statement.text = "<html><body style='font-family:sans-serif;margin:12px'>" +
                                "<h2>${escape(question.title)}</h2>" +
                                "<p><a href='${question.url}'>Open on LeetCode</a></p>" +
                                question.content + "</body></html>"
                            statement.caretPosition = 0
                        }
                    }.onFailure { error ->
                        status.text = "Could not load question: ${error.message ?: "Unknown error"}"
                    }
                }
            }
        }

        private fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;")
    }
}
