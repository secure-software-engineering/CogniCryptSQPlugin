from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from pydantic_models.VulnerabilityAnalysis import VulnerabilityAnalysis
from logger_config import get_logger
logger = get_logger(__name__)

# IMPROVED VERSION
DBSearch_Prompt = ChatPromptTemplate.from_template(
"""Generate a concise, semantically enriched search query for a CWE vector database.

Input Context:
- Code snippet: {code}
- Security context: {context}

Requirements:
1. Focus on HIGH-LEVEL vulnerability patterns and security concepts
2. Use technical security terminology that matches CWE descriptions
3. Exclude: CWE identifiers, specific class/package names, implementation details
4. Include: vulnerability types, security mechanisms, cryptographic concepts

Output: Single line, optimized for semantic vector similarity search.

Examples of good queries:
- "weak cryptographic algorithm implementation"
- "insufficient entropy random number generation"
- "hardcoded cryptographic key usage"
"""
)

CWE_Selection_Prompt = ChatPromptTemplate.from_template(
"""You are a cybersecurity expert specializing in precise CWE classification.

ANALYSIS INPUTS:
- Vulnerable Code: {vulnerable_code}
- CrySL Rule Context: {context}
- Error Message: {error_message}
- Candidate CWE IDs: {candidate_cwe_ids}

SELECTION CRITERIA (in order of importance):
1. **Root Cause Match**: Does the CWE directly address the underlying vulnerability?
2. **Cryptographic Relevance**: How well does it align with JCA/cryptographic context?
3. **Error Pattern Alignment**: Does it match the specific error manifestation?

TASK: Select exactly 3 CWE IDs that best match the vulnerability, ranked by relevance.

OUTPUT FORMAT: CWE-XXX, CWE-YYY, CWE-ZZZ
- No explanations
- No brackets or quotes  
- Comma-separated only
- Most relevant first

Be highly selective - prefer precision over coverage."""
)


CodeAnalysis_Prompt = ChatPromptTemplate.from_template(
"""
You are Java Cryptography Architecture (JCA) developer and you are tasked with analyzing a given code
snippet and providing a secure alternate code snippet with modern standards. Analyse the given code
snippet: {question} Relevant Context: {context}
Based on this information, give your analysis, and provide a secure code snippet.
Return only:
Vulnerability Name: [Name]
Possible Solution: [Few lines of code]
Explanation: [Text explanation of the issue and the solution, maximum 150 words]
IMPORTANT: Do not change the output format, and ensure the possible solution is always a few lines of code
IMPORTANT: The solution should just be the code snippet fixing the logic, do not add import statements, create
functions or try-catch blocks
Also do not add comments inside the possible solution, but integrate the logic behind them in the explanation section
"""
)

CogniCrypt_Prompt = ChatPromptTemplate.from_template(
"""
You are a Java Cryptography Architecture (JCA) expert.

You had provided me with this code snippet after the user had given you an insecure code snippet. Here is a secure solution for a vulnerability you provided:
{possible_solution}

Use this logic to generate a full self-contained Java class named `Main`. 
Do NOT wrap your response in triple backticks.
Do NOT include markdown, quotes, or explanations — return only raw Java code.

Return only the valid Java source code, starting with import statements.
"""
)

SARIF_Repair_Prompt = ChatPromptTemplate.from_template(
"""
You wrote the following Java code:

{previous_code}

This code was analyzed by CogniCrypt, and the following SARIF JSON report was generated, which contains one or more security violations:

{sarif_json}

Update the code to fix all security issues described in the SARIF report.
Do not return markdown, explanations, comments, or code fences.
Return only valid Java source code (starting with `import` lines).
"""
)
ExtractSnippet_Prompt = ChatPromptTemplate.from_template(
"""
You generated this given secure code snippet after correcting the inscure code snippet given by the developer:

{original_code}

After multiple improvements with cognicrypt, you generated this final secure full Java class:

{full_java_code}

From the final code, extract only the minimal code lines that directly replace and secure the original snippet.

Return only the fixed code snippet (a few lines). 
Do NOT include import statements, class wrappers, or explanations.
Do not return markdown, explanations, comments, or code fences.
Do NOT wrap your response in triple backticks.
IMPORTANT: Do not change the output format, and ensure the possible solution is always a few lines of code
IMPORTANT: The solution should just be the code snippet fixing the logic, do not add import statements, create
functions or try-catch blocks
Return only the valid Java source snippet.
Also do not add comments inside the possible solution, but integrate the logic behind them in the explanation section
"""
)
FinalExplanation_Prompt = ChatPromptTemplate.from_template(
"""
You were given this insecure Java code snippet:

{original_code}

After applying multiple fixes and verifying it with CogniCrypt, this is the final secure version:

{final_code}

Explain the vulnerability and how the final code fixes it. Use clear, technical language, max 250 words.
Do not include the code in your answer — only return the explanation.
"""
)

