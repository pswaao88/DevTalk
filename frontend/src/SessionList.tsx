// src/SessionList.tsx
import { useState, useEffect } from 'react';

interface SessionSummary {
  sessionId: string;
  title: string;
  status: 'ACTIVE' | 'RESOLVED';
  description?: string;
  aiSummary?: string;
  createdAt: string;
  lastUpdatedAt: string;
}

interface SessionListProps {
  onSelectSession: (sessionId: string) => void;
}

const API_BASE = 'http://localhost:8080/api/devtalk';

function SessionList({ onSelectSession }: SessionListProps) {
  const [sessions, setSessions] = useState<SessionSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [showTitleInput, setShowTitleInput] = useState(false);
  const [newSessionTitle, setNewSessionTitle] = useState('');

  // 편집 모달 상태
  const [showEditModal, setShowEditModal] = useState(false);
  const [editingSession, setEditingSession] = useState<SessionSummary | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [updating, setUpdating] = useState(false);

  // AI 요약 상태
  const [aiSummaryText, setAiSummaryText] = useState('');
  const [showAiSummary, setShowAiSummary] = useState(false);

  useEffect(() => {
    loadSessions();
  }, []);

  const loadSessions = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/sessions`);
      const data = await response.json();
      const sortedData = data.sort((a: SessionSummary, b: SessionSummary) => {
        return new Date(b.lastUpdatedAt).getTime() - new Date(a.lastUpdatedAt).getTime();
      });
      setSessions(sortedData);
    } catch (error) {
      console.error('세션 목록 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleNewSessionClick = () => {
    setShowTitleInput(true);
    setNewSessionTitle('');
  };

  const createNewSession = async () => {
    if (!newSessionTitle.trim()) {
      alert('제목을 입력해주세요.');
      return;
    }

    setCreating(true);
    try {
      const response = await fetch(`${API_BASE}/sessions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: newSessionTitle.trim() })
      });
      const newSession = await response.json();
      setSessions([newSession, ...sessions]);
      setShowTitleInput(false);
      setNewSessionTitle('');
      onSelectSession(newSession.sessionId);
    } catch (error) {
      console.error('세션 생성 실패:', error);
      alert('세션 생성에 실패했습니다.');
    } finally {
      setCreating(false);
    }
  };

  const cancelNewSession = () => {
    setShowTitleInput(false);
    setNewSessionTitle('');
  };

  // 편집 모달 열기
  const handleEditClick = (e: React.MouseEvent, session: SessionSummary) => {
    e.stopPropagation();
    setEditingSession(session);
    setEditTitle(session.title);
    setEditDescription(session.description || '');
    setAiSummaryText(session.aiSummary || '');
    setShowAiSummary(false);
    setShowEditModal(true);
  };

  // 세션 업데이트
  const updateSession = async () => {
    if (!editingSession) return;
    if (!editTitle.trim()) {
      alert('제목을 입력해주세요.');
      return;
    }

    setUpdating(true);
    try {
      const response = await fetch(`${API_BASE}/sessions/${editingSession.sessionId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: editTitle.trim(),
          description: editDescription.trim() || null
        })
      });

      if (response.ok) {
        setSessions(sessions.map(s =>
            s.sessionId === editingSession.sessionId
                ? { ...s, title: editTitle.trim(), description: editDescription.trim() || undefined }
                : s
        ));
        setShowEditModal(false);
        setEditingSession(null);
      } else {
        alert('세션 수정에 실패했습니다.');
      }
    } catch (error) {
      console.error('세션 수정 실패:', error);
      alert('세션 수정에 실패했습니다.');
    } finally {
      setUpdating(false);
    }
  };

  const cancelEdit = () => {
    setShowEditModal(false);
    setEditingSession(null);
    setEditTitle('');
    setEditDescription('');
    setAiSummaryText('');
    setShowAiSummary(false);
  };

  // AI 요약 보기
  const handleViewAiSummary = () => {
    setShowAiSummary(true);
  };

  // AI 요약 생성
  const handleGenerateAiSummary = () => {
    alert('🚧 추후 기능 추가 예정\n\nAI가 세션 내용을 분석하여 요약을 생성합니다.');
  };

  // AI 요약 다시 생성
  const handleRegenerateAiSummary = () => {
    alert('🚧 추후 기능 추가 예정\n\n새로운 요약을 생성합니다.');
  };

  // AI 요약 확정
  const handleConfirmAiSummary = () => {
    alert('🚧 추후 기능 추가 예정\n\n요약 내용이 저장됩니다.');
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
  };

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}.${month}.${day} ${hours}:${minutes}`;
  };

  const groupByDate = (sessions: SessionSummary[]) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    const grouped: { [key: string]: SessionSummary[] } = {
      '오늘': [],
      '어제': [],
      '이전': []
    };

    sessions.forEach(session => {
      const sessionDate = new Date(session.lastUpdatedAt);
      sessionDate.setHours(0, 0, 0, 0);

      if (sessionDate.getTime() === today.getTime()) {
        grouped['오늘'].push(session);
      } else if (sessionDate.getTime() === yesterday.getTime()) {
        grouped['어제'].push(session);
      } else {
        grouped['이전'].push(session);
      }
    });

    return grouped;
  };

  const getRecentWeekSessions = (sessions: SessionSummary[]) => {
    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);
    weekAgo.setHours(0, 0, 0, 0);

    return sessions.filter(session => {
      const sessionDate = new Date(session.lastUpdatedAt);
      return sessionDate >= weekAgo;
    });
  };

  const filteredSessions = sessions.filter(session =>
      session.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      session.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      session.aiSummary?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const groupedSessions = groupByDate(filteredSessions);
  const recentWeekSessions = getRecentWeekSessions(filteredSessions);

  if (loading) {
    return (
        <div style={{ display: 'flex', height: '100vh' }}>
          <div style={{ padding: '40px', textAlign: 'center', flex: 1 }}>로딩 중...</div>
        </div>
    );
  }

  return (
      <div className="home-layout">
        {/* 새 세션 제목 입력 모달 */}
        {showTitleInput && (
            <div className="modal-overlay">
              <div className="modal-content">
                <h3>새 세션 제목</h3>
                <input
                    type="text"
                    className="title-input"
                    placeholder="세션 제목을 입력하세요"
                    value={newSessionTitle}
                    onChange={(e) => setNewSessionTitle(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && createNewSession()}
                    autoFocus
                />
                <div className="modal-buttons">
                  <button onClick={cancelNewSession} className="btn-cancel">
                    취소
                  </button>
                  <button
                      onClick={createNewSession}
                      className="btn-create"
                      disabled={creating || !newSessionTitle.trim()}
                  >
                    {creating ? '생성 중...' : '생성'}
                  </button>
                </div>
              </div>
            </div>
        )}

        {/* 세션 편집 모달 */}
        {showEditModal && editingSession && (
            <div className="modal-overlay">
              <div className="modal-content modal-large">
                <h3>세션 편집</h3>

                {/* 기본 정보 섹션 */}
                <div className="modal-section">
                  <label className="modal-label">제목</label>
                  <input
                      type="text"
                      className="title-input"
                      placeholder="세션 제목"
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                  />
                </div>

                <div className="modal-section">
                  <label className="modal-label">설명 (선택)</label>
                  <textarea
                      className="description-input"
                      placeholder="세션 설명 (선택사항)"
                      value={editDescription}
                      onChange={(e) => setEditDescription(e.target.value)}
                      rows={3}
                  />
                </div>

                {/* AI 요약 섹션 */}
                <div className="modal-section ai-summary-section">
                  <div className="ai-summary-header">
                    <label className="modal-label">🤖 AI 요약</label>
                    <div className="ai-summary-buttons">
                      {aiSummaryText ? (
                          <button
                              onClick={handleViewAiSummary}
                              className="btn-view-summary"
                          >
                            {showAiSummary ? '요약 숨기기' : '요약 보기'}
                          </button>
                      ) : (
                          <button
                              onClick={handleGenerateAiSummary}
                              className="btn-generate-summary"
                          >
                            AI 요약 생성하기
                          </button>
                      )}
                    </div>
                  </div>

                  {showAiSummary && aiSummaryText && (
                      <div className="ai-summary-content">
                        <div className="ai-summary-box">
                          <p>{aiSummaryText}</p>
                        </div>
                        <div className="ai-summary-actions">
                          <button
                              onClick={handleRegenerateAiSummary}
                              className="btn-regenerate"
                          >
                            🔄 다시 요약하기
                          </button>
                          <button
                              onClick={handleConfirmAiSummary}
                              className="btn-confirm"
                          >
                            ✓ 확정
                          </button>
                        </div>
                      </div>
                  )}
                </div>

                {/* 하단 버튼 */}
                <div className="modal-buttons">
                  <button onClick={cancelEdit} className="btn-cancel">
                    취소
                  </button>
                  <button
                      onClick={updateSession}
                      className="btn-create"
                      disabled={updating || !editTitle.trim()}
                  >
                    {updating ? '저장 중...' : '저장'}
                  </button>
                </div>
              </div>
            </div>
        )}

        {/* 사이드바 */}
        <nav className="sidebar">
          <div className="logo">DevTalk</div>

          <div className="nav-group">
            <span className="nav-label">모든 세션 ({sessions.length})</span>

            {Object.entries(groupedSessions).map(([dateLabel, sessionList]) => {
              if (sessionList.length === 0) return null;

              return (
                  <div key={dateLabel} style={{ marginBottom: '20px' }}>
                    <div className="date-divider">{dateLabel}</div>
                    {sessionList.map(session => (
                        <div
                            key={session.sessionId}
                            className="sidebar-session-item"
                            onClick={() => onSelectSession(session.sessionId)}
                        >
                          <span className="sidebar-session-title">{session.title}</span>
                          <span className={`sidebar-session-status ${session.status.toLowerCase()}`}>
                      {session.status === 'RESOLVED' ? '✓' : '○'}
                    </span>
                        </div>
                    ))}
                  </div>
              );
            })}
          </div>
        </nav>

        {/* 메인 콘텐츠 */}
        <main className="main-content">
          <div className="top-bar">
            <input
                type="text"
                className="search-input"
                placeholder="세션 검색..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
            />
            <button
                className="btn-new"
                onClick={handleNewSessionClick}
            >
              + 새 세션 시작
            </button>
          </div>

          <div className="content-scroll">
            <div className="section-header">
              <h2 className="section-title">최근 세션 (일주일)</h2>
              <span style={{ fontSize: '14px', color: '#636e72' }}>
              {recentWeekSessions.length}개
            </span>
            </div>

            <div className="session-grid">
              {recentWeekSessions.length === 0 ? (
                  <div className="empty-state">
                    <p>최근 일주일 동안 세션이 없습니다</p>
                    <p>새 세션을 시작해보세요!</p>
                  </div>
              ) : (
                  recentWeekSessions.map(session => (
                      <div
                          key={session.sessionId}
                          className="session-card"
                          onClick={() => onSelectSession(session.sessionId)}
                      >
                        <div className="card-header-row">
                          <div className="card-meta">
                            <span>{formatDateTime(session.lastUpdatedAt)}</span>
                            <span className={session.status === 'RESOLVED' ? 'status-resolved' : 'status-active'}>
                        {session.status === 'RESOLVED' ? '✓' : '○'}
                      </span>
                          </div>
                          <button
                              className="edit-button"
                              onClick={(e) => handleEditClick(e, session)}
                              title="편집"
                          >
                            ✏️
                          </button>
                        </div>
                        <div className="card-title">{session.title}</div>

                        {/* ★ Key Finding(AI 요약) 부분 제거 및 설명(Description)만 표시 ★ */}
                        {session.description && (
                            <div className="card-description">
                              {session.description}
                            </div>
                        )}

                        <div className="card-tags">
                    <span className={`tag ${session.status === 'RESOLVED' ? 'tag-resolved' : 'tag-active'}`}>
                      {session.status === 'RESOLVED' ? '해결됨' : '진행중'}
                    </span>
                        </div>
                      </div>
                  ))
              )}
            </div>
          </div>
        </main>
      </div>
  );
}

export default SessionList;
