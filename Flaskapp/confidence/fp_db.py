from datetime import datetime
import sqlite3

DB_FILE = "fp.db"

def init_db():
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()

        cursor.execute("""
        CREATE TABLE IF NOT EXISTS fp_db (
        hashcode TEXT NOT NULL PRIMARY KEY,
        prediction BOOLEAN NOT NULL,
        probability FLOAT NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)
        """)

        conn.commit()


def save_fp_score(hashcode, prediction, probability):
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()

        cursor.execute("""
        INSERT OR REPLACE INTO fp_db (hashcode, prediction, probability, created_at)
        VALUES (?, ?, ?, ?)   
        """, (hashcode, prediction, probability, datetime.now()))

        conn.commit()


def get_fp_score(hashcode, last_analysis):
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()

        if not last_analysis is datetime:
            try:
                last_analysis = datetime.fromtimestamp(last_analysis)
            except OSError:
                last_analysis = datetime.fromtimestamp(last_analysis / 1000)

        # If the last analysis happened before the entry was created, then it's still up-to-date
        cursor.execute("""
        SELECT prediction, probability 
        FROM fp_db 
        WHERE hashcode = ?
            AND created_at > ?
        """, (hashcode, last_analysis))

        return cursor.fetchone()


def is_outdated(last_analysis) -> bool:
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()

        if not last_analysis is datetime:
            try:
                last_analysis = datetime.fromtimestamp(last_analysis)
            except OSError:
                last_analysis = datetime.fromtimestamp(last_analysis / 1000)

        cursor.execute("""
        SELECT * FROM fp_db WHERE created_at > ?
        """, (last_analysis,))

        if cursor.fetchone() is None:
            # Delete all rows as there are no up-to-date entries
            # If we at some point have different branches -> filter for branch
            cursor.execute("""
            DELETE FROM fp_db
            """)
            return True
        else:
            return False