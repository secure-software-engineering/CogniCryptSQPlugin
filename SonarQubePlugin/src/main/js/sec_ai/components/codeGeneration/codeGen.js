import React, { useMemo, useState, useEffect } from "react";
import sonarRequest from "sonar-request";

/* ---------- helpers ---------- */
function splitCodeAndAnalysis(payload) {
  if (!payload) return { code: "", analysis: "" };
  const compileFail = "Unable to compile, try to use a stronger LLM model";
  if (payload.trim().startsWith(compileFail)) return { code: "", analysis: compileFail };

  let codeOnly = payload, analysis = "";
  const blockRe = /\/\*\s*CogniCrypt analysis results:\s*([\s\S]*?)\*\//g;
  let m, lastBlock = null;
  while ((m = blockRe.exec(payload)) !== null) lastBlock = m;
  if (lastBlock) {
    analysis = lastBlock[1].trim();
    codeOnly = payload.replace(lastBlock[0], "").trimEnd();
    return { code: codeOnly, analysis };
  }
  const lineRe = /\/\/\s*CogniCrypt analysis completed:\s*([^\n]*)/g;
  let lastLine = null;
  while ((m = lineRe.exec(payload)) !== null) lastLine = m;
  if (lastLine) {
    analysis = `CogniCrypt analysis completed: ${lastLine[1].trim()}`;
    codeOnly = payload.replace(lastLine[0], "").trimEnd();
  }
  return { code: codeOnly, analysis };
}

function summarizeAnalysis(text = "") {
  const clean = text.trim();
  if (!clean) return { status: "empty", count: 0, preview: [] };
  if (clean.toLowerCase().includes("no violation"))
    return { status: "ok", count: 0, preview: ["No violations detected."] };

  const lines = clean.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
  const vio = lines.filter(l => /^(-|\*|•|\d+\.)\s+/.test(l) || /\bviolation\b/i.test(l));
  return { status: "violations", count: Math.max(vio.length, 1), preview: (vio.length ? vio : lines).slice(0, 3) };
}

function copyText(t=""){ navigator.clipboard?.writeText(t); }
function downloadText(fn,t){ const b=new Blob([t||""],{type:"text/plain;charset=utf-8"}); const u=URL.createObjectURL(b); const a=document.createElement("a"); a.href=u;a.download=fn;a.click();URL.revokeObjectURL(u); }

// Define the models for each provider
const models = {
  openai: ["gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini", "gpt-4.1-nano"],
};

/* ---------- UI atoms ---------- */
function ProviderToggle({ provider, setProvider, disabled }) {
  return (
    <div style={ui.toggle} role="tablist" aria-label="Model Provider">
      <button
        role="tab" aria-selected={provider==="openai"} type="button"
        onClick={()=>setProvider("openai")} disabled={disabled}
        style={{...ui.toggleBtn, ...(provider==="openai"?ui.toggleBtnActive:{})}}
        title="Use OpenAI pipeline"
      >OpenAI</button>
      <button
        role="tab" aria-selected={provider==="ollama"} type="button" disabled
        style={{...ui.toggleBtn, ...ui.toggleBtnDisabled}}
        title="Ollama (coming soon)"
      >Ollama</button>
    </div>
  );
}

function ModelSelector({ provider, model, setModel, disabled }) {
  const modelOptions = models[provider] || [];

  return (
    <select
      value={model}
      onChange={(e) => setModel(e.target.value)}
      disabled={disabled || modelOptions.length === 0}
      className="button"
      style={{ minWidth: 200, height: 38, border: "1px solid #e5e7eb", padding: "6px 12px" }}
    >
      {modelOptions.map((m) => (
        <option key={m} value={m}>
          {m}
        </option>
      ))}
    </select>
  );
}

function IterationsInput({ iterations, setIterations, disabled }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      <label htmlFor="iterations-select" style={{ fontSize: 14, color: "#374151" }}>Iterations:</label>
      <select
        id="iterations-select"
        value={iterations}
        onChange={(e) => setIterations(parseInt(e.target.value, 10))}
        disabled={disabled}
        className="button"
        style={{ width: 60, height: 38, border: "1px solid #e5e7eb", borderRadius: 6, padding: "6px 8px", textAlign: "center" }}
      >
        {[1, 2, 3, 4, 5].map(num => (
          <option key={num} value={num}>{num}</option>
        ))}
      </select>
    </div>
  );
}

