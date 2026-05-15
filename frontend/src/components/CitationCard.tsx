import { useState } from 'react';
import type { Citation } from '../types';

interface Props {
  citation: Citation;
  index: number;
}

export const CitationCard = ({ citation, index }: Props) => {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden">
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between p-3 bg-gray-50 hover:bg-gray-100 transition-colors text-left"
      >
        <div className="flex items-center gap-2">
          <span className="bg-blue-600 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center flex-shrink-0">
            {index + 1}
          </span>
          <div>
            <p className="text-sm font-medium text-gray-800 truncate max-w-xs">
              {citation.documentName}
            </p>
            <p className="text-xs text-gray-500">
              Chunk {citation.chunkIndex} •{' '}
              <span className="text-green-600 font-medium">
                {(citation.similarityScore * 100).toFixed(1)}% match
              </span>
            </p>
          </div>
        </div>
        <span className="text-gray-400 text-sm ml-2">
          {expanded ? '▲' : '▼'}
        </span>
      </button>

      {expanded && (
        <div className="p-3 bg-white">
          <p className="text-sm text-gray-600 leading-relaxed">
            {citation.relevantText}
          </p>
        </div>
      )}
    </div>
  );
};