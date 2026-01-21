from __future__ import annotations

import os

from langchain_google_genai import ChatGoogleGenerativeAI

from .base import BaseLLM

from logger_config import get_logger

logger = get_logger(__name__)

_GEMINI_MODEL_ALIASES = {
    # === Gemini 2.5 family (only supported models) ===
    "gemini-2.5-pro": "gemini-2.5-pro",
    "gemini-2.5-flash": "gemini-2.5-flash", 
    "gemini-2.5-flash-lite": "gemini-2.5-flash-lite",
    "2.5-pro": "gemini-2.5-pro",
    "2.5-flash": "gemini-2.5-flash",
    "2.5-flash-lite": "gemini-2.5-flash-lite",
    
    # Short aliases for convenience
    "pro": "gemini-2.5-pro",
    "flash": "gemini-2.5-flash",
    "flash-lite": "gemini-2.5-flash-lite",
    "lite": "gemini-2.5-flash-lite",
}

def _resolve_gemini_model(name: str | None) -> str:
    raw = (name or os.getenv("GEMINI_MODEL") or "gemini-2.5-flash").strip()
    resolved = _GEMINI_MODEL_ALIASES.get(raw, raw)
    
    # Validate that only supported models are used
    supported_models = {
        "gemini-2.5-pro",
        "gemini-2.5-flash", 
        "gemini-2.5-flash-lite"
    }
    
    if resolved not in supported_models:
        logger.warning(
            f"[GeminiHandler] Unsupported model '{resolved}' requested. "
            f"Falling back to 'gemini-2.5-flash'. "
            f"Supported models: {', '.join(supported_models)}"
        )
        resolved = "gemini-2.5-flash"
    
    logger.info(
        f"[GeminiHandler] Requested={name!r}, "
        f"Default='gemini-2.5-flash' -> Resolved={resolved}"
    )
    return resolved

class GeminiHandler(BaseLLM):
    def __init__(self, model: str | None = None, **kwargs):
        model = _resolve_gemini_model(model)
        logger.info(f"[GeminiHandler] Using model: {model}")
        super().__init__(ChatGoogleGenerativeAI(model=model, **kwargs))
