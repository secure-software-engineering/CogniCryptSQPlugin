from flask import Flask, request, jsonify
from flask_cors import CORS
from aifix import ai_fix, new_ai_fix
from logger_config import get_logger
import payload_extraction
import app_db

app_db.init_db()
logger = get_logger(__name__)

app = Flask(__name__)
CORS(app)

@app.route('/aifix', methods=['POST'])
def aifix():
    logger.info("Post API function to start the AI Fix analysis")
    try:
        request_data = request.get_json()
        code = request_data.get("code")
        rule = request_data.get("rule")
        message = request_data.get("msg")
        llm_model = request_data.get("llm_model", "openai")
        iterations_cc = request_data.get("iterations", 3)

        logger.info("Fetched the vulnerable code snippet, CrySL rule violated, error type, selected LLM model and number of iterations")

        if not code:
            logger.error("Error: Missing code snippet")
            return jsonify({"error": "Missing code snippet"}), 400
        
        input_data = {
            "code": code,
            "rule": rule,
            "msg": message,
            "llm_model": llm_model,
            "iterations": iterations_cc
        }

        # DB Cache Lookup
        cached = app_db.get_record_by_input(input_data)
        if cached is not None:
            logger.info("Returning cached LLM result from DB.")
            return jsonify(cached["output"])

        logger.info("Data not found in cache, starting the analysis")
        result = ai_fix(code, rule, message, llm_model.lower(), iterations_cc)

         # Normalize error dicts returned by ai_fix (non-exception path)
        if isinstance(result, dict) and "error" in result:
            err = str(result["error"])
            logger.error(f"ai_fix returned error: {err}")
            if "COMPILATION_ERROR" in err or "Compilation failed" in err:
                return jsonify({"error": "Error compiling code. Please select a different model."}), 400
            return jsonify({"error": "An error occurred during analysis. Please try again."}), 500

        # Only save to DB if result is not an error
        app_db.save_analysis_record(input_data, result)
        return jsonify(result)

    except Exception as e:
        msg = str(e)
        logger.error(f"Error: {msg}")
        if "COMPILATION_ERROR" in msg or "Compilation failed" in msg:
            return jsonify({"error": "Error compiling code. Please select a different model."}), 400
        return jsonify({"error": "An error occurred during analysis. Please try again."}), 500

@app.route('/newfix', methods=['POST'])
def new_aifix():
    """
    Handles the new payload and passes the extracted data to the sequential fixer.
    Now includes caching functionality with conditional saving.
    """
    logger.info("Received request on the new /newfix endpoint.")
    try:
        # 1. Get the raw payload
        payload = request.get_json()
        if not payload:
            logger.error("Error: Missing JSON payload for /newfix")
            return jsonify({"error": "Missing JSON payload"}), 400

        # 2. Call the payload extraction module to process the data
        extracted_data = payload_extraction.process_payload(payload)
        logger.info("Payload processed successfully by payload_extraction module.")

        # 3. NEW: Check cache before processing
        cached_result = app_db.get_newfix_record_by_input(extracted_data)
        if cached_result is not None:
            logger.info("Returning cached newfix result from DB.")
            return jsonify(cached_result["output"])

        logger.info("Data not found in newfix cache, starting the analysis")

        # 4. Call the new sequential fixing function in aifix.py
        final_result = new_ai_fix(extracted_data)

        # 5. NEW: Conditional caching logic
        # Only save if CogniCrypt verified and no errors
        if app_db._should_save_newfix_record(final_result):
            logger.info("Saving newfix result to cache (CogniCrypt verified, no errors)")
            saved = app_db.save_newfix_analysis_record(extracted_data, final_result)
            if saved:
                logger.info("Newfix result successfully cached")
            else:
                logger.warning("Failed to save newfix result to cache")
        else:
            logger.info("Skipping cache save: CogniCrypt not verified or contains errors")

        # 6. Return the final result
        return jsonify(final_result)

    except Exception as e:
        logger.error(f"An unexpected error occurred in /newfix: {str(e)}", exc_info=True)
        return jsonify({"error": "An internal server error occurred."}), 500

# # Optional: Add endpoint to view cache statistics
# @app.route('/cache-stats', methods=['GET'])
# def cache_stats():
#     """
#     Endpoint to view caching statistics for both endpoints.
#     """
#     try:
#         # Get counts for both cache types
#         aifix_records = app_db.get_all_records()
#         newfix_records = app_db.get_all_newfix_records()
        
#         # Count verified vs unverified for newfix
#         newfix_verified = sum(1 for record in newfix_records if record.get('cognicrypt_verified', False))
        
#         stats = {
#             "aifix_cache": {
#                 "total_records": len(aifix_records),
#                 "latest_record": aifix_records[0]["created_at"] if aifix_records else None
#             },
#             "newfix_cache": {
#                 "total_records": len(newfix_records),
#                 "verified_records": newfix_verified,
#                 "unverified_records": len(newfix_records) - newfix_verified,
#                 "latest_record": newfix_records[0]["created_at"] if newfix_records else None
#             }
#         }
        
#         return jsonify(stats)
        
#     except Exception as e:
#         logger.error(f"Error retrieving cache stats: {str(e)}")
#         return jsonify({"error": "Failed to retrieve cache statistics"}), 500

if __name__ == '__main__':
    logger.info("Starting the API")
    app.run(host='0.0.0.0', port=8000)
