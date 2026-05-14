import React, { useState, useRef, useEffect } from 'react';
import { aiAPI } from '../api';
import { RadarChart, Radar, PolarGrid, PolarAngleAxis, ResponsiveContainer, Tooltip } from 'recharts';

const QUICK_PROMPTS = [
  'How can I reduce my monthly expenses?',
  'What is a good savings rate?',
  'How should I allocate my budget?',
  'Give me tips to avoid overspending',
  'How to build an emergency fund?',
  'Explain the 50/30/20 rule',
];

function ChatMessage({ msg }) {
  const isUser = msg.role === 'user';
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} gap-3 animate-slide-up`}>
      {!isUser && (
        <div className="w-9 h-9 rounded-xl gradient-ai flex items-center justify-center text-sm flex-shrink-0">🤖</div>
      )}
      <div className={`max-w-[75%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${
        isUser
          ? 'gradient-primary text-white rounded-br-sm'
          : 'bg-gray-100 text-gray-800 rounded-bl-sm'
      }`}>
        {msg.content}
        <p className={`text-xs mt-1.5 ${isUser ? 'text-white/60' : 'text-gray-400'}`}>
          {new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </p>
      </div>
      {isUser && (
        <div className="w-9 h-9 rounded-xl gradient-primary flex items-center justify-center text-sm flex-shrink-0">👤</div>
      )}
    </div>
  );
}

export default function AiAssistantPage() {
  const [messages, setMessages] = useState([{
    role: 'assistant',
    content: '👋 Hi! I\'m your AI Finance Assistant. I can help you analyze spending, suggest savings strategies, and answer finance questions. What would you like to know?',
    timestamp: Date.now()
  }]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [analysis, setAnalysis] = useState(null);
  const [analysisLoading, setAnalysisLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('chat');
  const messagesEndRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async (text) => {
    const msg = text || input.trim();
    if (!msg) return;
    setInput('');
    const userMsg = { role: 'user', content: msg, timestamp: Date.now() };
    setMessages(prev => [...prev, userMsg]);
    setLoading(true);
    try {
      const res = await aiAPI.chat({ message: msg });
      const aiMsg = { role: 'assistant', content: res.data.data, timestamp: Date.now() };
      setMessages(prev => [...prev, aiMsg]);
    } catch {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Sorry, I\'m having trouble connecting. Please try again.',
        timestamp: Date.now()
      }]);
    }
    finally { setLoading(false); }
  };

  const loadAnalysis = async () => {
    setAnalysisLoading(true);
    try {
      const res = await aiAPI.analyze({});
      setAnalysis(res.data.data);
    } catch {}
    finally { setAnalysisLoading(false); }
  };

  useEffect(() => {
    if (activeTab === 'analysis' && !analysis) loadAnalysis();
  }, [activeTab]);

  const gradeColor = { A: '#10b981', B: '#3b82f6', C: '#f59e0b', D: '#ef4444' };
  const radarData = analysis ? [
    { subject: 'Savings Rate', value: Math.min(analysis.healthScore, 100) },
    { subject: 'Spending Control', value: Math.max(100 - analysis.healthScore * 0.3, 20) },
    { subject: 'Budget Adherence', value: Math.min(analysis.healthScore * 1.1, 100) },
    { subject: 'Income Stability', value: Math.min(analysis.healthScore * 0.9, 100) },
    { subject: 'Diversity', value: Math.min(analysis.healthScore * 0.8, 100) },
  ] : [];

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="page-title">AI Assistant 🤖</h1>
        <p className="text-gray-500 mt-1">Powered by GPT · Your personal finance advisor</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-gray-100 p-1 rounded-xl w-fit">
        {[{ id: 'chat', label: '💬 Chat', }, { id: 'analysis', label: '📊 Analysis' }].map(tab => (
          <button key={tab.id} onClick={() => setActiveTab(tab.id)}
            className={`px-5 py-2 rounded-lg text-sm font-medium transition-all ${
              activeTab === tab.id ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}>
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'chat' ? (
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Chat */}
          <div className="lg:col-span-3 card p-0 flex flex-col" style={{ height: '600px' }}>
            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-6 space-y-4 scrollbar-hide">
              {messages.map((msg, i) => <ChatMessage key={i} msg={msg} />)}
              {loading && (
                <div className="flex justify-start gap-3">
                  <div className="w-9 h-9 rounded-xl gradient-ai flex items-center justify-center text-sm flex-shrink-0">🤖</div>
                  <div className="bg-gray-100 rounded-2xl rounded-bl-sm px-4 py-3 flex items-center gap-1">
                    {[0,1,2].map(i => (
                      <div key={i} className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"
                        style={{ animationDelay: `${i * 0.15}s` }}></div>
                    ))}
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className="border-t border-gray-100 p-4">
              <div className="flex gap-3">
                <input type="text" className="input-field flex-1" placeholder="Ask about your finances..."
                  value={input} onChange={e => setInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && !e.shiftKey && sendMessage()} />
                <button onClick={() => sendMessage()} disabled={loading || !input.trim()} className="btn-primary px-4 py-3 disabled:opacity-50">
                  {loading ? '⏳' : '➤'}
                </button>
              </div>
            </div>
          </div>

          {/* Quick Prompts */}
          <div className="space-y-4">
            <div className="card">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">💡 Quick Questions</h3>
              <div className="space-y-2">
                {QUICK_PROMPTS.map((prompt, i) => (
                  <button key={i} onClick={() => sendMessage(prompt)} disabled={loading}
                    className="w-full text-left text-sm px-3 py-2.5 rounded-xl bg-gray-50 hover:bg-primary-50 hover:text-primary-700 transition-colors text-gray-600 disabled:opacity-50">
                    {prompt}
                  </button>
                ))}
              </div>
            </div>
            <div className="card bg-gradient-to-br from-purple-50 to-pink-50 border-purple-100">
              <p className="text-xs text-purple-600 font-semibold uppercase tracking-wide mb-2">AI Capabilities</p>
              <ul className="space-y-2 text-sm text-gray-700">
                {['Spending pattern analysis','Saving suggestions','Budget advice','Investment basics','Debt management tips'].map(item => (
                  <li key={item} className="flex items-center gap-2">
                    <span className="text-purple-500">✓</span>{item}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      ) : (
        /* Analysis Tab */
        <div>
          {analysisLoading ? (
            <div className="flex flex-col items-center justify-center h-64">
              <div className="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mb-4"></div>
              <p className="text-gray-500">AI is analyzing your finances...</p>
            </div>
          ) : analysis ? (
            <div className="space-y-6">
              {/* Health Score */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="card text-center md:col-span-1">
                  <p className="text-sm text-gray-500 mb-2">Financial Health Score</p>
                  <div className="relative w-32 h-32 mx-auto">
                    <svg viewBox="0 0 36 36" className="w-full h-full -rotate-90">
                      <circle cx="18" cy="18" r="15.9" fill="none" stroke="#e5e7eb" strokeWidth="3"/>
                      <circle cx="18" cy="18" r="15.9" fill="none"
                        stroke={gradeColor[analysis.healthGrade] || '#667eea'} strokeWidth="3"
                        strokeDasharray={`${analysis.healthScore} 100`} strokeLinecap="round"/>
                    </svg>
                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                      <span className="text-3xl font-black" style={{ color: gradeColor[analysis.healthGrade] }}>
                        {analysis.healthGrade}
                      </span>
                      <span className="text-xs text-gray-500">{analysis.healthScore?.toFixed(0)}/100</span>
                    </div>
                  </div>
                  <p className="text-sm font-medium text-gray-700 mt-3">{analysis.spendingPattern}</p>
                </div>

                <div className="card md:col-span-2">
                  <h3 className="section-title">Radar Analysis</h3>
                  <ResponsiveContainer width="100%" height={200}>
                    <RadarChart data={radarData}>
                      <PolarGrid stroke="#e5e7eb" />
                      <PolarAngleAxis dataKey="subject" tick={{ fontSize: 11, fill: '#6b7280' }} />
                      <Radar dataKey="value" stroke="#667eea" fill="#667eea" fillOpacity={0.25} strokeWidth={2} />
                      <Tooltip formatter={(v) => [`${v.toFixed(0)}%`]} />
                    </RadarChart>
                  </ResponsiveContainer>
                </div>
              </div>

              {/* Summary */}
              <div className="card border-l-4 border-primary-500">
                <p className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">📝 AI Summary</p>
                <p className="text-gray-800 leading-relaxed">{analysis.summary}</p>
              </div>

              {/* Insights, Suggestions, Warnings */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="card">
                  <h3 className="font-semibold text-gray-800 mb-3 flex items-center gap-2">💡 Insights</h3>
                  <ul className="space-y-2">
                    {analysis.insights?.map((insight, i) => (
                      <li key={i} className="flex items-start gap-2 text-sm text-gray-700">
                        <span className="text-blue-500 mt-0.5 flex-shrink-0">→</span>{insight}
                      </li>
                    ))}
                  </ul>
                </div>
                <div className="card">
                  <h3 className="font-semibold text-gray-800 mb-3 flex items-center gap-2">🎯 Suggestions</h3>
                  <ul className="space-y-2">
                    {analysis.suggestions?.map((s, i) => (
                      <li key={i} className="flex items-start gap-2 text-sm text-gray-700">
                        <span className="text-green-500 mt-0.5 flex-shrink-0">✓</span>{s}
                      </li>
                    ))}
                  </ul>
                </div>
                <div className="card">
                  <h3 className="font-semibold text-gray-800 mb-3 flex items-center gap-2">⚠️ Warnings</h3>
                  {analysis.warnings?.length > 0 ? (
                    <ul className="space-y-2">
                      {analysis.warnings.map((w, i) => (
                        <li key={i} className="flex items-start gap-2 text-sm text-gray-700">
                          <span className="text-yellow-500 mt-0.5 flex-shrink-0">!</span>{w}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-sm text-gray-500">No warnings. Keep up the good work! 🌟</p>
                  )}
                </div>
              </div>

              <button onClick={loadAnalysis} className="btn-secondary flex items-center gap-2">
                🔄 Refresh Analysis
              </button>
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-48 text-gray-400">
              <span className="text-4xl mb-3">📊</span>
              <p className="font-medium">Unable to load analysis</p>
              <button onClick={loadAnalysis} className="btn-primary mt-4">Try Again</button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