function ChatBubble({ side="left", variant="text", title, content, onCopy, onDownload }) {
  const isRight = side === "right";
  const isCode  = variant === "code";
  return (
    <div style={{ display:"flex", justifyContent: isRight?"flex-end":"flex-start" }}>
      <div style={{ ...ui.bubble, ...(isRight?ui.bubbleRight:ui.bubbleLeft), ...(isCode?ui.bubbleCode:{}) }}>
        {title && <div style={ui.bubbleTitle}>{title}</div>}
        <div style={isCode ? ui.codeBlock : ui.bubbleText}>{content || "—"}</div>
        {(onCopy || onDownload) && (
          <div style={ui.bubbleActions}>
            {onCopy && <button style={ui.actionBtn} onClick={onCopy}>Copy</button>}
            {onDownload && <button style={ui.actionBtn} onClick={onDownload}>Download</button>}
          </div>
        )}
      </div>
    </div>
  );
}

function LoadingBubble(){
  return (
    <div style={{display:"flex",justifyContent:"flex-start"}}>
      <div style={{...ui.bubble,...ui.bubbleLeft}}>
        <div style={ui.loadingDots}><span>.</span><span>.</span><span>.</span></div>
      </div>
    </div>
  );
}

function SlideOver({ open, title, onClose, children }) {
  if (!open) return null;
  return (
    <>
      <div style={ui.backdrop} onClick={onClose}/>
      <aside style={ui.drawer} aria-modal="true" role="dialog">
        <div style={ui.drawerHeader}>
          <strong>{title}</strong>
          <button className="button" onClick={onClose}>Close</button>
        </div>
        <div style={ui.drawerBody}>
          <pre style={ui.drawerPre}>{children}</pre>
        </div>
      </aside>
    </>
  );
}

function AnalysisSummary({ text }) {
  const [open, setOpen] = useState(false);
  const s = summarizeAnalysis(text);
  return (
    <>
      <div style={{display:"flex",justifyContent:"flex-start"}}>
        <div style={{...ui.bubble,...ui.bubbleLeft}}>
          <div style={ui.summaryHeader}>
            <span style={{...ui.badge, ...(s.status==="ok"?ui.badgeOk: s.status==="violations"?ui.badgeBad:ui.badgeMuted)}}>
              {s.status==="ok" ? "No violations" : s.status==="violations" ? `${s.count} violation(s)` : "Analysis"}
            </span>
            <div style={{flex:1}}/>
            <button style={ui.viewDetailsBtn} onClick={()=>setOpen(true)}>View Details</button>
          </div>
          <div style={{marginTop:8, color:"#374151", fontSize:13}}>
            {s.preview.length ? s.preview.map((l,i)=><div key={i} style={{marginBottom:4}}>• {l}</div>) : <div>No summary available.</div>}
          </div>
        </div>
      </div>
      <SlideOver open={open} title="CogniCrypt Analysis" onClose={()=>setOpen(false)}>
        {text || "—"}
      </SlideOver>
    </>
  );
}

