import React, { memo } from 'react';
import { Handle, Position, type NodeProps } from 'reactflow';
import ReactMarkdown from 'react-markdown';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';
import 'katex/dist/katex.min.css';
import type { ConceptNodeData, ConceptType } from '../../types';

// ── Color / Style config per concept type ────────────────────────────────────

const TYPE_CONFIG: Record<ConceptType, {
  bg: string;
  border: string;
  badge: string;
  badgeText: string;
  label: string;
}> = {
  DEFINITION: {
    bg: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
    border: '#4fc3f7',
    badge: '#4fc3f7',
    badgeText: '#0d0d0d',
    label: 'DEF',
  },
  LEMMA: {
    bg: 'linear-gradient(135deg, #1a2e1a 0%, #1b3a1b 100%)',
    border: '#81c784',
    badge: '#81c784',
    badgeText: '#0d0d0d',
    label: 'LEM',
  },
  THEOREM: {
    bg: 'linear-gradient(135deg, #2e1a1a 0%, #3a1b1b 100%)',
    border: '#ef9a9a',
    badge: '#ef9a9a',
    badgeText: '#0d0d0d',
    label: 'THM',
  },
  COROLLARY: {
    bg: 'linear-gradient(135deg, #2a1a2e 0%, #321b3a 100%)',
    border: '#ce93d8',
    badge: '#ce93d8',
    badgeText: '#0d0d0d',
    label: 'COR',
  },
  PROPOSITION: {
    bg: 'linear-gradient(135deg, #1a2a2e 0%, #1b323a 100%)',
    border: '#80cbc4',
    badge: '#80cbc4',
    badgeText: '#0d0d0d',
    label: 'PRO',
  },
};

// ── Shared MathNode component ─────────────────────────────────────────────────

interface MathNodeProps extends NodeProps<ConceptNodeData> { }

export const MathConceptNode = memo(({ data, selected }: MathNodeProps) => {
  const conceptType = data.conceptType as ConceptType;
  const config = TYPE_CONFIG[conceptType] ?? TYPE_CONFIG.DEFINITION;

  return (
    <div
      style={{
        background: config.bg,
        border: `2px solid ${selected ? '#ffffff' : config.border}`,
        borderRadius: '12px',
        padding: '14px 16px',
        minWidth: '240px',
        maxWidth: '300px',
        boxShadow: selected
          ? `0 0 0 3px ${config.border}44, 0 8px 32px rgba(0,0,0,0.5)`
          : `0 4px 16px rgba(0,0,0,0.4), 0 0 0 0px ${config.border}00`,
        transition: 'all 0.2s ease',
        cursor: 'grab',
        fontFamily: "'IBM Plex Mono', 'Fira Code', monospace",
      }}
    >
      {/* Source handle (top) */}
      <Handle
        type="source"
        position={Position.Bottom}
        style={{ background: config.border, width: 10, height: 10, border: 'none' }}
      />

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
        <span
          style={{
            background: config.badge,
            color: config.badgeText,
            fontSize: '0.6rem',
            fontWeight: 700,
            letterSpacing: '0.08em',
            padding: '2px 7px',
            borderRadius: '4px',
            flexShrink: 0,
          }}
        >
          {config.label}
        </span>
        <span
          style={{
            color: '#e8e8e8',
            fontSize: '0.82rem',
            fontWeight: 600,
            lineHeight: 1.3,
          }}
        >
          {data.label}
        </span>
      </div>

      {/* Statement with KaTeX */}
      {data.statement && (
        <div
          style={{
            color: '#b0b8c8',
            fontSize: '0.72rem',
            lineHeight: 1.55,
            borderTop: `1px solid ${config.border}33`,
            paddingTop: 8,
          }}
        >
          <ReactMarkdown
            remarkPlugins={[remarkMath]}
            rehypePlugins={[rehypeKatex]}
          >
            {data.statement.length > 200
              ? data.statement.slice(0, 200) + '…'
              : data.statement}
          </ReactMarkdown>
        </div>
      )}

      {/* Target handle (bottom) */}
      <Handle
        type="target"
        position={Position.Top}
        style={{ background: config.border, width: 10, height: 10, border: 'none' }}
      />
    </div>
  );
});

MathConceptNode.displayName = 'MathConceptNode';

// ── Named exports for nodeTypes map ──────────────────────────────────────────

export const definitionNode = MathConceptNode;
export const lemmaNode = MathConceptNode;
export const theoremNode = MathConceptNode;
export const corollaryNode = MathConceptNode;
export const propositionNode = MathConceptNode;
