// src/ChatView.tsx
import { useState, useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

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

const API_BASE = 'http://localhost:8080/api/devtalk';

function ChatView({ sessionId, onBack }: ChatViewProps) {
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);

  // 스트리밍 중인 AI 답변 (임시)
  const [streamingAiContent, setStreamingAiContent] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [isDone, setIsDone] = useState(false); // 스트리밍은 끝났지만 타이핑 중

  // 타이핑 애니메이션용
  const [typingQueue, setTypingQueue] = useState<string[]>([]);
  const typingIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    loadSession();
    loadMessages();

    // 컴포넌트 언마운트 시 SSE 연결 정리
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
      if (typingIntervalRef.current) {
        clearInterval(typingIntervalRef.current);
      }
    };
  }, [sessionId]);

  // 타이핑 애니메이션 처리
  useEffect(() => {
    if (typingQueue.length === 0) {
      // 타이핑 큐가 비었고, 스트리밍이 완료되었으면 메시지 재조회
      if (isDone) {
        setIsStreaming(false);
        setStreamingAiContent('');
        setIsDone(false);
        loadMessages();
      }
      return;
    }

    if (typingIntervalRef.current) {
      clearInterval(typingIntervalRef.current);
    }

    typingIntervalRef.current = setInterval(() => {
      setTypingQueue(queue => {
        if (queue.length === 0) {
          if (typingIntervalRef.current) {
            clearInterval(typingIntervalRef.current);
            typingIntervalRef.current = null;
          }
          return queue;
        }

        // 큐에서 첫 글자 꺼내서 화면에 추가
        const [firstChar, ...rest] = queue;
        setStreamingAiContent(prev => prev + firstChar);
        return rest;
      });
    }, 30); // 30ms마다 한 글자씩 (속도 조절 가능: 20~50ms 권장)

    return () => {
      if (typingIntervalRef.current) {
        clearInterval(typingIntervalRef.current);
      }
    };
  }, [typingQueue, isDone]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, streamingAiContent]);

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
      // ============================================
      // 1️⃣ USER 메시지 저장
      // ============================================
      const userResponse = await fetch(`${API_BASE}/sessions/${sessionId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: userMessage, marker: null })
      });

      if (!userResponse.ok) {
        throw new Error('유저 메시지 저장 실패');
      }

      const userMsg = await userResponse.json();
      setMessages(prev => [...prev, userMsg]);

      // ============================================
      // 2️⃣ AI 스트리밍 시작
      // ============================================
      setIsStreaming(true);
      setIsDone(false);
      setStreamingAiContent('');
      setTypingQueue([]);

      // SSE 연결 생성
      const streamUrl = `${API_BASE}/sessions/${sessionId}/ai/stream?replyToUserMessageId=${userMsg.messageId}`;
      console.log('🔗 SSE 연결 시도:', streamUrl);

      const eventSource = new EventSource(streamUrl);
      eventSourceRef.current = eventSource;

      // 📡 연결 성공
      eventSource.onopen = () => {
        console.log('✅ SSE 연결 성공');
      };

      // 📡 start 이벤트
      eventSource.addEventListener('start', (e) => {
        console.log('🚀 스트리밍 시작:', e.data);
        setStreamingAiContent(''); // 초기화
      });

      // 📡 delta 이벤트 (실시간 텍스트 조각)
      eventSource.addEventListener('delta', (e) => {
        const deltaText = e.data;
        console.log('📝 Delta 수신:', deltaText);

        // 받은 텍스트를 글자 단위로 큐에 추가
        const chars = deltaText.split('');
        setTypingQueue(prev => [...prev, ...chars]);
      });

      // 📡 done 이벤트 (완료)
      eventSource.addEventListener('done', () => {
        console.log('✅ 스트리밍 완료 - 타이핑 대기 중');
        eventSource.close();
        eventSourceRef.current = null;

        setIsDone(true); // 타이핑이 끝나면 자동으로 메시지 재조회됨
      });

      // 📡 일반 message 이벤트 (SSE 기본 이벤트)
      eventSource.onmessage = (e) => {
        console.log('💬 일반 message 이벤트:', e.data);
      };

      // 📡 error 이벤트
      eventSource.onerror = (error) => {
        console.error('❌ SSE 에러:', error);
        console.log('SSE readyState:', eventSource.readyState);
        eventSource.close();
        eventSourceRef.current = null;

        if (typingIntervalRef.current) {
          clearInterval(typingIntervalRef.current);
          typingIntervalRef.current = null;
        }

        setIsStreaming(false);
        setIsDone(false);
        setStreamingAiContent('');
        setTypingQueue([]);

        alert('AI 응답 생성 중 오류가 발생했습니다.');
      };

    } catch (error) {
      console.error('메시지 전송 실패:', error);
      alert('메시지 전송에 실패했습니다.');
    } finally {
      setSending(false);
    }
  };

  const toggleResolve = async () => {
    if (!session || isStreaming) return; // 스트리밍 중에는 상태 변경 막기

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
      alert('상태 변경에 실패했습니다.');
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
        {/* 메인 채팅 영역 */}
        <div className="chat-main">
          {/* 헤더 */}
          <div className="chat-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <button onClick={onBack} className="back-button">←</button>
              <div>
                <h2 className="chat-title">{session.title}</h2>
                <span className="chat-session-id">Session ID: {session.sessionId}</span>
              </div>
            </div>

            {/* 상태 토글 버튼 그룹 */}
            <div className="status-toggle-container">
              <span className="status-label">상태</span>
              <div className={`toggle-switch ${session.status === 'RESOLVED' ? 'resolved' : 'active'} ${isStreaming ? 'disabled' : ''}`}>
                <button
                    className="toggle-option"
                    onClick={toggleResolve}
                    disabled={isStreaming}
                >
                  ○ 진행중
                </button>
                <button
                    className="toggle-option"
                    onClick={toggleResolve}
                    disabled={isStreaming}
                >
                  ✓ 해결됨
                </button>
              </div>
            </div>
          </div>

          {/* 메시지 영역 */}
          <div className="messages-area">
            {loading ? (
                <div style={{ textAlign: 'center', color: '#999' }}>메시지 로딩 중...</div>
            ) : (
                <>
                  {/* 확정된 메시지들 */}
                  {messages.map(msg => {
                    if (msg.role === 'SYSTEM') {
                      return (
                          <div key={msg.messageId} className="system-message">
                            {msg.content}
                          </div>
                      );
                    }

                    if (msg.role === 'USER') {
                      return (
                          <div key={msg.messageId} className="message-row user-row">
                            <div className="message-bubble user-bubble">
                              {msg.content}
                            </div>
                          </div>
                      );
                    }

                    if (msg.role === 'AI') {
                      return (
                          <div key={msg.messageId} className="message-row ai-row">
                            <div className="ai-avatar">🤖</div>
                            <div className={`message-bubble ai-bubble markdown-content ${msg.status === 'FAILED' ? 'failed' : ''}`}>
                              {msg.status === 'FAILED' && (
                                  <div style={{ color: '#c00', marginBottom: '8px', fontWeight: '600' }}>
                                    ⚠️ AI 응답 실패
                                  </div>
                              )}
                              <ReactMarkdown
                                  remarkPlugins={[remarkGfm]}
                                  components={{
                                    code({ node, inline, className, children, ...props }) {
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
                                  }}
                              >
                                {msg.content}
                              </ReactMarkdown>
                            </div>
                          </div>
                      );
                    }

                    return null;
                  })}

                  {/* 스트리밍 중인 AI 답변 (임시) */}
                  {isStreaming && (
                      <div className="message-row ai-row">
                        <div className="ai-avatar">🤖</div>
                        <div className="message-bubble ai-bubble markdown-content streaming" style={{
                          border: '2px dashed #4a90e2',
                          opacity: 0.9
                        }}>
                          <ReactMarkdown
                              remarkPlugins={[remarkGfm]}
                              components={{
                                code({ node, inline, className, children, ...props }) {
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
                              }}
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

          {/* 입력 영역 */}
          <div className="input-area">
          <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="생각을 입력하거나 코드를 붙여넣으세요 (Markdown 지원)"
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

        {/* 오른쪽 사이드바 */}
        <div className="chat-sidebar">
          <div className="sidebar-section">
            <h3 className="sidebar-title">CURRENT CONTEXT</h3>
            <div style={{
              padding: '20px',
              textAlign: 'center',
              color: '#999',
              fontSize: '14px'
            }}>
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
  );
}

export default ChatView;
