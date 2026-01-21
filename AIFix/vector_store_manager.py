from langchain_community.vectorstores import FAISS
from langchain_huggingface import HuggingFaceEmbeddings
from logger_config import get_logger

logger = get_logger(__name__)
# Vector Store Manager class is used to create or load the vector store
class VectorStoreManager:
    def __init__(self, embedding_model="sentence-transformers/all-MiniLM-L6-v2"):
        self.embeddings = HuggingFaceEmbeddings(model_name=embedding_model)
        self.vector_store = None

    def create_store(self, documents):
        logger.info("Creating the vector DB")
        self.vector_store = FAISS.from_documents(
            documents=documents,
            embedding=self.embeddings
        )
        return self.vector_store

    def save_store(self, path="faiss_index"):
        logger.info("Storing the vector DB in local storage")
        self.vector_store.save_local(path)

    def load_store(self, path="faiss_index"):
        logger.info("Loading the vector DB from local storage")
        self.vector_store = FAISS.load_local(
            folder_path=path,
            embeddings=self.embeddings,
            allow_dangerous_deserialization=True
        )
        return self.vector_store