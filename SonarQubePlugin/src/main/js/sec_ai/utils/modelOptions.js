// src/utils/modelOptions.js

export const MODEL_OPTIONS = [
  // === Gemini 2.5 family ===
  { value: "GEMINI:gemini-2.5-pro", label: "Gemini 2.5 Pro" },
  { value: "GEMINI:gemini-2.5-flash", label: "Gemini 2.5 Flash" },
  { value: "GEMINI:gemini-2.5-flash-lite", label: "Gemini 2.5 Flash Lite" },

  // === Gemini 2.0 family ===
//  { value: "GEMINI:gemini-2.0-flash", label: "Gemini 2.0 Flash" },
//  { value: "GEMINI:gemini-2.0-flash-lite", label: "Gemini 2.0 Flash Lite" },

  // === GPT-4.1 family ===
  { value: "OPENAI:gpt-4.1", label: "GPT-4.1" },
  { value: "OPENAI:gpt-4.1-mini", label: "GPT-4.1 Mini" },
  { value: "OPENAI:gpt-4.1-nano", label: "GPT-4.1 Nano" },

  // === GPT-4o family ===
//  { value: "OPENAI:gpt-4o", label: "GPT-4o" },
//  { value: "OPENAI:gpt-4o-mini", label: "GPT-4o Mini" },
];

export const WEIGHT = {
  "INFO": 0.01,
  "LOW": 0.33,
  "MEDIUM": 0.66,
  "HIGH": 0.99
}
