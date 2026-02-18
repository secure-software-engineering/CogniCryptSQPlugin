from enum import Enum, auto

class LLMName(Enum):
    OPENAI = auto()
    GEMINI = auto()
    OLLAMA = auto()

from .openai import OpenAIHandler
from .gemini import GeminiHandler
from .ollama import OllamaHandler

_LOOKUP = {
    LLMName.OPENAI: OpenAIHandler,
    LLMName.GEMINI: GeminiHandler,
    LLMName.OLLAMA: OllamaHandler
}

def get_handler(name: str | LLMName, **options):
    """
    Return an initialised LLM handler.
    Example:  get_handler("openai", temperature=0.1)
    """
    if isinstance(name, str):
        name = LLMName[name.upper()]      # turn "openai" → LLMName.OPENAI
    return _LOOKUP[name](**options)
