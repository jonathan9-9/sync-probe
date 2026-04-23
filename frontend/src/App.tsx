import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";

type ScanJobStatus = "RUNNING" | "SUCCEEDED" | "FAILED";
type FileStatus = "HEALTHY" | "STALE" | "SYNCING";

type Suggestion = {
  text?: string;
};

type ScanResult = {
  proposedFixes?: Suggestion[];
};

type ScanStartResponse = {
  scanId: string;
};

type ScanStatusResponse = {
  status: ScanJobStatus;
  result?: ScanResult;
  error?: string;
};

type FileRow = {
  fileName: string;
  status: FileStatus;
  score: number | null;
  fixText: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const POLL_INTERVAL_MS = 2500;

const filePathRegex =
  /([A-Za-z0-9_\-./]+\.(md|markdown|txt|adoc|rst|java|kt|js|ts|tsx|jsx|py|go|rb|rs|cs|cpp|c|h))/gi;

function extractCandidateFileNames(text: string): string[] {
  const matches = text.match(filePathRegex);
  if (!matches || matches.length === 0) {
    return [];
  }
  return matches;
}

function createRowsFromSuggestions(suggestions: Suggestion[]): FileRow[] {
  if (suggestions.length === 0) {
    return [
      {
        fileName: "Repository Documentation",
        status: "HEALTHY",
        score: 100,
        fixText: "No stale docs detected. Everything looks in sync.",
      },
    ];
  }

  const rows: FileRow[] = [];
  let fallbackIndex = 1;
  const seenFiles = new Set<string>();

  for (const suggestion of suggestions) {
    const fixText = suggestion.text?.trim() || "No suggestion text provided.";
    const fileNames = extractCandidateFileNames(fixText);

    if (fileNames.length === 0) {
      rows.push({
        fileName: `Unmapped Suggestion ${fallbackIndex}`,
        status: "STALE",
        score: null,
        fixText,
      });
      fallbackIndex += 1;
      continue;
    }

    for (const fileName of fileNames) {
      if (seenFiles.has(fileName)) {
        continue;
      }
      seenFiles.add(fileName);
      rows.push({
        fileName,
        status: "STALE",
        score: null,
        fixText,
      });
    }
  }

  return rows.length > 0
    ? rows
    : [
        {
          fileName: "Repository Documentation",
          status: "STALE",
          score: null,
          fixText: suggestions[0]?.text?.trim() || "No suggestion text provided.",
        },
      ];
}

async function startScan(repoUrl: string): Promise<ScanStartResponse> {
  const response = await fetch(`${API_BASE_URL}/api/scans`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ repoUrl }),
  });

  if (!response.ok) {
    throw new Error(`Failed to start scan (${response.status})`);
  }

  return (await response.json()) as ScanStartResponse;
}

async function fetchScanStatus(scanId: string): Promise<ScanStatusResponse> {
  const response = await fetch(`${API_BASE_URL}/api/scans/${scanId}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch scan status (${response.status})`);
  }
  return (await response.json()) as ScanStatusResponse;
}

function StatusPill({ status }: { status: FileStatus }) {
  if (status === "HEALTHY") {
    return (
      <span className="inline-flex rounded-full border border-emerald-300 bg-emerald-100 px-3 py-1 text-xs font-semibold tracking-wide text-emerald-700">
        HEALTHY
      </span>
    );
  }

  if (status === "SYNCING") {
    return (
      <span className="inline-flex rounded-full border border-amber-300 bg-amber-100 px-3 py-1 text-xs font-semibold tracking-wide text-amber-700">
        SYNCING
      </span>
    );
  }

  return (
    <span className="inline-flex rounded-full border border-rose-300 bg-rose-100 px-3 py-1 text-xs font-semibold tracking-wide text-rose-700">
      STALE
    </span>
  );
}

