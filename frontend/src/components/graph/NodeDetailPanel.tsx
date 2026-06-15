import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';
import 'katex/dist/katex.min.css';
import { X } from 'lucide-react';
import type { ConceptNodeData } from '../../types';

interface NodeDetailPanelProps {
  data: ConceptNodeData;
  onClose: () => void;
}

export const NodeDetailPanel: React.FC<NodeDetailPanelProps> = ({ data, onClose }) => {
  const TYPE_COLORS: Record<string, string> = {
    DEFINITION: '#4fc3f7',
    LEMMA: '#81c784',
    THEOREM: '#ef9a9a',
    COROLLARY: '#ce93d8',
    PROPOSITION: '#80cbc4',
  };

  const color = TYPE_COLORS[data.conceptType] ?? '#a0aec0';

  return (
    <div
      style={{
        position: 'absolute',
        top: 0,
        right: 0,
        width: '360px',
        height: '100%',
        background: '#0f0f1e',
        borderLeft: `2px solid ${color}44`,
        overflowY: 'auto',
        zIndex: 10,
        boxShadow: '-8px 0 32px rgba(0,0,0,0.5)',
        display: 'flex',
        flexDirection: 'column',
        fontFamily: "'IBM Plex Mono', monospace",
      }}
    >
      {/* Header */}
      <div
        style={{
          padding: '16px 20px',
          borderBottom: `1px solid ${color}33`,
          display: 'flex',
          alignItems: 'flex-start',
          gap: 12,
        }}
      >
        <div style={{ flex: 1 }}>
          <div
            style={{
              fontSize: '0.6rem',
              color,
              letterSpacing: '0.15em',
              fontWeight: 700,
              marginBottom: 4,
            }}
          >
            {data.conceptType}
          </div>
          <div style={{ color: '#e8e8e8', fontSize: '0.95rem', fontWeight: 600 }}>
            {data.label}
          </div>
        </div>
        <button
          onClick={onClose}
          style={{
            background: 'transparent',
            border: 'none',
            color: '#4a5568',
            cursor: 'pointer',
            padding: 4,
          }}
        >
          <X size={16} />
        </button>
      </div>

      {/* Statement */}
      <div style={{ padding: '16px 20px', borderBottom: `1px solid #1e2d40` }}>
        <div style={{ fontSize: '0.65rem', color: '#4a5568', marginBottom: 10, letterSpacing: '0.1em' }}>
          STATEMENT
        </div>
        <div style={{ color: '#b0b8c8', fontSize: '0.8rem', lineHeight: 1.7 }}>
          <ReactMarkdown remarkPlugins={[remarkMath]} rehypePlugins={[rehypeKatex]}>
            {data.statement}
          </ReactMarkdown>
        </div>
      </div>

      {/* Proof */}
      {data.proof && (
        <div style={{ padding: '16px 20px', borderBottom: `1px solid #1e2d40` }}>
          <div style={{ fontSize: '0.65rem', color: '#4a5568', marginBottom: 10, letterSpacing: '0.1em' }}>
            PROOF
          </div>
          <div style={{ color: '#8899aa', fontSize: '0.78rem', lineHeight: 1.7, fontStyle: 'italic' }}>
            <ReactMarkdown remarkPlugins={[remarkMath]} rehypePlugins={[rehypeKatex]}>
              {data.proof}
            </ReactMarkdown>
          </div>
        </div>
      )}

      <DependencyList
        title="THIS CONCEPT DEPENDS ON"
        items={data.dependsOn ?? []}
      />

      <DependencyList
        title="USED BY"
        items={data.usedBy ?? []}
      />
    </div>
  );
};

const DependencyList: React.FC<{
  title: string;
  items: NonNullable<ConceptNodeData['dependsOn']>;
}> = ({ title, items }) => (
  <div style={{ padding: '16px 20px', borderBottom: `1px solid #1e2d40` }}>
    <div style={{ fontSize: '0.65rem', color: '#4a5568', marginBottom: 10, letterSpacing: '0.1em' }}>
      {title}
    </div>
    {items.length === 0 ? (
      <div style={{ color: '#5f6b7a', fontSize: '0.75rem', lineHeight: 1.5 }}>
        None recorded.
      </div>
    ) : (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {items.map(item => (
          <div
            key={`${item.conceptId}-${item.relationType ?? 'DEPENDS_ON'}`}
            style={{
              border: '1px solid #263244',
              borderRadius: 6,
              padding: '8px 10px',
              background: '#0d0d1a',
            }}
          >
            <div style={{ color: '#e8e8e8', fontSize: '0.76rem', marginBottom: 3 }}>
              {item.label}
            </div>
            <div style={{ color: '#718096', fontSize: '0.62rem', marginBottom: item.reason ? 6 : 0 }}>
              {item.conceptType} - {item.relationType ?? 'DEPENDS_ON'}
            </div>
            {item.reason && (
              <div style={{ color: '#9aa8b8', fontSize: '0.72rem', lineHeight: 1.45 }}>
                {item.reason}
              </div>
            )}
          </div>
        ))}
      </div>
    )}
  </div>
);
