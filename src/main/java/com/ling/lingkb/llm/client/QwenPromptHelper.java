package com.ling.lingkb.llm.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.List;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/24
 */
public class QwenPromptHelper {

    public static void buildPrompt(JSONObject requestJson, String question, List<String> answerA1List) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("针对问题‘").append(question).append("’，请按以下规则回答：\n");
        boolean isA1Empty = answerA1List == null || answerA1List.isEmpty() ||
                answerA1List.stream().allMatch(a -> a == null || a.trim().isEmpty());

        if (isA1Empty) {
            prompt.append("1. 经检索，常规文本库中没有找到相关答案，你需要基于自身逻辑推理和知识储备进行回答；\n");
        } else {
            prompt.append("1. 经检索，常规文本库中找到以下答案：\n");
            for (int i = 0; i < answerA1List.size(); i++) {
                String a1 = answerA1List.get(i);
                if (a1 != null && !a1.trim().isEmpty() && a1.trim().length() > 1) {
                    prompt.append("   （").append(i + 1).append("）").append(a1).append("\n");
                }
            }
            prompt.append("   请结合上述所有答案，提炼核心信息，以清晰、连贯的方式进行回答。注意：无需重复罗列序号，并且不要回复逻辑思考过程，需要纯粹的答案文本；\n");
        }

        JSONArray messages = requestJson.getJSONArray("messages");
        JSONObject lastJson = messages.getJSONObject(messages.size() - 1);
        lastJson.put("content", prompt.toString());
    }

    public static void buildToEnPrompt(JSONObject requestJson, String question) {
        JSONArray messages = requestJson.getJSONArray("messages");
        JSONObject lastJson = messages.getJSONObject(messages.size() - 1);
        String prompt = "你的任务是将提供的中文文本直接翻译成英文，且输出中不要有文本之外的字符。\n以下是需要翻译的中文文本：\n<chinese_text>\n" + question +
                "\n</chinese_text>\n在翻译时，直接输出该中文文本的英文翻译，不要添加额外的内容或标签。\n";
        lastJson.put("content", prompt);
    }

    public static void buildToZhPrompt(JSONObject requestJson, String question) {
        JSONArray messages = requestJson.getJSONArray("messages");
        JSONObject lastJson = messages.getJSONObject(messages.size() - 1);
        String prompt = "将以下英文问题翻译成中文。仅提供翻译后的文本且收尾不带引号，不包含任何解释或额外内容:\n\n" + "\"" + question + "\"";
        lastJson.put("content", prompt);
    }
}
