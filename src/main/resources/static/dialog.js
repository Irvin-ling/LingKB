const API_BASE_URL = "http://127.0.0.1:8080";
const {createApp, ref, onMounted, watch, nextTick, onUnmounted} = Vue;

function createItalicText(dataStr) {
    const style = 'color:#6a5acd; font-style:italic; border-left:3px solid #6a5acd; padding-left:8px;';
    return `<br><i style="${style}">${dataStr}</i></br>`;
}

let translationTag = null;

function translateText(element, tag) {
    if (translationTag === null) {
        translationTag = tag;
        selected(element);
    } else if (translationTag === tag) {
        translationTag = null;
        unselected(element);
    } else {
        const elements = document.querySelectorAll('span[data-tag="translate"]');
        elements.forEach(element => {
            unselected(element);
        });
        translationTag = tag;
        selected(element);
    }
}

function selected(element) {
    element.classList.remove('bg-gray-300', 'text-gray-700');
    element.classList.add('bg-primary', 'text-white', 'scale-105');
    element.classList.add('animate-select');
    setTimeout(() => element.classList.remove('animate-select'), 300);
}

function unselected(element) {
    element.classList.remove('bg-primary', 'text-white', 'scale-105');
    element.classList.add('bg-gray-300', 'text-gray-700');
    element.classList.add('animate-unselect');
    setTimeout(() => element.classList.remove('animate-unselect'), 300);
}

