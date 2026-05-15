import { useState, useRef, useEffect } from 'react';
import { queryApiCalls } from '../api/client';
import type { ChatMessage } from '../types';
import { CitationCard } from './CitationCard';

interface Props {
  selectedDocumentIds: string[];
}

export const ChatInterface = ({ selectedDocumentIds }: Props) => {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: '0',
      role: 'assistant',
      content: selectedDocumentIds.length > 0
        ? `I have ${selectedDocumentIds.length} document(s) selected. Ask me anything about them!`
        : 'Upload and select documents from the sidebar, then ask me anything about them.',
      timestamp: new Date(),
    }
  ]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    const question = input.trim();
    setInput('');

    // Add user message immediately
    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: question,
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setIsLoading(true);

    // Add a loading placeholder for the assistant
    const loadingId = (Date.now() + 1).toString();
    setMessages(prev => [...prev, {
      id: loadingId,
      role: 'assistant',
      content: '...',
      timestamp: new Date(),
    }]);

    try {
      const documentIds = selectedDocumentIds.length > 0
        ? selectedDocumentIds
        : undefined;

      const response = await queryApiCalls.query(question, documentIds);
      const data = response.data;

      // Replace loading placeholder with real response
      setMessages(prev => prev.map(msg =>
        msg.id === loadingId
          ? {
              ...msg,
              content: data.answer,
              citations: data.citations,
              processingTimeMs: data.processingTimeMs,
            }
          : msg
      ));

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (error) {
      setMessages(prev => prev.map(msg =>
        msg.id === loadingId
          ? {
              ...msg,
              content: 'Sorry, I encountered an error processing your question. Please try again.',
            }
          : msg
      ));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-full">

      {/* Messages area */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.map((message) => (
          <div
            key={message.id}
            className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            <div className={`max-w-3xl ${message.role === 'user' ? 'order-2' : 'order-1'}`}>

              {/* Avatar */}
              <div className={`flex items-end gap-2 ${message.role === 'user' ? 'flex-row-reverse' : 'flex-row'}`}>
                <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm flex-shrink-0 ${
                  message.role === 'user'
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-200 text-gray-600'
                }`}>
                  {message.role === 'user' ? '👤' : '🤖'}
                </div>

                {/* Message bubble */}
                <div className={`rounded-2xl px-4 py-3 ${
                  message.role === 'user'
                    ? 'bg-blue-600 text-white rounded-br-sm'
                    : 'bg-white border border-gray-200 text-gray-800 rounded-bl-sm shadow-sm'
                }`}>
                  {message.content === '...' ? (
                    <div className="flex space-x-1 py-1">
                      <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{animationDelay: '0ms'}}/>
                      <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{animationDelay: '150ms'}}/>
                      <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{animationDelay: '300ms'}}/>
                    </div>
                  ) : (
                    <p className="text-sm leading-relaxed whitespace-pre-wrap">
                      {message.content}
                    </p>
                  )}
                </div>
              </div>

              {/* Processing time */}
              {message.processingTimeMs && (
                <p className="text-xs text-gray-400 mt-1 ml-10">
                  Answered in {message.processingTimeMs}ms
                </p>
              )}

              {/* Citations */}
              {message.citations && message.citations.length > 0 && (
                <div className="mt-3 ml-10 space-y-2">
                  <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                    Sources ({message.citations.length})
                  </p>
                  {message.citations.map((citation, index) => (
                    <CitationCard
                      key={`${citation.documentId}-${citation.chunkIndex}`}
                      citation={citation}
                      index={index}
                    />
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      {/* Input area */}
      <div className="border-t border-gray-200 p-4 bg-white">
        {selectedDocumentIds.length === 0 && (
          <p className="text-xs text-amber-600 mb-2 text-center">
            ⚠️ No documents selected — searching across all documents
          </p>
        )}
        <form onSubmit={handleSubmit} className="flex gap-3">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask a question about your documents..."
            disabled={isLoading}
            className="flex-1 border border-gray-300 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50 disabled:bg-gray-50"
          />
          <button
            type="submit"
            disabled={isLoading || !input.trim()}
            className="bg-blue-600 text-white px-5 py-2.5 rounded-xl text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
          >
            {isLoading ? (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"/>
            ) : (
              '→'
            )}
            Send
          </button>
        </form>
      </div>
    </div>
  );
};