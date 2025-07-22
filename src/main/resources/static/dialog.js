const API_BASE_URL = "http://127.0.0.1:8080";
const {createApp, ref, onMounted, watch, nextTick} = Vue;

createApp({
    setup() {
        const messageContainer = ref(null);
        const messages = ref([]);
        const inputMsg = ref('');
        const isLoading = ref(false);

        const scrollToBottom = () => {
            nextTick(() => {
                if (messageContainer.value) {
                    messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
                    console.log('Scroll to bottom triggered');
                }
            });
        };

        watch(messages, scrollToBottom);

        watch(isLoading, (newVal) => {
            if (!newVal) {
                scrollToBottom();
            }
        });

        onMounted(scrollToBottom);

        const sendMessage = async () => {
            if (!inputMsg.value.trim()) return;

            const userMessage = {
                role: 'user',
                content: inputMsg.value,
                time: new Date().toTimeString().slice(0, 5)
            };
            messages.value.push(userMessage);
            inputMsg.value = '';

            try {
                isLoading.value = true;

                const assistantMessage = {
                    role: 'assistant',
                    content: '',
                    displayContent: '',
                    time: new Date().toTimeString().slice(0, 5)
                };

                const response = await fetch(API_BASE_URL + '/ling/dialog', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({
                        messages: messages.value
                    })
                });

                if (!response.ok) {
                    throw new Error(`HTTP 错误: ${response.status}`);
                }

                const reader = response.body.getReader();
                const decoder = new TextDecoder('utf-8');
                let accumulated = '';
                let typingTimer = null;
                let assistantAdded = false;

                const typeNextChar = () => {
                    if (assistantMessage.content.length > assistantMessage.displayContent.length) {
                        assistantMessage.displayContent += assistantMessage.content[assistantMessage.displayContent.length];
                        messages.value = [...messages.value];
                        typingTimer = setTimeout(typeNextChar, 50);
                    }
                };

                while (true) {
                    const {done, value} = await reader.read();
                    if (done) break;

                    accumulated += decoder.decode(value);
                    const lines = accumulated.split('\n');
                    accumulated = lines.pop() || '';

                    for (const line of lines) {
                        const trimmed = line.trim();
                        if (!trimmed) continue;

                        if (trimmed.startsWith('data: ')) {
                            const dataStr = trimmed.slice(6);
                            if (dataStr === '[DONE]') continue;

                            try {
                                const data = JSON.parse(dataStr);
                                let content = data.choices?.[0]?.delta?.content || '';
                                const endTagIndex = content.indexOf('think>', content);
                                if (endTagIndex !== -1) {
                                    content = content.substring(endTagIndex + 6);
                                }

                                if (!assistantAdded && content) {
                                    messages.value.push(assistantMessage);
                                    assistantAdded = true;
                                }

                                assistantMessage.content += content;
                                if (assistantAdded && !typingTimer) {
                                    typeNextChar();
                                }
                            } catch (e) {
                                console.error('解析失败:', e);
                                if (!assistantAdded) {
                                    messages.value.push(assistantMessage);
                                    assistantAdded = true;
                                }
                                assistantMessage.content += dataStr;
                                if (!typingTimer) {
                                    typeNextChar();
                                }
                            }
                        }
                    }
                }
            } catch (error) {
                console.error('发送消息失败:', error);
            } finally {
                isLoading.value = false;
            }
        };

        return {
            messageContainer,
            messages,
            inputMsg,
            sendMessage,
            isLoading
        };
    }
}).mount('#app');