createApp({
    setup() {
        const messageContainer = ref(null);
        const messages = ref([]);
        const sdMessages = ref([]);
        const inputMsg = ref('');
        const isLoading = ref(false);
        const userScrollLock = ref(false);
        const showModal = ref(false);
        const modalContent = ref('');
        const modalTitle = ref('');
        const modalIsHtml = ref(false);
        let scrollObserver = null;

        const scrollToBottom = (force = false) => {
            nextTick(() => {
                if (!messageContainer.value) return;
                if (force || !userScrollLock.value) {
                    messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
                }
            });
        };

        const setupScrollObserver = () => {
            if (!messageContainer.value) return;

            const options = {
                root: null,
                threshold: 0.9,
                rootMargin: "0px 0px -100px 0px"
            };

            scrollObserver = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.target === messageContainer.value) {
                        userScrollLock.value = !entry.isIntersecting;
                    }
                });
            }, options);

            scrollObserver.observe(messageContainer.value);
        };

        const escapeHtml = (text) => {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        };

        const copyMessage = (content) => {
            // 创建一个临时textarea元素来复制纯文本
            content = content.replace(/[\r\n\u2028\u2029]/g, '');
            const textarea = document.createElement('textarea');
            // 如果是HTML内容，我们需要提取纯文本
            if (typeof content === 'string' && content.includes('<')) {
                const div = document.createElement('div');
                div.innerHTML = content;
                textarea.value = div.textContent || div.innerText || '';
            } else {
                textarea.value = content;
            }

            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);

            // 显示复制成功的提示
            const notification = document.createElement('div');
            notification.className = 'fixed bottom-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg';
            notification.textContent = '已复制到剪贴板';
            document.body.appendChild(notification);

            setTimeout(() => {
                document.body.removeChild(notification);
            }, 2000);
        };

        const showFullMessage = (msg) => {
            modalContent.value = msg.content;
            modalTitle.value = msg.role === 'user' ? '我的消息' : '助手回复';
            modalIsHtml.value = msg.isHtml;
            showModal.value = true;
        };

        const handleSpecialContent = (message, data) => {
            let html = '';
            switch (data.type) {
                case 'code':
                    html = `<pre style="margin-top: 7px"><code class="language-${data.language}">${escapeHtml(data.content)}</code></pre>`;
                    break;
                case 'image':
                    html = `<img src="${data.content}" class="max-w-full h-auto rounded-lg" style="max-height:300px; margin-top: 7px">`;
                    break;
                case 'table':
                    let theadData = data.data;
                    let table = '<div class="table-container"><table class="custom-table" style="margin-top: 7px">';
                    table += '<thead><tr>';
                    for (let j = 0; j < data.cols; j++) {
                        table += `<td>${escapeHtml(theadData[0][j])}</td>`;
                    }
                    table += '</tr></thead><tbody>';
                    for (let i = 1; i < data.rows; i++) {
                        table += '<tr>';
                        for (let j = 0; j < data.cols; j++) {
                            table += `<td>${escapeHtml(theadData[i][j])}</td>`;
                        }
                        table += '</tr>';
                    }
                    table += '</tbody></table></div>';
                    html = table;
                    break;
                case 'link':
                    const displayText = data.webText || data.content;
                    html = `<a href="${escapeHtml(data.content)}" target="_blank" class="text-blue-500 hover:underline" style="margin-top: 7px">${escapeHtml(displayText)}</a>`;
                    break;
                default:
                    html = escapeHtml(data.content);
            }
            message.content += html;
            message.isHtml = true;
        };

        const sendMessage = async () => {
            if (!inputMsg.value.trim()) return;

            const tags = {
                translation: translationTag
            };

            const userMessage = {
                role: 'user',
                content: inputMsg.value,
                time: new Date().toTimeString().slice(0, 5),
                isHtml: false
            };
            messages.value.push(userMessage);
            sdMessages.value.push(userMessage);
            inputMsg.value = '';
            scrollToBottom(true);

            try {
                isLoading.value = true;
                const assistantMessage = {
                    role: 'assistant',
                    content: '',
                    time: new Date().toTimeString().slice(0, 5),
                    isHtml: false
                };

                const response = await fetch(API_BASE_URL + '/ling/dialog', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({messages: sdMessages.value, chatTag: tags})
                });

                if (!response.ok) throw new Error(`HTTP error: ${response.status}`);

                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let buffer = '';
                let assistantAdded = false;

                while (true) {
                    const {done, value} = await reader.read();
                    if (done) break;

                    buffer += decoder.decode(value, {stream: true});
                    const lines = buffer.split('\n');
                    buffer = lines.pop() || '';

                    for (const line of lines) {
                        if (!line.trim()) continue;
                        if (line.startsWith('data: ')) {
                            const dataStr = line.substring(6).trim();
                            if (dataStr === '[DONE]') continue;

                            try {
                                const data = JSON.parse(dataStr);
                                let content = data.choices?.[0]?.delta?.content || '';
                                const endTagIndex = content.indexOf('think>');
                                if (endTagIndex !== -1) {
                                    content = content.substring(endTagIndex + 6);
                                }

                                if (!assistantAdded && content) {
                                    messages.value.push(assistantMessage);
                                    sdMessages.value.push(JSON.parse(JSON.stringify(assistantMessage)));
                                    assistantAdded = true;
                                }

                                assistantMessage.content += content;
                                if (assistantAdded) {
                                    messages.value = [...messages.value];
                                    scrollToBottom();
                                }
                            } catch (e) {
                                console.error('Data parse error:', e);
                            }
                        } else if (line.startsWith('added: ')) {
                            const dataStr = line.substring(7);
                            if (!assistantAdded) {
                                messages.value.push(assistantMessage);
                                assistantAdded = true;
                            }

                            assistantMessage.content += createItalicText(dataStr);
                            assistantMessage.isHtml = true;
                            if (assistantAdded) {
                                messages.value = [...messages.value];
                                scrollToBottom();
                            }
                        } else if (line.startsWith('link: ')) {
                            const dataStr = line.substring(6).trim();
                            if (dataStr === '[DONE]') continue;
                            try {
                                const data = JSON.parse(dataStr);
                                if (!assistantAdded) {
                                    messages.value.push(assistantMessage);
                                    assistantAdded = true;
                                }
                                handleSpecialContent(assistantMessage, data);
                                if (assistantAdded) {
                                    messages.value = [...messages.value];
                                    scrollToBottom();
                                }
                            } catch (e) {
                                console.error('Link parse error:', e);
                            }
                        }
                    }
                }
            } catch (error) {
                console.error('Request failed:', error);
            } finally {
                isLoading.value = false;
                scrollToBottom(true);
            }
        };

        onMounted(() => {
            setupScrollObserver();
            scrollToBottom(true);
        });

        onUnmounted(() => {
            if (scrollObserver) scrollObserver.disconnect();
        });

        watch(messages, () => scrollToBottom());
        watch(isLoading, (val) => !val && scrollToBottom());

        return {
            messageContainer,
            messages,
            inputMsg,
            isLoading,
            showModal,
            modalContent,
            modalTitle,
            modalIsHtml,
            copyMessage,
            showFullMessage,
            sendMessage
        };
    }
}).mount('#app');