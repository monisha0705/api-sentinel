
import { useEffect, useState } from 'react'
import SpecificationsPage, { type EndpointSummary } from './SpecificationsPage'
import SecurityGraphPage from './SecurityGraphPage'
import TestRunsPage from './TestRunsPage'
import FindingsPage from './FindingsPage'
import api from './api/client'

import {
  Shield,
  LayoutDashboard,
  FileCode,
  Network,
  Play,
  Bug,
  Settings,
  Search,
  Bell,
  ChevronDown,
  Activity,
  Globe,
  CheckCircle2,
  AlertTriangle,
  Clock,
} from 'lucide-react'

const navigation = [
  { name: 'Overview', icon: LayoutDashboard },
  { name: 'Specifications', icon: FileCode },
  { name: 'Security Graph', icon: Network },
  { name: 'Test Runs', icon: Play },
  { name: 'Findings', icon: Bug },
]

interface SecurityFinding {
  id: number
  findingId: string
  vulnerabilityType: string
  severity: string
  identity: string
  endpoint: string
  detectedAt: string
}

function App() {
  const [activePage, setActivePage] = useState('Overview')
  const [findings, setFindings] = useState<SecurityFinding[]>([])
  const [endpoints, setEndpoints] = useState<EndpointSummary[]>([])
  useEffect(() => {
    api.get<SecurityFinding[]>('/findings')
      .then((response) => setFindings(response.data))
      .catch((error) => console.error('Failed to load overview findings:', error))
  }, [])

  return (
    <div className="flex min-h-screen bg-[#09090b] text-gray-200">
      <aside className="flex w-64 shrink-0 flex-col border-r border-white/10 bg-[#0d0d12]">
        <div className="flex h-20 items-center gap-3 border-b border-white/10 px-6">
          <div className="rounded-xl bg-violet-500/15 p-2.5">
            <Shield className="h-6 w-6 text-violet-400" />
          </div>

          <div>
            <h1 className="text-lg font-bold tracking-wide text-white">
              API Sentinel
            </h1>
            <p className="text-xs text-gray-500">
              REST API Security Platform
            </p>
          </div>
        </div>

        <div className="px-4 pt-8">
          <p className="mb-3 px-3 text-xs font-semibold uppercase tracking-widest text-gray-600">
            Workspace
          </p>

          <nav className="space-y-1">
            {navigation.map((item) => {
              const Icon = item.icon
              const active = activePage === item.name

              return (
                <button
                  key={item.name}
                  onClick={() => setActivePage(item.name)}
                  className={`flex w-full items-center gap-3 rounded-lg px-3 py-3 text-sm transition ${
                    active
                      ? 'bg-violet-500/15 font-medium text-violet-300'
                      : 'text-gray-400 hover:bg-white/5 hover:text-white'
                  }`}
                >
                  <Icon size={18} />
                  {item.name}
                </button>
              )
            })}
          </nav>
        </div>

        <div className="mt-auto border-t border-white/10 p-5">
          <div className="flex items-center gap-3 rounded-lg bg-white/[0.03] p-3">
            <div className="rounded-lg bg-emerald-500/10 p-2">
              <Activity className="h-5 w-5 text-emerald-400" />
            </div>

            <div>
              <p className="text-xs font-medium text-white">
                Scanner status
              </p>
              <p className="mt-1 text-xs text-emerald-400">
                Backend connected
              </p>
            </div>
          </div>

          <button className="mt-4 flex items-center gap-3 px-3 text-sm text-gray-500 hover:text-white">
            <Settings size={17} />
            Settings
          </button>
        </div>
      </aside>

      <main className="min-w-0 flex-1">
        <header className="flex h-20 items-center justify-between border-b border-white/10 px-8">
          <div>
            <p className="text-xs text-gray-500">
              Workspace / {activePage}
            </p>
            <h2 className="mt-1 text-lg font-semibold text-white">
              {activePage}
            </h2>
          </div>

          <div className="flex items-center gap-5">
            <div className="hidden items-center gap-2 rounded-lg border border-white/10 bg-white/[0.03] px-3 py-2 md:flex">
              <Globe size={15} className="text-gray-500" />
              <span className="text-xs text-gray-400">
                localhost:8081
              </span>
              <span className="h-2 w-2 rounded-full bg-emerald-400" />
            </div>

            <button className="text-gray-400 hover:text-white">
              <Search size={19} />
            </button>

            <button className="text-gray-400 hover:text-white">
              <Bell size={19} />
            </button>

            <div className="flex items-center gap-2 border-l border-white/10 pl-5">
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-violet-500/20 text-sm font-semibold text-violet-300">
                AS
              </div>
              <ChevronDown size={15} className="text-gray-500" />
            </div>
          </div>
        </header>

        <div className="mx-auto max-w-7xl space-y-8 p-8">

          {/* OVERVIEW PAGE */}
          {activePage === 'Overview' ? (
            <>
              <section className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <h3 className="text-2xl font-semibold text-white">
                    Security overview
                  </h3>
                  <p className="mt-2 text-sm text-gray-500">
                    Monitor API security testing and detected vulnerabilities.
                  </p>
                </div>

                <button
                  onClick={() => setActivePage('Test Runs')}
                  className="rounded-lg bg-violet-600 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-violet-500"
                >
                  + New test run
                </button>
              </section>

              <section className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
                {[
                  {
                    label: 'Specifications',
                    value: '1',
                    note: 'API spec parsed',
                    icon: FileCode,
                    color: 'text-blue-400',
                    bg: 'bg-blue-400/10',
                  },
                  {
                    label: 'Endpoints',
                    value: '3',
                    note: 'Discovered endpoints',
                    icon: Globe,
                    color: 'text-violet-400',
                    bg: 'bg-violet-400/10',
                  },
                  {
                    label: 'Tests executed',
                    value: '2',
                    note: 'Authorization tests',
                    icon: CheckCircle2,
                    color: 'text-emerald-400',
                    bg: 'bg-emerald-400/10',
                  },
                  {
                    label: 'Vulnerabilities',
                    value: findings.length.toString(),
                    note: 'Persisted findings',
                    icon: AlertTriangle,
                    color: 'text-red-400',
                    bg: 'bg-red-400/10',
                  },
                ].map((stat) => {
                  const Icon = stat.icon

                  return (
                    <div
                      key={stat.label}
                      className="rounded-xl border border-white/10 bg-[#111116] p-5"
                    >
                      <div className="flex items-center justify-between">
                        <p className="text-sm text-gray-400">
                          {stat.label}
                        </p>
                        <div className={`rounded-lg p-2 ${stat.bg}`}>
                          <Icon size={18} className={stat.color} />
                        </div>
                      </div>

                      <p className="mt-5 text-3xl font-semibold text-white">
                        {stat.value}
                      </p>
                      <p className="mt-2 text-xs text-gray-500">
                        {stat.note}
                      </p>
                    </div>
                  )
                })}
              </section>

              <section className="grid gap-5 xl:grid-cols-3">
                <div className="rounded-xl border border-white/10 bg-[#111116] p-6 xl:col-span-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="font-semibold text-white">
                        Recent findings
                      </h3>
                      <p className="mt-1 text-xs text-gray-500">
                        Latest detected security issues
                      </p>
                    </div>

                    <button
                      onClick={() => setActivePage('Findings')}
                      className="text-xs text-violet-400 hover:text-violet-300"
                    >
                      View all
                    </button>
                  </div>

                  <div className="mt-6 overflow-x-auto">
                    <table className="w-full text-left text-sm">
                      <thead className="border-b border-white/10 text-xs text-gray-500">
                        <tr>
                          <th className="pb-3 font-medium">Finding</th>
                          <th className="pb-3 font-medium">Identity</th>
                          <th className="pb-3 font-medium">Severity</th>
                          <th className="pb-3 font-medium">Status</th>
                        </tr>
                      </thead>

                      <tbody>
                        {findings.slice(0, 5).map((finding) => (
                          <tr
                            key={finding.id}
                            className="border-b border-white/5 last:border-0"
                          >
                            <td className="py-4">
                              <p className="font-medium text-gray-200">
                                {finding.vulnerabilityType}
                              </p>
                              <p className="mt-1 text-xs text-gray-500">
                                {finding.endpoint}
                              </p>
                            </td>

                            <td className="py-4 text-gray-400">
                              {finding.identity}
                            </td>

                            <td className="py-4">
                              <span className="rounded-md bg-red-500/10 px-2 py-1 text-xs font-semibold text-red-400">
                                {finding.severity}
                              </span>
                            </td>

                            <td className="py-4 text-xs text-emerald-400">
                              Confirmed
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                <div className="rounded-xl border border-white/10 bg-[#111116] p-6">
                  <h3 className="font-semibold text-white">
                    Target environment
                  </h3>
                  <p className="mt-1 text-xs text-gray-500">
                    Current benchmark configuration
                  </p>

                  <div className="mt-6 space-y-5">
                    <div>
                      <p className="text-xs text-gray-500">Target API</p>
                      <p className="mt-1 text-sm text-gray-200">
                        Sentinel Bank
                      </p>
                      <p className="mt-1 text-xs text-gray-500">
                        http://localhost:8081
                      </p>
                    </div>

                    <div className="border-t border-white/10 pt-4">
                      <p className="text-xs text-gray-500">
                        Authorization mode
                      </p>
                      <p className="mt-1 text-sm text-emerald-400">
                        SECURE
                      </p>
                    </div>

                    <div className="border-t border-white/10 pt-4">
                      <p className="text-xs text-gray-500">
                        Last activity
                      </p>
                      <div className="mt-1 flex items-center gap-2 text-sm text-gray-300">
                        <Clock size={14} className="text-gray-500" />
                        Findings persisted
                      </div>
                    </div>
                  </div>
                </div>
              </section>
            </>
          ) : activePage === 'Specifications' ? (
            <SpecificationsPage
              endpoints={endpoints}
              onEndpointsParsed={setEndpoints}
            />
          ) : activePage === 'Security Graph' ? (
  <SecurityGraphPage endpoints={endpoints} />
) : activePage === 'Test Runs' ? (
  <TestRunsPage endpoints={endpoints} />
) : activePage === 'Findings' ? (
  <FindingsPage />
) : (
            <section className="rounded-xl border border-white/10 bg-[#111116] p-8">
              <h3 className="text-xl font-semibold text-white">
                {activePage}
              </h3>
              <p className="mt-2 text-sm text-gray-500">
                This section will be connected to the API Sentinel backend next.
              </p>
            </section>
          )}

        </div>
      </main>
    </div>
  )
}

export default App
