# Enhanced app_db.py script
import sqlite3
import json
import time
import hashlib
from datetime import datetime

DB_FILE = "analysis_results.db"

def init_db():
    """Initialize database with both original and new tables for caching."""
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()
        
        # Original table for /aifix endpoint
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS analysis_records (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            code TEXT,
            rule TEXT,
            msg TEXT,
            llm_model TEXT,
            iterations INTEGER,
            output_json TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(code, rule, msg, llm_model, iterations)
        );
        """)
        
        # New table for /newfix endpoint
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS newfix_analysis_records (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            payload_hash TEXT UNIQUE,
            payload_json TEXT,
            llm_model TEXT,
            iterations INTEGER,
            output_json TEXT,
            cognicrypt_verified BOOLEAN,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        """)
        
        conn.commit()


def _generate_payload_hash(extracted_data):
    """
    Generate a deterministic hash for the extracted payload data.
    This ensures consistent cache lookups for identical inputs.
    """
    # Create a normalized string representation of the key data
    cache_key_data = {
        'all_node_details': extracted_data.get('all_node_details'),
        'simplified_trace': extracted_data.get('simplified_trace'),
        'source_code': extracted_data.get('source_code'),
        'package_info': extracted_data.get('package_info'),
        'class_name': extracted_data.get('class_name'),
        'llm_model': extracted_data.get('llm_model', 'openai'),
        'iterations': extracted_data.get('iterations', 3)
    }
    
    # Convert to JSON string with sorted keys for consistency
    cache_string = json.dumps(cache_key_data, sort_keys=True, separators=(',', ':'))
    
    # Generate SHA-256 hash
    return hashlib.sha256(cache_string.encode('utf-8')).hexdigest()


def save_newfix_analysis_record(extracted_data, output_data):
    """
    Save newfix analysis record only if CogniCrypt verified and no errors.
    
    Args:
        extracted_data: The input payload data
        output_data: The analysis result
    """
    # Check if we should save this record
    if not _should_save_newfix_record(output_data):
        return False
        
    payload_hash = _generate_payload_hash(extracted_data)
    cognicrypt_verified = output_data.get('CogniCrypt_Verified', False)
    
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()
        try:
            cursor.execute("""
                INSERT OR REPLACE INTO newfix_analysis_records
                (payload_hash, payload_json, llm_model, iterations, output_json, cognicrypt_verified, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """, (
                payload_hash,
                json.dumps(extracted_data),
                extracted_data.get("llm_model", "openai"),
                extracted_data.get("iterations", 3),
                json.dumps(output_data),
                cognicrypt_verified,
                datetime.now()
            ))
            conn.commit()
            return True
        except Exception as e:
            print(f"Error saving newfix record: {e}")
            return False


def get_newfix_record_by_input(extracted_data, delay_seconds=5):
    """
    Get cached newfix analysis record if it exists.
    
    Args:
        extracted_data: The input payload data
        delay_seconds: Artificial delay for cache hits
        
    Returns:
        Cached record or None if not found
    """
    payload_hash = _generate_payload_hash(extracted_data)
    
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, payload_json, llm_model, iterations, output_json, cognicrypt_verified, created_at
            FROM newfix_analysis_records
            WHERE payload_hash = ?
            ORDER BY created_at DESC LIMIT 1
        """, (payload_hash,))
        
        row = cursor.fetchone()
        if not row:
            return None  # No cache entry
            
        # Cache hit: apply artificial delay
        time.sleep(delay_seconds)
        
        try:
            output_parsed = json.loads(row[4])
            payload_parsed = json.loads(row[1])
        except Exception as e:
            output_parsed = {"error": str(e), "raw": row[4]}
            payload_parsed = {"error": str(e), "raw": row[1]}
            
        return {
            "id": row[0],
            "payload": payload_parsed,
            "llm_model": row[2],
            "iterations": row[3],
            "output": output_parsed,
            "cognicrypt_verified": bool(row[5]),
            "created_at": row[6]
        }


def _should_save_newfix_record(output_data):
    """
    Determine if a newfix record should be saved based:
    1. Only save if CogniCrypt_Verified is True
    2. Don't save if there are errors
    
    Args:
        output_data: The analysis result dictionary
        
    Returns:
        bool: True if record should be saved, False otherwise
    """
    # Check for errors in the response
    if "error" in output_data:
        return False
        
    # Check if CogniCrypt verified
    cognicrypt_verified = output_data.get('CogniCrypt_Verified', False)
    if not cognicrypt_verified:
        return False
        
    # Additional check: ensure we have essential fields
    required_fields = ['Vulnerability_name', 'Final_Secure_Code_Snippet']
    if not all(field in output_data for field in required_fields):
        return False
        
    return True


def get_all_newfix_records():
    """Return all saved newfix analysis records."""
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, payload_hash, llm_model, iterations, output_json, cognicrypt_verified, created_at 
            FROM newfix_analysis_records 
            ORDER BY created_at DESC
        """)
        rows = cursor.fetchall()
        
        records = []
        for row in rows:
            try:
                output_parsed = json.loads(row[4])
            except:
                output_parsed = {"error": "Failed to parse output", "raw": row[4]}
                
            record = {
                "id": row[0],
                "payload_hash": row[1],
                "llm_model": row[2],
                "iterations": row[3],
                "output": output_parsed,
                "cognicrypt_verified": bool(row[5]),
                "created_at": row[6]
            }
            records.append(record)
        return records


# Keep existing functions for backward compatibility
def save_analysis_record(input_data, output_data):
    """Original function for /aifix endpoint caching."""
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()
        cursor.execute("""
            INSERT OR IGNORE INTO analysis_records
            (code, rule, msg, llm_model, iterations, output_json, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (
            input_data.get("code"),
            input_data.get("rule"),
            input_data.get("msg"),
            input_data.get("llm_model"),
            input_data.get("iterations"),
            json.dumps(output_data),
            datetime.now()
        ))
        conn.commit()


def get_record_by_input(input_data, delay_seconds=5):
    """Original function for /aifix endpoint cache lookup."""
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, code, rule, msg, llm_model, iterations, output_json, created_at
            FROM analysis_records
            WHERE code=? AND rule=? AND msg=? AND llm_model=? AND iterations=?
            ORDER BY created_at DESC LIMIT 1
        """, (
            input_data.get("code"),
            input_data.get("rule"),
            input_data.get("msg"),
            input_data.get("llm_model"),
            input_data.get("iterations")
        ))
        row = cursor.fetchone()
        if not row:
            return None
            
        time.sleep(delay_seconds)
        
        try:
            output_parsed = json.loads(row[6])
        except Exception as e:
            output_parsed = {"error": str(e), "raw": row[6]}
            
        return {
            "id": row[0],
            "code": row[1],
            "rule": row[2],
            "msg": row[3],
            "llm_model": row[4],
            "iterations": row[5],
            "output": output_parsed,
            "created_at": row[7]
        }


def get_all_records():
    """Original function to get all /aifix records."""
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT id, code, rule, msg, llm_model, iterations, output_json, created_at FROM analysis_records ORDER BY id DESC")
        rows = cursor.fetchall()
        records = []
        for row in rows:
            record = {
                "id": row[0],
                "code": row[1],
                "rule": row[2],
                "msg": row[3],
                "llm_model": row[4],
                "iterations": row[5],
                "output": json.loads(row[6]),
                "created_at": row[7]
            }
            records.append(record)
        return records
