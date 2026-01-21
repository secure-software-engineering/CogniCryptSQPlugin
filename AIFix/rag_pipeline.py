import os
import re
import pandas as pd
from typing import Tuple, List, Set
from logger_config import get_logger
from pydantic_models.VulnerabilityAnalysis import VulnerabilityAnalysis

logger = get_logger(__name__)

# CSV path for Excel-based CWE mapping
CSV_PATH = "CWE_Mapping/CWE_Mapping.csv"

class CWEMapper:
    def __init__(self):
        try:
            if not os.path.exists(CSV_PATH):
                raise FileNotFoundError(f"CWE mapping file not found: {CSV_PATH}")
            
            self.mapping_df = pd.read_csv(CSV_PATH)
            
            # Validate required columns
            required_columns = ["CrySL File", "CWE-ID(s)"]
            missing_columns = [col for col in required_columns if col not in self.mapping_df.columns]
            if missing_columns:
                raise ValueError(f"Missing required columns in CSV: {missing_columns}")
            
            self.mapping_df.fillna("", inplace=True)
            logger.info(f"Loaded CWE mapping from {CSV_PATH} with {len(self.mapping_df)} rows.")
            
        except Exception as e:
            logger.error(f"Failed to initialize CWE mapper: {e}")
            raise

    # In CWEMapper.get_static_cwe_ids() method
    def get_static_cwe_ids(self, crysl_rule: str) -> Set[str]:
        logger.debug(f"Looking up static CWE IDs for CrySL rule: {crysl_rule}")
        
        # Generate possible rule name variants
        rule_variants = []
        
        # Original rule as provided
        rule_variants.append(crysl_rule)
        
        # Add .crysl extension if not present
        if not crysl_rule.endswith('.crysl'):
            rule_variants.append(f"{crysl_rule}.crysl")
        
        # Handle full Java class names (e.g., "java.security.KeyPairGenerator")
        if "." in crysl_rule:
            class_name = crysl_rule.split(".")[-1]  # Extract "KeyPairGenerator"
            rule_variants.append(class_name)
            rule_variants.append(f"{class_name}.crysl")
        
        logger.debug(f"Rule variants to search: {rule_variants}")
        
        cwe_ids = set()
        found_variant = None
        
        for variant in rule_variants:
            # Filter by CrySL File column
            filtered = self.mapping_df[
                self.mapping_df["CrySL File"].str.strip().str.lower() == variant.strip().lower()
            ]
            
            if not filtered.empty:
                found_variant = variant
                for _, row in filtered.iterrows():
                    cwe = row["CWE-ID(s)"]
                    if pd.notna(cwe):
                        cwe_ids.update([c.strip() for c in str(cwe).split(",") if c.strip()])
                break  # Stop on first match
        
        if cwe_ids:
            logger.info(f"Static CWE IDs found for {crysl_rule} (matched as '{found_variant}'): {cwe_ids}")
        else:
            logger.warning(f"No static CWE IDs found for {crysl_rule} (tried variants: {rule_variants})")
        
        return cwe_ids


