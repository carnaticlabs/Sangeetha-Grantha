import os
import psycopg
from contextlib import contextmanager

def get_connection_string() -> str:
    host = os.environ.get("DB_HOST", "localhost")
    port = os.environ.get("DB_PORT", "5432")
    dbname = os.environ.get("DB_NAME", "sangita_grantha")
    user = os.environ.get("DB_USER", "postgres")
    password = os.environ.get("DB_PASSWORD", "postgres")
    
    return f"host={host} port={port} dbname={dbname} user={user} password={password}"

def get_default_postgres_connection_string() -> str:
    """Connection string for the default 'postgres' database (used for drop/create)."""
    host = os.environ.get("DB_HOST", "localhost")
    port = os.environ.get("DB_PORT", "5432")
    dbname = "postgres"  # connect to default db
    user = os.environ.get("DB_USER", "postgres")
    password = os.environ.get("DB_PASSWORD", "postgres")
    
    return f"host={host} port={port} dbname={dbname} user={user} password={password}"

@contextmanager
def get_connection(autocommit: bool = False):
    conn_str = get_connection_string()
    conn = psycopg.connect(conn_str, autocommit=autocommit)
    try:
        yield conn
    finally:
        conn.close()

@contextmanager
def get_postgres_db_connection(autocommit: bool = True):
    """Returns a connection to the 'postgres' default database. Used for dropping/creating."""
    conn_str = get_default_postgres_connection_string()
    conn = psycopg.connect(conn_str, autocommit=autocommit)
    try:
        yield conn
    finally:
        conn.close()
