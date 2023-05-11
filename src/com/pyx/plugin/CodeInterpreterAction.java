package com.pyx.plugin;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressManager;

import com.intellij.openapi.ui.Messages;
import com.sun.prism.Texture;


public class CodeInterpreterAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();

        if (selectedText != null) {
            // Run the API call in a separate process with a progress indicator
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                String explanation = callApiToGetCodeExplanation(selectedText);
                ApplicationManager.getApplication().invokeLater(() -> {
                    addCommentAboveSelectedCode(editor, explanation);
                });
            }, "在读了，诶，这代码谁写的！", true, editor.getProject());
        }
    }

    private void addCommentAboveSelectedCode(Editor editor, String comment) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            Document document = editor.getDocument();
            Caret caret = editor.getCaretModel().getCurrentCaret();
            int startOffset = caret.getSelectionStart();
            int lineStartOffset = document.getLineStartOffset(document.getLineNumber(startOffset));

            String cc = comment;
            // 如果所选文本的第一行不是文件的第一行，则需要添加一个换行符
            if (startOffset > lineStartOffset) {
                cc = "\n\t" + cc;
            }
            String finalCc = "\t/**" + "\t" + wrapStringByLength(cc, 50) + "*/\n";
            ApplicationManager.getApplication().runWriteAction(() -> {
                WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                    document.insertString(lineStartOffset, finalCc);
                });
            });
        });
    }


    private String callApiToGetCodeExplanation(String code) {
        String apiUrl = "https://pyx.redhat.team/apis/message"; // 替换为你的 API 地址
        try {
            // 发送API请求
            HttpResponse response = HttpRequest.post(apiUrl)
                    .form("message", code)
                    .form("temperature", "0")
                    .form("system", "请用文本解释这段代码，若解释文本长度大于50个字符，则每50个字符后面添加一个换行符。除此之外不要做任何回复！谢谢")
                    .execute();
            if (response.isOk()) {
                String responseBody = response.body();
                return parseApiResponse(responseBody);
            } else {
                return "Error: API returned status 不要慌，重新试一下吧。" + response.getStatus();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "不要慌，重新试一下吧。Error: " + e.getMessage();
        }
    }

    private String parseApiResponse(String response) {
        JSONObject jsonResponse = JSONUtil.parseObj(response);
        // 假设你的 API 响应中有一个名为 "explanation" 的字段
        return jsonResponse.getStr("result");
    }


    public static String wrapStringByLength(String str, int lineLength) {
        StringBuilder sb = new StringBuilder();
        int currentIndex = 0;
        int strLength = str.length();

        while (currentIndex < strLength) {
            int endIndex = Math.min(currentIndex + lineLength, strLength);
            String line = str.substring(currentIndex, endIndex);
            sb.append(line).append("\n\t");
            currentIndex = endIndex;
        }

        return sb.toString();
    }
}
