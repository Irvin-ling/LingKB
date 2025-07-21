package com.ling.lingkb.entity;


/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/21
 */
public enum QwenQuestion {
    //提问
    CHAT_NO_ANSWER("问题是[%s]，但未搜索到相关答案。需要你以`灵库`的身份回答问题，不要输出其它无关内容。示例(自行用答案替换XXX)：知识库中没有搜索到相关信息，但经过灵知库我的考虑：XXX"),
    CHAT_HAS_ANSWER("问题是[%s]，知识库搜索到的topk句子是[%s]。需要你从这些句子里选择最合适的一句，重新组织内容后返回，不要输出其它无关内容");
    ;

    QwenQuestion(String fragment) {
        this.fragment = fragment;
    }

    private String fragment;

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }
}
