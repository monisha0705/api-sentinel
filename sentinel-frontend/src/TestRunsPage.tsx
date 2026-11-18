
import { useState } from 'react'
import {
  Play,
  Loader2,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ShieldCheck,
} from 'lucide-react'
import api from './api/client'
import type { EndpointSummary } from './SpecificationsPage'

interface SecurityTestCase {
  testId: string
  testType: string
  identity: string
  method: string
  endpoint: string
  targetResource: string
  description: string
  expectedStatus: number
}

interface TestExecutionResult {
  testId: string
  identity: string
  endpoint: string
  expectedStatus: number
  actualStatus: number
  passed: boolean
  responseBody: string
  executedAt: string
}

interface TestRunsPageProps {
  endpoints: EndpointSummary[]
}

function TestRunsPage({ endpoints }: TestRunsPageProps) {
  const [testCases, setTestCases] = useState<SecurityTestCase[]>([])
  const [results, setResults] = useState<TestExecutionResult[]>([])
  const [loading, setLoading] = useState(false)
  const [executing, setExecuting] = useState(false)
  const [error, setError] = useState('')

  // Generate security tests using the backend graph-guided engine
  const generateTests = async () => {
    if (endpoints.length === 0) {
      setError('Upload an OpenAPI specification first.')
      return
    }

    try {
      setLoading(true)
      setError('')
      setResults([])

      const response = await api.post<SecurityTestCase[]>(
        '/tests/generate',
        endpoints
      )

      setTestCases(response.data)
    } catch (err) {
      console.error('Test generation failed:', err)
      setError('Unable to generate tests. Check the backend connection.')
    } finally {
      setLoading(false)
    }
  }

  // Execute generated security tests against Sentinel Bank
  const executeTests = async () => {
    if (testCases.length === 0) {
      setError('Generate security tests first.')
      return
    }

    try {
      setExecuting(true)
      setError('')

     const response = await api.post<TestExecutionResult[]>(
  '/tests/execute',
  testCases
)

setResults(response.data)

// Automatically detect and persist security findings
await api.post('/findings/detect', {
  testCases: testCases,
  executionResults: response.data,
})
    } catch (err) {
      console.error('Test execution failed:', err)
      setError('Unable to execute tests. Check whether Sentinel Bank is running.')
    } finally {
      setExecuting(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">
            Security Test Runs
          </h1>
          <p className="mt-1 text-slate-400">
            Generate and execute graph-guided authorization tests.
          </p>
        </div>

        <div className="flex gap-3">
          <button
            onClick={generateTests}
            disabled={loading || endpoints.length === 0}
            className="flex items-center gap-2 rounded-lg bg-violet-600 px-4 py-2 text-white hover:bg-violet-500 disabled:opacity-50"
          >
            {loading ? <Loader2 className="animate-spin" size={18} /> : <ShieldCheck size={18} />}
            {loading ? 'Generating...' : 'Generate Tests'}
          </button>

          <button
            onClick={executeTests}
            disabled={executing || testCases.length === 0}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-500 disabled:opacity-50"
          >
            {executing ? <Loader2 className="animate-spin" size={18} /> : <Play size={18} />}
            {executing ? 'Executing...' : 'Execute Tests'}
          </button>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-lg bg-red-950 p-3 text-red-300">
          <AlertCircle size={18} />
          {error}
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-xl border border-white/10 bg-[#111116] p-5">
          <p className="text-sm text-gray-400">Generated tests</p>
          <p className="mt-2 text-3xl font-semibold text-white">
            {testCases.length}
          </p>
        </div>

        <div className="rounded-xl border border-white/10 bg-[#111116] p-5">
          <p className="text-sm text-gray-400">Executed tests</p>
          <p className="mt-2 text-3xl font-semibold text-white">
            {results.length}
          </p>
        </div>

        <div className="rounded-xl border border-white/10 bg-[#111116] p-5">
          <p className="text-sm text-gray-400">Passed tests</p>
          <p className="mt-2 text-3xl font-semibold text-emerald-400">
            {results.filter((result) => result.passed).length}
          </p>
        </div>
      </div>

      <section className="rounded-xl border border-white/10 bg-[#111116] p-6">
        <h2 className="font-semibold text-white">Generated test cases</h2>

        {testCases.length === 0 ? (
          <p className="mt-4 text-sm text-gray-500">
            No tests generated yet. Click Generate Tests to begin.
          </p>
        ) : (
          <div className="mt-4 space-y-3">
            {testCases.map((test) => (
              <div
                key={test.testId}
                className="rounded-lg border border-white/10 p-4"
              >
                <div className="flex flex-wrap justify-between gap-2">
                  <span className="font-medium text-white">
                    {test.testId}
                  </span>
                  <span className="text-xs text-violet-400">
                    {test.testType}
                  </span>
                </div>

                <p className="mt-2 text-sm text-gray-300">
                  {test.identity} → {test.endpoint}
                </p>
                <p className="mt-1 text-xs text-gray-500">
                  Target: {test.targetResource}
                </p>
                <p className="mt-2 text-xs text-gray-400">
                  {test.description}
                </p>
                <p className="mt-2 text-xs text-gray-400">
                  Expected HTTP status: {test.expectedStatus}
                </p>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="rounded-xl border border-white/10 bg-[#111116] p-6">
        <h2 className="font-semibold text-white">Execution results</h2>

        {results.length === 0 ? (
          <p className="mt-4 text-sm text-gray-500">
            No execution results yet. Generate and execute tests first.
          </p>
        ) : (
          <div className="mt-4 space-y-3">
            {results.map((result) => (
              <div
                key={result.testId}
                className="rounded-lg border border-white/10 p-4"
              >
                <div className="flex items-center justify-between gap-3">
                  <span className="font-medium text-white">
                    {result.testId}
                  </span>

                  {result.passed ? (
                    <span className="flex items-center gap-1 text-sm text-emerald-400">
                      <CheckCircle2 size={16} />
                      Passed
                    </span>
                  ) : (
                    <span className="flex items-center gap-1 text-sm text-red-400">
                      <XCircle size={16} />
                      Failed
                    </span>
                  )}
                </div>

                <p className="mt-2 text-sm text-gray-400">
                  {result.identity} → {result.endpoint}
                </p>

                <div className="mt-3 flex gap-6 text-xs text-gray-400">
                  <span>Expected: {result.expectedStatus}</span>
                  <span>Actual: {result.actualStatus}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

export default TestRunsPage
