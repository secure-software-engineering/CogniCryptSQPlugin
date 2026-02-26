import re
import unicodedata
import logging
from typing import Optional, Tuple

logger = logging.getLogger(__name__)

# Regex to find content within triple-backtick fenced blocks.
# It captures the text *between* the fences.
FENCE_BLOCK_CONTENT = re.compile(r"```(?:[^\n]*)?\n(.*?)\n```", re.DOTALL)


def _select_largest_fenced_block(text: str) -> str:
    """Return the content of the largest ``` block if present; else whole text."""
    blocks = FENCE_BLOCK_CONTENT.findall(text)
    if not blocks:
        return text
    # Choose the largest block by length.
    return max(blocks, key=len)


def extract_java_source(text: str) -> str:
    """
    Extracts clean source code from LLM output by removing backtick fences.

    - Picks the largest fenced code block if one exists.
    - Removes language fences (e.g., ```java) and closing fences (```).
    - Normalizes newlines and trims whitespace for clean output.
    """
    if not text or not text.strip():
        logger.warning("Empty input to extract_code_from_llm")
        return ""

    # 1) First, try to extract the content from the largest fenced block.
    code = _select_largest_fenced_block(text)

    # 2) As a fallback, remove any stray fences that might remain.
    #    This handles cases where the input isn't a perfect, complete block.
    code = re.sub(r"^\s*```[^\n]*\n", "", code) # Removes the opening fence
    code = re.sub(r"\n?```\s*$", "", code)      # Removes the closing fence

    # 3) Perform other general text cleaning useful for code.
    # Remove invisible formatting chars that can break compilers
    code = "".join(ch for ch in code if unicodedata.category(ch) != "Cf")
    # Normalize line endings
    code = code.replace("\r\n", "\n").replace("\r", "\n")
    # Collapse excessive blank lines
    code = re.sub(r"\n{3,}", "\n\n", code)
    # Trim outer whitespace but ensure a final newline
    code = code.strip()
    return (code + "\n") if code else ""