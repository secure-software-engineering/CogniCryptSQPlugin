# **AI-Assisted Code Enhancement**

## **1 Introduction**

The AI-Assisted Security Enhancement feature is an innovative plugin capability designed to bridge the critical gap between cryptographic security requirements and practical implementation capabilities. Modern software development often requires developers to implement secure cryptographic operations without specialized security expertise, leading to potential vulnerabilities in production systems that are detected by CogniCrypt static analysis.

This feature addresses this challenge by leveraging Large Language Models (LLMs) to automatically analyze and fix security vulnerabilities in Java cryptographic code that have been flagged with errors by CogniCrypt. The system creates an iterative feedback loop where vulnerable code identified by CogniCrypt are processed by AI to generate secure solutions, which are then validated again through CogniCrypt static analysis in the background.

***

## **2 Architecture Overview**

The service operates on an architecture that combines retrieval-augmented generation (RAG), static security analysis, and iterative refinement processes. The system is built using Flask as the web framework with a Python backend that orchestrates various AI models including OpenAI GPT and Google Gemini implementations (`main.py`, `llm_files/base.py`, `aifix.py`, `rag_pipeline.py`).

### **Core Components**

The architecture consists of five primary components working in concert: the Flask API layer that handles client requests and response formatting, the AI orchestration layer that manages multiple LLM providers and model selection, the RAG pipeline that enriches analysis with relevant CWE from vector database and CrySL rules, the static analysis verification system powered by CogniCrypt, and the caching layer that optimizes performance through SQLite-based result storage (`app_db.py`, `aifix.py`, `rag_pipeline.py`, `main.py`).

### **Data Flow Architecture**

The system processes vulnerable code through a carefully orchestrated pipeline where each stage builds upon the previous one's output. Input validation occurs at the API layer, followed by cache lookup for previously analyzed code patterns, then AI-powered vulnerability analysis using contextual knowledge retrieval, static verification through CogniCrypt integration, iterative refinement until security compliance is achieved, and finally structured response delivery with detailed explanations and verified secure code (`aifix.py`, `rag_pipeline.py`, `main.py`).

***

## **3 API Endpoints and Caching**

The platform provides two distinct API endpoints designed for different use cases.

### **3.1 Legacy `/aifix` Endpoint - Single Vulnerability Analysis**

**Purpose**: The `/aifix` endpoint is designed for simple, isolated vulnerability analysis where users need to fix a single code snippet with a specific CrySL rule violation. This endpoint is ideal for quick analysis of individual security issues identified by static analysis tools.

**Key Features**:
- **Database Caching**: Implements caching through `app_db.py` for all requests with 5-second delay simulation for cache hits
- **Simple Input**: Requires only the vulnerable code snippet, error message, and CrySL rule
- **Focused Analysis**: Analyzes and fixes the specific security violation without considering code dependencies
- **Quick Response**: Optimized for fast turnaround on single vulnerability fixes

### **3.2 `/newfix` Endpoint - Vulnerability Chain Analysis**

**Purpose**: The `/newfix` endpoint handles complex, multi-layered vulnerability analysis where security issues are interconnected through preceding and subsequent errors. This endpoint provides comprehensive analysis of vulnerability chains and their contextual relationships within larger code blocks.

**Key Features**:
- **Caching**: Supports caching with CogniCrypt-verified results without errors are cached
- **Vulnerability Chain Analysis**: Processes `precedingErrors` and `subsequentErrors` to understand the complete vulnerability context
- **Full Source Code Context**: Utilizes complete source code for comprehensive analysis
- **Multi-Node Processing**: Handles complex payloads with multiple interconnected security violations
- **Contextual Fixes**: Generates solutions that consider the broader code context and related vulnerabilities
- **Enhanced Payload Processing**: Leverages `payload_extraction.py` for complex data validation

***

## **4 Workflow Pipeline**

The security enhancement workflow operates through a sophisticated multi-stage pipeline that ensures comprehensive vulnerability analysis and verified secure code generation (`rag_pipeline.py`, `aifix.py`).