/* ---------- main page ---------- */
export default function CodeGenerationPage() {
  const [prompt, setPrompt]     = useState("");
  const [provider, setProvider] = useState("openai");
  const [model, setModel]       = useState(models.openai[0]);
  const [iterations, setIterations] = useState(2);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState("");
  const [messages, setMessages] = useState([
    { role:"assistant", type:"text", content:"Hi! Describe the code you need. I’ll generate it and run CogniCrypt checks automatically." }
  ]);

  const hasContent = useMemo(()=>messages.length>0,[messages.length]);
  
  // Effect to update the model when the provider changes
  useEffect(() => {
    const availableModels = models[provider] || [];
    setModel(availableModels[0] || ""); // Set to the first available model
  }, [provider]);

  async function send(){
    const text = prompt.trim();
    if(!text || loading) return;
    setPrompt(""); setError("");
    setMessages(p=>[...p,{role:"user",type:"text",content:text}]);
    setLoading(true);
    try{
      const { code } = await sonarRequest.postJSON("/api/secai/runCodeGeneration",{ prompt:text, provider, model, iterations });
      const { code:genCode, analysis } = splitCodeAndAnalysis(code||"");
      if(genCode){
        setMessages(p=>[...p,{ role:"assistant", type:"code", title:"Generated Code", content:genCode }]);
      }
      setMessages(p=>[...p,{ role:"assistant", type:"analysis-summary", content: analysis || "No analysis text found." }]);
    }catch(e){
      const msg = e?.message || "Unexpected error";
      setMessages(p=>[...p,{ role:"assistant", type:"text", content:`⚠️ ${msg}` }]);
      setError(msg);
    }finally{ setLoading(false); }
  }
  function onKeyDown(e){ if(e.key==="Enter" && (e.ctrlKey||e.metaKey)){ e.preventDefault(); send(); } }

  return (
    <div style={ui.page}>
      {/* header */}
      <div style={ui.header}>
        <div style={ui.headerL}>
          <h2 style={{margin:0}}>💬 SecAI Chat Generator</h2>
          <span style={ui.beta}>chat mode</span>
        </div>
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <ProviderToggle provider={provider} setProvider={setProvider} disabled={loading}/>
          <ModelSelector provider={provider} model={model} setModel={setModel} disabled={loading} />
          <IterationsInput iterations={iterations} setIterations={setIterations} disabled={loading} />
        </div>
      </div>

      {/* chat area (only this scrolls) */}
      <div style={ui.chatCard}>
        <div style={ui.chatScroll} id="secai-chat-scroll">
          {!hasContent && (
            <div style={ui.empty}>
              <div style={{fontSize:18,fontWeight:700}}>Start a request</div>
              <div style={{color:"#6b7280"}}>Tell me what to build and constraints (lang, libs, APIs).</div>
            </div>
          )}
          {messages.map((m,i)=>{
            const side = m.role==="user" ? "right" : "left";
            if(m.type==="code"){
              return (
                <ChatBubble
                  key={i} side={side} variant="code" title={m.title} content={m.content}
                  onCopy={()=>copyText(m.content)}
                  onDownload={()=>downloadText("demo.java", m.content)}
                />
              );
            }
            if(m.type==="analysis-summary"){ return <AnalysisSummary key={i} text={m.content}/>; }
            return <ChatBubble key={i} side={side} variant="text" content={m.content}/>;
          })}
          {loading && <LoadingBubble/>}
        </div>

        {/* input */}
        <div style={ui.inputRow}>
          <textarea
            value={prompt} onChange={e=>setPrompt(e.target.value)} onKeyDown={onKeyDown}
            placeholder="Type your request…  (Press ⌘/Ctrl + Enter to send)" style={ui.input} rows={2}
          />
          <button className="button button-primary" onClick={send} disabled={loading || !prompt.trim()} style={ui.sendBtn}>
            {loading ? "Generating…" : "Send"}
          </button>
        </div>

        {!!error && <div role="alert" style={ui.err}>{error}</div>}
      </div>
    </div>
  );
}

