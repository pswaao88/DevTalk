// src/ChatView.tsx
import { useState, useEffect, useRef } from 'react';

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
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    loadSession();
    loadMessages();
  }, [sessionId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

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
      // 1. USER 메시지 전송
      const userResponse = await fetch(`${API_BASE}/sessions/${sessionId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: userMessage, marker: null })
      });
      const userMsg = await userResponse.json();
      setMessages(prev => [...prev, userMsg]);

      // 2. AI 응답 자동 생성
      const aiResponse = await fetch(`${API_BASE}/sessions/${sessionId}/ai/messages`, {
        method: 'POST'
      });
      const aiMsg = await aiResponse.json();
      setMessages(prev => [...prev, aiMsg]);

    } catch (error) {
      console.error('메시지 전송 실패:', error);
      alert('메시지 전송에 실패했습니다.');
    } finally {
      setSending(false);
    }
  };

  const toggleResolve = async () => {
    if (!session) return;

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
              <div className={`toggle-switch ${session.status === 'RESOLVED' ? 'resolved' : 'active'}`}>
                <button className="toggle-option" onClick={toggleResolve}>
                  ○ 진행중
                </button>
                <button className="toggle-option" onClick={toggleResolve}>
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
                            <div className={`message-bubble ai-bubble ${msg.status === 'FAILED' ? 'failed' : ''}`}>
                              {msg.status === 'FAILED' && (
                                  <div style={{ color: '#c00', marginBottom: '8px', fontWeight: '600' }}>
                                    ⚠️ AI 응답 실패
                                  </div>
                              )}
                              {msg.content}
                            </div>
                          </div>
                      );
                    }

                    return null;
                  })}
                  {sending && (
                      <div className="ai-row">
                        <div style={{ color: '#999', fontSize: '14px' }}>
                          AI가 응답을 생성하고 있습니다...
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
              disabled={sending}
              className="message-input"
          />
            <button
                onClick={sendMessage}
                disabled={!input.trim() || sending}
                className="send-button"
            >
              전송 →
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
