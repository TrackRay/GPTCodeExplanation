package com.pyx.plugin;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressManager;

import com.intellij.openapi.ui.Messages;


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
                ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(editor.getProject(), explanation, "愿天下没有难懂的代码！祝答辩顺利！By_PYX", AllIcons.Actions.SetDefault));
            }, "在读了，诶，这代码谁写的！", true, editor.getProject());
        }
    }

    private String callApiToGetCodeExplanation(String code) {
        String apiUrl = "https://xxx/apis/message"; // 替换为你的 API 地址
        try {
            // 发送API请求
            HttpResponse response = HttpRequest.post(apiUrl)
                    .form("message", code)
                    .form("temperature", "0")
                    .form("system", "请用文本解释这段代码，除此之外不要做任何回复！谢谢")
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
}