InitialFix_Prompt = ChatPromptTemplate.from_template(
"""You are an expert Java security developer specializing in cryptographic vulnerability remediation.

**OBJECTIVE:** Fix a single, specific security vulnerability with surgical precision while preserving all existing functionality.

**PRESERVATION CONSTRAINTS:**
• NEVER modify: variable names, class names, method signatures, or access modifiers
• NEVER alter: program logic, control flow, or functional behavior  
• NEVER add: new methods, fields, imports, or structural changes
• NEVER remove: existing functionality or code sections
• ONLY modify: the specific vulnerable code pattern causing the security issue

**SOURCE CODE:**
The full Java source code is provided below. It contains a chain of related security errors

{full_source_code}


**TARGET VULNERABILITY:**
• **Error Hashcode/ID:** {error_id}
• **Location:** Line {error_line_number}  
• **Security Issue:** {error_message}
• **Violated Rule:** {error_rule}

**ERROR CHAIN CONTEXT:**
• **Upstream Dependency:** {preceding_error_message}
• **Downstream Impact:** {subsequent_error_message}

**SECURITY FIX STRATEGY:**
1. **Locate** the vulnerable pattern at line {error_line_number}
2. **Identify** the specific security weakness (e.g., weak algorithm, insufficient key size, improper initialization)
3. **Apply** minimal security hardening (typically 1-3 lines changed)
4. **Ensure** fix aligns with cryptographic best practices for {error_rule}
5. **Preserve** all existing variable assignments and method calls

**CONSTRAINTS VALIDATION:**
1. Does the fix address the specific vulnerability at line {error_line_number}?
2. Are all variable names and method signatures unchanged?
3. Is the fix minimal?
4. Does the code still compile and maintain original behavior?

**OUTPUT INSTRUCTION:**
Return ONLY the complete, corrected Java source code. Begin immediately with imports or class declaration. No explanations, no markdown, no surrounding text.

---
"""
)

Refinement_Prompt = ChatPromptTemplate.from_template(
    """
    You are an expert Java security developer. Your previous attempt to fix a vulnerability was incorrect and was rejected by CogniCrypt. You must now refine your fix based on the scanner's report.

    **Critical Rules:**
    - **DO NOT** change variable names, class names, or method signatures.
    - **DO NOT** alter the program's logic or functionality.
    - **ONLY** modify the code to satisfy the requirements of the new error report.
    - **ONLY** output the complete, modified Java source code.

    **CONTEXT:**

    Here is the Java code from your previous, incorrect attempt:

    {previous_code_attempt}

    **YOUR TASK:**

    The code above FAILED a security scan. The scanner produced a new report detailing the remaining violations. Here is a summary of the current error graph:

    **Remaining Errors Summary:**
    {error_graph_summary}

    Analyze this new error summary and the code. Apply the minimal changes necessary to fix the violations described.

    **Output the complete, corrected Java code now.**
    """
)

CompilationFix_Prompt = ChatPromptTemplate.from_template(
    """
    You are a Java compiler expert. The Java code you previously generated has a compilation error and could not be compiled.

    **Critical Rules:**
    - Your ONLY goal is to fix the syntax and make the code compile.
    - **DO NOT** attempt to fix any security issues in this step.
    - **DO NOT** change variable names or program logic.
    - **ONLY** output the complete, compilable Java source code.

    ---

    **CONTEXT:**

    The following Java code is syntactically incorrect. The compilation failed with this error:
    {compilation_error_message}
    

    {code_with_compilation_error}

    **YOUR TASK:**

    Fix the compilation error so that the code is valid Java.

    **Output the complete, corrected Java code now.**
    """
)

