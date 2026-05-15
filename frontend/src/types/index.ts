export interface Document {
  documentId: string;
  fileName: string;
  status: 'UPLOADED' | 'EXTRACTING' | 'EXTRACTED' | 'EMBEDDING' | 'READY' | 'FAILED';
  uploadedAt: string;
  updatedAt: string;
}

export interface Citation {
  documentId: string;
  documentName: string;
  chunkIndex: number;
  relevantText: string;
  similarityScore: number;
}

export interface QueryResponse {
  queryId: string;
  question: string;
  answer: string;
  citations: Citation[];
  totalChunksSearched: number;
  processingTimeMs: number;
  answeredAt: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  citations?: Citation[];
  processingTimeMs?: number;
  timestamp: Date;
}