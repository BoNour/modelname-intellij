package io.whizi.modelname

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/** Editor > right click > "Parse AI Model ID": shows vendor, family and version. */
class ParseSelectionAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        var text = editor.selectionModel.selectedText?.trim()?.trim('"', '\'', '`') ?: ""
        if (text.isEmpty()) text = wordAtCaret(editor.document.charsSequence, editor.caretModel.offset)

        val group = NotificationGroupManager.getInstance().getNotificationGroup("Model Name")
        if (text.isEmpty()) {
            group.createNotification("Select a model ID first, like claude-sonnet-4-5.", NotificationType.INFORMATION)
                .notify(e.project)
            return
        }
        val m = ModelName.modelIn(text) ?: ModelName.parse(text)
        val body = if (m.family.isEmpty() && m.vendor.isEmpty()) {
            "nothing recognised"
        } else {
            listOf(ModelName.title(m), ModelName.details(m)).filter { it.isNotEmpty() }.joinToString("<br>")
        }
        group.createNotification(text, body, NotificationType.INFORMATION).notify(e.project)
    }

    // The ID-looking run of characters under the caret, quotes excluded.
    private fun wordAtCaret(chars: CharSequence, offset: Int): String {
        fun ok(c: Char) = c.isLetterOrDigit() || c in "._:/+-"
        var start = offset.coerceIn(0, chars.length)
        var end = start
        while (start > 0 && ok(chars[start - 1])) start--
        while (end < chars.length && ok(chars[end])) end++
        return chars.subSequence(start, end).toString()
    }
}
