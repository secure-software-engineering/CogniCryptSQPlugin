import subprocess
import os
import time
from pydantic_models.VulnerabilityAnalysis import VulnerabilityAnalysis
from llm_files import get_handler
from logger_config import get_logger
from typing import Tuple
import json
import glob
from utils.code_sanitizer import extract_java_source


logger = get_logger(__name__)

def find_sarif_report(report_dir):
    # Look for SARIF JSON files in the folder
    matches = glob.glob(os.path.join(report_dir, "*.json"))
    if not matches:
        matches = glob.glob(os.path.join(report_dir, "*.sarif"))
    if not matches:
        raise FileNotFoundError(f"No SARIF file found in {report_dir}")
    return matches[0]  # Return the first match

class CCRUN:
    def __init__(self, llm_handler):
        self.llm = llm_handler

    def sarif_has_violations(self, path: str) -> bool:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return len(data["runs"][0].get("results", [])) > 0

    def iterate_until_verified(self, initial_solution: str, max_iterations: int = 10, class_name=None) -> Tuple[str, bool]:
        current_solution = initial_solution

        for i in range(max_iterations):
            logger.info(f" Iteration {i + 1} - Saving, compiling, and analyzing Java code")

            work_dir = os.path.abspath("GeneratedCode")
            os.makedirs(work_dir, exist_ok=True)
            # Step 1: Save code
            if class_name:
                java_filename = f"{class_name}.java"
            else:
                java_filename = "Main.java"

            java_path = os.path.join(work_dir, java_filename)
            save_llm_output(current_solution, java_path)
            logger.info(f"[Iteration {i + 1}] Using class name: {java_filename}")

            logger.info(f"[Iteration {i + 1}]  Java code saved to {java_path}")

            # Step 2: Compile code
            # around the compile step
            try:
                class_path = compile_java(java_path)
            except Exception:
                # log the detailed error internally
                logger.error("Compilation failed", exc_info=True)  # keep details in logs
                # raise a normalized error for the API layer
                raise RuntimeError("COMPILATION_ERROR")

            # Step 3: Package into .jar
            try:
                jar_path = convert_to_jar(class_path)
                logger.info(f"[Iteration {i + 1}]  JAR file created at: {jar_path}")
            except Exception as e:
                logger.error(f"[Iteration {i + 1}]  JAR creation failed: {str(e)}")
                raise

            # Step 4: Run CogniCrypt analysis
            sarif_report_dir = os.path.abspath("GeneratedCode")
            os.makedirs(sarif_report_dir, exist_ok=True)
            logger.info(f"[Iteration {i + 1}]  Running CogniCrypt...")

            run_cognicrypt(
                scanner_jar_path=r"CCJar/HeadlessJavaScanner-5.0.1-SNAPSHOT-jar-with-dependencies.jar",
                app_jar_path=jar_path,
                rules_dir=r"JCA-CrySL-rules",
                report_format="SARIF",
                report_path=sarif_report_dir
            )

            # Wait for SARIF file to appear in the folder
            timeout = 10  # seconds
            waited = 0
            sarif_file = None
            while waited < timeout:
                try:
                    sarif_file = find_sarif_report(sarif_report_dir)
                    break
                except FileNotFoundError:
                    time.sleep(0.2)
                    waited += 0.2

            if not sarif_file:
                logger.error(f"SARIF report not found in directory after waiting: {sarif_report_dir}")
                raise FileNotFoundError(f"SARIF report not found in directory after waiting: {sarif_report_dir}")

            # Step 5: Check SARIF results
            if not self.sarif_has_violations(sarif_file):
                logger.info(f"[Iteration {i + 1}]  No violations found. CogniCrypt verification successful.")
                return current_solution, True

            logger.info(f"[Iteration {i + 1}]  Violations detected — passing SARIF to LLM for refinement")

            with open(sarif_file, "r", encoding="utf-8") as f:
                sarif_json = f.read()

            # Step 6: Refine with LLM
            current_solution = self.llm.improve_based_on_sarif(
                previous_code=current_solution,
                sarif_json=sarif_json
            )
            logger.info(f"[Iteration {i + 1}]  LLM provided refined code — continuing to next round")

        logger.warning("Max iterations reached. Final code may still contain violations.")
        return current_solution, False

    def new_get_violations_from_sarif(self, sarif_path: str):
        logger.info(f"Parsing SARIF report at: {sarif_path}")
        error_graph = {}
        try:
            with open(sarif_path, "r", encoding="utf-8") as f:
                data = json.load(f)

            results = data.get("runs", [{}])[0].get("results", [])

            for res in results:
                error_id = res.get("errorID")
                if not error_id:
                    continue

                location = res.get("locations", [[]])[0][0]

                error_graph[error_id] = {
                    "violatedRule": res.get("violatedRule"),
                    "message": res.get("message", {}).get("text"),
                    "startLine": location.get("physicalLocation", {}).get("region", {}).get("startLine"),
                    "precedingErrors": res.get("precedingErrors", []),
                    "subsequentErrors": res.get("subsequentErrors", [])
                }

            logger.info(f"Found {len(error_graph)} violations in SARIF report and built error graph.")
            return error_graph
        except (IOError, json.JSONDecodeError, KeyError, IndexError) as e:
            logger.error(f"Could not parse SARIF file at {sarif_path}: {e}", exc_info=True)
            return {}

    def new_run_single_scan(self, java_code: str, class_name: str) -> str:
        logger.info(f"Starting single scan for class: {class_name}")

        # --- Define paths locally, mirroring the original function's logic ---
        work_dir = os.path.abspath("GeneratedCode")
        scanner_jar = os.path.abspath(r"CCJar/HeadlessJavaScanner-5.0.1-SNAPSHOT-jar-with-dependencies.jar")
        rules_dir = os.path.abspath(r"JCA-CrySL-rules")
        os.makedirs(work_dir, exist_ok=True)

        java_path = os.path.join(work_dir, f"{class_name}.java")

        try:
            save_llm_output(java_code, java_path)
            class_path = compile_java(java_path)
            jar_path = convert_to_jar(class_path)

            run_cognicrypt(
                scanner_jar_path=scanner_jar,
                app_jar_path=jar_path,
                rules_dir=rules_dir,
                report_format="SARIF",
                report_path=work_dir
            )

            timeout = 10
            start_time = time.time()
            while time.time() - start_time < timeout:
                try:
                    return find_sarif_report(work_dir)
                except FileNotFoundError:
                    time.sleep(0.5)

            raise FileNotFoundError(f"SARIF report not found in {work_dir} after {timeout} seconds.")

        except Exception as e:
            # The logger is already called in the helper functions, so just re-raise
            raise


