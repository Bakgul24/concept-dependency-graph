import React, { useState } from 'react';
import { FileText, Keyboard, Loader2, Sparkles, Upload } from 'lucide-react';
import axios from 'axios';
import { graphApi } from '../../services/api';
import type { AnalysisResponse } from '../../types';

interface UploadFormProps {
  onAnalysisComplete: (response: AnalysisResponse) => void;
}

type InputMode = 'text' | 'file';

const SAMPLE_TEXT = `Let $f: \\mathbb{R} \\to \\mathbb{R}$ be a real-valued function.

**Definition (Continuity).** A function $f$ is continuous at a point $c$ if
$\\lim_{x \\to c} f(x) = f(c)$.

**Definition (Intermediate Value Property).** A function $f$ on $[a,b]$ has the
intermediate value property if for every $y$ between $f(a)$ and $f(b)$, there
exists $c \\in [a,b]$ with $f(c) = y$.

**Lemma (Boundedness Lemma).** If $f$ is continuous on $[a,b]$, then $f$ is bounded
on $[a,b]$. This lemma relies on the definition of continuity.

**Theorem (Intermediate Value Theorem).** If $f$ is continuous on $[a,b]$ and $y$
is any value between $f(a)$ and $f(b)$, then there exists $c \\in (a,b)$ such that
$f(c) = y$.
*Proof.* This follows from the definition of continuity, the intermediate value
property, and the boundedness lemma. $\\square$

**Corollary.** Every continuous function on $[a,b]$ attains its maximum and minimum.
This follows directly from the Intermediate Value Theorem.`;

export const UploadForm: React.FC<UploadFormProps> = ({ onAnalysisComplete }) => {
  const [mode, setMode] = useState<InputMode>('text');
  const [title, setTitle] = useState('Real Analysis - Continuity');
  const [text, setText] = useState(SAMPLE_TEXT);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = mode === 'text' ? text.trim().length > 0 : selectedFile !== null;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setLoading(true);
    setError(null);

    try {
      const response = mode === 'text'
        ? await graphApi.analyzeText({ title, text })
        : await graphApi.analyzeFile(selectedFile as File, title);
      onAnalysisComplete(response);
    } catch (err: unknown) {
      const serverMessage = axios.isAxiosError(err)
        ? err.response?.data?.message
        : undefined;
      const msg = serverMessage
        ?? (err instanceof Error ? err.message : 'Analysis failed. Check your API keys and server.');
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        border: '1px solid #2d3748',
        borderRadius: 8,
        overflow: 'hidden',
      }}>
        <button
          type="button"
          onClick={() => setMode('text')}
          style={modeButtonStyle(mode === 'text')}
        >
          <Keyboard size={13} />
          Text
        </button>
        <button
          type="button"
          onClick={() => setMode('file')}
          style={modeButtonStyle(mode === 'file')}
        >
          <Upload size={13} />
          File
        </button>
      </div>

      <div>
        <label style={labelStyle}>
          DOCUMENT TITLE
        </label>
        <input
          type="text"
          value={title}
          onChange={e => setTitle(e.target.value)}
          style={inputStyle}
          placeholder="e.g. Real Analysis - Chapter 3"
        />
      </div>

      {mode === 'text' ? (
        <div>
          <label style={labelStyle}>
            MATHEMATICAL TEXT
          </label>
          <textarea
            value={text}
            onChange={e => setText(e.target.value)}
            rows={14}
            style={{
              ...inputStyle,
              color: '#b0b8c8',
              fontSize: '0.75rem',
              lineHeight: 1.6,
              resize: 'vertical',
            }}
            placeholder="Paste your mathematical text here..."
          />
        </div>
      ) : (
        <div>
          <label style={labelStyle}>
            DOCUMENT FILE
          </label>
          <label style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 8,
            minHeight: 82,
            background: '#0d0d1a',
            border: '1px dashed #2d3748',
            borderRadius: 8,
            color: '#b0b8c8',
            fontSize: '0.75rem',
            fontFamily: 'monospace',
            cursor: loading ? 'not-allowed' : 'pointer',
            padding: 12,
            boxSizing: 'border-box',
            textAlign: 'center',
          }}>
            <FileText size={16} />
            {selectedFile ? selectedFile.name : 'Choose .txt, .md, or .pdf'}
            <input
              type="file"
              accept=".txt,.md,.pdf,text/plain,text/markdown,application/pdf"
              disabled={loading}
              onChange={e => setSelectedFile(e.target.files?.[0] ?? null)}
              style={{ display: 'none' }}
            />
          </label>
        </div>
      )}

      {error && (
        <div style={{
          background: '#7f1d1d33',
          border: '1px solid #ef4444',
          borderRadius: 6,
          padding: '8px 12px',
          color: '#fca5a5',
          fontSize: '0.75rem',
          fontFamily: 'monospace',
        }}>
          {error}
        </div>
      )}

      <button
        onClick={handleSubmit}
        disabled={loading || !canSubmit}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
          background: loading ? '#1e2d40' : 'linear-gradient(135deg, #1a4a6e, #0e7490)',
          border: '1px solid #0e7490',
          borderRadius: 8,
          color: '#e8e8e8',
          padding: '10px 20px',
          fontSize: '0.8rem',
          fontFamily: 'monospace',
          cursor: loading ? 'not-allowed' : 'pointer',
          letterSpacing: '0.05em',
          transition: 'all 0.2s',
          opacity: loading || !canSubmit ? 0.6 : 1,
        }}
      >
        {loading ? (
          <>
            <Loader2 size={14} style={{ animation: 'spin 1s linear infinite' }} />
            Analyzing with AI...
          </>
        ) : (
          <>
            <Sparkles size={14} />
            Extract Dependency Graph
          </>
        )}
      </button>
    </div>
  );
};

const labelStyle: React.CSSProperties = {
  display: 'block',
  color: '#4a5568',
  fontSize: '0.65rem',
  letterSpacing: '0.12em',
  marginBottom: 6,
};

const inputStyle: React.CSSProperties = {
  width: '100%',
  background: '#0d0d1a',
  border: '1px solid #2d3748',
  borderRadius: 6,
  color: '#e8e8e8',
  padding: '8px 12px',
  fontSize: '0.82rem',
  fontFamily: 'monospace',
  outline: 'none',
  boxSizing: 'border-box',
};

const modeButtonStyle = (active: boolean): React.CSSProperties => ({
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 6,
  minHeight: 34,
  background: active ? '#123047' : '#0d0d1a',
  border: 0,
  color: active ? '#e8e8e8' : '#718096',
  fontFamily: 'monospace',
  fontSize: '0.72rem',
  cursor: 'pointer',
});
