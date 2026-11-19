
import { useState } from 'react'
import {
  Play,
  Loader2,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ShieldCheck,
  Workflow,
  AlertTriangle,
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

interface StatefulResult {
  scenarioId: string
  scenarioName: string
  resourceId: string
  initialState: string
  finalState: string
  expectedFinalState: string
  invariantPreserved: boolean
  passed: boolean
  steps: TestExecutionResult[]
  executedAt: string
}

interface SecurityFinding {
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

interface TestRunsPageProps {
  endpoints: EndpointSummary[]
}

function TestRunsPage({ endpoints }: TestRunsPageProps) {
  const [testCases, setTestCases] = useState<SecurityTestCase[]>([])
  const [results, setResults] = useState<TestExecutionResult[]>([])
  const [statefulResult, setStatefulResult] =
    useState<StatefulResult | null>(null)

  const [statefulFindings, setStatefulFindings] =
    useState<SecurityFinding[]>([])

  const [loading, setLoading] = useState(false)
  const [executing, setExecuting] = useState(false)
  const [statefulExecuting, setStatefulExecuting] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const generateTests = async () => {
    if (endpoints.length === 0) {
      setError('Upload an OpenAPI specification first.')
      return
    }

    try {
      setLoading(true)
      setError('')
      setSuccessMessage('')
      setResults([])
      setStatefulResult(null)
      setStatefulFindings([])

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

  const executeTests = async () => {
    if (testCases.length === 0) {
      setError('Generate security tests first.')
      return
    }

    try {
      setExecuting(true)
      setError('')
      setSuccessMessage('')

      const response = await api.post<TestExecutionResult[]>(
        '/tests/execute',
        testCases
      )

      setResults(response.data)

      const findingsResponse = await api.post<SecurityFinding[]>(
        '/findings/detect',
        {
          testCases,
          executionResults: response.data,
        }
      )

      setSuccessMessage(
        `${response.data.length} tests executed. ${findingsResponse.data.length} new or updated findings detected.`
      )
    } catch (err) {
      console.error('Test execution failed:', err)
      setError('Unable to execute tests or persist findings. Check the backend and Sentinel Bank.')
    } finally {
      setExecuting(false)
    }
  }

  const executeStatefulTest = async () => {
    try {
      setStatefulExecuting(true)
      setError('')
      setSuccessMessage('')
      setStatefulResult(null)
      setStatefulFindings([])

      const response = await api.post<StatefulResult>(
        '/tests/execute/stateful'
      )

      setStatefulResult(response.data)

      // Detect and persist stateful business-logic vulnerabilities.
      const findingsResponse = await api.post<SecurityFinding[]>(
        '/findings/detect/stateful',
        response.data
      )

      setStatefulFindings(findingsResponse.data)

      if (findingsResponse.data.length > 0) {
        setSuccessMessage(
          `${findingsResponse.data.length} business-logic vulnerability finding(s) persisted successfully.`
        )
      } else {
        setSuccessMessage(
          'Stateful test completed. No business-logic vulnerability detected.'
        )
      }
    } catch (err) {
      console.error('Stateful execution or finding persistence failed:', err)
      setError(
        'Stateful execution completed or failed, but finding persistence could not be confirmed. Check backend logs.'
      )
    } finally {
      setStatefulExecuting(false)
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
            Generate and execute graph-guided authorization and business-logic tests.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <button
            onClick={generateTests}
            disabled={loading || endpoints.length === 0}
            className="flex items-center gap-2 rounded-lg bg-violet-600 px-4 py-2 text-white hover:bg-violet-500 disabled:opacity-50"
          >
            {loading ? (
              <Loader2 className="animate-spin" size={18} />
            ) : (
              <ShieldCheck size={18} />
            )}
            {loading ? 'Generating...' : 'Generate Tests'}
          </button>

          <button
            onClick={executeTests}
            disabled={executing || testCases.length === 0}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-500 disabled:opacity-50"
          >
            {executing ? (
              <Loader2 className="animate-spin" size={18} />
            ) : (
              <Play size={18} />
            )}
            {executing ? 'Executing...' : 'Execute Tests'}
          </button>

          <button
            onClick={executeStatefulTest}
            disabled={statefulExecuting}
            className="flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-white hover:bg-emerald-500 disabled:opacity-50"
          >
            {statefulExecuting ? (
              <Loader2 className="animate-spin" size={18} />
            ) : (
              <Workflow size={18} />
            )}
            {statefulExecuting ? 'Running...' : 'Run Stateful Test'}
          </button>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-lg bg-red-950 p-3 text-red-300">
          <AlertCircle size={18} />
          {error}
        </div>
      )}

      {successMessage && (
        <div className="flex items-center gap-2 rounded-lg bg-emerald-950 p-3 text-emerald-300">
          <CheckCircle2 size={18} />
          {successMessage}
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
        <div className="flex items-center gap-2">
          <Workflow className="text-emerald-400" size={20} />
          <h2 className="font-semibold text-white">
            Stateful Business Logic Testing
          </h2>
        </div>

        <p className="mt-2 text-sm text-gray-400">
          Tests multi-step payment workflows and validates security invariants
          against the actual resource state.
        </p>

        {!statefulResult ? (
          <div className="mt-5 rounded-lg border border-dashed border-white/10 p-5 text-center">
            <p className="text-sm text-gray-500">
              No stateful execution results yet.
            </p>
            <p className="mt-1 text-xs text-gray-600">
              Run BL-001 to test unauthorized payment approval.
            </p>
          </div>
        ) : (
          <div className="mt-5 space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-semibold text-white">
                  {statefulResult.scenarioId} — {statefulResult.scenarioName}
                </p>
                <p className="mt-1 text-xs text-gray-500">
                  Resource ID: {statefulResult.resourceId}
                </p>
              </div>

              {statefulResult.passed ? (
                <span className="flex items-center gap-2 rounded-lg bg-emerald-950 px-3 py-2 text-sm text-emerald-400">
                  <CheckCircle2 size={17} />
                  Security Test Passed
                </span>
              ) : (
                <span className="flex items-center gap-2 rounded-lg bg-red-950 px-3 py-2 text-sm text-red-400">
                  <AlertTriangle size={17} />
                  Vulnerability Detected
                </span>
              )}
            </div>

            <div className="grid gap-3 sm:grid-cols-3">
              <div className="rounded-lg border border-white/10 p-4">
                <p className="text-xs text-gray-500">Initial state</p>
                <p className="mt-2 font-semibold text-white">
                  {statefulResult.initialState}
                </p>
              </div>

              <div className="rounded-lg border border-white/10 p-4">
                <p className="text-xs text-gray-500">Final state</p>
                <p className={`mt-2 font-semibold ${
                  statefulResult.invariantPreserved
                    ? 'text-emerald-400'
                    : 'text-red-400'
                }`}>
                  {statefulResult.finalState}
                </p>
              </div>

              <div className="rounded-lg border border-white/10 p-4">
                <p className="text-xs text-gray-500">Expected final state</p>
                <p className="mt-2 font-semibold text-white">
                  {statefulResult.expectedFinalState}
                </p>
              </div>
            </div>

            <div className={`rounded-lg p-4 ${
              statefulResult.invariantPreserved
                ? 'bg-emerald-950/50 text-emerald-300'
                : 'bg-red-950/50 text-red-300'
            }`}>
              <div className="flex items-center gap-2 font-medium">
                {statefulResult.invariantPreserved ? (
                  <CheckCircle2 size={18} />
                ) : (
                  <XCircle size={18} />
                )}
                Security invariant {statefulResult.invariantPreserved
                  ? 'preserved'
                  : 'violated'}
              </div>
              <p className="mt-1 text-sm opacity-80">
                {statefulResult.invariantPreserved
                  ? 'Unauthorized approval was rejected and payment state remained unchanged.'
                  : 'Unauthorized approval changed the payment state. The expected security invariant was violated.'}
              </p>
            </div>

            <div>
              <h3 className="mb-3 text-sm font-semibold text-white">
                Execution steps
              </h3>

              <div className="space-y-3">
                {statefulResult.steps.map((step, index) => (
                  <div
                    key={step.testId}
                    className="rounded-lg border border-white/10 p-4"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div>
                        <p className="text-xs text-gray-500">
                          Step {index + 1}
                        </p>
                        <p className="mt-1 font-medium text-white">
                          {step.testId}
                        </p>
                      </div>

                      <span className={`flex items-center gap-1 text-sm ${
                        step.passed ? 'text-emerald-400' : 'text-red-400'
                      }`}>
                        {step.passed ? (
                          <CheckCircle2 size={16} />
                        ) : (
                          <XCircle size={16} />
                        )}
                        {step.passed ? 'Passed' : 'Failed'}
                      </span>
                    </div>

                    <p className="mt-2 text-sm text-gray-400">
                      {step.identity} → {step.endpoint}
                    </p>

                    <div className="mt-3 flex flex-wrap gap-5 text-xs text-gray-400">
                      <span>Expected: {step.expectedStatus}</span>
                      <span>Actual: {step.actualStatus}</span>
                    </div>

                    <details className="mt-3">
                      <summary className="cursor-pointer text-xs text-violet-400">
                        View response evidence
                      </summary>
                      <pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-all rounded-lg bg-black/40 p-3 text-xs text-gray-300">
                        {step.responseBody}
                      </pre>
                    </details>
                  </div>
                ))}
              </div>
            </div>

            <div className="rounded-lg border border-white/10 p-4">
              <h3 className="text-sm font-semibold text-white">
                Finding persistence
              </h3>

              {statefulFindings.length === 0 ? (
                <p className="mt-2 text-sm text-gray-400">
                  No business-logic findings were generated for this execution.
                </p>
              ) : (
                <div className="mt-3 space-y-3">
                  {statefulFindings.map((finding) => (
                    <div
                      key={finding.findingId}
                      className="rounded-lg bg-red-950/40 p-3"
                    >
                      <div className="flex flex-wrap justify-between gap-2">
                        <span className="font-medium text-white">
                          {finding.findingId}
                        </span>
                        <span className="text-xs font-semibold text-red-400">
                          {finding.severity} · {finding.vulnerabilityType}
                        </span>
                      </div>
                      <p className="mt-2 text-sm text-gray-300">
                        {finding.description}
                      </p>
                      <p className="mt-2 text-xs text-gray-500">
                        Identity: {finding.identity}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </section>

      <section className="rounded-xl border border-white/10 bg-[#111116] p-6">
        <h2 className="font-semibold text-white">
          Authorization execution results
        </h2>

        {results.length === 0 ? (
          <p className="mt-4 text-sm text-gray-500">
            No authorization execution results yet.
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
