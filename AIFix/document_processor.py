import json
import os
import re
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.docstore.document import Document
from logger_config import get_logger

logger = get_logger(__name__)
# Document process class is for all the functions which are related to preprocessing documents and creating vector DB
class DocumentProcessor:
    def __init__(self, chunk_size=1000, chunk_overlap=200):
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap
        )

    # Load and split function is used to create the vector database
    # It creates the metadata for the CWE files which is used to create the embeddings
    def load_and_split(self, doc_dir: str):
        logger.info("Splitting up the CWE files and creating metadata for using in vector DB")
        documents = []
        for filename in os.listdir(doc_dir):
            if filename.endswith('.txt'):
                with open(os.path.join(doc_dir, filename), 'r', encoding='utf-8') as f:
                    content = f.read()
                    documents.append(Document(
                        page_content=content,
                        metadata={"source": filename, "doc_id": filename[:-4]}
                    ))
        return self.text_splitter.split_documents(documents)


    # Error Description Processing function is used to search through the static error descriptions
    # and find the relevant one according to the crysl rule violated and the error type
    def error_description_processing(self, file_path: str, crysl_rule: str):

        error_type = file_path.split('/')[-1].split('.')[0]

        with open(file_path, 'r', encoding='utf-8') as file:
            data = json.load(file)

        error_data = data.get(error_type, {})
        description = error_data.get("description", "")
        examples = error_data.get("examples", [])

        rule_example = next(
            (ex for ex in examples if ex["rule"].lower() == crysl_rule.lower()),
            None
        )
        misuse = rule_example.get("misuse", "") if rule_example else "No example found for this rule."
        solution = rule_example.get("solution", "") if rule_example else ""

        # Remove all HTML tags
        clean_html = lambda text: re.sub(r'<[^>]+>', '', text)
        description = clean_html(description)
        misuse = clean_html(misuse)
        solution = clean_html(solution)

        result = f"Error Type: {error_type}\n\nDescription:\n{description}\n\nExample Misuse for Rule '{crysl_rule}':\n{misuse}"
        if solution:
            result += f"\n\nSuggested Solution:\n{solution}"

        return result