import os
import hashlib
import re
from typing import List, Tuple
from db_migrate.connection import get_connection, get_postgres_db_connection

def get_file_checksum(filepath: str) -> str:
    hash_sha256 = hashlib.sha256()
    with open(filepath, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_sha256.update(chunk)
    return hash_sha256.hexdigest()

class Migrator:
    def __init__(self, migrations_dir: str):
        self.migrations_dir = migrations_dir
        self.migrations_table = "schema_migrations"

    def _ensure_migrations_table(self, conn):
        with conn.cursor() as cur:
            cur.execute(f"""
                CREATE TABLE IF NOT EXISTS {self.migrations_table} (
                    id SERIAL PRIMARY KEY,
                    filename TEXT NOT NULL UNIQUE,
                    checksum TEXT NOT NULL,
                    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                );
            """)
        conn.commit()

    def _get_migration_files(self) -> List[str]:
        if not os.path.exists(self.migrations_dir):
            return []
            
        files = [f for f in os.listdir(self.migrations_dir) if f.endswith('.sql')]
        
        # Sort numerically based on prefix (e.g. 01, 02)
        def get_number(filename: str) -> int:
            match = re.match(r'^(\d+)__', filename)
            if match:
                return int(match.group(1))
            return 99999 # Push invalid formats to end

        return sorted(files, key=get_number)

    def _get_applied_migrations(self, conn) -> dict:
        self._ensure_migrations_table(conn)
        with conn.cursor() as cur:
            cur.execute(f"SELECT filename, checksum FROM {self.migrations_table}")
            return {row[0]: row[1] for row in cur.fetchall()}

    def status(self):
        try:
            with get_connection() as conn:
                applied = self._get_applied_migrations(conn)
        except Exception as e:
            print(f"Error connecting to database: {e}")
            return

        files = self._get_migration_files()
        
        print(f"{'Migration':<40} | {'Status':<10} | {'Checksum'}")
        print("-" * 80)
        
        for f in files:
            is_applied = f in applied
            status = "Applied" if is_applied else "Pending"
            
            if is_applied:
                expected_sum = applied[f]
                actual_sum = get_file_checksum(os.path.join(self.migrations_dir, f))
                if expected_sum != actual_sum:
                    status = "MODIFIED"
                    
            print(f"{f:<40} | {status:<10} | {'OK' if status == 'Applied' else 'N/A'}")

    def migrate(self, dry_run: bool = False):
        files = self._get_migration_files()
        
        with get_connection() as conn:
            applied = self._get_applied_migrations(conn)
            
            pending = [f for f in files if f not in applied]
            
            if not pending:
                print("No pending migrations found.")
                return

            print(f"Found {len(pending)} pending migration(s).")
            
            for index, f in enumerate(pending, 1):
                filepath = os.path.join(self.migrations_dir, f)
                checksum = get_file_checksum(filepath)
                
                with open(filepath, 'r') as fp:
                    sql = fp.read()
                
                print(f"[{index}/{len(pending)}] Applying {f}...")
                
                if dry_run:
                    print(f"--- DRY RUN: {f} ---")
                    print(sql)
                    print("--- END DRY RUN ---\n")
                    continue
                    
                try:
                    with conn.cursor() as cur:
                        cur.execute(sql)
                        cur.execute(f"""
                            INSERT INTO {self.migrations_table} (filename, checksum)
                            VALUES (%s, %s)
                        """, (f, checksum))
                    conn.commit()
                    print(f"✓ Successfully applied {f}")
                except Exception as e:
                    conn.rollback()
                    print(f"❌ Failed to apply {f}: {e}")
                    raise SystemExit(1)
            
            if not dry_run:
                print("All pending migrations applied successfully.")

    def reset(self):
        db_name = os.environ.get("DB_NAME", "sangita_grantha")
        
        print(f"Resetting database '{db_name}'...")
        
        # Connect to 'postgres' db with autocommit to drop/create
        try:
            with get_postgres_db_connection(autocommit=True) as conn:
                with conn.cursor() as cur:
                    # Terminate other connections before dropping
                    cur.execute(f"""
                        SELECT pg_terminate_backend(pg_stat_activity.pid)
                        FROM pg_stat_activity
                        WHERE pg_stat_activity.datname = '{db_name}'
                        AND pid <> pg_backend_pid();
                    """)
                    print(f"Dropped existing connections.")
                    
                    cur.execute(f"DROP DATABASE IF EXISTS {db_name};")
                    print(f"Dropped database '{db_name}'.")
                    
                    cur.execute(f"CREATE DATABASE {db_name};")
                    print(f"Created database '{db_name}'.")
        except Exception as e:
            print(f"Failed to reset database: {e}")
            raise SystemExit(1)
            
        print("Running migrations on fresh database...")
        self.migrate()

    def create(self, name: str):
        files = self._get_migration_files()
        
        # Find next number
        next_num = 1
        if files:
            match = re.match(r'^(\d+)__', files[-1])
            if match:
                next_num = int(match.group(1)) + 1
                
        num_str = f"{next_num:02d}"
        
        # Sanitize name
        clean_name = re.sub(r'[^a-zA-Z0-9]', '_', name).lower()
        clean_name = re.sub(r'_+', '_', clean_name).strip('_')
        
        filename = f"{num_str}__{clean_name}.sql"
        filepath = os.path.join(self.migrations_dir, filename)
        
        os.makedirs(self.migrations_dir, exist_ok=True)
        
        with open(filepath, 'w') as f:
            f.write(f"-- Migration: {name}\n-- Created automatically\n\n")
            
        print(f"Created new migration file: {filepath}")
