import os
from .base import BaseLLM

# Prefer the dedicated package; fall back to the community import if needed
try:
    from langchain_ollama import ChatOllama        # pip install langchain-ollama
except Exception:
    from langchain_community.chat_models import ChatOllama

class OllamaHandler(BaseLLM):
    def __init__(self, model: str | None = None, base_url: str | None = None, **kwargs):
        model = model or os.getenv("OLLAMA_MODEL", "qwen2.5-coder:3b")
        base_url = base_url or os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        llm = ChatOllama(model=model, base_url=base_url, **kwargs)
        super().__init__(llm)

    # Optional: graceful fallback if structured outputs aren't supported by a given local model
    def analyse_vulnerability(self, context: str, question: str):
        try:
            return super().analyse_vulnerability(context, question)
        except Exception:
            # Fallback: ask for JSON and parse into your Pydantic model
            from langchain_core.prompts import ChatPromptTemplate
            from langchain_core.output_parsers import StrOutputParser
            from pydantic_models.VulnerabilityAnalysis import VulnerabilityAnalysis
            import json

            prompt = ChatPromptTemplate.from_template(
                "Return ONLY valid JSON for this schema:\n{schema}\n\nContext:\n{context}\n\nQuestion:\n{question}"
            )
            chain = prompt | self.llm | StrOutputParser()
            raw = chain.invoke({
                "schema": VulnerabilityAnalysis.model_json_schema(),
                "context": context,
                "question": question
            })
            return VulnerabilityAnalysis(**json.loads(raw))
