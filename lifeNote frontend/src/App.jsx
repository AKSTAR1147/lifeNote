import React, { useEffect, useMemo, useState } from "react";
import {
  Archive,
  ArchiveRestore,
  Clock3,
  Diff,
  Heart,
  LogOut,
  Pin,
  Plus,
  RefreshCw,
  Save,
  Search,
  Star,
  Trash2,
  Undo2,
  UserPlus,
  Settings,
  Activity,
  X,
  CheckCircle,
  AlertCircle,
  User
} from "lucide-react";
import { api } from "./api";

const emptyNote = {
  title: "",
  content: "",
  category: "",
  tags: "",
  commitMessage: "",
  pinned: false,
  favorite: false
};

function parseTags(value) {
  if (!value) return [];
  return value
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean);
}

function formatDate(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function formatAction(action) {
  if (!action) return "";
  return action
    .split("_")
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(" ");
}

function parseUserAgent(userAgent) {
  if (!userAgent) return "Browser Client";
  if (userAgent.includes("Firefox")) return "Firefox Browser";
  if (userAgent.includes("Edge")) return "Edge Browser";
  if (userAgent.includes("Chrome")) return "Chrome Browser";
  if (userAgent.includes("Safari")) return "Safari Browser";
  if (userAgent.includes("Postman")) return "Postman Client";
  if (userAgent.includes("curl")) return "Curl Tool";
  return userAgent.split(" ")[0] || "Web Client";
}

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem("lifenote.token") || "");
  const [user, setUser] = useState(null);
  const [authMode, setAuthMode] = useState("login");
  const [authForm, setAuthForm] = useState({ displayName: "", email: "", password: "" });
  const [notes, setNotes] = useState([]);
  const [selectedNote, setSelectedNote] = useState(null);
  const [noteForm, setNoteForm] = useState(emptyNote);
  const [versions, setVersions] = useState([]);
  const [diffResult, setDiffResult] = useState(null);
  const [query, setQuery] = useState("");
  const [includeArchived, setIncludeArchived] = useState(false);
  const [diffFrom, setDiffFrom] = useState("");
  const [diffTo, setDiffTo] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Additional Premium Features State
  const [showSettings, setShowSettings] = useState(false);
  const [settingsForm, setSettingsForm] = useState({ displayName: "", newPassword: "" });
  const [showAudit, setShowAudit] = useState(false);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loadingAudit, setLoadingAudit] = useState(false);
  const [searchMode, setSearchMode] = useState("notes"); // 'notes' or 'history'
  const [historyResults, setHistoryResults] = useState([]);
  const [toast, setToast] = useState({ show: false, message: "", type: "success" });

  const authTitle = authMode === "login" ? "Sign In" : "Create Account";

  const sortedVersions = useMemo(
    () => [...versions].sort((a, b) => b.versionNumber - a.versionNumber),
    [versions]
  );

  // Ensure theme settings are cleared from localStorage
  useEffect(() => {
    localStorage.removeItem("lifenote.theme");
    document.documentElement.classList.remove("light-mode");
  }, []);

  // Load audit trail logs when modal is opened
  useEffect(() => {
    if (showAudit && token) {
      loadAuditTrail();
    }
  }, [showAudit]);

  // Toast Notification Auto-dismiss
  useEffect(() => {
    if (toast.show) {
      const timer = setTimeout(() => {
        setToast((prev) => ({ ...prev, show: false }));
      }, 3500);
      return () => clearTimeout(timer);
    }
  }, [toast.show]);

  useEffect(() => {
    if (!token) return;
    newNote(false);
    loadWorkspace(token).catch((err) => {
      setError(err.message);
      logout();
    });
  }, [token]);

  function triggerToast(message, type = "success") {
    setToast({ show: true, message, type });
  }

  async function loadWorkspace(activeToken = token) {
    setLoading(true);
    try {
      const profile = await api("/api/users/me", { token: activeToken });
      setUser(profile);
      setSettingsForm({ displayName: profile.displayName, newPassword: "" });
      await loadNotes(activeToken);
    } finally {
      setLoading(false);
    }
  }

  async function loadNotes(activeToken = token) {
    if (searchMode === "history") {
      await searchVersionsHistory();
      return;
    }
    
    const params = new URLSearchParams();
    if (query.trim()) params.set("q", query.trim());
    params.set("includeArchived", String(includeArchived));
    const data = await api(`/api/notes?${params.toString()}`, { token: activeToken });
    setNotes(data);
    if (selectedNote) {
      const refreshed = data.find((note) => note.id === selectedNote.id);
      if (refreshed) {
        selectNote(refreshed);
      } else {
        newNote(false);
      }
    }
  }

  async function loadVersions(noteId) {
    const data = await api(`/api/notes/${noteId}/versions`, { token });
    setVersions(data);
    if (data.length >= 2) {
      setDiffTo(String(data[0].versionNumber));
      setDiffFrom(String(data[1].versionNumber));
    } else {
      setDiffTo("");
      setDiffFrom("");
    }
  }

  async function loadAuditTrail() {
    setLoadingAudit(true);
    try {
      const data = await api("/api/audit/me?limit=50", { token });
      setAuditLogs(data || []);
    } catch (err) {
      triggerToast(err.message, "error");
    } finally {
      setLoadingAudit(false);
    }
  }

  async function searchVersionsHistory() {
    if (!query.trim()) {
      setHistoryResults([]);
      return;
    }
    setLoading(true);
    setError("");
    try {
      const data = await api(`/api/notes/versions/search?q=${encodeURIComponent(query.trim())}`, { token });
      setHistoryResults(data || []);
      if (data.length === 0) {
        triggerToast("No note versions matched your query.", "error");
      } else {
        triggerToast(`Found ${data.length} version match(es).`);
      }
    } catch (err) {
      setError(err.message);
      triggerToast(err.message, "error");
    } finally {
      setLoading(false);
    }
  }

  async function handleAuth(event) {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      const path = authMode === "login" ? "/api/auth/login" : "/api/auth/register";
      const payload =
        authMode === "login"
          ? { email: authForm.email, password: authForm.password }
          : authForm;
      const data = await api(path, { method: "POST", body: payload });
      localStorage.setItem("lifenote.token", data.accessToken);
      setToken(data.accessToken);
      setUser(data.user);
      triggerToast(authMode === "login" ? "Signed in successfully!" : "Account created successfully!");
    } catch (err) {
      setError(err.message);
      triggerToast(err.message, "error");
    } finally {
      setLoading(false);
    }
  }

  async function handleProfileUpdate(event) {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      if (settingsForm.displayName.trim().length < 2) {
        throw new Error("Display name must be at least 2 characters.");
      }
      const payload = {
        displayName: settingsForm.displayName.trim()
      };
      if (settingsForm.newPassword) {
        if (settingsForm.newPassword.length < 8) {
          throw new Error("Password must be at least 8 characters.");
        }
        payload.password = settingsForm.newPassword;
      }
      
      const updated = await api("/api/users/me", { token, method: "PUT", body: payload });
      setUser(updated);
      setSettingsForm((prev) => ({ ...prev, newPassword: "" }));
      triggerToast("Profile updated successfully!");
      setShowSettings(false);
    } catch (err) {
      setError(err.message);
      triggerToast(err.message, "error");
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    localStorage.removeItem("lifenote.token");
    setToken("");
    setUser(null);
    setNotes([]);
    setSelectedNote(null);
    setVersions([]);
    setDiffResult(null);
    setHistoryResults([]);
    triggerToast("Logged out successfully.");
  }

  function newNote(showToast = true) {
    setSelectedNote(null);
    setNoteForm(emptyNote);
    setVersions([]);
    setDiffResult(null);
    if (showToast) triggerToast("Draft initialized.");
  }

  function selectNote(note) {
    if (!note) {
      newNote(false);
      return;
    }
    setSelectedNote(note);
    setNoteForm({
      title: note.title,
      content: note.content,
      category: note.category || "",
      tags: (note.tags || []).join(", "),
      commitMessage: "",
      pinned: note.pinned,
      favorite: note.favorite
    });
    setDiffResult(null);
    loadVersions(note.id).catch((err) => setError(err.message));
  }

  async function selectHistoryItem(item) {
    setLoading(true);
    setError("");
    try {
      // Resolve the parent active note
      const activeNote = await api(`/api/notes/${item.noteId}`, { token });
      setSelectedNote(activeNote);
      // Load versions list for note
      const historyList = await api(`/api/notes/${item.noteId}/versions`, { token });
      setVersions(historyList);
      
      if (historyList.length >= 2) {
        setDiffTo(String(historyList[0].versionNumber));
        setDiffFrom(String(historyList[1].versionNumber));
      } else {
        setDiffTo("");
        setDiffFrom("");
      }

      // Pre-fill editor with historical snapshot values
      setNoteForm({
        title: item.title,
        content: item.content,
        category: item.category || "",
        tags: (item.tags || []).join(", "),
        commitMessage: `Restoring version ${item.versionNumber}`,
        pinned: activeNote.pinned,
        favorite: activeNote.favorite
      });
      setDiffResult(null);
      triggerToast(`Loaded version v${item.versionNumber} of "${item.title}" into editor.`);
    } catch (err) {
      setError("Active note is not found: " + err.message);
      triggerToast("Could not load note versions.", "error");
    } finally {
      setLoading(false);
    }
  }

  async function saveNote(event) {
    event.preventDefault();
    setError("");
    setLoading(true);
    const payload = {
      title: noteForm.title,
      content: noteForm.content,
      category: noteForm.category,
      tags: parseTags(noteForm.tags),
      commitMessage: noteForm.commitMessage,
      pinned: noteForm.pinned,
      favorite: noteForm.favorite
    };

    try {
      const saved = selectedNote
        ? await api(`/api/notes/${selectedNote.id}`, { token, method: "PUT", body: payload })
        : await api("/api/notes", { token, method: "POST", body: payload });
      await loadNotes();
      selectNote(saved);
      setNoteForm((current) => ({ ...current, commitMessage: "" }));
      triggerToast(selectedNote ? "Note updated successfully!" : "Note created successfully!");
    } catch (err) {
      setError(err.message);
      triggerToast(err.message, "error");
    } finally {
      setLoading(false);
    }
  }

  async function mutateNote(path, method = "POST", body) {
    if (!selectedNote) return;
    setError("");
    setLoading(true);
    try {
      const updated = await api(path, { token, method, body });
      await loadNotes();
      if (updated) {
        selectNote(updated);
        if (path.includes("/pin")) {
          triggerToast(body.enabled ? "Note pinned!" : "Note unpinned!");
        } else if (path.includes("/favorite")) {
          triggerToast(body.enabled ? "Added to favorites!" : "Removed from favorites!");
        } else if (path.includes("/archive")) {
          triggerToast("Note archived!");
        } else if (path.includes("/restore")) {
          triggerToast("Note restored from archive!");
        } else {
          triggerToast("Note state updated.");
        }
      }
    } catch (err) {
      setError(err.message);
      triggerToast(err.message, "error");
    } finally {
      setLoading(false);
    }
  }

  async function deleteNote() {
    if (!selectedNote) return;
    if (!confirm("Are you sure you want to permanently delete this note? This deletes all version history and cannot be undone.")) return;
    setError("");
    setLoading(true);
    try {
      await api(`/api/notes/${selectedNote.id}`, { token, method: "DELETE" });
      newNote();
      await loadNotes();
      triggerToast("Note deleted successfully.");
    } catch (err) {
      setError(err.message);
      triggerToast(err.message, "error");
    } finally {
      setLoading(false);
    }
  }

  async function compareVersions() {
    if (!selectedNote || !diffFrom || !diffTo) return;
    setError("");
    try {
      const data = await api(`/api/notes/${selectedNote.id}/diff?from=${diffFrom}&to=${diffTo}`, { token });
      setDiffResult(data);
      triggerToast("Differences loaded.");
    } catch (err) {
      setError(err.message);
      triggerToast(err.message, "error");
    }
  }

  async function restoreVersion(versionNumber) {
    if (!selectedNote) return;
    await mutateNote(`/api/notes/${selectedNote.id}/versions/${versionNumber}/restore`);
    triggerToast(`Restored version v${versionNumber}`);
  }

  // Handle switching search tabs
  function handleSearchModeChange(mode) {
    setSearchMode(mode);
    setNotes([]);
    setHistoryResults([]);
    setError("");
    if (mode === "notes") {
      loadNotes().catch((err) => setError(err.message));
    } else {
      searchVersionsHistory();
    }
  }

  if (!token) {
    return (
      <main className="auth-shell">
        <section className="auth-panel">
          <div className="brand-row">
            <div className="brand-mark">LN</div>
            <div>
              <h1>LifeNote</h1>
              <p>Personal knowledge with immutable history.</p>
            </div>
          </div>

          <form onSubmit={handleAuth} className="auth-form">
            <div className="form-title">
              <h2>{authTitle}</h2>
              <button
                type="button"
                className="ghost-button"
                onClick={() => setAuthMode(authMode === "login" ? "register" : "login")}
              >
                {authMode === "login" ? "Register" : "Login"}
              </button>
            </div>

            {authMode === "register" && (
              <label>
                Name
                <input
                  value={authForm.displayName}
                  onChange={(event) => setAuthForm({ ...authForm, displayName: event.target.value })}
                  autoComplete="name"
                  placeholder="Your display name"
                  required
                />
              </label>
            )}

            <label>
              Email
              <input
                type="email"
                value={authForm.email}
                onChange={(event) => setAuthForm({ ...authForm, email: event.target.value })}
                autoComplete="email"
                placeholder="your@email.com"
                required
              />
            </label>

            <label>
              Password
              <input
                type="password"
                value={authForm.password}
                onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })}
                autoComplete={authMode === "login" ? "current-password" : "new-password"}
                minLength={8}
                placeholder="••••••••"
                required
              />
            </label>

            {error && (
              <div className="error-banner">
                <AlertCircle size={18} />
                <span>{error}</span>
              </div>
            )}

            <button className="primary-button" disabled={loading}>
              {authMode === "login" ? <Star size={18} /> : <UserPlus size={18} />}
              {loading ? "Working..." : authTitle}
            </button>
          </form>
        </section>
        
        {toast.show && (
          <div className={`toast-notification ${toast.type}`}>
            {toast.type === "success" ? <CheckCircle size={18} /> : <AlertCircle size={18} />}
            <span>{toast.message}</span>
          </div>
        )}
      </main>
    );
  }

  return (
    <main className="workspace-shell">
      {/* 1. SIDEBAR PANEL */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <div
            className="user-info"
            onClick={() => {
              setSettingsForm({ displayName: user?.displayName || "", newPassword: "" });
              setShowSettings(true);
            }}
            title="Profile Settings"
          >
            <span className="eyebrow">LifeNote Workspace</span>
            <h1>{user?.displayName || "Loading..."}</h1>
          </div>
          
          <div style={{ display: "flex", gap: "6px" }}>
            <button
              className="icon-button"
              type="button"
              onClick={() => setShowAudit(true)}
              title="Activity Logs"
              aria-label="Activity Logs"
            >
              <Activity size={17} />
            </button>
            <button
              className="icon-button"
              type="button"
              onClick={logout}
              title="Log out"
              aria-label="Log out"
            >
              <LogOut size={17} />
            </button>
          </div>
        </div>

        {/* Tab Selection: Active Notes vs Version Search */}
        <div className="search-mode-tabs">
          <button
            type="button"
            className={`search-mode-tab ${searchMode === "notes" ? "active" : ""}`}
            onClick={() => handleSearchModeChange("notes")}
          >
            Active Notes
          </button>
          <button
            type="button"
            className={`search-mode-tab ${searchMode === "history" ? "active" : ""}`}
            onClick={() => handleSearchModeChange("history")}
          >
            Version History
          </button>
        </div>

        <div className="search-row">
          <Search size={18} />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") loadNotes().catch((err) => setError(err.message));
            }}
            placeholder={searchMode === "notes" ? "Search active notes" : "Search version contents..."}
          />
          <button
            className="icon-button"
            type="button"
            style={{ border: 0, background: "transparent" }}
            onClick={() => loadNotes().catch((err) => setError(err.message))}
            title="Refresh list"
            aria-label="Refresh"
          >
            <RefreshCw size={14} />
          </button>
        </div>

        {searchMode === "notes" && (
          <label className="toggle-row">
            <input
              type="checkbox"
              checked={includeArchived}
              onChange={(event) => {
                setIncludeArchived(event.target.checked);
                // Delay slightly to allow state to settle
                setTimeout(() => loadNotes().catch((err) => setError(err.message)), 50);
              }}
            />
            <span>Include Archived Notes</span>
          </label>
        )}

        {searchMode === "notes" && (
          <button className="new-note-button" type="button" onClick={newNote}>
            <Plus size={18} />
            New Note
          </button>
        )}

        {/* Sidebar Results List */}
        <div className="note-list">
          {searchMode === "notes" ? (
            notes.length === 0 ? (
              <div className="empty-state">No notes found</div>
            ) : (
              notes.map((note) => (
                <button
                  className={`note-item ${selectedNote?.id === note.id ? "active" : ""}`}
                  type="button"
                  key={note.id}
                  onClick={() => selectNote(note)}
                >
                  <div className="note-item-top">
                    <span>{note.title}</span>
                    <span>v{note.currentVersion}</span>
                  </div>
                  <div className="note-meta">
                    {note.category || "Unsorted"} · {formatDate(note.updatedAt)}
                  </div>
                  <div className="tag-row">
                    {note.pinned && <Pin size={13} className="active-icon" />}
                    {note.favorite && <Heart size={13} style={{ fill: "currentColor" }} />}
                    {note.archived && <Archive size={13} />}
                    {(note.tags || []).slice(0, 3).map((tag) => (
                      <span key={tag}>{tag}</span>
                    ))}
                  </div>
                </button>
              ))
            )
          ) : (
            // Historical Search Mode Results
            historyResults.length === 0 ? (
              <div className="empty-state">No matching versions. Type query & press Enter.</div>
            ) : (
              historyResults.map((item) => (
                <button
                  className="note-item history-search-item"
                  type="button"
                  key={item.id}
                  onClick={() => selectHistoryItem(item)}
                >
                  <div className="note-item-top">
                    <span>{item.title}</span>
                    <span style={{ color: "var(--teal)", background: "var(--teal-glow)" }}>
                      v{item.versionNumber}
                    </span>
                  </div>
                  <div className="note-meta" style={{ fontStyle: "italic" }}>
                    Match: "{item.commitMessage || "Edited Note"}"
                  </div>
                  <div className="note-meta">
                    {item.category || "Unsorted"} · {formatDate(item.createdAt)}
                  </div>
                </button>
              ))
            )
          )}
        </div>
      </aside>

      {/* 2. MAIN WORKBENCH PANEL */}
      <section className="editor-area">
        <header className="topbar">
          <div>
            <span className="eyebrow">
              {selectedNote ? (selectedNote.archived ? "Archived Note" : "Editing Note") : "New Draft"}
            </span>
            <h2>{selectedNote ? selectedNote.title : "Untitled Note"}</h2>
          </div>
          
          <div className="toolbar">
            <button
              className={`icon-button ${selectedNote?.pinned ? "active-state" : ""}`}
              type="button"
              disabled={!selectedNote}
              onClick={() => mutateNote(`/api/notes/${selectedNote.id}/pin`, "PATCH", { enabled: !selectedNote.pinned })}
              title={selectedNote?.pinned ? "Unpin Note" : "Pin Note"}
              aria-label="Pin"
            >
              <Pin size={18} className={selectedNote?.pinned ? "active-icon" : ""} />
            </button>
            <button
              className={`icon-button ${selectedNote?.favorite ? "active-state" : ""}`}
              type="button"
              disabled={!selectedNote}
              onClick={() =>
                mutateNote(`/api/notes/${selectedNote.id}/favorite`, "PATCH", { enabled: !selectedNote.favorite })
              }
              title={selectedNote?.favorite ? "Remove from Favorites" : "Mark as Favorite"}
              aria-label="Favorite"
            >
              <Heart size={18} className={selectedNote?.favorite ? "active-icon" : ""} />
            </button>
            <button
              className="icon-button"
              type="button"
              disabled={!selectedNote}
              onClick={() =>
                mutateNote(
                  selectedNote.archived
                    ? `/api/notes/${selectedNote.id}/restore`
                    : `/api/notes/${selectedNote.id}/archive`
                )
              }
              title={selectedNote?.archived ? "Restore Note from Archive" : "Archive Note"}
              aria-label={selectedNote?.archived ? "Restore" : "Archive"}
            >
              {selectedNote?.archived ? <ArchiveRestore size={18} /> : <Archive size={18} />}
            </button>
            <button
              className="icon-button danger"
              type="button"
              disabled={!selectedNote}
              onClick={deleteNote}
              title="Delete Permanently"
              aria-label="Delete"
            >
              <Trash2 size={18} />
            </button>
          </div>
        </header>

        {error && (
          <div className="error-banner" style={{ marginBottom: "16px" }}>
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        <div className="content-grid">
          {/* Note Form */}
          <form className="editor-form" onSubmit={saveNote}>
            <input
              className="title-input"
              value={noteForm.title}
              onChange={(event) => setNoteForm({ ...noteForm, title: event.target.value })}
              placeholder="Title"
              required
            />

            <div className="compact-fields">
              <label>
                Category
                <input
                  value={noteForm.category}
                  onChange={(event) => setNoteForm({ ...noteForm, category: event.target.value })}
                  placeholder="e.g. Work, Personal"
                />
              </label>
              <label>
                Tags (comma separated)
                <input
                  value={noteForm.tags}
                  onChange={(event) => setNoteForm({ ...noteForm, tags: event.target.value })}
                  placeholder="e.g. ideas, spring-boot"
                />
              </label>
            </div>

            <textarea
              className="content-input"
              value={noteForm.content}
              onChange={(event) => setNoteForm({ ...noteForm, content: event.target.value })}
              placeholder="Start writing your thoughts here..."
              required
            />

            <div className="save-row">
              <input
                value={noteForm.commitMessage}
                onChange={(event) => setNoteForm({ ...noteForm, commitMessage: event.target.value })}
                placeholder="Commit message (optional)"
              />
              <button className="primary-button" disabled={loading}>
                <Save size={18} />
                Save Version
              </button>
            </div>
          </form>

          {/* 3. VERSION PANEL */}
          <aside className="version-panel">
            <div className="panel-heading">
              <div>
                <span className="eyebrow">Version Timeline</span>
                <h3>{versions.length} immutable records</h3>
              </div>
              <Clock3 size={19} style={{ color: "var(--teal)" }} />
            </div>

            {versions.length > 0 && (
              <div className="diff-controls">
                <select value={diffFrom} onChange={(event) => setDiffFrom(event.target.value)}>
                  <option value="">Compare From</option>
                  {sortedVersions.map((version) => (
                    <option key={`from-${version.id}`} value={version.versionNumber}>
                      v{version.versionNumber}
                    </option>
                  ))}
                </select>
                <select value={diffTo} onChange={(event) => setDiffTo(event.target.value)}>
                  <option value="">Compare To</option>
                  {sortedVersions.map((version) => (
                    <option key={`to-${version.id}`} value={version.versionNumber}>
                      v{version.versionNumber}
                    </option>
                  ))}
                </select>
                <button
                  className="icon-button"
                  type="button"
                  onClick={compareVersions}
                  disabled={!diffFrom || !diffTo}
                  title="Compare Versions"
                  aria-label="Compare"
                >
                  <Diff size={18} />
                </button>
              </div>
            )}

            {diffResult && (
              <div className="diff-list">
                <div className="diff-item-header" style={{ display: "flex", justifyContent: "space-between", fontSize: "11px", color: "var(--muted)", paddingBottom: "4px", borderBottom: "1px solid var(--border)" }}>
                  <span>From v{diffResult.fromVersion} → To v{diffResult.toVersion}</span>
                  <button type="button" className="ghost-button" style={{ height: "18px", fontSize: "9px", padding: "0 4px" }} onClick={() => setDiffResult(null)}>Clear</button>
                </div>
                {diffResult.changes.length === 0 ? (
                  <div className="empty-state">No lines added or removed.</div>
                ) : (
                  diffResult.changes.map((change, index) => (
                    <div className={`diff-line ${change.changeType.toLowerCase()}`} key={`${change.changeType}-${index}`}>
                      <span>
                        {change.changeType} {change.changeType === "MODIFIED" ? `(Line old:${change.oldLineNumber} → new:${change.newLineNumber})` : (change.changeType === "ADDED" ? `(Line ${change.newLineNumber})` : `(Line ${change.oldLineNumber})`)}
                      </span>
                      {change.changeType === "MODIFIED" ? (
                        <>
                          <p style={{ textDecoration: "line-through", opacity: 0.6 }}>- {change.oldText}</p>
                          <p>+ {change.newText}</p>
                        </>
                      ) : (
                        <p>{change.changeType === "ADDED" ? `+ ${change.newText}` : `- ${change.oldText}`}</p>
                      )}
                    </div>
                  ))
                )}
              </div>
            )}

            <div className="version-list">
              {sortedVersions.map((version) => (
                <div className="version-item" key={version.id}>
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <strong>v{version.versionNumber}</strong>
                      <span>{formatDate(version.createdAt)}</span>
                    </div>
                    {version.commitMessage && <p>"{version.commitMessage}"</p>}
                    <div style={{ display: "flex", flexWrap: "wrap", gap: "4px", marginTop: "6px" }}>
                      <span style={{ fontSize: "9px", background: "var(--surface-hover)", padding: "1px 5px", borderRadius: "99px" }}>
                        Category: {version.category || "none"}
                      </span>
                      {(version.tags || []).map((t) => (
                        <span key={t} style={{ fontSize: "9px", color: "var(--teal)", background: "var(--teal-glow)", padding: "1px 5px", borderRadius: "99px" }}>
                          #{t}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                    <button
                      className="icon-button"
                      type="button"
                      onClick={() => {
                        setNoteForm({
                          title: version.title,
                          content: version.content,
                          category: version.category || "",
                          tags: (version.tags || []).join(", "),
                          commitMessage: `Restoring version ${version.versionNumber}`,
                          pinned: noteForm.pinned,
                          favorite: noteForm.favorite
                        });
                        triggerToast(`Loaded v${version.versionNumber} snapshot in editor.`);
                      }}
                      title="Load snapshot to edit"
                      aria-label="Load version"
                    >
                      <User size={14} />
                    </button>
                    <button
                      className="icon-button"
                      type="button"
                      onClick={() => restoreVersion(version.versionNumber)}
                      title="Restore directly as latest version"
                      aria-label="Restore version"
                    >
                      <Undo2 size={14} />
                    </button>
                  </div>
                </div>
              ))}
              {versions.length === 0 && (
                <div className="empty-state">No version history available. Create a note and save to begin.</div>
              )}
            </div>
          </aside>
        </div>
      </section>

      {/* ==========================================================================
         SETTINGS / USER PROFILE MODAL
         ========================================================================== */}
      {showSettings && (
        <div className="modal-overlay" onClick={() => setShowSettings(false)}>
          <div className="modal-container" onClick={(event) => event.stopPropagation()}>
            <header className="modal-header">
              <h3>Profile & Security Settings</h3>
              <button className="icon-button" style={{ border: 0, background: "transparent" }} onClick={() => setShowSettings(false)} aria-label="Close settings">
                <X size={18} />
              </button>
            </header>
            <form onSubmit={handleProfileUpdate}>
              <div className="modal-body">
                <p style={{ fontSize: "13px", marginBottom: "8px" }}>
                  Registered email: <strong style={{ color: "var(--ink)" }}>{user?.email}</strong> (Workspace owner)
                </p>
                <div className="settings-form">
                  <label>
                    Display Name
                    <input
                      value={settingsForm.displayName}
                      onChange={(event) => setSettingsForm({ ...settingsForm, displayName: event.target.value })}
                      required
                    />
                  </label>
                  <label>
                    New Password (optional)
                    <input
                      type="password"
                      value={settingsForm.newPassword}
                      onChange={(event) => setSettingsForm({ ...settingsForm, newPassword: event.target.value })}
                      placeholder="Leave blank to keep current"
                      minLength={8}
                    />
                  </label>
                </div>
              </div>
              <footer className="modal-footer">
                <button type="button" className="ghost-button" onClick={() => setShowSettings(false)}>
                  Cancel
                </button>
                <button className="primary-button" type="submit" disabled={loading}>
                  Save Changes
                </button>
              </footer>
            </form>
          </div>
        </div>
      )}

      {/* ==========================================================================
         AUDIT TRAIL LOGS MODAL
         ========================================================================== */}
      {showAudit && (
        <div className="modal-overlay" onClick={() => setShowAudit(false)}>
          <div className="modal-container" style={{ width: "min(100%, 650px)" }} onClick={(event) => event.stopPropagation()}>
            <header className="modal-header">
              <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                <Activity size={20} style={{ color: "var(--teal)" }} />
                <h3>Security & Activity Log Trail</h3>
              </div>
              <button className="icon-button" style={{ border: 0, background: "transparent" }} onClick={() => setShowAudit(false)} aria-label="Close logs">
                <X size={18} />
              </button>
            </header>
            <div className="modal-body" style={{ maxHeight: "65vh" }}>
              {loadingAudit ? (
                <div className="empty-state">Loading audit trail logs...</div>
              ) : auditLogs.length === 0 ? (
                <div className="empty-state">No activities recorded yet.</div>
              ) : (
                <div className="audit-list">
                  {auditLogs.map((log) => (
                    <div className="audit-item" key={log.id}>
                      <div className="audit-item-top">
                        <span className="audit-action-badge">{formatAction(log.action)}</span>
                        <span className="audit-item-time">{formatDate(log.createdAt)}</span>
                      </div>
                      <div className="audit-item-detail">{log.detail}</div>
                      <div className="audit-item-meta">
                        <span>IP: {log.ipAddress || "local"}</span>
                        <span>Client: {parseUserAgent(log.userAgent)}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <footer className="modal-footer">
              <button type="button" className="primary-button" onClick={() => setShowAudit(false)}>
                Done
              </button>
            </footer>
          </div>
        </div>
      )}

      {/* ==========================================================================
         TOAST SYSTEM
         ========================================================================== */}
      {toast.show && (
        <div className={`toast-notification ${toast.type}`}>
          {toast.type === "success" ? <CheckCircle size={18} /> : <AlertCircle size={18} />}
          <span>{toast.message}</span>
        </div>
      )}
    </main>
  );
}
