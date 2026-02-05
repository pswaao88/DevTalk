import { useState, useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

/* ================= Type Definitions ================= */
type MessageRole = 'USER' | 'AI' | 'SYSTEM';
type MessageStatus = 'SUCCESS' | 'FAILED';

interface MessageResponse {
  messageId: string;
  role: MessageRole;
  content: string;
  markers: string | null;
  status: MessageStatus;
  createdAt: string;
}

interface SessionResponse {
  sessionId: string;
  title: string;
  status: 'ACTIVE' | 'RESOLVED';
  createdAt: string;
  lastUpdatedAt: string;
}

interface ChatViewProps {
  sessionId: string;
  onBack: () => void;
}

/* ================= Constants & Utils ================= */
const API_BASE = 'http://localhost:8080/api/devtalk';

const markdownComponents = {
  code({ node, inline, className, children, ...props }: any) {
    const match = /language-(\w+)/.exec(className || '');
    return !inline && match ? (
        <SyntaxHighlighter
            style={vscDarkPlus}
            language={match[1]}
            PreTag="div"
            {...props}
        >
          {String(children).replace(/\n$/, '')}
        </SyntaxHighlighter>
    ) : (
        <code className={className} {...props}>
          {children}
        </code>
    );
  },
  img: ({node, ...props}: any) => (
      <img style={{maxWidth: '100%', borderRadius: '8px'}} {...props} alt="content" />
  )
};

function ChatView({ sessionId, onBack }: ChatViewProps) {
  /* ================= State ================= */
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);

  // 사이드바 토글 상태 (기본값: 닫힘)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const [streamingAiContent, setStreamingAiContent] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [isDone, setIsDone] = useState(false);
  const [shouldAutoScroll, setShouldAutoScroll] = useState(true);
  const [typingQueue, setTypingQueue] = useState<string[]>([]);

  /* ================= Refs ================= */
  const typingIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const lastScrollTop = useRef<number>(0);

  const CHUNK_SIZE = 5;
  const TYPING_SPEED = 15;

  /* ================= Effects ================= */
  useEffect(() => {
    loadSession();
    loadMessages();

    return () => {
      if (eventSourceRef.current) eventSourceRef.current.close();
      if (typingIntervalRef.current) clearInterval(typingIntervalRef.current);
    };
  }, [sessionId]);

  useEffect(() => {
    if (typingQueue.length === 0) {
      if (isDone) {
        setIsStreaming(false);
        setStreamingAiContent('');
        setIsDone(false);
        loadMessages();
      }
      return;
    }

    if (typingIntervalRef.current) clearInterval(typingIntervalRef.current);

    typingIntervalRef.current = setInterval(() => {
      setTypingQueue(queue => {
        if (queue.length === 0) {
          if (typingIntervalRef.current) {
            clearInterval(typingIntervalRef.current);
            typingIntervalRef.current = null;
          }
          return queue;
        }
        const [firstChar, ...rest] = queue;
        setStreamingAiContent(prev => prev + firstChar);
        return rest;
      });
    }, TYPING_SPEED);

    return () => {
      if (typingIntervalRef.current) clearInterval(typingIntervalRef.current);
    };
  }, [typingQueue, isDone]);

  useEffect(() => {
    if (shouldAutoScroll) {
      scrollToBottom();
    }
  }, [messages, streamingAiContent, shouldAutoScroll]);

  /* ================= Handlers ================= */
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadSession = async () => {
    try {
      const response = await fetch(`${API_BASE}/sessions/${sessionId}`);
      const data = await response.json();
      setSession(data);
    } catch (error) {
      console.error('세션 로드 실패:', error);
    }
  };

  const loadMessages = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/sessions/${sessionId}/messages`);
      const data = await response.json();
      setMessages(data);
    } catch (error) {
      console.error('메시지 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const sendMessage = async () => {
    if (!input.trim() || sending) return;

    const userMessage = input;
    setInput('');
    setSending(true);

    try {
      const userResponse = await fetch(`${API_BASE}/sessions/${sessionId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: userMessage, marker: null })
      });

      if (!userResponse.ok) throw new Error('유저 메시지 저장 실패');

      const userMsg = await userResponse.json();
      setMessages(prev => [...prev, userMsg]);

      setIsStreaming(true);
      setIsDone(false);
      setStreamingAiContent('');
      setTypingQueue([]);

      const streamUrl = `${API_BASE}/sessions/${sessionId}/ai/stream?replyToUserMessageId=${userMsg.messageId}`;
      const eventSource = new EventSource(streamUrl);
      eventSourceRef.current = eventSource;

      eventSource.onopen = () => console.log('✅ SSE 연결 성공');
      eventSource.addEventListener('start', () => setStreamingAiContent(''));

      eventSource.addEventListener('delta', (e) => {
        const deltaText = e.data;
        const chunks: string[] = [];
        for (let i = 0; i < deltaText.length; i += CHUNK_SIZE) {
          chunks.push(deltaText.slice(i, i + CHUNK_SIZE));
        }
        setTypingQueue(prev => [...prev, ...chunks]);
      });

      eventSource.addEventListener('done', () => {
        eventSource.close();
        eventSourceRef.current = null;
        setIsDone(true);
      });

      eventSource.onerror = (error) => {
        console.error('❌ SSE 에러:', error);
        eventSource.close();
        eventSourceRef.current = null;
        setIsStreaming(false);
        setIsDone(false);
        setSending(false);
        alert('AI 응답 중 오류 발생');
      };

    } catch (error) {
      console.error('메시지 전송 실패:', error);
      alert('메시지 전송 실패');
      setSending(false);
    } finally {
      setSending(false);
    }
  };

  const toggleResolve = async () => {
    if (!session || isStreaming) return;

    const isResolved = session.status === 'RESOLVED';
    const endpoint = isResolved ? 'unresolved' : 'resolve';

    try {
      const response = await fetch(`${API_BASE}/sessions/${sessionId}/${endpoint}`, {
        method: 'POST'
      });
      const data = await response.json();

      setSession(prev => prev ? { ...prev, status: data.resolve.status } : null);
      setMessages(prev => [...prev, data.systemMessage]);

    } catch (error) {
      console.error('상태 변경 실패:', error);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  if (!session) {
    return <div style={{ padding: '20px', textAlign: 'center' }}>로딩 중...</div>;
  }

  return (
      <div className="chat-layout">
        {/* ========== Main Chat Area ========== */}
        <div className="chat-main">
          {/* Header */}
          <div className="chat-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <button onClick={onBack} className="back-button">←</button>
              <div>
                <h2 className="chat-title">{session.title}</h2>
                <span className="chat-session-id">Session ID: {session.sessionId}</span>
              </div>
            </div>

            <div className="status-toggle-container">
              <span className="status-label">상태</span>
              <div className={`toggle-switch ${session.status === 'RESOLVED' ? 'resolved' : 'active'} ${isStreaming ? 'disabled' : ''}`}>
                <button className="toggle-option" onClick={toggleResolve} disabled={isStreaming}>
                  ○ 진행중
                </button>
                <button className="toggle-option" onClick={toggleResolve} disabled={isStreaming}>
                  ✓ 해결됨
                </button>
              </div>

              {/* ★ 사이드바 토글 버튼 추가 ★ */}
              <button
                  className={`sidebar-toggle-btn ${isSidebarOpen ? 'active' : ''}`}
                  onClick={() => setIsSidebarOpen(!isSidebarOpen)}
                  title={isSidebarOpen ? "사이드바 접기" : "사이드바 펼치기"}
              >
                {/* 오른쪽 사이드바 아이콘 SVG */}
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                  <line x1="15" y1="3" x2="15" y2="21" />
                </svg>
              </button>
            </div>
          </div>

          {/* Messages Area */}
          <div
              className="messages-area"
              onScroll={(e) => {
                const target = e.currentTarget;
                const isNearBottom = target.scrollHeight - target.scrollTop - target.clientHeight < 150;

                if (target.scrollTop < lastScrollTop.current) {
                  setShouldAutoScroll(false);
                } else if (isNearBottom) {
                  setShouldAutoScroll(true);
                }
                lastScrollTop.current = target.scrollTop;
              }}
          >
            {loading ? (
                <div style={{ textAlign: 'center', color: '#999', marginTop: '20px' }}>메시지 로딩 중...</div>
            ) : (
                <>
                  {messages.map(msg => {
                    if (msg.role === 'SYSTEM') {
                      return <div key={msg.messageId} className="system-message">{msg.content}</div>;
                    }
                    if (msg.role === 'USER') {
                      return (
                          <div key={msg.messageId} className="message-row user-row">
                            <div className="message-bubble user-bubble">{msg.content}</div>
                          </div>
                      );
                    }
                    if (msg.role === 'AI') {
                      return (
                          <div key={msg.messageId} className="message-row ai-row">
                            <div className="ai-avatar">🤖</div>
                            <div className={`message-bubble ai-bubble markdown-content ${msg.status === 'FAILED' ? 'failed' : ''}`}>
                              {msg.status === 'FAILED' && (
                                  <div style={{ color: '#c00', marginBottom: '8px', fontWeight: '600' }}>⚠️ AI 응답 실패</div>
                              )}
                              <ReactMarkdown
                                  remarkPlugins={[remarkGfm]}
                                  components={markdownComponents}
                              >
                                {msg.content}
                              </ReactMarkdown>
                            </div>
                          </div>
                      );
                    }
                    return null;
                  })}

                  {/* Streaming Content */}
                  {isStreaming && (
                      <div className="message-row ai-row">
                        <div className="ai-avatar">🤖</div>
                        <div className="message-bubble ai-bubble markdown-content streaming" style={{
                          border: '2px dashed #4a90e2',
                          opacity: 0.9
                        }}>
                          <ReactMarkdown
                              remarkPlugins={[remarkGfm]}
                              components={markdownComponents}
                          >
                            {streamingAiContent || '생각 중...'}
                          </ReactMarkdown>
                          <span className="typing-cursor" />
                        </div>
                      </div>
                  )}
                  <div ref={messagesEndRef} />
                </>
            )}
          </div>

          <button
              className={`scroll-to-bottom ${!shouldAutoScroll ? 'visible' : ''}`}
              onClick={() => {
                setShouldAutoScroll(true);
                scrollToBottom();
              }}
              title="맨 아래로"
          >
            ↓
          </button>

          <div className="input-area">
          <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="메시지를 입력하거나 코드를 붙여넣으세요 (Markdown 지원)"
              disabled={sending || isStreaming}
              className="message-input"
          />
            <button
                onClick={sendMessage}
                disabled={!input.trim() || sending || isStreaming}
                className="send-button"
            >
              {isStreaming ? '생성 중' : '전송'}
            </button>
          </div>
        </div>

        {/* ========== Right Sidebar (Collapsible) ========== */}
        <div className={`chat-sidebar ${!isSidebarOpen ? 'closed' : ''}`}>
          <div className="sidebar-content-wrapper">
            <div className="sidebar-section">
              <h3 className="sidebar-title">CURRENT CONTEXT</h3>
              <div style={{ padding: '20px', textAlign: 'center', color: '#999', fontSize: '13px' }}>
                <p>추후 구현 예정</p>
                <p style={{ fontSize: '12px', marginTop: '8px' }}>
                  자동 태그 추출<br/>
                  핵심 발견 사항<br/>
                  액션 아이템
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
  );
}

export default ChatView;
