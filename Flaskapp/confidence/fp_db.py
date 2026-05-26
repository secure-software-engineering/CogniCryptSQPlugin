from datetime import datetime
import sqlite3

DB_FILE = "fp.db"

def init_db():
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()

        cursor.execute("""
        CREATE TABLE IF NOT EXISTS fp_db (
        hashcode TEXT NOT NULL,
        project TEXT NOT NULL,
        branch TEXT NOT NULL,
        prediction BOOLEAN NOT NULL,
        probability FLOAT NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (hashcode, project, branch))
        """)

        conn.commit()


def save_fp_score(hashcode, project, branch, prediction, probability):
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()

        cursor.execute("""
        INSERT OR REPLACE INTO fp_db (hashcode, project, branch, prediction, probability, created_at)
        VALUES (?, ?, ?, ?, ?, ?)   
        """, (hashcode, project, branch, prediction, probability, datetime.now()))

        conn.commit()


def get_fp_score(hashcode, project, branch, last_analysis):
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()

        if not last_analysis is datetime:
            try:
                last_analysis = datetime.fromtimestamp(last_analysis)
            except (OSError, ValueError) as e:
                last_analysis = datetime.fromtimestamp(last_analysis / 1000)

        # If the last analysis happened before the entry was created, then it's still up-to-date
        cursor.execute("""
        SELECT prediction, probability 
        FROM fp_db 
        WHERE hashcode = ?
            AND created_at > ?
            AND project = ?
            AND branch = ?
        """, (hashcode, last_analysis, project, branch))

        return cursor.fetchone()


def is_outdated(last_analysis, project, branch) -> bool:
    with sqlite3.connect(DB_FILE) as conn:
        cursor = conn.cursor()

        if not last_analysis is datetime:
            try:
                last_analysis = datetime.fromtimestamp(last_analysis)
            except (OSError, ValueError) as e:
                last_analysis = datetime.fromtimestamp(last_analysis / 1000)

        cursor.execute("""
        SELECT * FROM fp_db 
        WHERE created_at > ?
            AND project = ?
            AND branch = ?
        """, (last_analysis, project, branch))

        if cursor.fetchone() is None:
            # Delete all rows for the given project and branch as there are no up-to-date entries
            cursor.execute("""
            DELETE FROM fp_db
            WHERE project = ?
            AND branch = ?
            """, (project, branch))
            return True
        else:
            return False