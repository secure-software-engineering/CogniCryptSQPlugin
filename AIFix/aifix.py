import os
from dotenv import load_dotenv
from document_processor import DocumentProcessor
from vector_store_manager import VectorStoreManager
from llm_files import get_handler
from rag_pipeline import RAGPipeline
import re
from typing import Dict, Any, Tuple
from logger_config import get_logger
from ccrun import CCRUN
from utils.code_sanitizer import extract_java_source
import shutil

# Initialize logging and load environment variables (API keys, etc.)
logger = get_logger(__name__)
load_dotenv()

def cleanup_generated_files():
    """
    Clean up all generated files from the GeneratedCode directory after pipeline completion.
    This ensures a fresh environment for the next analysis run.
    """
    generated_dir = os.path.abspath("GeneratedCode")

    if os.path.exists(generated_dir):
        try:
            # Get list of files before deletion for logging
            files_to_delete = []
            for root, dirs, files in os.walk(generated_dir):
                for file in files:
                    files_to_delete.append(os.path.join(root, file))

            if files_to_delete:
                logger.info(f"Cleaning up {len(files_to_delete)} generated files from {generated_dir}")
                # Delete all contents but keep the directory
                for filename in os.listdir(generated_dir):
                    file_path = os.path.join(generated_dir, filename)
                    try:
                        if os.path.isfile(file_path) or os.path.islink(file_path):
                            os.unlink(file_path)
                            logger.debug(f"Deleted file: {filename}")
                        elif os.path.isdir(file_path):
                            shutil.rmtree(file_path)
                            logger.debug(f"Deleted directory: {filename}")
                    except Exception as e:
                        logger.warning(f"Failed to delete {file_path}: {e}")

                logger.info("GeneratedCode directory cleaned successfully")
            else:
                logger.info("GeneratedCode directory already empty")

        except Exception as e:
            logger.warning(f"Error during cleanup: {e}. Continuing with response...")
    else:
        logger.info("GeneratedCode directory doesn't exist, no cleanup needed")

def _parse_provider_and_model(s: str) -> Tuple[str, str | None]:
    """
    Parses the LLM provider string to extract provider name and optional model.
    This allows flexible model specification from the frontend.
    
    Examples of accepted formats:
      - "OPENAI"                      -> ("OPENAI", None) - uses default model
      - "GEMINI"                      -> ("GEMINI", None) - uses default model  
      - "OPENAI:gpt-4o-mini"          -> ("OPENAI", "gpt-4o-mini") - specific model
      - "GEMINI:gemini-1.5-pro"       -> ("GEMINI", "gemini-1.5-pro") - specific model
      - Case-insensitive provider names supported
    
    Args:
        s (str): Provider string in format "PROVIDER" or "PROVIDER:model"
    
    Returns:
        Tuple[str, str | None]: (provider_name, model_name_or_none)
    
    Raises:
        ValueError: If no provider is specified
    """
    if not s:
        raise ValueError("llm_model (provider) must be specified: OPENAI | GEMINI | OLLAMA[:model]")
    
    # Split on first colon to separate provider from model
    parts = s.split(":", 1)
    provider = parts[0].strip().upper()  # Normalize to uppercase
    # Extract model name if provided, otherwise None (will use handler's default)
    model = parts[1].strip() if len(parts) == 2 and parts[1].strip() else None
    return provider, model


