import React, { useCallback, useMemo, useState } from 'react';
import ReactFlow, {
  Background,
  BackgroundVariant,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
  type Connection,
  type Node,
  type Edge,
  Panel,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { Download } from 'lucide-react';
import {
  definitionNode,
  lemmaNode,
  theoremNode,
  corollaryNode,
  propositionNode,
} from './MathConceptNode';
import { NodeDetailPanel } from './NodeDetailPanel';
import { graphApi } from '../../services/api';
import type { ConceptNodeData, GraphResponse } from '../../types';
import type { ReactFlowEdgeData } from '../../types';

interface GraphCanvasProps {
  graphData: GraphResponse;
}

// Register custom node types
const nodeTypes = {
  definitionNode,
  lemmaNode,
  theoremNode,
  corollaryNode,
  propositionNode,
};

export const GraphCanvas: React.FC<GraphCanvasProps> = ({ graphData }) => {
  const [nodes, setNodes, onNodesChange] = useNodesState(graphData.nodes as Node[]);
  const [edges, setEdges, onEdgesChange] = useEdgesState(graphData.edges as Edge<ReactFlowEdgeData>[]);
  const [selectedNode, setSelectedNode] = useState<ConceptNodeData | null>(null);
  const [selectedEdge, setSelectedEdge] = useState<Edge<ReactFlowEdgeData> | null>(null);
  const [exportError, setExportError] = useState<string | null>(null);

  const onConnect = useCallback(
    (connection: Connection) => setEdges(eds => addEdge(connection, eds)),
    [setEdges]
  );

  const onNodeClick = useCallback((_: React.MouseEvent, node: Node) => {
    setSelectedNode(node.data as ConceptNodeData);
    setSelectedEdge(null);
  }, []);

  const onPaneClick = useCallback(() => {
    setSelectedNode(null);
    setSelectedEdge(null);
  }, []);

  const onEdgeClick = useCallback((_: React.MouseEvent, edge: Edge<ReactFlowEdgeData>) => {
    setSelectedEdge(edge);
    setSelectedNode(null);
  }, []);

  const onEdgeMouseEnter = useCallback((_: React.MouseEvent, edge: Edge<ReactFlowEdgeData>) => {
    setSelectedEdge(edge);
  }, []);

  const downloadJson = useCallback(async () => {
    setExportError(null);
    try {
      const exported = await graphApi.exportGraph(graphData.graphId);
      const blob = new Blob([JSON.stringify(exported, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `${graphData.title || graphData.graphId}.json`.replace(/[^a-z0-9_.-]+/gi, '_');
      anchor.click();
      URL.revokeObjectURL(url);
    } catch {
      setExportError('Could not export graph JSON.');
    }
  }, [graphData.graphId, graphData.title]);

  // Highlight cycle nodes in red
  const styledNodes = useMemo(() => {
    if (!graphData.hasCycle || !graphData.cycleNodes?.length) return nodes;
    return nodes.map(n => ({
      ...n,
      style: graphData.cycleNodes.includes(n.id)
        ? { filter: 'drop-shadow(0 0 8px #ff4444)' }
        : {},
    }));
  }, [nodes, graphData.hasCycle, graphData.cycleNodes]);

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      <ReactFlow
        nodes={styledNodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeClick={onNodeClick}
        onEdgeClick={onEdgeClick}
        onEdgeMouseEnter={onEdgeMouseEnter}
        onPaneClick={onPaneClick}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.2 }}
        defaultEdgeOptions={{
          animated: false,
          style: { stroke: '#4a5568', strokeWidth: 2 },
          labelStyle: { fill: '#a0aec0', fontSize: 11 },
          labelBgStyle: { fill: '#1a1a2e', fillOpacity: 0.9 },
          labelBgPadding: [6, 4],
          labelBgBorderRadius: 4,
        }}
        proOptions={{ hideAttribution: true }}
      >
        <Background
          variant={BackgroundVariant.Dots}
          gap={24}
          size={1}
          color="#2d3748"
        />
        <Controls
          style={{
            background: '#1a1a2e',
            border: '1px solid #2d3748',
            borderRadius: 8,
          }}
        />
        <MiniMap
          nodeColor={n => {
            const type = (n.data as ConceptNodeData)?.conceptType;
            if (type === 'DEFINITION') return '#4fc3f7';
            if (type === 'LEMMA') return '#81c784';
            if (type === 'THEOREM') return '#ef9a9a';
            if (type === 'COROLLARY') return '#ce93d8';
            return '#80cbc4';
          }}
          style={{
            background: '#0d0d1a',
            border: '1px solid #2d3748',
            borderRadius: 8,
          }}
        />

        {/* Cycle warning banner */}
        <Panel position="top-left">
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              background: '#0d0d1af2',
              border: '1px solid #2d3748',
              borderRadius: 8,
              padding: '8px 10px',
              color: '#b0b8c8',
              fontSize: '0.72rem',
              fontFamily: 'monospace',
            }}
          >
            <span>{graphData.nodes.length} concepts</span>
            <span>{graphData.edges.length} dependencies</span>
            <button
              type="button"
              onClick={downloadJson}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 5,
                background: '#123047',
                border: '1px solid #1a4a6e',
                borderRadius: 6,
                color: '#e8e8e8',
                cursor: 'pointer',
                fontFamily: 'monospace',
                fontSize: '0.68rem',
                padding: '5px 8px',
              }}
            >
              <Download size={12} />
              JSON
            </button>
          </div>
          {exportError && (
            <div style={{ color: '#fca5a5', fontSize: '0.7rem', fontFamily: 'monospace', marginTop: 6 }}>
              {exportError}
            </div>
          )}
        </Panel>

        {graphData.hasCycle && (
          <Panel position="top-center">
            <div
              style={{
                background: '#7f1d1d',
                border: '1px solid #ef4444',
                borderRadius: 8,
                padding: '8px 16px',
                color: '#fca5a5',
                fontSize: '0.8rem',
                fontFamily: 'monospace',
              }}
            >
              ⚠ Circular dependency detected in: {graphData.cycleNodes.join(' → ')}
            </div>
          </Panel>
        )}

        {/* Legend */}
        <Panel position="bottom-left">
          <div
            style={{
              background: '#0d0d1aee',
              border: '1px solid #2d3748',
              borderRadius: 8,
              padding: '10px 14px',
              fontSize: '0.7rem',
              fontFamily: 'monospace',
              color: '#718096',
            }}
          >
            {[ 
              { color: '#4fc3f7', label: 'Definition' },
              { color: '#81c784', label: 'Lemma' },
              { color: '#ef9a9a', label: 'Theorem' },
              { color: '#ce93d8', label: 'Corollary' },
              { color: '#80cbc4', label: 'Proposition' },
            ].map(({ color, label }) => (
              <div key={label} style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                <div style={{ width: 10, height: 10, borderRadius: 2, background: color }} />
                <span style={{ color: '#a0aec0' }}>{label}</span>
              </div>
            ))}
            <div style={{ borderTop: '1px solid #2d3748', marginTop: 6, paddingTop: 6, color: '#a0aec0' }}>
              Edge: source depends on target
            </div>
          </div>
        </Panel>

        {selectedEdge && (
          <Panel position="top-right">
            <div
              style={{
                width: 280,
                background: '#0d0d1af2',
                border: '1px solid #2d3748',
                borderRadius: 8,
                padding: '10px 12px',
                fontSize: '0.72rem',
                fontFamily: 'monospace',
                color: '#b0b8c8',
                boxShadow: '0 8px 24px rgba(0,0,0,0.35)',
              }}
            >
              <div style={{ color: '#4fc3f7', fontSize: '0.62rem', letterSpacing: '0.12em', marginBottom: 6 }}>
                DEPENDENCY REASON
              </div>
              <div style={{ color: '#e8e8e8', marginBottom: 6 }}>
                {selectedEdge.source} depends on {selectedEdge.target}
              </div>
              <div style={{ lineHeight: 1.5 }}>
                {selectedEdge.data?.reason || 'No reason was provided by the extraction model.'}
              </div>
            </div>
          </Panel>
        )}
      </ReactFlow>

      {/* Node detail side panel */}
      {selectedNode && (
        <NodeDetailPanel
          data={selectedNode}
          onClose={() => setSelectedNode(null)}
        />
      )}
    </div>
  );
};
