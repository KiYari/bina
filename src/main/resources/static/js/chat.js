class AIChatStream {
    constructor() {
        this.eventSource = null;
        this.isConnected = false;
        this.currentMessage = '';
        this.streamId = null;
        this.initializeEventListeners();
    }

    initializeEventListeners() {
        document.getElementById('chatForm').addEventListener('submit', (e) => {
            e.preventDefault();
            const message = document.getElementById('userMessage').value.trim();
            if (message) {
                this.sendMessage(message);
            }
        });

        document.getElementById('stopButton').addEventListener('click', () => {
            this.stopGeneration();
        });

        document.getElementById('clearButton').addEventListener('click', () => {
            this.clearChat();
        });

        document.getElementById('userMessage').focus();
    }

    startStream(message) {
        if (this.isConnected) {
            this.stopStream();
        }

        this.streamId = 'stream_' + Date.now();
        this.displayMessage(message, 'user');
        document.getElementById('userMessage').value = '';
        this.showTypingIndicator();
        this.updateStopButton(true);

        const url = '/chat/ai/stream-simple?message=' + encodeURIComponent(message) +
                   '&streamId=' + this.streamId +
                   '&_=' + Date.now();

        console.log('Starting stream with URL:', url);
        this.eventSource = new EventSource(url);
        this.currentMessage = '';

        this.eventSource.onmessage = (event) => {
            console.log('Raw chunk received:', event.data);
            // Заменяем неразрывные пробелы обратно на обычные пробелы
            const processedChunk = event.data.replace(/\u00A0/g, ' ');
            this.handleStreamChunk(processedChunk);
        };

        this.eventSource.onopen = () => {
            this.isConnected = true;
            console.log('SSE connection opened');
        };

        this.eventSource.onerror = (event) => {
            console.error('SSE Error:', event);
            if (this.currentMessage === '') {
                this.displayMessage('Произошла ошибка при подключении к AI. Попробуйте еще раз.', 'ai');
            }
            this.stopStream();
        };

        this.eventSource.addEventListener('complete', (event) => {
            console.log('Stream completed:', event.data);
            this.handleStreamComplete();
        });
    }

    handleStreamChunk(chunk) {
        this.hideTypingIndicator();

        // Просто добавляем чанк без дополнительной обработки пробелов
        this.currentMessage += chunk;
        this.displayMessage(this.currentMessage, 'ai', true);
    }

    handleStreamComplete() {
        this.stopStream();
        console.log('Stream processing complete');
    }

    stopStream() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
        this.isConnected = false;
        this.hideTypingIndicator();
        this.updateStopButton(false);
    }

    stopGeneration() {
        if (this.streamId) {
            fetch('/chat/ai/stop-stream?streamId=' + this.streamId, {
                method: 'POST'
            }).then(response => {
                console.log('Stop request sent');
            }).catch(error => {
                console.error('Error sending stop request:', error);
            });
        }
        this.stopStream();
    }

    sendMessage(message) {
        this.startStream(message);
    }

    displayMessage(content, type, isStreaming = false) {
        const responseArea = document.getElementById('responseArea');

        if (responseArea.querySelector('.text-muted')) {
            responseArea.innerHTML = '';
        }

        let messageElement;

        if (type === 'user') {
            messageElement = document.createElement('div');
            messageElement.className = 'message user-message';
            messageElement.textContent = content;
            responseArea.appendChild(messageElement);
        } else if (type === 'ai') {
            let aiMessage = responseArea.querySelector('.ai-message:last-child');
            let aiContent = responseArea.querySelector('.ai-message-content:last-child');

            if (!aiMessage || !isStreaming) {
                aiMessage = document.createElement('div');
                aiMessage.className = 'message ai-message';

                const contentDiv = document.createElement('div');
                contentDiv.className = 'ai-message-content';
                aiMessage.appendChild(contentDiv);

                responseArea.appendChild(aiMessage);
                aiContent = contentDiv;
            }

            // Минимальная обработка - только Markdown и экранирование HTML
            aiContent.innerHTML = this.markdownToHtml(content);
        }

        responseArea.scrollTop = responseArea.scrollHeight;
    }

    markdownToHtml(text) {
        if (!text) return '';

        // Сначала экранируем HTML
        let html = this.escapeHtml(text);

        // Затем применяем Markdown преобразования
        html = this.simpleMarkdownReplacements(html);

        return html;
    }

    simpleMarkdownReplacements(html) {
        // Обрабатываем блоки кода сначала
        html = html.replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre><code class="$1">$2</code></pre>');
        html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

        // Заголовки
        html = html.replace(/### (.*?)(\n|$)/g, '<h3>$1</h3>');
        html = html.replace(/## (.*?)(\n|$)/g, '<h2>$1</h2>');
        html = html.replace(/# (.*?)(\n|$)/g, '<h1>$1</h1>');

        // Жирный текст
        html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

        // Курсив
        html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');

        // Списки
        let lines = html.split('\n');
        let inList = false;
        let newHtml = '';

        for (let line of lines) {
            const trimmedLine = line.trim();
            if (trimmedLine.startsWith('- ') || trimmedLine.startsWith('* ')) {
                if (!inList) {
                    newHtml += '<ul>';
                    inList = true;
                }
                newHtml += '<li>' + trimmedLine.substring(2) + '</li>';
            } else {
                if (inList) {
                    newHtml += '</ul>';
                    inList = false;
                }
                newHtml += line + '<br>';
            }
        }

        if (inList) {
            newHtml += '</ul>';
        }

        html = newHtml;

        // Убираем лишние <br> в конце блоков
        html = html.replace(/<br><\/h[1-6]>/g, '</h1>');
        html = html.replace(/<br><\/ul>/g, '</ul>');
        html = html.replace(/<br><\/pre>/g, '</pre>');

        return html;
    }

    showTypingIndicator() {
        document.getElementById('typingIndicator').style.display = 'block';
    }

    hideTypingIndicator() {
        document.getElementById('typingIndicator').style.display = 'none';
    }

    updateStopButton(isStreaming) {
        const stopButton = document.getElementById('stopButton');
        stopButton.disabled = !isStreaming;
    }

    clearChat() {
        document.getElementById('responseArea').innerHTML =
            '<div class="text-muted text-center py-4">' +
            '<div>Здесь появится ответ от AI...</div>' +
            '<small class="text-muted">AI поддерживает Markdown-разметку для красивого форматирования</small>' +
            '</div>';
        this.currentMessage = '';
        this.streamId = null;
    }

    escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
}

// Инициализация после загрузки DOM
document.addEventListener('DOMContentLoaded', function() {
    window.aiChat = new AIChatStream();

    document.getElementById('userMessage').addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            document.getElementById('chatForm').dispatchEvent(new Event('submit'));
        }
    });
});