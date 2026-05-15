import { useQuery } from '@tanstack/react-query';
import { documentApi } from '../api/client';
import type { Document } from '../types';
import { StatusBadge } from './StatusBadge';

interface Props {
  onDocumentSelect: (doc: Document) => void;
  selectedIds: string[];
}

export const DocumentList = ({ onDocumentSelect, selectedIds }: Props) => {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['documents'],
    queryFn: () => documentApi.getAll().then(r => r.data),
    refetchInterval: 5000,  // poll every 5 seconds for status updates
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-32">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"/>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-red-500 p-4 text-sm">
        Failed to load documents. Is the ingestion service running?
      </div>
    );
  }

  const documents: Document[] = data || [];

  return (
    <div className="space-y-2">
      <div className="flex justify-between items-center mb-3">
        <h3 className="font-medium text-gray-700 text-sm">
          Your Documents ({documents.length})
        </h3>
        <button
          onClick={() => refetch()}
          className="text-xs text-blue-600 hover:text-blue-800"
        >
          Refresh
        </button>
      </div>

      {documents.length === 0 ? (
        <p className="text-gray-400 text-sm text-center py-8">
          No documents yet. Upload one to get started.
        </p>
      ) : (
        documents.map((doc) => (
          <div
            key={doc.documentId}
            onClick={() => doc.status === 'READY' && onDocumentSelect(doc)}
            className={`p-3 rounded-lg border transition-all cursor-pointer ${
              selectedIds.includes(doc.documentId)
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-200 hover:border-gray-300 bg-white'
            } ${doc.status !== 'READY' ? 'opacity-60 cursor-not-allowed' : ''}`}
          >
            <div className="flex justify-between items-start gap-2">
              <p className="text-sm font-medium text-gray-800 truncate flex-1">
                {doc.fileName}
              </p>
              <StatusBadge status={doc.status} />
            </div>
            <p className="text-xs text-gray-400 mt-1">
              {new Date(doc.uploadedAt).toLocaleDateString()}
            </p>
            {doc.status === 'READY' && (
              <p className="text-xs text-blue-600 mt-1">
                {selectedIds.includes(doc.documentId)
                  ? '✓ Selected for query'
                  : 'Click to select for query'}
              </p>
            )}
          </div>
        ))
      )}
    </div>
  );
};