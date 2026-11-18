
import { useCallback, useEffect, useState } from 'react'
import {
  AlertCircle,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Clock,
  User,
  Server,
  ChevronDown,
  ChevronUp,
} from 'lucide-react'
import api from './api/client'

interface SecurityFinding {
  id: number
  findingId: string
  vulnerabilityType: string
  severity: string
  identity: string
  endpoint: string
  targetResource: string
  description: string
  expectedStatus: number
  actualStatus: number
  evidence: string
  detectedAt: string
}

export default function FindingsPage() {
  const [findings, setFindings] = useState<SecurityFinding[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [expandedId, setExpandedId] = useState<number | null>(null)

  const fetchFindings = useCallback(async () => {
    try {
      setLoading(true)
      setError('')

      const response = await api.get<SecurityFinding[]>('/findings')
      setFindings(response.data)
    } catch (err) {
      console.error('Failed to fetch findings:', err)
      setError('Unable to load findings. Check whether the backend is running.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void fetchFindings()
  }, [fetchFindings])

  const highCount = findings.filter(
    (finding) => finding.severity.toUpperCase() === 'HIGH'
  ).length

  const mediumCount = findings.filter(
    (finding) => finding.severity.toUpperCase() === 'MEDIUM'
  ).length

  const lowCount = findings.filter(
    (finding) => finding.severity.toUpperCase() === 'LOW'
  ).length

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-white">
            Security Findings
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Vulnerabilities detected and persisted by API Sentinel
          </p>
        </div>

        <button
          onClick={fetchFindings}
          disabled={loading}
          className="flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-sm text-white transition hover:bg-slate-700 disabled:opacity-50"
        >
          <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
          Refresh
        </button>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-xl border border-slate-800 bg-slate-900 p-5">
          <p className="text-sm text-slate-400">Total Findings</p>
          <p className="mt-2 text-3xl font-semibold text-white">
            {findings.length}
          </p>
        </div>

        <div className="rounded-xl border border-red-900/50 bg-slate-900 p-5">
          <p className="text-sm text-slate-400">High Severity</p>
          <p className="mt-2 text-3xl font-semibold text-red-400">
            {highCount}
          </p>
        </div>

        <div className="rounded-xl border border-amber-900/50 bg-slate-900 p-5">
          <p className="text-sm text-slate-400">Medium Severity</p>
          <p className="mt-2 text-3xl font-semibold text-amber-400">
            {mediumCount}
          </p>
        </div>

        <div className="rounded-xl border border-blue-900/50 bg-slate-900 p-5">
          <p className="text-sm text-slate-400">Low Severity</p>
          <p className="mt-2 text-3xl font-semibold text-blue-400">
            {lowCount}
          </p>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-3 rounded-lg border border-red-900/50 bg-red-950/30 p-4 text-sm text-red-300">
          <AlertCircle size={18} />
          {error}
        </div>
      )}

      {loading ? (
        <div className="rounded-xl border border-slate-800 bg-slate-900 p-10 text-center text-slate-400">
          Loading security findings...
        </div>
      ) : findings.length === 0 ? (
        <div className="rounded-xl border border-slate-800 bg-slate-900 p-10 text-center">
          <ShieldCheck size={36} className="mx-auto text-emerald-400" />
          <h3 className="mt-4 font-medium text-white">No findings recorded</h3>
          <p className="mt-2 text-sm text-slate-400">
            Run security tests to detect and record vulnerabilities.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {findings.map((finding) => {
            const expanded = expandedId === finding.id

            return (
              <div
                key={finding.id}
                className="overflow-hidden rounded-xl border border-slate-800 bg-slate-900"
              >
                <button
                  onClick={() =>
                    setExpandedId(expanded ? null : finding.id)
                  }
                  className="flex w-full flex-wrap items-center justify-between gap-4 p-5 text-left transition hover:bg-slate-800/50"
                >
                  <div className="flex min-w-0 items-center gap-4">
                    <div className="rounded-lg bg-red-950/50 p-3">
                      <ShieldAlert className="text-red-400" size={22} />
                    </div>

                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="font-semibold text-white">
                          {finding.vulnerabilityType}
                        </h3>

                        <span className="rounded-full border border-red-800 bg-red-950/50 px-2.5 py-1 text-xs font-medium text-red-300">
                          {finding.severity}
                        </span>
                      </div>

                      <p className="mt-1 break-all text-sm text-slate-400">
                        {finding.findingId}
                      </p>
                    </div>
                  </div>

                  {expanded ? (
                    <ChevronUp size={20} className="text-slate-400" />
                  ) : (
                    <ChevronDown size={20} className="text-slate-400" />
                  )}
                </button>

                <div className="grid grid-cols-1 gap-3 px-5 pb-5 md:grid-cols-3">
                  <div className="flex items-center gap-2 text-sm text-slate-400">
                    <User size={16} />
                    <span>{finding.identity}</span>
                  </div>

                  <div className="flex items-center gap-2 text-sm text-slate-400">
                    <Server size={16} />
                    <span className="break-all">{finding.targetResource}</span>
                  </div>

                  <div className="flex items-center gap-2 text-sm text-slate-400">
                    <Clock size={16} />
                    <span>
                      {new Date(finding.detectedAt).toLocaleString()}
                    </span>
                  </div>
                </div>

                {expanded && (
                  <div className="space-y-4 border-t border-slate-800 p-5">
                    <div>
                      <h4 className="mb-2 text-sm font-medium text-white">
                        Description
                      </h4>
                      <p className="text-sm leading-6 text-slate-400">
                        {finding.description}
                      </p>
                    </div>

                    <div>
                      <h4 className="mb-2 text-sm font-medium text-white">
                        Affected Endpoint
                      </h4>
                      <p className="break-all rounded-lg bg-slate-950 p-3 font-mono text-sm text-slate-300">
                        {finding.endpoint}
                      </p>
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                      <div className="rounded-lg bg-slate-950 p-4">
                        <p className="text-xs text-slate-500">Expected Status</p>
                        <p className="mt-1 text-xl font-semibold text-emerald-400">
                          {finding.expectedStatus}
                        </p>
                      </div>

                      <div className="rounded-lg bg-slate-950 p-4">
                        <p className="text-xs text-slate-500">Actual Status</p>
                        <p className="mt-1 text-xl font-semibold text-red-400">
                          {finding.actualStatus}
                        </p>
                      </div>
                    </div>

                    <div>
                      <h4 className="mb-2 text-sm font-medium text-white">
                        Execution Evidence
                      </h4>
                      <pre className="max-h-72 overflow-auto whitespace-pre-wrap break-all rounded-lg bg-slate-950 p-4 text-xs leading-5 text-slate-400">
                        {finding.evidence || 'No evidence available.'}
                      </pre>
                    </div>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
