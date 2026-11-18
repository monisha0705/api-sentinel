
import { useState } from 'react'
import { Upload, FileCode, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react'
import api from './api/client'

export interface EndpointSummary {
  path: string
  method: string
  operationId: string
  summary: string
  tags: string[]
  parameterCount: number
  requestBodyRequired: boolean
}

interface SpecificationsPageProps {
  endpoints: EndpointSummary[]
  onEndpointsParsed: (endpoints: EndpointSummary[]) => void
}



function SpecificationsPage({ endpoints, onEndpointsParsed }: SpecificationsPageProps) {
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [uploaded, setUploaded] = useState(false)

  const uploadSpecification = async () => {
    if (!file) {
      setError('Please select an OpenAPI JSON or YAML file.')
      return
    }

    const formData = new FormData()
    formData.append('file', file)

    setLoading(true)
    setError('')
    setUploaded(false)

    try {
      const response = await api.post<EndpointSummary[]>(
        '/specifications/upload',
        formData,
        {
          headers: { 'Content-Type': 'multipart/form-data' },
        },
      )

      onEndpointsParsed(response.data)
      setUploaded(true)
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
        err.response?.data ||
        'Upload failed. Check whether the backend is running and the file is valid.',
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-2xl font-semibold text-white">API Specifications</h3>
        <p className="mt-2 text-sm text-gray-500">
          Upload an OpenAPI or Swagger specification to discover API endpoints.
        </p>
      </div>

      <div className="rounded-xl border border-white/10 bg-[#111116] p-7">
        <div className="flex items-center gap-3">
          <div className="rounded-lg bg-violet-500/10 p-3">
            <FileCode className="text-violet-400" size={23} />
          </div>
          <div>
            <h3 className="font-semibold text-white">Upload specification</h3>
            <p className="text-xs text-gray-500">JSON or YAML OpenAPI document</p>
          </div>
        </div>

        <label className="mt-6 flex cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed border-white/20 bg-white/[0.02] px-5 py-10 transition hover:border-violet-400/60">
          <Upload size={30} className="text-violet-400" />
          <p className="mt-4 text-sm font-medium text-gray-200">
            {file ? file.name : 'Choose an OpenAPI specification'}
          </p>
          <p className="mt-2 text-xs text-gray-500">
            Select a .json, .yaml or .yml file
          </p>
          <input
            type="file"
            accept=".json,.yaml,.yml,application/json"
            className="hidden"
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null)
              setError('')
              setUploaded(false)
            }}
          />
        </label>

        <button
          onClick={uploadSpecification}
          disabled={!file || loading}
          className="mt-5 flex items-center gap-2 rounded-lg bg-violet-600 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-violet-500 disabled:cursor-not-allowed disabled:opacity-40"
        >
          {loading ? <Loader2 size={17} className="animate-spin" /> : <Upload size={17} />}
          {loading ? 'Parsing specification...' : 'Upload and parse'}
        </button>

        {error && (
          <div className="mt-5 flex items-start gap-2 rounded-lg border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-400">
            <AlertCircle size={18} className="shrink-0" />
            <span>{String(error)}</span>
          </div>
        )}

        {uploaded && (
          <div className="mt-5 flex items-center gap-2 rounded-lg border border-emerald-500/20 bg-emerald-500/10 p-3 text-sm text-emerald-400">
            <CheckCircle2 size={18} />
            Successfully parsed {endpoints.length} endpoints.
          </div>
        )}
      </div>

      {endpoints.length > 0 && (
        <div className="rounded-xl border border-white/10 bg-[#111116] p-6">
          <div className="mb-5">
            <h3 className="font-semibold text-white">Discovered endpoints</h3>
            <p className="mt-1 text-xs text-gray-500">
              {endpoints.length} endpoints extracted from your specification
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-white/10 text-xs text-gray-500">
                <tr>
                  <th className="pb-3">Method</th>
                  <th className="pb-3">Endpoint</th>
                  <th className="pb-3">Summary</th>
                  <th className="pb-3">Parameters</th>
                  <th className="pb-3">Request body</th>
                </tr>
              </thead>
              <tbody>
                {endpoints.map((endpoint, index) => (
                  <tr key={`${endpoint.method}-${endpoint.path}-${index}`} className="border-b border-white/5">
                    <td className="py-4">
                      <span className="rounded bg-blue-500/10 px-2 py-1 text-xs font-semibold text-blue-400">
                        {endpoint.method}
                      </span>
                    </td>
                    <td className="py-4 font-mono text-xs text-gray-200">
                      {endpoint.path}
                    </td>
                    <td className="py-4 text-gray-400">
                      {endpoint.summary || endpoint.operationId || '—'}
                    </td>
                    <td className="py-4 text-gray-400">
                      {endpoint.parameterCount}
                    </td>
                    <td className="py-4 text-gray-400">
                      {endpoint.requestBodyRequired ? 'Required' : 'No'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

export default SpecificationsPage
