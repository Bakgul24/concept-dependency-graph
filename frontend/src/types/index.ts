// ─── API Types (mirror backend DTOs) ──────────────────────────────────────────

export type ConceptType =
  | 'DEFINITION'
  | 'LEMMA'
  | 'THEOREM'
  | 'COROLLARY'
  | 'PROPOSITION';

export interface AnalysisRequest {
  title: string;
  text: string;
}

export interface AnalysisResponse {
  graphId: string;
  title: string;
  conceptCount: number;
  dependencyCount: number;
  hasCycle: boolean;
  message: string;
  warningCount?: number;
  warnings?: string[];
}

export interface GraphListItem {
  graphId: string;
  title: string;
  conceptCount: number;
  createdAt: string;
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
}

// ─── React Flow Types ──────────────────────────────────────────────────────────

export interface ConceptNodeData {
  label: string;
  conceptType: ConceptType;
  statement: string;
  proof?: string;
  dependsOn?: DependencySummary[];
  usedBy?: DependencySummary[];
}

export interface ReactFlowEdgeData {
  reason?: string;
  relationType?: string;
}

export interface DependencySummary {
  conceptId: string;
  label: string;
  conceptType: ConceptType;
  reason?: string;
  relationType?: string;
}

export interface GraphResponse {
  graphId: string;
  title: string;
  nodes: ReactFlowNodeDto[];
  edges: ReactFlowEdgeDto[];
  hasCycle: boolean;
  cycleNodes: string[];
}

export interface ReactFlowNodeDto {
  id: string;
  type: string;
  data: ConceptNodeData;
  position: { x: number; y: number };
}

export interface ReactFlowEdgeDto {
  id: string;
  source: string;
  target: string;
  label?: string;
  reason?: string;
  relationType?: string;
  data?: ReactFlowEdgeData;
  animated: boolean;
  style?: { stroke: string; strokeWidth: number };
}

export interface GraphExportResponse {
  graphId: string;
  title: string;
  createdAt: string;
  concepts: Array<{
    id: string;
    name: string;
    type: ConceptType;
    statement: string;
    proof?: string;
  }>;
  dependencies: Array<{
    from: string;
    to: string;
    reason?: string;
    relationType?: string;
    confidence?: number;
  }>;
  hasCycle: boolean;
  cycleNodes: string[];
}