/* ---------- styles ---------- */
const ui = {
  page: { maxWidth: 960, margin:"0 auto", padding:16 },

  header: { display:"flex", alignItems:"center", justifyContent:"space-between", marginBottom:12 },
  headerL: { display:"flex", alignItems:"center", gap:8 },
  beta: { fontSize:12, padding:"2px 6px", borderRadius:999, background:"#eef2ff", color:"#3730a3", border:"1px solid #c7d2fe" },

  /* provider pills */
  toggle:{ display:"inline-flex", gap:8, padding:4, borderRadius:999, background:"#f3f4f6", border:"1px solid #e5e7eb" },
  toggleBtn:{ padding:"6px 16px", border:"none", borderRadius:999, fontSize:14, fontWeight:600, background:"transparent", cursor:"pointer", transition:"all .2s", color:"#111827" },
  toggleBtnActive:{ background:"#2563eb", color:"#fff", boxShadow:"0 0 0 1px #1d4ed8 inset" },
  toggleBtnDisabled:{ opacity:.5, cursor:"not-allowed", color:"#6b7280", background:"transparent", border:"1px dashed #e5e7eb" },

  /* chat container height locks scrolling to this pane */
  chatCard:{
    background:"#fff", border:"1px solid #e5e7eb", borderRadius:12, display:"flex", flexDirection:"column",
    height: "calc(100vh - 480px)", 
    minHeight: 300 // Reduced minHeight as well
  },
  chatScroll:{ flex:1, overflow:"auto", padding:16, display:"flex", flexDirection:"column", gap:12 },

  bubble:{ maxWidth:"80%", borderRadius:12, padding:12, border:"1px solid #e5e7eb" },
  bubbleLeft:{ background:"#f8fafc", color:"#0f172a", alignSelf:"flex-start" },
  bubbleRight:{ background:"#7C98D3", color:"#fff", alignSelf:"flex-end", borderColor:"transparent" },
  bubbleCode:{ background:"#0a0f14", color:"#e5edf4", borderColor:"#0a0f14" },
  bubbleTitle:{ fontSize:12, fontWeight:700, opacity:.8, marginBottom:6 },

  /* no inner scroll in bubbles — let the whole chat pane scroll */
  bubbleText:{ whiteSpace:"pre-wrap", wordBreak:"break-word" },
  codeBlock:{
    fontFamily:"ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono','Courier New', monospace",
    fontSize:13, lineHeight:1.45,
    whiteSpace:"pre-wrap",      // wraps long lines to avoid horizontal scroll
    wordBreak:"break-word"      // breaks very long tokens
  },

  bubbleActions:{ display:"flex", gap:8, marginTop:10 },
  actionBtn:{ 
    padding: "8px 16px", 
    borderRadius: 6, 
    border: "1px solid #3b82f6", 
    background: "#3b82f6", 
    color: "#fff", 
    fontSize: 12, 
    fontWeight: 600, 
    cursor: "pointer", 
    transition: "all 0.2s",
    ":hover": { background: "#2563eb" }
  },
  viewDetailsBtn:{ 
    padding: "6px 12px", 
    borderRadius: 6, 
    border: "1px solid #10b981", 
    background: "#10b981", 
    color: "#fff", 
    fontSize: 12, 
    fontWeight: 600, 
    cursor: "pointer", 
    transition: "all 0.2s"
  },

  loadingDots:{ width:40, display:"flex", gap:4, color:"#6b7280", fontWeight:700, animation:"blink 1.2s infinite" },

  inputRow:{ display:"flex", gap:8, borderTop:"1px solid #e5e7eb", padding:12 },
  input:{ flex:1, resize:"none", borderRadius:10, border:"1px solid #e5e7eb", padding:10, fontSize:14, outline:"none" },
  sendBtn:{ padding:"10px 16px" },

  empty:{ textAlign:"center", padding:"32px 8px", color:"#6b7280" },
  err:{ margin:"0 12px 12px", padding:"8px 10px", borderRadius:8, border:"1px solid #fecaca", background:"#fef2f2", color:"#991b1b" },

  /* Slide-over (separate from chat bubbles; okay to scroll here) */
  backdrop:{ position:"fixed", inset:0, background:"rgba(0,0,0,.35)", zIndex:50 },
  drawer:{ position:"fixed", top:220, right:0, bottom:0, width:"min(520px,92vw)", background:"#fff", borderLeft:"1px solid #e5e7eb", zIndex:51, display:"flex", flexDirection:"column" },
  drawerHeader:{ padding:12, borderBottom:"1px solid #e5e7eb", display:"flex", alignItems:"center", justifyContent:"space-between" },
  drawerBody:{ padding:12, overflow:"auto" },
  drawerPre:{ margin:0, whiteSpace:"pre-wrap", fontFamily:"ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono','Courier New'", fontSize:13, lineHeight:1.5 },

  summaryHeader:{ display:"flex", alignItems:"center", gap:8 },
  badge:{ fontSize:12, padding:"2px 8px", borderRadius:999, border:"1px solid transparent" },
  badgeOk:{ background:"#ecfdf5", color:"#065f46", borderColor:"#a7f3d0" },
  badgeBad:{ background:"#fef2f2", color:"#991b1b", borderColor:"#fecaca" },
  badgeMuted:{ background:"#f3f4f6", color:"#374151", borderColor:"#e5e7eb" }
};