def ai_fix(code_input: str, rule: str, message: str, llm_model: str, iterations_cc: int) -> Dict[str, Any]:
    """
    Main orchestration function that analyzes vulnerable Java code and provides secure fixes.
    
    This function coordinates the entire AI-powered security analysis workflow:
    1. Parses LLM provider and model preferences
    2. Initializes the chosen LLM handler with proper authentication
    3. Sets up document processing and vector database for CWE knowledge retrieval
    4. Runs RAG pipeline
    5. Uses CogniCrypt for iterative security verification and refinement
    6. Returns structured response with vulnerability details and secure code
    
    Args:
        code_input (str): The vulnerable Java code snippet to analyze
        rule (str): CogniCrypt rule identifier (e.g., "cognicrypt:requiredpredicateerror")  
        message (str): Error message from CogniCrypt containing rule violation details
        llm_model (str): LLM specification in format "PROVIDER" or "PROVIDER:model"
        iterations_cc (int): Maximum iterations for CogniCrypt verification refinement
    
    Returns:
        Dict[str, Any]: Structured response containing:
            - Vulnerability_name: High-level vulnerability classification
            - Explanation: Technical explanation of the issue and fix
            - CWE_references: List of relevant CWE entries with links and descriptions
            - CogniCrypt_Verified: Boolean indicating if final code passed security scan
            - Final_Secure_Code_Snippet: The verified secure replacement code
            
    Returns error dict if analysis fails:
            - error: Error message string
    """
    logger.info("Inside analysis function")

    # Parse the LLM provider string to determine which AI service to use
    provider, selected_model = _parse_provider_and_model(llm_model)
    logger.info(f"[ai_fix] Provider requested: {provider}, Model arg: {selected_model or 'None'}")
    
    # Map each provider to its corresponding API key environment variable
    if provider == "OPENAI":
        api_key_env = "OPENAI_API_KEY"
    elif provider == "GEMINI":
        api_key_env = "GOOGLE_API_KEY"  
    elif provider == "OLLAMA":
        api_key_env = None  # Ollama runs locally, no API key needed
    else:
        raise ValueError(f"Unsupported provider: {provider}")

    # Initialize the LLM handler with provider-specific configuration
    # Only pass API key for cloud providers, not for local Ollama
    handler = get_handler(
        provider,
        # Conditionally include API key only when needed (cloud providers)
        **({"api_key": os.getenv(api_key_env)} if api_key_env else {}),
        # Pass through model preference to handler's model resolution logic
        model=selected_model,
        # Low temperature
        temperature= 0.1,
    )
    logger.info(f"[ai_fix] Handler initialized for provider={provider}")

    # Initialize the core components of our analysis pipeline
    doc_processor = DocumentProcessor()      # Handles CWE document chunking and metadata
    vs_manager = VectorStoreManager()        # Manages FAISS vector database  
    rag_pipeline = RAGPipeline(doc_processor, vs_manager, handler)  # Orchestrates retrieval-augmented generation

    # Set up or load the CWE knowledge base for contextual analysis
    CWE_File_Path = r"data/CWE"
    if not os.path.exists("faiss_index"):
        # First-time setup: create vector database from CWE documents
        logger.info("Index does not exist, creating one")
        chunks = doc_processor.load_and_split(CWE_File_Path)  # Chunk CWE docs for embedding
        vs_manager.create_store(chunks)                       # Create FAISS vector store
        vs_manager.save_store()                              # Persist to disk for future use
        logger.info("Index created successfully")
    else:
        # Load existing vector database - much faster than rebuilding
        logger.info("Index exists, loading vector store")
        vs_manager.load_store()

    try:
        # === PHASE 1: RAG-based Analysis ===
        # Use retrieval-augmented generation to analyze code.
        logger.info("Starting the RAG pipeline by sending the code snippet")
        response, links, names, java_code = rag_pipeline.run(code_input, rule, message)
        # Returns: vulnerability analysis, CWE links, CWE names, initial secure Java code

        # === PHASE 2: Iterative Security Verification ===
        # Use CogniCrypt to verify and refine the generated secure code
        ccrunner = CCRUN(handler)
        try:
            final_code, verified = ccrunner.iterate_until_verified(java_code, max_iterations=iterations_cc, class_name=None)
        except RuntimeError as re_err:
            # Propagate normalized compilation signal upward
            if "COMPILATION_ERROR" in str(re_err):
                logger.error("Compilation error reported by CCRUN; re-raising for API mapping")
                raise
            # Non-compilation runtime error inside CCRUN — convert to generic error
            logger.exception("Runtime error during CCRUN")
            return {"error": "Analysis failed during secure verification."}
        except Exception as ex:
            # If deep code raised raw compile text, normalize here so the API can map it
            msg = str(ex)
            if "Compilation failed" in msg or "javac" in msg:
                logger.error("Raw compilation failure encountered; normalizing to COMPILATION_ERROR")
                raise RuntimeError("COMPILATION_ERROR")
            logger.exception("Unexpected error during CCRUN")
            return {"error": "Analysis failed during secure verification."}
        # This compiles, tests, and iteratively improves code until it passes security analysis
        
        # === PHASE 3: Final Code Processing ===  
        # Extract just the essential secure code snippet (not the full class)
        secure_snippet = handler.extract_fixed_snippet(code_input, final_code)
        if secure_snippet.lstrip().startswith("```"):
            logger.info("Fenced block detected at start of snippet, applying code sanitizer")
            cleaned = extract_java_source(secure_snippet)
            secure_snippet = cleaned if cleaned else secure_snippet
        # Generate a final technical explanation of the vulnerability and fix
        final_explanation = handler.final_explanation(code_input, final_code)

        # === PHASE 4: Response Formatting ===
        # Transform CWE links into structured format for frontend consumption
        cwe_links = [
            {
                "cwe": re.sub(r'.*/definitions/(\d+)\.html', r'CWE-\1', link),  # Extract CWE-### from URL
                "link": link   # Full URL to official CWE documentation
            }
            for link in links
        ]

        # Return comprehensive analysis results to the API caller
        logger.info("Response is returned via the API")
        return {
            "Vulnerability_name": response.vulnerability_name,        # High-level vulnerability type
            "Explanation": final_explanation,                        # Technical explanation post-verification  
            "CWE_references": cwe_links,                            # Structured CWE information with links
            "CogniCrypt_Verified": verified,                        # Boolean: did final code pass security scan?
            "Final_Secure_Code_Snippet": secure_snippet             # The verified secure replacement code
        }

    except RuntimeError as e:
        # If compilation normalization reached here, let the route handler map it.
        if "COMPILATION_ERROR" in str(e):
            logger.error("Analysis failed with COMPILATION_ERROR (re-raising to API layer)")
            raise
        # Other runtime errors -> structured error
        logger.exception("Runtime error in ai_fix")
        return {"error": "Analysis failed due to a runtime error."}

    except Exception as e:
        # Generic fallback for all non-compilation errors
        logger.error(f"Analysis failed: {str(e)}")
        return {"error": "Analysis failed during processing."}

