
import { useState } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
} from '@xyflow/react'
import type { Node, Edge } from '@xyflow/react'
import '@xyflow/react/dist/style.css'

import {
  Network,
  Loader2,
  AlertCircle,
} from 'lucide-react'

import api from './api/client'
import type { EndpointSummary } from './SpecificationsPage'

// Backend graph node response
interface GraphNodeResponse {
  id: string
  type: string
  name: string
  description: string
}

// Backend graph relationship response
interface GraphEdgeResponse {
  source: string
  target: string
  relationship: string
  description: string
}

// Complete backend graph response
interface SecurityGraphResponse {
  nodeCount: number
  relationshipCount: number
  nodes: GraphNodeResponse[]
  relationships: GraphEdgeResponse[]
}

// Props received from App.tsx
interface SecurityGraphPageProps {
  endpoints: EndpointSummary[]
}

function SecurityGraphPage({ endpoints }: SecurityGraphPageProps) {
  const [nodes, setNodes] = useState<Node[]>([])
  const [edges, setEdges] = useState<Edge[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [graphInfo, setGraphInfo] =
    useState<SecurityGraphResponse | null>(null)

  // Build graph using the Spring Boot backend
  const buildGraph = async () => {
    if (endpoints.length === 0) {
      setError('Upload an OpenAPI specification first.')
      return
    }

    try {
      setLoading(true)
      setError('')

      const response = await api.post<SecurityGraphResponse>(
        '/graph/build',
        endpoints
      )

      const graph = response.data
      setGraphInfo(graph)

      // Position different node types in separate columns
      const columns: Record<string, number> = {
        IDENTITY: 0,
        ROLE: 300,
        RESOURCE: 600,
        ENDPOINT: 900,
        WORKFLOW_STATE: 1200,
      }

      // Convert backend nodes into React Flow nodes
      const graphNodes: Node[] = graph.nodes.map((node, index) => ({
        id: node.id,
        position: {
          x: columns[node.type] ?? 0,
          y: index * 110,
        },
        data: {
          label: `${node.type}: ${node.name}`,
        },
        style: {
          background: '#1e293b',
          color: '#ffffff',
          border: '1px solid #475569',
          borderRadius: '8px',
          padding: '12px',
          width: 220,
        },
      }))

      // Convert backend relationships into React Flow edges
      const graphEdges: Edge[] = graph.relationships.map(
        (edge, index) => ({
          id: `edge-${index}`,
          source: edge.source,
          target: edge.target,
          label: edge.relationship,
          animated: false,
          style: {
            stroke: '#64748b',
          },
        })
      )

      setNodes(graphNodes)
      setEdges(graphEdges)

    } catch (err) {
      console.error('Graph building failed:', err)
      setError(
        'Unable to build the security graph. Check whether the backend is running.'
      )
    } finally {
      setLoading(false)
    }
  }

  // Security Graph page interface
  return (
    <div className="space-y-6">

      {/* Page heading and build button */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">
            Security Graph
          </h1>

          <p className="mt-1 text-slate-400">
            Visualize identities, roles, resources and API endpoints.
          </p>
        </div>

        <button
          onClick={buildGraph}
          disabled={loading || endpoints.length === 0}
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-500 disabled:opacity-50"
        >
          {loading ? (
            <Loader2 className="animate-spin" size={18} />
          ) : (
            <Network size={18} />
          )}

          {loading ? 'Building...' : 'Build Security Graph'}
        </button>
      </div>

      {/* Error message */}
      {error && (
        <div className="flex items-center gap-2 rounded-lg bg-red-950 p-3 text-red-300">
          <AlertCircle size={18} />
          {error}
        </div>
      )}

      {/* Graph statistics */}
      {graphInfo && (
        <div className="flex gap-6 text-sm text-slate-300">
          <span>
            Nodes: {graphInfo.nodeCount}
          </span>

          <span>
            Relationships: {graphInfo.relationshipCount}
          </span>
        </div>
      )}

      {/* Interactive graph */}
      <div className="h-[600px] overflow-hidden rounded-xl border border-slate-700 bg-slate-950">

        {nodes.length > 0 ? (
          <ReactFlow
            nodes={nodes}
            edges={edges}
            fitView
          >
            <Background />
            <Controls />
            <MiniMap />
          </ReactFlow>
        ) : (
          <div className="flex h-full items-center justify-center text-slate-500">
            Upload an OpenAPI specification and build your graph.
          </div>
        )}

      </div>
    </div>
  )
}

export default SecurityGraphPage