class BaseLLM:
    def __init__(self, llm):
        self.llm           = llm
        self.output_parser = StrOutputParser()
        self.temperature   = 0.1

    def build_query(self, code: str, context: str) -> str:
        logger.info("Prompting the LLM to create an optimized search query for vector DB search")
        chain = DBSearch_Prompt | self.llm | self.output_parser
        return chain.invoke({"code": code, "context": context}).strip()

    def analyse_vulnerability(self, context: str, question: str) -> VulnerabilityAnalysis:
        logger.info("Prompting the LLM to analyse the code snippet using all the provided context and return a "
                    "solution with explanation")
        chain = CodeAnalysis_Prompt | self.llm.with_structured_output(VulnerabilityAnalysis)
        return chain.invoke({"context": context, "question": question})

    # def analysis_iterations(self, prev_sol: VulnerabilityAnalysis) -> VulnerabilityAnalysis:
    #     logger.info("Performing another analysis round with the LLM")
    #     chain = Iterations_Prompt| self.llm.with_structured_output(VulnerabilityAnalysis)
    #     return chain.invoke(prev_sol.model_dump())
    
    def cogniCrypt_analysis(self, possible_solution: str) -> str:
        logger.info("Prompting the LLM to wrap the solution into full Java code for CogniCrypt testing")
        chain = CogniCrypt_Prompt | self.llm | self.output_parser
        return chain.invoke({"possible_solution": possible_solution}).strip()
    
    def improve_based_on_sarif(self, previous_code: str, sarif_json: str) -> str:
        logger.info("Prompting LLM to fix code using SARIF feedback from CogniCrypt")
        chain = SARIF_Repair_Prompt | self.llm | self.output_parser
        return chain.invoke({
            "previous_code": previous_code,
            "sarif_json": sarif_json
        }).strip()
    
    def extract_fixed_snippet(self, original_code: str, full_java_code: str) -> str:
        logger.info("Prompting LLM to extract final secure code snippet from full class")
        chain = ExtractSnippet_Prompt | self.llm | self.output_parser
        return chain.invoke({
            "original_code": original_code,
            "full_java_code": full_java_code
        }).strip()
    
    def final_explanation(self, original_code: str, final_code: str) -> str:
        logger.info("Prompting LLM for final explanation based on verified secure code")
        chain = FinalExplanation_Prompt | self.llm | self.output_parser
        return chain.invoke({
            "original_code": original_code,
            "final_code": final_code
        }).strip()

    def select_relevant_cwes(self, vulnerable_code: str, context: str, error_message: str, candidate_cwe_ids: list) -> list:
        """
        Use LLM to select the most relevant CWE IDs with simple parsing
        """
        logger.info(f"Prompting LLM to select most relevant CWEs from {len(candidate_cwe_ids)} candidates")
        
        cwe_list_str = ", ".join(sorted(set(candidate_cwe_ids)))
        
        chain = CWE_Selection_Prompt | self.llm | self.output_parser
        
        try:
            response = chain.invoke({
                "vulnerable_code": vulnerable_code,
                "context": context,
                "error_message": error_message,
                "candidate_cwe_ids": cwe_list_str
            }).strip()
            
            logger.info(f"LLM response: {response}")
            
            # Simple regex extraction
            import re
            cwe_matches = re.findall(r'CWE-?\d+', response, re.IGNORECASE)
            
            # Format and filter
            candidate_set = set(candidate_cwe_ids)
            selected = []
            
            for match in cwe_matches:
                number_search = re.search(f'\d+',match)
                if number_search:
                    cwe_id = f"CWE-{number_search.group(0)}"
                if cwe_id in candidate_set and cwe_id not in selected:
                    selected.append(cwe_id)
                    if len(selected) >= 3:
                        break
            
            if selected:
                logger.info(f"LLM selected CWEs: {selected}")
                return selected
            else:
                # Fallback
                fallback = list(sorted(candidate_set))[:3]
                logger.info(f"No valid selection, using fallback: {fallback}")
                return fallback
            
        except Exception as e:
         logger.error(f"CWE selection failed: {e}")
        return list(sorted(set(candidate_cwe_ids)))[:3]

    def new_fix_targeted_error(self,full_code: str,error_details: dict,sarif_report: str = None,compilation_error: str = None):
        logger.info(f"Initiating new targeted fix for error ID: {error_details.get('hashcode')}")

        if compilation_error:
            logger.info("Using CompilationFix_Prompt to fix syntax error.")
            prompt = CompilationFix_Prompt
            payload = {
                "code_with_compilation_error": full_code,
                "compilation_error_message": compilation_error
            }
        elif sarif_report:
            logger.info("Using Refinement_Prompt with new SARIF report.")
            prompt = Refinement_Prompt
            payload = {
                "previous_code_attempt": full_code,
                "sarif_report": sarif_report
            }
        else:
            logger.info("Using InitialFix_Prompt for the first attempt.")
            prompt = InitialFix_Prompt
            payload = {
                "full_source_code": full_code,
                "error_id": error_details.get('hashcode'), # Using hashcode as the unique ID from the initial payload
                "error_line_number": error_details.get('line'),
                "error_message": error_details.get('message'),
                "error_rule": error_details.get('rule'),
                "preceding_error_message": error_details.get('preceding_error_message', 'None'),
                "subsequent_error_message": error_details.get('subsequent_error_message', 'None')
            }

        chain = prompt | self.llm | self.output_parser

        # Invoke the LLM and return the resulting code
        proposed_new_code = chain.invoke(payload).strip()
        return proposed_new_code