class RAGPipeline:
    def __init__(self, document_processor, vector_store_manager, llm_handler):
        self.document_processor = document_processor
        self.vs_manager = vector_store_manager
        self.llm = llm_handler

    def run(self, vulnerable_code: str, rule: str, message: str) -> Tuple[VulnerabilityAnalysis, List[str], List[str], str]:
        logger.info("Starting the run function to create context")
        
        CryslRules_Path = r"data/Crysl_Rules"
        ErrorDesc_Path = r"data/CogniCrypt_ErrorDesc"
        
        context = ""
        error_type = rule.split(":")[1].split("_")[0]
        crysl_rule = rule.split("_")[1]
        desc_file = f"{ErrorDesc_Path}/{error_type}.json"

        # Load CrySL rule file
        rule_path = os.path.join(CryslRules_Path, crysl_rule + ".txt")
        if os.path.isfile(rule_path):
            with open(rule_path, 'r', encoding='utf-8') as file:
                context += f"\n\nCrySL Rule: {crysl_rule}\n{file.read()}"

        # Add static error description
        context += "\n\nStatic Error Descriptions\n"
        context += self.document_processor.error_description_processing(desc_file, crysl_rule)
        logger.info("Context built successfully")

        # RAG search query
        query = self.llm.build_query(vulnerable_code, context)
        logger.info(f"Optimized search query: {query}")

        results = self.vs_manager.vector_store.similarity_search(query, k=3)
        logger.info("Vector DB search completed for relevant CWEs")

        # Load static CWE IDs from Excel/CSV
        try:
            cwe_mapper = CWEMapper()
            static_cwe_ids = cwe_mapper.get_static_cwe_ids(crysl_rule)
            logger.info(f"Successfully loaded {len(static_cwe_ids)} static CWE IDs from Excel")
        except Exception as e:
            logger.error(f"Failed to retrieve static CWE IDs for '{crysl_rule}': {e}")
            static_cwe_ids = set()

        # Extract dynamic CWE IDs and doc content from vector DB
        dynamic_cwe_ids = set()
        cwe_doc_map = {}
        for doc in results:
            doc_id = str(doc.metadata.get("doc_id"))
            if doc_id:
                dynamic_cwe_ids.add(f"CWE-{doc_id}")
                cwe_doc_map[doc_id] = doc.page_content

        logger.info(f"Dynamic CWE IDs from vector DB: {dynamic_cwe_ids}")
        logger.info(f"Static CWE IDs from Excel: {static_cwe_ids}")

                # Combine static + dynamic with proper formatting
        combined_cwe_ids = set()
        
        # Add static CWE IDs (ensure CWE- prefix)
        for cwe in static_cwe_ids:
            formatted_cwe = cwe if cwe.startswith("CWE-") else f"CWE-{cwe}"
            combined_cwe_ids.add(formatted_cwe)
        
        # Add dynamic CWE IDs
        combined_cwe_ids.update(dynamic_cwe_ids)

        logger.info(f"Combined CWE candidates for '{crysl_rule}': {combined_cwe_ids}")
        logger.info(f"Pipeline statistics - Static CWEs: {len(static_cwe_ids)}, "
                   f"Dynamic CWEs: {len(dynamic_cwe_ids)}, "
                   f"Total unique CWEs: {len(combined_cwe_ids)}")

        # NEW: Use LLM to select most relevant CWE IDs
        if combined_cwe_ids:
            selected_cwe_ids = self.llm.select_relevant_cwes(
                vulnerable_code=vulnerable_code,
                context=context,
                error_message=message,
                candidate_cwe_ids=list(combined_cwe_ids)
            )
            logger.info(f"LLM selected top relevant CWEs: {selected_cwe_ids}")
        else:
            selected_cwe_ids = []
            logger.warning("No CWE candidates found for LLM selection")

        # Build links and names list from LLM-selected CWEs
        links_list = []
        names_list = []

        for cwe_id in selected_cwe_ids:
            cwe_num = cwe_id.replace("CWE-", "").strip()
            link = f"https://cwe.mitre.org/data/definitions/{cwe_num}.html"
            links_list.append(link)

        logger.info(f"Generated {len(links_list)} CWE links from LLM selection")


            # Try to extract name from vector DB match
            # name = None
            # if cwe_num in cwe_doc_map:
            #     match = re.search(r"Name:\s*(.+)", str(cwe_doc_map[cwe_num]))
            #     if match:
            #         name = match.group(1).strip()
            
            # names_list.append(name if name else f"{cwe_id} (name unavailable)")

        logger.info(f"CWE links - {links_list}")

        # Vulnerability analysis
        logger.info("Calling the analyse_vulnerability function which performs analysis of code snippet")
        response = self.llm.analyse_vulnerability(context, vulnerable_code)
        logger.info("Vulnerability analysis completed")
        logger.info(vars(response))

        # Secure Java generation
        response1 = self.llm.cogniCrypt_analysis(response.possible_solution)
        logger.info(f"Generated Java class for CogniCrypt:\n{response1}")

        # Final return (unchanged)
        return response, links_list, names_list, response1


    def new_run(self, error_node: dict, full_source_code: str,
                preceding_context: str = "", sarif_report: str = None,
                compilation_error: str = None):
        # Determine error identification method
        error_id = error_node.get("hashcode") or error_node.get("nodeId", "unknown")
        logger.info(f"Starting trace-aware processing for error: {error_id}")

        CryslRules_Path = r"data/Crysl_Rules"
        ErrorDesc_Path = r"data/CogniCrypt_ErrorDesc"
        context = ""

        # Extract rule and message from error node (handle both formats)
        rule = error_node.get("rule", "")
        message = error_node.get("message", "")

        # Handle SARIF report format vs initial payload format
        if not rule and sarif_report:
            # Extract from SARIF format: "violatedRule" instead of "rule"
            rule = error_node.get("violatedRule", "")

        if not rule:
            logger.warning("No rule found in error node, using minimal context")
            error_type = "unknown"
            crysl_rule = "unknown"
        else:
            # Parse rule format (same as original)
            try:
                # Handle different rule formats:
                # Initial payload: "java.security.KeyPairGenerator"
                # SARIF: "javax.crypto.Cipher"
                if ":" in rule:
                    error_type = rule.split(":")[1].split("_")[0].lower()
                    crysl_rule = rule.split("_")[1]
                else:
                    # Direct rule name (SARIF format)
                    crysl_rule = rule
                    error_type = error_node.get("errorType", "unknown").lower()
            except (IndexError, AttributeError):
                logger.warning(f"Could not parse rule format: {rule}")
                error_type = "unknown"
                crysl_rule = rule if rule else "unknown"

        desc_file = f"{ErrorDesc_Path}/{error_type}.json"

        # Load CrySL rule file (same as original)
        if crysl_rule != "unknown":
            # Handle both file formats: direct name or with .txt
            rule_variants = [
                f"{crysl_rule}.txt",
                f"{crysl_rule.split('.')[-1]}.txt",  # Last part after dot
                f"{crysl_rule.replace('.', '_')}.txt"  # Replace dots with underscores
            ]

            rule_loaded = False
            for variant in rule_variants:
                rule_path = os.path.join(CryslRules_Path, variant)
                if os.path.isfile(rule_path):
                    with open(rule_path, 'r', encoding='utf-8') as file:
                        context += f"\n\nCrySL Rule: {crysl_rule}\n{file.read()}"
                        rule_loaded = True
                        logger.info(f"Loaded CrySL rule from: {variant}")
                        break

            if not rule_loaded:
                logger.warning(f"Could not find CrySL rule file for: {crysl_rule}")

        # Add static error description (same as original)
        context += "\n\nStatic Error Descriptions\n"
        context += self.document_processor.error_description_processing(desc_file, crysl_rule)

        # NEW: Add trace-specific context
        if preceding_context:
            context += f"\n\nTrace Context:\n{preceding_context}"

        # Add error node specific details (handle both formats)
        context += f"\n\nCurrent Error Details:\n"
        context += f"Line: {error_node.get('line', error_node.get('startLine', 'unknown'))}\n"
        context += f"Error ID: {error_id}\n"
        context += f"Error Type: {error_node.get('error_type', error_node.get('errorType', 'unknown'))}\n"

        # Add method and file information if available
        if error_node.get("method"):
            context += f"Method: {error_node.get('method')}\n"
        elif error_node.get("locations"):
            # SARIF format method extraction
            try:
                method_info = error_node["locations"][0][0]["physicalLocation"]["region"].get("method", "unknown")
                context += f"Method: {method_info}\n"
            except (KeyError, IndexError, TypeError):
                pass

        logger.info("Context built successfully for trace node")

        # ===== REUSE: CWE Analysis Pipeline (same as original run() method) =====

        # Build RAG search query (using error node details instead of vulnerable_code snippet)
        search_context = f"Error: {message}\nRule: {rule}\nLine: {error_node.get('line', error_node.get('startLine', ''))}"
        query = self.llm.build_query(search_context, context)
        logger.info(f"Optimized search query for trace node: {query}")

        # Vector DB search (same as original)
        results = self.vs_manager.vector_store.similarity_search(query, k=3)
        logger.info("Vector DB search completed for relevant CWEs")

        # Load static CWE IDs from Excel/CSV (same as original)
        try:
            cwe_mapper = CWEMapper()
            static_cwe_ids = cwe_mapper.get_static_cwe_ids(crysl_rule)
            logger.info(f"Successfully loaded {len(static_cwe_ids)} static CWE IDs from Excel")
        except Exception as e:
            logger.error(f"Failed to retrieve static CWE IDs for '{crysl_rule}': {e}")
            static_cwe_ids = set()

        # Extract dynamic CWE IDs (same as original)
        dynamic_cwe_ids = set()
        cwe_doc_map = {}
        for doc in results:
            doc_id = str(doc.metadata.get("doc_id"))
            if doc_id:
                dynamic_cwe_ids.add(f"CWE-{doc_id}")
                cwe_doc_map[doc_id] = doc.page_content

        logger.info(f"Dynamic CWE IDs from vector DB: {dynamic_cwe_ids}")
        logger.info(f"Static CWE IDs from Excel: {static_cwe_ids}")

        # Combine static + dynamic CWE IDs (same as original)
        combined_cwe_ids = set()

        # Add static CWE IDs (ensure CWE- prefix)
        for cwe in static_cwe_ids:
            formatted_cwe = cwe if cwe.startswith("CWE-") else f"CWE-{cwe}"
            combined_cwe_ids.add(formatted_cwe)

        # Add dynamic CWE IDs
        combined_cwe_ids.update(dynamic_cwe_ids)

        logger.info(f"Combined CWE candidates for trace node '{crysl_rule}': {combined_cwe_ids}")

        # LLM CWE selection (same as original)
        if combined_cwe_ids:
            selected_cwe_ids = self.llm.select_relevant_cwes(
                vulnerable_code=search_context,  # Use error context instead of code snippet
                context=context,
                error_message=message,
                candidate_cwe_ids=list(combined_cwe_ids)
            )
            logger.info(f"LLM selected top relevant CWEs for trace node: {selected_cwe_ids}")
        else:
            selected_cwe_ids = []
            logger.warning("No CWE candidates found for LLM selection")

        # Build links and names list (same as original)
        links_list = []
        names_list = []
        for cwe_id in selected_cwe_ids:
            cwe_num = cwe_id.replace("CWE-", "").strip()
            link = f"https://cwe.mitre.org/data/definitions/{cwe_num}.html"
            links_list.append(link)

        logger.info(f"Generated {len(links_list)} CWE links from LLM selection")

        # ===== NEW: Trace-Aware Code Fixing (different from original) =====

        logger.info("Calling trace-aware targeted error fixing")

        # Use the new_fix_targeted_error method from base.py (already implemented!)
        fixed_code = self.llm.new_fix_targeted_error(
            full_code=full_source_code,
            error_details=error_node,
            sarif_report=sarif_report,
            compilation_error=compilation_error
        )

        logger.info("Trace-aware error fixing completed")

        vulnerability_analysis = self.llm.analyse_vulnerability(
            context=context,
            question=search_context  # Using error context instead of code snippet
        )

        # Return fixed code and CWE information
        return fixed_code, links_list, names_list, vulnerability_analysis