def new_ai_fix(extracted_data: dict):
    logger.info("Starting the new AI fix pipeline...")

    all_node_details = extracted_data['all_node_details']
    error_trace = extracted_data['simplified_trace']['trace_flow']
    current_source_code = extracted_data['source_code']
    package_info = extracted_data['package_info']
    class_name = extracted_data['class_name']
    class_name = class_name.split('.')[-1]
    llm_model_name = extracted_data.get("llm_model", "openai")
    max_iterations = extracted_data.get("iterations", 3)

    try:
        # This logic is adapted from your original ai_fix function
        provider, selected_model = _parse_provider_and_model(llm_model_name)
        if provider == "OPENAI":
            api_key_env = "OPENAI_API_KEY"
        elif provider == "GEMINI":
            api_key_env = "GOOGLE_API_KEY"
        elif provider == "OLLAMA":
            api_key_env = None
        else:
            raise ValueError(f"Unsupported provider: {provider}")

        handler = get_handler(
            provider,
            **({"api_key": os.getenv(api_key_env)} if api_key_env else {}),
            model=selected_model,
            temperature=0.1,
        )
        ccrun_verifier = CCRUN(handler)
        logger.info(f"Tools initialized for provider={provider}")

        # Initialize Document Processor, Vector Store, and RAG Pipeline
        doc_processor = DocumentProcessor()
        vs_manager = VectorStoreManager()
        rag_pipeline = RAGPipeline(doc_processor, vs_manager, handler)

        # Set up or load the CWE knowledge base
        CWE_File_Path = r"data/CWE"
        if not os.path.exists("faiss_index"):
            logger.info("FAISS index not found, creating a new one...")
            chunks = doc_processor.load_and_split(CWE_File_Path)
            vs_manager.create_store(chunks)
            vs_manager.save_store()
            logger.info("FAISS index created successfully.")
        else:
            logger.info("Existing FAISS index found, loading vector store.")
            vs_manager.load_store()


        logger.info(f"Starting sequential processing of {len(error_trace)} errors in trace flow")

        # Track processing state
        processed_errors = []
        all_cwe_references = []
        current_code = current_source_code
        remaining_errors = error_trace.copy()  # Track which errors still need processing
        vulnerability_analysis = None
        all_vulnerability_analyses = []
        try:
            # Process each error in the trace flow sequentially
            iteration_count = 0

            while remaining_errors and iteration_count < len(error_trace) + 2:  # Safety limit
                iteration_count += 1
                current_error_hashcode = remaining_errors[0]  # Always process first remaining error

                logger.info(f"Iteration {iteration_count}: Processing error {current_error_hashcode}")
                logger.info(f"Remaining errors to process: {len(remaining_errors)}")

                # Find the corresponding node details
                current_error_node = None
                if isinstance(all_node_details, list):
                    all_node_details = {node['hashcode']: node for node in all_node_details}

                current_error_node = all_node_details.get(current_error_hashcode)

                if not current_error_node:
                    logger.warning(f"Could not find node details for error: {current_error_hashcode}")
                    remaining_errors.remove(current_error_hashcode)  # Remove from remaining list
                    continue

                # Build context from previously processed errors
                if processed_errors:
                    preceding_context = f"Previously fixed {len(processed_errors)} errors:\n"
                    for prev_error in processed_errors[-3:]:  # Last 3 for context
                        preceding_context += f"- {prev_error['hashcode']}: {prev_error['errorType']} on line {prev_error['line']}\n"
                else:
                    preceding_context = "First error in trace flow - no preceding fixes"

                logger.info(f"Using RAG pipeline for error: {current_error_node.get('errorType', 'unknown')}")

                # === PHASE 1: RAG-based Analysis and Fixing ===
                try:
                    fixed_code, cwe_links, cwe_names, current_vulnerability_analysis  = rag_pipeline.new_run(
                        error_node=current_error_node,
                        full_source_code=current_code,
                        preceding_context=preceding_context
                    )

                    logger.info(f"RAG pipeline completed for error {current_error_hashcode}")
                    if current_vulnerability_analysis:
                        all_vulnerability_analyses.append(current_vulnerability_analysis)
                        vulnerability_analysis = current_vulnerability_analysis
                    # Store CWE references for final response
                    if cwe_links:
                        all_cwe_references.extend([
                            {
                                "cwe": re.sub(r'.*/definitions/(\d+)\.html', r'CWE-\1', link),
                                "link": link
                            } for link in cwe_links
                        ])

                except Exception as e:
                    logger.error(f"RAG pipeline failed for error {current_error_hashcode}: {e}")
                    remaining_errors.remove(current_error_hashcode)  # Remove from remaining
                    continue

                # === PHASE 2: Iterative Security Verification ===
                logger.info(f"Starting CogniCrypt verification for error {current_error_hashcode}")

                try:
                    # Verify and refine the code using CogniCrypt
                    verified_code, is_verified = ccrun_verifier.iterate_until_verified(
                        initial_solution=fixed_code,
                        max_iterations=max_iterations,
                        class_name=class_name
                    )

                    logger.info(f"CogniCrypt verification completed. Verified: {is_verified}")

                    # Update current code for next iteration
                    current_code = verified_code

                    # === PHASE 3: SMART ERROR DETECTION ===
                    # Check which errors are still present after this fix
                    logger.info("Checking which errors remain after current fix...")

                    try:
                        # Run a fresh scan to see what errors still exist
                        fresh_sarif_path = ccrun_verifier.new_run_single_scan(current_code, class_name)
                        still_existing_errors = ccrun_verifier.new_get_violations_from_sarif(fresh_sarif_path)

                        # Get hashcodes/IDs of errors that still exist
                        still_existing_ids = set(still_existing_errors.keys())
                        logger.info(f"Errors still present: {still_existing_ids}")

                        # Remove resolved errors from remaining_errors list
                        originally_remaining = remaining_errors.copy()
                        remaining_errors = [err_id for err_id in remaining_errors if err_id in still_existing_ids]

                        resolved_count = len(originally_remaining) - len(remaining_errors)
                        if resolved_count > 0:
                            logger.info(f"SUCCESS: {resolved_count} additional errors were automatically resolved by fixing {current_error_hashcode}!")

                        # Remove the current error from remaining list (it's been processed)
                        if current_error_hashcode in remaining_errors:
                            remaining_errors.remove(current_error_hashcode)

                    except Exception as scan_error:
                        logger.warning(f"Fresh scan failed: {scan_error}. Continuing with original error list.")
                        # Fallback: just remove current error
                        if current_error_hashcode in remaining_errors:
                            remaining_errors.remove(current_error_hashcode)

                    # Add to processed errors list
                    processed_error_info = {
                        "hashcode": current_error_hashcode,
                        "errorType": current_error_node.get("errorType", "unknown"),
                        "line": current_error_node.get("line", "unknown"),
                        "verified": is_verified,
                        "cwe_count": len(cwe_links) if cwe_links else 0,
                        "iteration": iteration_count
                    }
                    processed_errors.append(processed_error_info)

                    logger.info(f"Error {current_error_hashcode} processing completed. Remaining: {len(remaining_errors)}")

                except RuntimeError as re_err:
                    if "COMPILATION_ERROR" in str(re_err):
                        logger.error(f"Compilation error for error {current_error_hashcode}: {re_err}")
                        # Remove from remaining and try next
                        if current_error_hashcode in remaining_errors:
                            remaining_errors.remove(current_error_hashcode)
                        continue
                    else:
                        logger.error(f"Runtime error during verification for {current_error_hashcode}: {re_err}")
                        if current_error_hashcode in remaining_errors:
                            remaining_errors.remove(current_error_hashcode)
                        continue

                except Exception as e:
                    logger.error(f"Verification failed for error {current_error_hashcode}: {e}")
                    if current_error_hashcode in remaining_errors:
                        remaining_errors.remove(current_error_hashcode)
                    continue

            # === PHASE 4: Final Processing ===
            logger.info("Sequential processing completed. Generating final response...")

            # Generate final explanation considering all processed errors
            final_explanation = handler.final_explanation(
                f"Original code with {len(processed_errors)} trace errors",
                current_code
            )

            # Calculate overall verification status
            verified_count = sum(1 for e in processed_errors if e.get("verified", False))
            overall_verified = verified_count == len(processed_errors) and len(processed_errors) > 0

            # Remove duplicate CWE references
            unique_cwe_refs = []
            seen_cwes = set()
            for cwe_ref in all_cwe_references:
                cwe_id = cwe_ref["cwe"]
                if cwe_id not in seen_cwes:
                    unique_cwe_refs.append(cwe_ref)
                    seen_cwes.add(cwe_id)

            auto_resolved_count = len(error_trace) - len(processed_errors)

            logger.info(f"Final result: {len(processed_errors)} errors manually processed, {auto_resolved_count} auto-resolved, {verified_count} verified")


            logger.info("I reached final response area")
            if vulnerability_analysis:
                vulnerability_name = vulnerability_analysis.vulnerability_name
            elif all_vulnerability_analyses:
                # Use the first analysis for consistency
                vulnerability_name = all_vulnerability_analyses[0].vulnerability_name
            elif processed_errors:
                # Generate name based on processed errors
                main_error_type = processed_errors[0].get('errorType', 'Unknown')
                vulnerability_name = f"Cryptographic {main_error_type} in Multi-node Trace"
            else:
                # Complete fallback
                vulnerability_name = f"Multi-node Trace Analysis ({len(error_trace)} errors)"

            # === PHASE 5: Enhanced Response Formatting ===

            if current_code.lstrip().startswith("```"):
                logger.info("Fenced block detected at start of snippet, applying code sanitizer")
                cleaned = extract_java_source(current_code)
                current_code = cleaned if cleaned else current_code
            else:
                logger.info("No fenced block detected, applying code sanitizer for cleanup")
                # Still sanitize even if no fenced blocks to remove any unwanted text
                cleaned = extract_java_source(current_code)
                current_code = cleaned if cleaned else current_code

            if package_info:
                logger.info(f"Adding package declaration back: {package_info}")
                current_code = f"{package_info}\n\n{current_code}"

            cleanup_generated_files()

            return {
                "Vulnerability_name": vulnerability_name,
                "Explanation": final_explanation,
                "CWE_references": unique_cwe_refs,
                "CogniCrypt_Verified": overall_verified,
                "Final_Secure_Code_Snippet": current_code,
                "Processing_Details": {
                    "total_errors_in_trace": len(error_trace),
                    "manually_processed": len(processed_errors),
                    "auto_resolved_by_dependencies": auto_resolved_count,
                    "verified_errors": verified_count,
                    "total_iterations": iteration_count,
                    "processed_errors": processed_errors
                }
            }

        except Exception as e:
            cleanup_generated_files()
            logger.error(f"Sequential processing failed: {e}", exc_info=True)
            return {
                "error": f"Sequential processing failed: {str(e)}",
                "processed_errors": len(processed_errors) if 'processed_errors' in locals() else 0
            }


    except Exception as e:
        cleanup_generated_files()
        logger.error(f"Failed to initialize tools: {e}", exc_info=True)
        return {"error": "Tool initialization failed."}