### **4.1 Initial Analysis Phase**

The analysis begins when vulnerable Java code, along with the violated CrySL rule and error message, is submitted through the REST API. The system first performs cache lookup to check if identical input has been previously processed, implementing a 5-second delay for cached results to simulate processing time while delivering instant responses for repeated queries. If no cache entry exists, the system proceeds to initialize the selected LLM provider with appropriate authentication credentials (`main.py`, `app_db.py`, `aifix.py`).

### **4.2 Knowledge Retrieval and Context Building**

The RAG pipeline constructs comprehensive context by combining multiple knowledge sources. CrySL rule specifications are loaded from local storage to provide cryptographic specification context. Static error descriptions are retrieved and processed from JSON configuration files to explain the specific security violation. The system queries a FAISS vector database containing Common Weakness Enumeration (CWE) documentation to find semantically similar security patterns. Additionally, a CSV-based mapping system links CrySL rules to relevant CWE identifiers, creating a hybrid approach that combines static mappings with dynamic semantic search (`document_processor.py`, `vector_store_manager.py`, `rag_pipeline.py`).

### **4.3 AI-Powered Vulnerability Analysis**

Using the enriched context, the LLM analyzes the vulnerable code to identify the specific security weakness and generate an initial secure solution. The system employs structured output parsing through Pydantic models to ensure consistent response formatting with vulnerability names, secure code, and detailed explanations. The LLM then converts the secure code snippet into a complete, compilable Java class suitable for static analysis verification (`llm_files/base.py`, `pydantic_models/VulnerabilityAnalysis.py`).

### **4.4 Static Verification and Iterative Refinement**

The generated Java code undergoes compilation and static analysis through CogniCrypt integration. The system saves the generated code to a temporary Java file, compiles it using the system's Java compiler with proper error handling and normalization, packages the compiled bytecode into a JAR file for analysis, and executes CogniCrypt's HeadlessJavaScanner with SARIF output format. If security violations are detected in the SARIF report, the system enters an iterative refinement loop where the violations are parsed and fed back to the LLM for code improvement. This process continues until either all violations are resolved or the maximum iteration limit is reached (`ccrun.py`).

### **4.5 Final Code Processing and Response Generation**

Upon successful verification or reaching iteration limits, the system extracts the minimal secure code snippet from the full Java class and generates a final technical explanation of the vulnerability and its resolution. The response is structured with vulnerability classification, detailed technical explanation, relevant CWE references with official links, verification status from CogniCrypt analysis, and the final secure code snippet ready for implementation (`llm_files/base.py`, `aifix.py`).

***

## **5 Supported LLM Models**

The service supports multiple LLM providers with specific model configurations optimized for security analysis tasks.

### **5.1 OpenAI GPT Models**

**Supported Models**:
- `gpt-4.1` (standard) - Flagship model for comprehensive security analysis
- `gpt-4.1-mini` (mini) - Optimized for faster responses while maintaining quality
- `gpt-4.1-nano` (nano) - Lightweight model for simple vulnerability fixes

**Usage Format**: `openai:gpt-4.1-mini` or `openai:mini`

### **5.2 Google Gemini Models**

**Supported Models**:
- `gemini-2.5-pro` (pro) - Advanced model for complex security analysis
- `gemini-2.5-flash` (flash) - Balanced performance and speed (default)
- `gemini-2.5-flash-lite` (lite) - Fast model for simple vulnerability analysis

**Usage Format**: `gemini:gemini-2.5-flash` or `gemini:flash`

**Free API Access**: Google Gemini provides free API access up to generous usage limits, making it an excellent choice for development and testing.

***

## **6 API Key Setup Guide**

### **6.1 OpenAI API Key Configuration**

