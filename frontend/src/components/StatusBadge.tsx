import type { Document } from '../types';

interface Props {
  status: Document['status'];
}

const statusConfig = {
  UPLOADED: { label: 'Uploaded', color: 'bg-blue-100 text-blue-800' },
  EXTRACTING: { label: 'Extracting...', color: 'bg-yellow-100 text-yellow-800' },
  EXTRACTED: { label: 'Extracted', color: 'bg-purple-100 text-purple-800' },
  EMBEDDING: { label: 'Embedding...', color: 'bg-orange-100 text-orange-800' },
  READY: { label: 'Ready', color: 'bg-green-100 text-green-800' },
  FAILED: { label: 'Failed', color: 'bg-red-100 text-red-800' },
};

export const StatusBadge = ({ status }: Props) => {
  const config = statusConfig[status] || statusConfig.UPLOADED;
  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${config.color}`}>
      {config.label}
    </span>
  );
};