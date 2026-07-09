import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import "./styles.css";

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error("ErrorBoundary caught an error", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: "40px", color: "#ef4444", background: "#080c14", minHeight: "100vh", fontFamily: "monospace" }}>
          <h2 style={{ fontSize: "22px", marginBottom: "16px", color: "#f8fafc" }}>React Runtime Exception Detected</h2>
          <pre style={{ background: "rgba(255,255,255,0.05)", padding: "16px", borderRadius: "8px", overflowX: "auto", border: "1px solid rgba(239, 68, 68, 0.3)" }}>
            {this.state.error && this.state.error.toString()}
          </pre>
          <pre style={{ marginTop: "20px", opacity: 0.7, fontSize: "12px", whiteSpace: "pre-wrap" }}>
            {this.state.error && this.state.error.stack}
          </pre>
        </div>
      );
    }

    return this.props.children;
  }
}

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>
);