function ActionButton({
  row,
  onSelect,
}: {
  row: FileRow;
  onSelect: (text: string) => void;
}) {
  if (row.status === "SYNCING") {
    return (
      <button
        type="button"
        disabled
        className="min-w-28 rounded-lg border border-slate-300 bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-500"
      >
        Please Wait
      </button>
    );
  }

  if (row.status === "HEALTHY") {
    return (
      <button
        type="button"
        onClick={() => onSelect(row.fixText)}
        className="min-w-28 rounded-lg border border-cyan-300 bg-cyan-50 px-3 py-1.5 text-xs font-semibold text-cyan-700 transition hover:bg-cyan-100"
      >
        VIEW DOCS
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={() => onSelect(row.fixText)}
      className="min-w-28 rounded-lg border border-indigo-300 bg-indigo-50 px-3 py-1.5 text-xs font-semibold text-indigo-700 transition hover:bg-indigo-100"
    >
      HEAL DOCS
    </button>
  );
}

export default function App() {
  const [repoUrl, setRepoUrl] = useState("");
  const [scanId, setScanId] = useState<string | null>(null);
  const [scanStatus, setScanStatus] = useState<ScanJobStatus | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [proposedFix, setProposedFix] = useState(
    "Start a scan to generate AI proposed fixes."
  );
  const [feedbackMessage, setFeedbackMessage] = useState("");
  const [rows, setRows] = useState<FileRow[]>([
    {
      fileName: "Repository",
      status: "SYNCING",
      score: null,
      fixText: "Run a scan to begin analysis.",
    },
  ]);

  useEffect(() => {
    if (!scanId || scanStatus !== "RUNNING") {
      return;
    }

    const timer = window.setInterval(async () => {
      try {
        const payload = await fetchScanStatus(scanId);
        setScanStatus(payload.status);

        if (payload.status === "SUCCEEDED") {
          const suggestions = payload.result?.proposedFixes ?? [];
          const nextRows = createRowsFromSuggestions(suggestions);
          setRows(nextRows);
          setProposedFix(nextRows[0]?.fixText || "No proposed fixes were returned.");
          setFeedbackMessage("");
          window.clearInterval(timer);
        }

        if (payload.status === "FAILED") {
          setErrorMessage(payload.error || "Scan failed. Please try again.");
          setRows([
            {
              fileName: "Repository",
              status: "STALE",
              score: null,
              fixText: payload.error || "Scan failed.",
            },
          ]);
          setProposedFix(payload.error || "Scan failed.");
          window.clearInterval(timer);
        }
      } catch (error) {
        setErrorMessage(
          error instanceof Error ? error.message : "Unexpected polling error."
        );
        window.clearInterval(timer);
      }
    }, POLL_INTERVAL_MS);

    return () => {
      window.clearInterval(timer);
    };
  }, [scanId, scanStatus]);

  const handleScan = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const trimmed = repoUrl.trim();
    if (!trimmed) {
      setErrorMessage("Please enter a repository URL.");
      return;
    }

    setErrorMessage("");
    setFeedbackMessage("");
    setIsSubmitting(true);
    setRows([
      {
        fileName: "Repository",
        status: "SYNCING",
        score: null,
        fixText: "Scan in progress. Please wait.",
      },
    ]);
    setProposedFix("Analyzing repository and generating AI suggestions...");

    try {
      const startPayload = await startScan(trimmed);
      setScanId(startPayload.scanId);
      setScanStatus("RUNNING");
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Could not start repository scan."
      );
      setScanStatus("FAILED");
    } finally {
      setIsSubmitting(false);
    }
  };

  const headerStatusLabel = useMemo(() => {
    if (scanStatus === "RUNNING") {
      return "Live Scan Running";
    }
    if (scanStatus === "SUCCEEDED") {
      return "Latest Scan Completed";
    }
    if (scanStatus === "FAILED") {
      return "Scan Failed";
    }
    return "Ready";
  }, [scanStatus]);

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_10%_10%,#ecfeff,transparent_45%),radial-gradient(circle_at_90%_0%,#eef2ff,transparent_35%),linear-gradient(180deg,#f8fafc_0%,#eff6ff_100%)] px-4 py-10 text-slate-900 sm:px-8">
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6">
        <header className="rounded-2xl border border-slate-200/70 bg-white/80 p-6 shadow-[0_14px_40px_rgba(15,23,42,0.08)] backdrop-blur">
          <div className="mb-3 inline-flex rounded-full border border-cyan-200 bg-cyan-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-cyan-700">
            {headerStatusLabel}
          </div>
          <h1 className="text-balance text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
            SyncProbe: Self-Healing Dashboard
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            Scan docs against code drift, surface stale areas, and generate AI-backed fixes.
          </p>

          <form
            onSubmit={handleScan}
            className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center"
          >
            <input
              value={repoUrl}
              onChange={(event) => setRepoUrl(event.target.value)}
              placeholder="https://github.com/owner/repo.git"
              className="h-12 flex-1 rounded-xl border border-slate-300 bg-white px-4 text-sm outline-none ring-cyan-500 transition placeholder:text-slate-400 focus:ring-2"
            />
            <button
              type="submit"
              disabled={isSubmitting || scanStatus === "RUNNING"}
              className="h-12 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 px-6 text-sm font-semibold text-white shadow-[0_10px_20px_rgba(14,116,144,0.3)] transition hover:from-cyan-600 hover:to-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {scanStatus === "RUNNING" ? "Scanning..." : "Scan Repository"}
            </button>
          </form>

          {errorMessage ? (
            <p className="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
              {errorMessage}
            </p>
          ) : null}
        </header>

        <section className="overflow-hidden rounded-2xl border border-slate-200/70 bg-white/85 shadow-[0_14px_40px_rgba(15,23,42,0.06)] backdrop-blur">
          <div className="border-b border-slate-200 px-5 py-4">
            <h2 className="text-lg font-semibold text-slate-900">Repository Health Matrix</h2>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full text-left">
              <thead className="bg-slate-50">
                <tr className="text-xs uppercase tracking-[0.12em] text-slate-600">
                  <th className="px-5 py-3 font-semibold">File Name</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 font-semibold">Score</th>
                  <th className="px-5 py-3 font-semibold">Action</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.fileName} className="border-t border-slate-100 text-sm">
                    <td className="px-5 py-4 font-medium text-slate-800">{row.fileName}</td>
                    <td className="px-5 py-4">
                      <StatusPill status={row.status} />
                    </td>
                    <td className="px-5 py-4 font-semibold text-slate-700">
                      {row.score === null ? "--%" : `${Math.round(row.score)}%`}
                    </td>
                    <td className="px-5 py-4">
                      <ActionButton row={row} onSelect={setProposedFix} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200/70 bg-white/85 p-5 shadow-[0_14px_40px_rgba(15,23,42,0.06)] backdrop-blur">
          <h2 className="mb-3 text-lg font-semibold text-slate-900">Proposed Fix</h2>
          <div className="min-h-40 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700">
            {proposedFix}
          </div>

          <div className="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => {
                setFeedbackMessage("Proposed fix rejected.");
                setProposedFix("Fix rejected. Select another suggestion or run a new scan.");
              }}
              className="rounded-xl border border-slate-300 bg-white px-5 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100"
            >
              REJECT
            </button>
            <button
              type="button"
              onClick={() => {
                setFeedbackMessage("Fix committed (UI action only).");
              }}
              className="rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 px-5 py-2 text-sm font-semibold text-white shadow-[0_10px_20px_rgba(5,150,105,0.28)] transition hover:from-emerald-600 hover:to-teal-700"
            >
              COMMIT FIX
            </button>
          </div>

          {feedbackMessage ? (
            <p className="mt-3 text-sm text-slate-600">{feedbackMessage}</p>
          ) : null}
        </section>
      </div>
    </div>
  );
}
