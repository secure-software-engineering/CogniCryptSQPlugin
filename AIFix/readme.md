# AIFIX

An AI-powered security analysis tool that automatically fixes cryptographic vulnerabilities in Java code using Large Language Models (LLMs) and CogniCrypt verification.

## Overview

AIFIX combines the power of artificial intelligence with static analysis to identify and fix cryptographic security issues in Java applications. The tool integrates with CogniCrypt and supports multiple LLM providers for intelligent code fixes.

## Features

- **AI-Powered Code Fixes**: Leverages LLMs to automatically generate secure code fixes
- **CogniCrypt Integration**: Validates fixes against cryptographic security rules
- **Multiple LLM Support**: Compatible with various language models including OpenAI GPT & Gemini
- **Caching System**: Intelligent caching to avoid redundant analysis
- **RESTful API**: Easy integration with development workflows
- **Docker Support**: Containerized deployment for consistent environments

## API Endpoints

### `/aifix` (POST)
Analyzes and fixes cryptographic vulnerabilities in code snippets.
- **Input**: Code snippet, CrySL rule, error message, LLM model
- **Output**: Fixed code with security improvements

### `/newfix` (POST)
Processes complex multi-node error traces and sequentially fixes cryptographic vulnerabilities with smart dependency resolution.
- **Input**: Complex JSON payload with error trace flow, full source code analysis, selected node details, LLM model, and iteration count
- **Output**: Sequentially fixed code with comprehensive processing details, CogniCrypt verification status, CWE references, and auto-resolution tracking for dependent errors

## Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/secure-software-engineering/pg-secai.git
   cd SECAI
   ```

2. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

3. **Run the application**
   ```bash
   python main.py
   ```

4. **Docker deployment**
   ```bash
   docker build -t secai .
   docker run -p 8000:8000 secai
   ```

## Project Structure

```
SECAI/
├── .github/                     # GitHub workflows and configurations
├── CCJar/                       # CogniCrypt JAR files and dependencies
├── CWE_Mapping/                 # Common Weakness Enumeration mappings
├── JCA-CrySL-rules/             # Java Cryptographic Architecture CrySL rules
├── data/                        # CWE knowledge base documents
├── faiss_index/                 # FAISS vector database storage
├── llm_files/                   # LLM handler implementations
├── pydantic_models/             # Structured data models
│   └── VulnerabilityAnalysis.py
├── utils/                       # Utility functions
│   └── code_sanitizer.py
├── GeneratedCode/               # Temporary directory for code processing
├── main.py                      # Flask API server
├── aifix.py                     # Core AI logic
├── rag_pipeline.py             # RAG implementation
├── ccrun.py                    # CogniCrypt integration
├── payload_extraction.py      # Complex payload processing
├── app_db.py                  # Database operations
├── document_processor.py      # Document processing
├── vector_store_manager.py    # Vector store management
├── logger_config.py          # Logging configuration
├── analysis_results.db       # SQLite cache database
├── view_db.py               # Database viewer
├── requirements.txt         # Dependencies
└── Dockerfile              # Container config
```