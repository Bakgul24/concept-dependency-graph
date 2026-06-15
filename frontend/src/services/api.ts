import axios from 'axios';
import type {
  AnalysisRequest,
  AnalysisResponse,
  GraphListItem,
  GraphResponse,
  GraphExportResponse,
} from '../types';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
  timeout: 120_000, // AI calls can be slow
});

export const graphApi = {
  /** Analyze plain text and return graphId */
  analyzeText: (req: AnalysisRequest): Promise<AnalysisResponse> =>
    api.post<AnalysisResponse>('/graphs/analyze', req).then(r => r.data),

  /** Analyze an uploaded txt, md, or pdf file and return graphId */
  analyzeFile: (file: File, title?: string): Promise<AnalysisResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    if (title?.trim()) {
      formData.append('title', title.trim());
    }

    return api.post<AnalysisResponse>('/graphs/analyze/file', formData)
      .then(r => r.data);
  },

  /** Get the React Flow graph data */
  getGraph: (graphId: string): Promise<GraphResponse> =>
    api.get<GraphResponse>(`/graphs/${graphId}`).then(r => r.data),

  /** Export persisted graph as portable JSON */
  exportGraph: (graphId: string): Promise<GraphExportResponse> =>
    api.get<GraphExportResponse>(`/graphs/${graphId}/export`).then(r => r.data),

  /** List all past analyses */
  listGraphs: (): Promise<GraphListItem[]> =>
    api.get<GraphListItem[]>('/graphs').then(r => r.data),
};
