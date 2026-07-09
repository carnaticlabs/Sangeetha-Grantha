import React from 'react';

/** CSV manifest upload card for creating a new bulk-import batch. */
export const UploadPanel: React.FC<{
  selectedFile: File | null;
  creating: boolean;
  onFileSelect: (file: File | null) => void;
  onUpload: () => void;
}> = ({ selectedFile, creating, onFileSelect, onUpload }) => (
  <div className="bg-white border border-border-light rounded-xl shadow-sm p-4 md:p-6 flex flex-col gap-4">
    <div>
      <label htmlFor="bulk-import-file" className="text-xs font-semibold text-ink-600 mb-1 block">Upload Composition List</label>
      <div className="flex flex-col sm:flex-row sm:items-center gap-3">
        <input
          id="bulk-import-file"
          type="file"
          accept=".csv"
          onChange={e => onFileSelect(e.target.files?.[0] || null)}
          className="flex-1 min-w-0 px-3 py-2 rounded-lg border border-border-light text-sm file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-xs file:font-semibold file:bg-primary-light file:text-primary hover:file:bg-primary-light/80"
        />
        <button
          onClick={onUpload}
          disabled={creating || !selectedFile}
          className="shrink-0 px-4 py-2.5 h-[42px] box-border bg-primary text-white rounded-lg font-medium shadow-sm hover:bg-primary-dark disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
        >
          {creating ? 'Uploading…' : 'Upload & Process'}
        </button>
      </div>
      <p className="text-[11px] text-ink-400 mt-1">
        Your CSV should have columns for composition name, raga, and source link.
      </p>
    </div>
  </div>
);
