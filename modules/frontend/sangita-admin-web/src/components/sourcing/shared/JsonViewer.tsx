import React, { useState, useMemo } from 'react';

interface JsonViewerProps {
  data: object | string;
  collapsed?: boolean;
  maxHeight?: number;
}

const JsonViewer: React.FC<JsonViewerProps> = ({ data, collapsed = false, maxHeight = 400 }) => {
  const [isCollapsed, setIsCollapsed] = useState(collapsed);
  const [copied, setCopied] = useState(false);

  const jsonString = useMemo(() => {
    if (typeof data === 'string') {
      try {
        return JSON.stringify(JSON.parse(data), null, 2);
      } catch {
        return data;
      }
    }
    return JSON.stringify(data, null, 2);
  }, [data]);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(jsonString);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback
      const textarea = document.createElement('textarea');
      textarea.value = jsonString;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="border border-border-light rounded-lg overflow-hidden bg-slate-50">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-1.5 bg-slate-100 border-b border-border-light">
        <button
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="flex items-center gap-1 text-xs text-ink-600 hover:text-ink-800 transition-colors"
        >
          <span className="material-symbols-outlined text-sm transition-transform" style={{ transform: isCollapsed ? '' : 'rotate(90deg)' }}>
            chevron_right
          </span>
          {isCollapsed ? 'Expand' : 'Collapse'}
        </button>
        <button
          onClick={handleCopy}
          className="flex items-center gap-1 text-xs text-ink-500 hover:text-primary transition-colors"
          title="Copy to clipboard"
        >
          <span className="material-symbols-outlined text-sm">
            {copied ? 'check' : 'content_copy'}
          </span>
          {copied ? 'Copied!' : 'Copy'}
        </button>
      </div>

      {/* Content */}
      {!isCollapsed && (
        <pre
          className="p-3 text-xs font-mono text-ink-700 overflow-auto whitespace-pre-wrap break-words"
          style={{ maxHeight }}
        >
          {jsonString}
        </pre>
      )}
    </div>
  );
};

export default JsonViewer;
