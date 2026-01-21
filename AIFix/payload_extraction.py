import json
import os
import re
from logger_config import get_logger

# --- Logger Setup ---
logger = get_logger(__name__)

# --- Helper Functions (Internal to this module) ---
def _extract_and_strip_package(source_code):
    """
    Finds and removes the package declaration from Java source code.
    """
    package_pattern = re.compile(r"^\s*package\s+[\w\.]+;\s*$", re.MULTILINE)
    match = package_pattern.search(source_code)
    if match:
        package_declaration = match.group(0).strip()
        stripped_code = source_code[:match.start()] + source_code[match.end():]
        return package_declaration, stripped_code.strip()
    return None, source_code

def _save_source_to_file(class_name, source_code):
    """
    Saves the given source code string to a correctly named .java file.
    """
    if not source_code:
        logger.warning(f"No source code provided for {class_name}, skipping file save.")
        return
    work_dir = os.path.abspath("GeneratedCode")
    os.makedirs(work_dir, exist_ok=True)
    simple_name = class_name.split('.')[-1]
    file_name = f"{simple_name}.java"
    full_path = os.path.join(work_dir, file_name)
    try:
        with open(full_path, 'w', encoding='utf-8') as f:
            f.write(source_code)
        logger.info(f"Source code for '{class_name}' saved to '{os.path.abspath(full_path)}'")
    except IOError as e:
        logger.error(f"Error saving file '{file_name}': {e}")

def _extract_node_details(node):
    """Extracts detailed information from a single error node."""
    # ... (function content is the same as before) ...
    if not node: return {}
    report_location = node.get('reportLocation', {})
    return {
        "hashcode": node.get('hashcode'), "severity": node.get('severity'), "line": node.get('line'),
        "message": node.get('message'), "codeSnippet": node.get('codeSnippet'),
        "errorType": node.get('errorType'), "rule": node.get('rule'), "method": node.get('method'),
        "className": report_location.get('className'), "filePath": report_location.get('filePath'),
        "start_location": report_location.get('start'), "end_location": report_location.get('end'),
        "precedingErrors": node.get('precedingErrors', []), "subsequentErrors": node.get('subsequentErrors', [])
    }

# --- Main Processing Function ---
def process_payload(payload: dict) -> dict:
    """
    Processes the detailed payload to extract all necessary information for the fixing pipeline.
    """
    logger.info("Starting payload extraction and processing...")

    # --- Validate Payload ---
    selected_node_data = payload.get('selectedNode')
    full_path = payload.get('fullPathFromRootToBottom')
    source_code_analysis = payload.get('sourceCodeAnalysis')
    if not all([selected_node_data, full_path, source_code_analysis]):
        logger.error("Payload is missing required keys.")
        raise ValueError("Payload is missing required keys")

    # --- 1. Extract details for all nodes ---
    logger.info("--- Detailed Information for All Nodes ---")
    all_nodes_details = [_extract_node_details(node) for node in full_path]
    logger.info(json.dumps(all_nodes_details, indent=2))

    # --- 2. Build simplified error trace flow ---
    logger.info("--- Simplified Error Trace Flow ---")
    # ... (function content is the same as before) ...
    error_map = {node['hashcode']: node for node in full_path}
    root_node = next((node for node in full_path if not node.get('precedingErrors')), None)
    error_trace_flow = []
    if root_node:
        current_node = root_node
        while current_node:
            error_trace_flow.append(current_node.get('hashcode'))
            subsequent_hashes = current_node.get('subsequentErrors', [])
            current_node = error_map.get(subsequent_hashes[0]) if subsequent_hashes else None
    simplified_trace = {"selected_node_hashcode": selected_node_data.get('hashcode'), "trace_flow": error_trace_flow}
    logger.info(json.dumps(simplified_trace, indent=2))

    # --- 3. Process Source Code ---
    logger.info("--- Source Code Analysis ---")
    # ... (function content is the same as before) ...
    source_code_map = {item['nodeId']: item['fullSourceCode'] for item in source_code_analysis}
    details_map = {node['hashcode']: node for node in all_nodes_details}
    class_names_in_trace_order = [details_map[hashcode]['className'] for hashcode in error_trace_flow]
    seen_classes = set()
    ordered_unique_classes = [cls for cls in class_names_in_trace_order if not (cls in seen_classes or seen_classes.add(cls))]

    if len(ordered_unique_classes) == 1:
        class_name = ordered_unique_classes[0]
        full_source_code = source_code_map.get(error_trace_flow[0])
        package_info, stripped_code = _extract_and_strip_package(full_source_code)
        logger.info(f"Extracted Package: {package_info}")
        _save_source_to_file(class_name, stripped_code)
    else:
        raise NotImplementedError("The fixing pipeline currently supports only single-class files.")

    # --- 4. Extract LLM Model and Iterations ---
    llm_model = payload.get("llm_model", "openai")  # Default to openai if not provided
    iterations = payload.get("iterations", 3)      # Default to 3 iterations if not provided
    logger.info(f"LLM model requested: {llm_model}, Iterations: {iterations}")

    logger.info("Payload extraction complete.")

    # --- 5. Return all extracted data in a dictionary ---
    return {
        "all_node_details": {node['hashcode']: node for node in all_nodes_details},
        "simplified_trace": simplified_trace,
        "source_code": stripped_code,
        "package_info": package_info,
        "class_name": class_name,
        "llm_model": llm_model,
        "iterations": iterations
    }