from __future__ import annotations

import os

from langchain_openai import ChatOpenAI

from .base import BaseLLM

from logger_config import get_logger

logger = get_logger(__name__)

_OPENAI_MODEL_ALIASES = {
    # === GPT-4.1 family (only supported models) ===
    "gpt-4.1": "gpt-4.1",
    "gpt-4.1-mini": "gpt-4.1-mini", 
    "gpt-4.1-nano": "gpt-4.1-nano",
    "4.1": "gpt-4.1",
    "4.1-mini": "gpt-4.1-mini",
    "4.1-nano": "gpt-4.1-nano",
    
    # Short aliases for convenience
    "standard": "gpt-4.1",
    "mini": "gpt-4.1-mini",
    "nano": "gpt-4.1-nano",
}

def _resolve_openai_model(name: str | None) -> str:
    """Resolve OpenAI model name with proper fallback logic."""
    # Get the requested model or use environment variable or default
    raw = name or os.getenv("OPENAI_MODEL") or "gpt-4.1"

    # Apply alias mapping
    resolved = _OPENAI_MODEL_ALIASES.get(raw, raw)
    
    # Validate that only supported models are used
    supported_models = {
        "gpt-4.1",
        "gpt-4.1-mini", 
        "gpt-4.1-nano"
    }
    
    if resolved not in supported_models:
        logger.warning(
            f"[OpenAIHandler] Unsupported model '{resolved}' requested. "
            f"Falling back to 'gpt-4.1'. "
            f"Supported models: {', '.join(supported_models)}"
        )
        resolved = "gpt-4.1"

    logger.info(
        f"[OpenAIHandler] Requested={name!r}, Env={os.getenv('OPENAI_MODEL')}, "
        f"Default='gpt-4.1' -> Resolved={resolved}"
    )

    return resolved

class OpenAIHandler(BaseLLM):
    def __init__(self, model: str | None = None, **kwargs):
        resolved_model = _resolve_openai_model(model)
        logger.info(f"[OpenAIHandler] Using model: {resolved_model}")
        super().__init__(ChatOpenAI(model=resolved_model, **kwargs))