def save_llm_output(code_str: str, filepath: str) -> str:
    code_str = extract_java_source(code_str)
    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write(code_str)
    return filepath


def compile_java(filepath: str) -> str:
    compile_result = subprocess.run(["javac", "-encoding", "UTF-8", filepath],
                                    capture_output=True, text=True)
    if compile_result.returncode != 0:
        raise Exception(f"Compilation failed:\n{compile_result.stderr}")
    class_file = filepath.replace(".java", ".class")
    if not os.path.exists(class_file):
        raise FileNotFoundError(f"Expected .class file not found: {class_file}")
    return class_file


def convert_to_jar(java_class_path: str) -> str:
    jar_file_name = java_class_path.replace(".class", ".jar")
    class_dir = os.path.dirname(java_class_path)
    class_file = os.path.basename(java_class_path)

    if not os.path.exists(java_class_path):
        raise FileNotFoundError(f"Class file does not exist: {java_class_path}")

    java_version = subprocess.run(["java", "--version"], text=True, capture_output=True)
    print("JAVA VERSION:", java_version.stdout.strip())

    original_cwd = os.getcwd()
    os.chdir(class_dir or ".")

    jar_result = subprocess.run(
        ["jar", "cf", os.path.basename(jar_file_name), class_file],
        capture_output=True, text=True
    )

    os.chdir(original_cwd)

    if jar_result.returncode != 0:
        raise Exception(f"JAR creation failed:\n{jar_result.stderr}")

    return os.path.join(class_dir, os.path.basename(jar_file_name))

def run_cognicrypt(scanner_jar_path, app_jar_path, rules_dir, report_format, report_path):
    cmd = [
        "java", "-jar", scanner_jar_path,
        "--rulesDir", rules_dir,
        "--appPath", app_jar_path,
        "--reportFormat", report_format,
        "--reportPath", report_path
    ]
    try:
        logger.info("Running command: " + " ".join(cmd))
        result = subprocess.run(cmd, check=True, capture_output=True, text=True)
        logger.info("Analysis completed successfully.")
        logger.info("STDOUT:\n" + result.stdout)
        logger.info("STDERR:\n" + result.stderr)
    except subprocess.CalledProcessError as e:
        logger.error("Error during analysis:")
        logger.error(e.stderr)
        raise