**Obtaining an OpenAI API Key**:
1. Visit the [OpenAI Platform](https://platform.openai.com/api-keys)
2. Sign up for an account or log in to your existing account
3. Navigate to the "API Keys" section in your dashboard
4. Click "Create new secret key"
5. Copy the generated API key (it starts with `sk-`)
6. **Important**: Store this key securely - you won't be able to see it again

**Pricing**: OpenAI operates on a pay-per-use model. Check current pricing at [OpenAI Pricing](https://openai.com/pricing).

### **6.2 Google Gemini API Key Configuration**

**Obtaining a Google Gemini API Key**:
1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Select an existing Google Cloud project or create a new one
5. Copy the generated API key
6. **Important**: Keep this key secure and never share it publicly

**Free Tier Benefits**:
- **Free API access** up to generous monthly limits
- No credit card required for basic usage
- Excellent for development, testing, and small-scale production use
- Check current limits at [Google AI Pricing](https://ai.google.dev/pricing)

***

## **7 Implementation Guide**

This section provides comprehensive instructions for setting up, configuring, and using the AI-Assisted Code Enhancement (`aifix.py`, `main.py`).

### **7.1 Environment Setup**

**Prerequisites Installation**: Install Python 3.8 or higher with pip package manager, Java Development Kit (JDK) 11 or higher for compilation and static analysis, and download the CogniCrypt HeadlessJavaScanner JAR file and JCA-CrySL rules (`requirements.txt`, `ccrun.py`).

**Repository Selection**: The main development branch is `Main` which contains the latest features and improvements. Clone this branch for the most up-to-date functionality:

```bash
git clone https://github.com/secure-software-engineering/CogniCryptSQPlugin.git
cd SECAI
```

**Dependency Installation**: Use pip to install all required packages listed in `requirements.txt`, which includes LangChain ecosystem packages for multi-provider LLM support, Flask and Flask-CORS for web API functionality, FAISS-CPU for vector database operations, Pydantic for structured data validation, and additional utilities for document processing and caching.

```bash
pip install -r requirements.txt
```

**Environment File Configuration**: Create a `.env` file in the **project root directory** (same level as `main.py` and `requirements.txt`) with the following format:

```bash
# OpenAI Configuration
OPENAI_API_KEY=your_openai_api_key_here

# Google Gemini Configuration  
GOOGLE_API_KEY=your_google_api_key_here

```
The environment variables are accessed by the LLM provider implementations (`llm_files/openai.py`, `llm_files/gemini.py`) for secure authentication and configuration management.

### **7.2 Data Preparation**

**CWE Knowledge Base Setup**: Create a `data/CWE` directory and populate it with Common Weakness Enumeration documentation files in text format, with each file named using the CWE identifier (e.g., `319.txt` for CWE-319). These files will be automatically processed and indexed into the FAISS vector database on first run (`document_processor.py`, `vector_store_manager.py`, `aifix.py`).

**CrySL Rules Configuration**: Organize CrySL rule files in the `data/Crysl_Rules` directory in text format, and place corresponding error descriptions in `data/CogniCrypt_ErrorDesc` as JSON files. Create a `CWE_Mapping/CWE_Mapping.csv` file that maps CrySL rule files to their corresponding CWE identifiers for enhanced analysis precision (`document_processor.py`, `rag_pipeline.py`).

### **7.3 Service Startup**

**Database Initialization**: The SQLite database for caching analysis results is automatically initialized when the application starts. The system creates the necessary tables and indexes for efficient query performance, now supporting both `/aifix` and `/newfix` endpoint caching (`app_db.py`, `main.py`).

**Vector Database Creation**: On the first run, the system automatically processes CWE documents and creates the FAISS vector index. This process may take several minutes depending on the document corpus size, but subsequent startups will load the pre-built index quickly (`vector_store_manager.py`, `aifix.py`).

**API Server Launch**: Start the Flask development server by running `python main.py`, which will launch the API on `http://localhost:8000` by default. The server supports CORS for cross-origin requests and provides comprehensive logging for debugging and monitoring (`logger_config.py`, `main.py`).

### **7.4 Usage Instructions**

**Request Format**: Send POST requests to the `/aifix` endpoint with JSON payload containing the vulnerable code snippet, violated CrySL rule identifier, error message from static analysis, preferred LLM model (openai or gemini with optional model specification), and maximum number of verification iterations (`main.py`).

**Response Interpretation**: Successful responses include a vulnerability name for high-level classification, detailed technical explanation of the security issue and solution, list of relevant CWE references with official documentation links, boolean verification status indicating whether the final code passed CogniCrypt analysis, and the complete secure code snippet ready for implementation (`aifix.py`).

**Error Handling**: The system provides normalized error responses for common issues including compilation errors with suggestions to try different models, generic analysis failures with retry recommendations, and detailed logging for debugging purposes (`main.py`).

***

## **8 Backend Architecture**

The backend architecture implements a modular design with clear separation of concerns, enabling maintainable and scalable security analysis capabilities (`rag_pipeline.py`, `aifix.py`, `main.py`).

### **8.1 API Layer**

**Flask Application Structure**: The main Flask application (`main.py`) serves as the entry point, handling HTTP request routing, input validation, and response formatting. The API implements comprehensive error handling with normalized error messages and appropriate HTTP status codes for different failure scenarios.

**Request Processing Pipeline**: Incoming requests undergo JSON payload validation, cache lookup for performance optimization, input data normalization and sanitization, delegation to the core analysis function, and result caching for future requests. The system implements caching with configurable delay simulation to balance performance with user experience expectations (`app_db.py`, `main.py`).

### **8.2 AI Orchestration Layer**

**Multi-Provider LLM Support**: The system abstracts LLM interactions through a unified interface (`llm_files/base.py`) that supports multiple providers including OpenAI GPT models with configurable model selection and API key management, and Google Gemini with family-specific model resolution (`llm_files/openai.py`, `llm_files/gemini.py`, `llm_files/base.py`).

**Prompt Engineering System**: Sophisticated prompt templates are implemented for different analysis stages: database search query generation for semantic vector search, CWE selection for precision-focused vulnerability classification, vulnerability analysis with structured output requirements, SARIF-based code improvement for iterative refinement, and final explanation generation for comprehensive reporting (`llm_files/base.py`).

### **8.3 RAG Pipeline Implementation**

**Knowledge Base Management**: The RAG pipeline (`rag_pipeline.py`) orchestrates multiple knowledge sources through a `CWEMapper` class that manages Excel/CSV-based static mappings between CrySL rules and CWE identifiers. The system implements dynamic CWE identification through vector database similarity search and combines static and dynamic approaches for comprehensive coverage.

**Context Construction**: The pipeline builds rich analytical context by loading CrySL rule specifications from local text files, processing error descriptions from structured JSON configurations, performing semantic similarity searches against the CWE vector database, and applying LLM-based relevance filtering to select the most pertinent security references (`rag_pipeline.py`).

### **8.4 Static Analysis Integration**

**CogniCrypt Workflow Management**: The CCRUN class (`ccrun.py`) encapsulates the complete static analysis workflow including Java code compilation with proper error handling and normalization, JAR packaging for scanner compatibility, CogniCrypt execution with SARIF output generation, and violation detection and reporting.

**Iterative Refinement Loop**: The system implements sophisticated iteration logic that continues refinement until security compliance is achieved or maximum iterations are reached. Each iteration involves SARIF report parsing for specific violation details, structured feedback generation for LLM consumption, and code improvement with preservation of functional requirements (`ccrun.py`).

### **8.5 Data Processing and Utilities**

**Document Processing System**: The `DocumentProcessor` class handles CWE document chunking and metadata creation for vector database construction, error description processing and JSON report parsing and filtering (`document_processor.py`).

**Vector Store Management**: The `VectorStoreManager` implements FAISS vector database operations including document embedding using Hugging Face sentence transformers, efficient similarity search with configurable result limits, and persistent storage with automatic loading capabilities (`vector_store_manager.py`).

**Code Sanitization**: Utility functions provide robust code extraction from LLM outputs, handling markdown code fence removal, unicode normalization for compiler compatibility, and whitespace normalization for consistent formatting (`utils/code_sanitizer.py`).

**Enhanced Caching System**: The upgraded caching system (`app_db.py`) now supports both endpoints with conditional saving for `/newfix` - only caching CogniCrypt-verified results without errors, while maintaining full caching for `/aifix` requests.

***

## **9 Example Walkthrough**

This section demonstrates the complete analysis workflow using typical cryptographic vulnerability scenarios for both API endpoints (`aifix.py`, `rag_pipeline.py`, `main.py`).

### **9.1 Simple Vulnerability Analysis with `/aifix` Endpoint**

#### **9.1.1 Input Submission**

**Vulnerable Code Sample**: Consider a Java code snippet with insecure RSA key generation parameters: `RSAKeyGenParameterSpec parameters = new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F0);` This represents a security vulnerability where a weak public exponent (F0 = 3) is used for RSA key generation.

**Request Configuration**:
```json
{
  "code": "RSAKeyGenParameterSpec parameters = new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F0);",
  "iterations": 3,
  "llm_model": "gemini:gemini-2.5-flash",
  "msg": "Constraint on object 'parameters' was violated because: First parameter 'keySize' should be any of {3072, 4096}",
  "rule": "cognicrypt:ordererror_rsakeygenparameterspec"
}
```

#### **9.1.2 Analysis Execution**

**Knowledge Retrieval Phase**: The system constructs comprehensive context by loading the RSAKeyGenParameterSpec CrySL rule specification, retrieving error descriptions for the `ordererror` category, performing vector database search for CWE patterns related to insecure RSA parameters, and identifying relevant CWE references such as CWE-326 (Inadequate Encryption Strength) (`rag_pipeline.py`).

**AI Analysis Process**: The LLM analyzes the vulnerable code using the enriched context to identify the security weakness as "Insecure RSA Public Exponent and Key Size", recognizing both the weak public exponent (F0 = 3) and potential key size constraints, and generates a secure solution using proper cryptographic parameters (`llm_files/base.py`, `aifix.py`).

#### **9.1.3 Successful Analysis Result**

**Final Response**:
```json
{
    "CWE_references": [
        {
            "cwe": "CWE-326",
            "link": "https://cwe.mitre.org/data/definitions/326.html"
        },
        {
            "cwe": "CWE-1240",
            "link": "https://cwe.mitre.org/data/definitions/1240.html"
        }
    ],
    "CogniCrypt_Verified": true,
    "Explanation": "The original code snippet uses `RSAKeyGenParameterSpec.F0` as the public exponent for RSA key generation. This constant represents the value `3`. Using such a small public exponent is a cryptographic vulnerability...",
    "Final_Secure_Code_Snippet": "int keySize = 3072;\nRSAKeyGenParameterSpec parameters = new RSAKeyGenParameterSpec(keySize, BigInteger.valueOf(65537L));",
    "Vulnerability_name": "Insecure RSA Public Exponent and Key Size"
}
```

**Performance**: Cache lookup for identical requests provides responses in under 6 seconds, while initial processing takes 30-60 seconds with CogniCrypt verification.

### **9.2 Complex Vulnerability Chain Analysis with `/newfix` Endpoint**

#### **9.2.1 Input Submission**

**Complex Vulnerability Scenario**: A comprehensive analysis involving multiple interconnected errors in RSA key generation where the main error (RequiredPredicateError in KeyPairGenerator.initialize()) is connected to preceding parameter specification issues and subsequent key generation problems.

**Request Configuration** (abbreviated for readability):
```json
{
    "selectedNode": {
        "severity": "MEDIUM",
        "codeSnippet": "generator.initialize(parameters, new SecureRandom());",
        "errorType": "RequiredPredicateError",
        "rule": "java.security.KeyPairGenerator",
        "message": "First parameter was not properly generated as preparedRSA",
        "precedingErrors": ["530578927"],
        "subsequentErrors": ["-1488744807"]
    },
    "fullPathFromRootToBottom": [...],
    "sourceCodeAnalysis": [...],
    "llm_model": "gemini:gemini-2.5-flash-lite",
    "iterations": 5
}
```

#### **9.2.2 Comprehensive Analysis Process**

**Vulnerability Chain Analysis**:
- **Preceding Error Analysis**: Examines error "530578927" (ImpreciseValueExtractionError in RSAKeyGenParameterSpec on line 15) - unable to evaluate constraint on second parameter due to insufficient information
- **Main Error Processing**: Addresses the RequiredPredicateError in KeyPairGenerator.initialize() on line 16 - first parameter not properly generated as preparedRSA
- **Subsequent Error Consideration**: Accounts for error "-1488744807" (RequiredPredicateError in KeyPair generation on line 17) - return value not properly generated as generatedKeypair
- **Full Context Integration**: Utilizes complete source code showing the entire Main class with correct(), incorrect(), correctBigInteger(), and incorrectBigInteger() methods to understand the broader vulnerability context

**Enhanced Payload Processing**: The system processes the complex payload through `payload_extraction.py`, extracting vulnerability relationships, source code context, and error dependencies to provide comprehensive analysis (`payload_extraction.py`).

#### **9.2.3 Verification and Refinement**

**Iterative Analysis Results**: The system successfully resolves all three interconnected errors in a single iteration:
- Resolves ImpreciseValueExtractionError by replacing `RSAKeyGenParameterSpec.F4` with explicit `BigInteger.valueOf(65537)`
- Fixes RequiredPredicateError in generator initialization through proper parameter specification
- Addresses KeyPair generation error by ensuring the entire chain uses cryptographically strong parameters

**Processing Details**:
```json
"Processing_Details": {
    "auto_resolved_by_dependencies": 2,
    "manually_processed": 1,
    "processed_errors": [
        {
            "cwe_count": 3,
            "errorType": "ImpreciseValueExtractionError",
            "hashcode": "530578927",
            "iteration": 1,
            "line": 15,
            "verified": true
        }
    ],
    "total_errors_in_trace": 3,
    "total_iterations": 1,
    "verified_errors": 1
}
```

#### **9.2.4 Comprehensive Solution Result**

**Final Response**:
```json
{
    "CWE_references": [
        {
            "cwe": "CWE-326",
            "link": "https://cwe.mitre.org/data/definitions/326.html"
        },
        {
            "cwe": "CWE-358", 
            "link": "https://cwe.mitre.org/data/definitions/358.html"
        },
        {
            "cwe": "CWE-166",
            "link": "https://cwe.mitre.org/data/definitions/166.html"
        }
    ],
    "CogniCrypt_Verified": true,
    "Explanation": "The original code contained two primary vulnerabilities related to RSA key generation. First, it used an insufficient key size of 2048 bits. Modern cryptographic standards and recommendations... Second, the code either used a cryptographically weak public exponent or relied on a constant that caused issues with static analysis...",
    "Final_Secure_Code_Snippet": "package org.example;...", 
    "Vulnerability_name": "Insecure RSA Key Generation Parameters"
}
```

**Comprehensive Fix**: The `/newfix` endpoint provides the complete corrected source code for the entire class, addressing not only the primary vulnerability but also fixing all related methods (`correct()`, `incorrect()`, `correctBigInteger()`, `incorrectBigInteger()`) to ensure consistent security across the codebase.

### **9.3 Performance Comparison**

**Cache Benefits**: 
- **`/aifix`**: All requests benefit from caching with 5-second delay simulation for cache hits
- **`/newfix`**: conditional caching  only CogniCrypt verified results without errors are cached, providing performance benefits for high-quality analysis results

**Analysis Complexity**:
- **`/aifix`**: Focused single-vulnerability analysis typically completed in 30-60 seconds
- **`/newfix`**: Comprehensive multi-node analysis handling complex vulnerability chains, with processing time varying based on code complexity and interconnected error count

***

## **10 Code References**

### **Main Application Files**

**`main.py`** - Flask application entry point and API endpoint implementation
- `/aifix` endpoint: Handles POST requests for security analysis with comprehensive input validation and error handling
- `/newfix` endpoint: Enhanced payload processing with conditional caching
- Cache integration: Implements caching with database lookup and result storage for both endpoints
- Error normalization: Provides user-friendly error messages for compilation and analysis failures

**`aifix.py`** - Core orchestration function that coordinates the entire analysis workflow
- `ai_fix()`: Main function orchestrating RAG pipeline, LLM analysis, and CogniCrypt verification
- `new_ai_fix()`: Enhanced function for complex vulnerability chain analysis
- `_parse_provider_and_model()`: Flexible LLM provider and model specification parsing
- Multi-provider support: Handles OpenAI and Gemini configurations with proper authentication

### **AI and Analysis Components**

**`llm_files/base.py`** - Base LLM interface and prompt template definitions
- `BaseLLM`: Abstract base class providing unified interface for all LLM providers
- Prompt templates: Comprehensive collection of engineered prompts for different analysis stages
- `build_query()`: Optimized search query generation for vector database retrieval
- `analyse_vulnerability()`: Structured vulnerability analysis with Pydantic output validation
- `improve_based_on_sarif()`: SARIF-guided code improvement for iterative refinement

**`rag_pipeline.py`** - Retrieval-augmented generation pipeline implementation
- `RAGPipeline.run()`: Orchestrates context building, knowledge retrieval, and analysis coordination
- `CWEMapper`: Manages static CWE mappings from Excel/CSV files
- Hybrid knowledge retrieval: Combines static mappings with dynamic vector search
- LLM-based CWE selection: Applies AI filtering for precision-focused vulnerability classification

### **Static Analysis and Verification**

**`ccrun.py`** - CogniCrypt integration and iterative verification system
- `CCRUN.iterate_until_verified()`: Main iteration loop for security verification and refinement
- `compile_java()`: Java compilation with comprehensive error handling and normalization
- `convert_to_jar()`: JAR packaging for CogniCrypt scanner compatibility
- `run_cognicrypt()`: CogniCrypt execution with SARIF output generation
- SARIF processing: Violation detection and structured feedback generation

### **Data Processing and Storage**

**`document_processor.py`** - Document processing and knowledge base management
- `load_and_split()`: CWE document chunking and metadata creation for vector database
- `error_description_processing()`: JSON error description processing with HTML sanitization
- Metadata management: Document indexing and retrieval optimization

**`vector_store_manager.py`** - FAISS vector database operations
- `create_store()`: Vector database construction from processed documents
- `load_store()`: Efficient vector database loading with persistence support
- Similarity search: Semantic retrieval with configurable result limits

**`app_db.py`** - SQLite caching and performance optimization
- `save_analysis_record()`: Persistent storage of analysis results for `/aifix` caching
- `get_record_by_input()`: Cache lookup with configurable delay simulation for `/aifix`
- `save_newfix_analysis_record()`: Conditional caching for `/newfix` - only verified results
- `get_newfix_record_by_input()`: Hash-based cache lookup for complex `/newfix` payloads
- `_should_save_newfix_record()`: logic determining when to cache `/newfix` results
- Database schema: Optimized table structure for query performance with dual endpoint support

### **LLM Provider Implementations**

**`llm_files/openai.py`**, **`llm_files/gemini.py`** - Provider-specific LLM implementations
- Model resolution: Flexible model name mapping and default fallback logic
- Authentication: Secure API key management and environment variable integration
- Structured output: Provider-specific handling of Pydantic model responses
- Supported models: OpenAI GPT-4.1 family and Google Gemini 2.5 family

**`llm_files/ollama.py`** - Experimental self-hosted model support
- **Experimental functionality** - exists but requires testing and configuration
- Local model integration through Ollama framework
- **Not recommended for production use** without extensive testing

### **Utility and Configuration**

**`logger_config.py`** - Centralized logging configuration for debugging and monitoring
- File-based logging: Comprehensive logging to `aifix.log` for analysis tracking
- Log level management: Configurable logging levels for different environments

**`payload_extraction.py`** - Enhanced payload processing for the `/newfix` endpoint
- Complex payload handling: Processes multiple vulnerability scenarios in single requests
- Data validation: Ensures payload integrity before analysis processing
- Context extraction: Manages full source code analysis and error relationship mapping

***