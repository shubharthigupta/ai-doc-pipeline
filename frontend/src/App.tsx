import { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DocumentList } from './components/DocumentList';
import { UploadZone } from './components/UploadZone';
import { ChatInterface } from './components/ChatInterface';
import type { Document } from './types';
import { setAuthToken } from './api/client';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30000,
    },
  },
});

// For local development — inject your Cognito token here
// In production this comes from the OAuth PKCE flow automatically
const DEV_TOKEN = import.meta.env.VITE_DEV_TOKEN || '';
if (DEV_TOKEN) {
  setAuthToken(DEV_TOKEN);
}

function AppContent() {
  const [selectedDocuments, setSelectedDocuments] = useState<Document[]>([]);

  const handleDocumentSelect = (doc: Document) => {
    setSelectedDocuments(prev => {
      const isSelected = prev.some(d => d.documentId === doc.documentId);
      if (isSelected) {
        return prev.filter(d => d.documentId !== doc.documentId);
      }
      return [...prev, doc];
    });
  };

  const selectedIds = selectedDocuments.map(d => d.documentId);

  return (
    <div className="h-screen flex flex-col bg-gray-50">

      {/* Header */}
      <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between shadow-sm">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center text-white text-sm font-bold">
            AI
          </div>
          <div>
            <h1 className="font-semibold text-gray-900">Document Intelligence</h1>
            <p className="text-xs text-gray-500">Powered by AWS Bedrock + RAG</p>
          </div>
        </div>
        {selectedDocuments.length > 0 && (
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-600">
              {selectedDocuments.length} document{selectedDocuments.length > 1 ? 's' : ''} selected
            </span>
            <button
              onClick={() => setSelectedDocuments([])}
              className="text-xs text-red-500 hover:text-red-700"
            >
              Clear
            </button>
          </div>
        )}
      </header>

      {/* Main content */}
      <div className="flex-1 flex overflow-hidden">

        {/* Left sidebar */}
        <div className="w-80 bg-white border-r border-gray-200 flex flex-col overflow-hidden">
          <div className="p-4 border-b border-gray-100">
            <h2 className="font-medium text-gray-800 mb-3">Upload Document</h2>
            <UploadZone />
          </div>
          <div className="flex-1 overflow-y-auto p-4">
            <DocumentList
              onDocumentSelect={handleDocumentSelect}
              selectedIds={selectedIds}
            />
          </div>
        </div>

        {/* Chat area */}
        <div className="flex-1 flex flex-col overflow-hidden">
          <ChatInterface selectedDocumentIds={selectedIds} />
        </div>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppContent />
    </QueryClientProvider>
  );
}