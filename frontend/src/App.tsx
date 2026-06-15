import React, { useState, useEffect } from 'react';
import { GitBranch, History, ChevronRight, Loader2 } from 'lucide-react';
import { UploadForm } from './components/upload/UploadForm';
import { GraphCanvas } from './components/graph/GraphCanvas';
import { graphApi } from './services/api';
import type { AnalysisResponse, GraphListItem, GraphResponse } from './types';
import './styles/global.css';

type View = 'landing' | 'analyzing' | 'graph';

export default function App() {
  const [view, setView] = useState<View>('landing');
  const [currentGraph, setCurrentGraph] = useState<GraphResponse | null>(null);
  const [graphList, setGraphList] = useState<GraphListItem[]>([]);
  const [loadingGraph, setLoadingGraph] = useState(false);

  useEffect(() => {
    graphApi.listGraphs().then(setGraphList).catch(() => {});
  }, []);

  const handleAnalysisComplete = async (response: AnalysisResponse) => {
    setLoadingGraph(true);
    setView('analyzing');
    try {
      const graph = await graphApi.getGraph(response.graphId);
      setCurrentGraph(graph);
      setView('graph');
      graphApi.listGraphs().then(setGraphList).catch(() => {});
    } catch (e) {
      console.error('Failed to load graph', e);
      setView('landing');
    } finally {
      setLoadingGraph(false);
    }
  };

  const loadHistoryGraph = async (graphId: string) => {
    setLoadingGraph(true);
    try {
      const graph = await graphApi.getGraph(graphId);
      setCurrentGraph(graph);
      setView('graph');
    } catch (e) {
      console.error('Failed to load graph', e);
    } finally {
      setLoadingGraph(false);
    }
  };

  return (
    <div className="app-layout">
      {/* ── Sidebar ──────────────────────────────────────────────── */}
      <aside className="sidebar">
        {/* Logo */}
        <div className="sidebar-logo" onClick={() => setView('landing')}>
          <GitBranch size={20} color="#4fc3f7" />
          <div>
            <div className="logo-title">Math Dep.</div>
            <div className="logo-sub">Mapper</div>
          </div>
        </div>

        {/* Upload form */}
        <div className="sidebar-section">
          <div className="sidebar-label">NEW ANALYSIS</div>
          <UploadForm onAnalysisComplete={handleAnalysisComplete} />
        </div>

        {/* History */}
        {graphList.length > 0 && (
          <div className="sidebar-section">
            <div className="sidebar-label">
              <History size={11} style={{ display: 'inline', marginRight: 5 }} />
              HISTORY
            </div>
            <div className="history-list">
              {graphList.map(g => (
                <button
                  key={g.graphId}
                  className="history-item"
                  onClick={() => loadHistoryGraph(g.graphId)}
                >
                  <div className="history-title">{g.title || 'Untitled'}</div>
                  <div className="history-meta">
                    {g.conceptCount} concepts
                    <ChevronRight size={11} />
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}
      </aside>

      {/* ── Main Canvas ───────────────────────────────────────────── */}
      <main className="main-canvas">
        {view === 'landing' && (
          <div className="landing-placeholder">
            <GitBranch size={64} color="#2d3748" strokeWidth={1} />
            <p className="landing-text">
              Paste mathematical text in the panel →<br />
              and watch your concepts become a dependency graph.
            </p>
          </div>
        )}

        {(view === 'analyzing' || loadingGraph) && (
          <div className="landing-placeholder">
            <Loader2 size={48} color="#4fc3f7" style={{ animation: 'spin 1s linear infinite' }} />
            <p className="landing-text">AI is extracting concepts and dependencies…</p>
          </div>
        )}

        {view === 'graph' && currentGraph && !loadingGraph && (
          <GraphCanvas graphData={currentGraph} />
        )}
      </main>
    </div>
  );
}
