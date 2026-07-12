import argparse
import sys
import os

from db_migrate.migrator import Migrator

def main():
    parser = argparse.ArgumentParser(description="Database Migration Runner")
    subparsers = parser.add_subparsers(dest="command", help="Command to run")
    
    # Needs to be required in newer Python versions
    subparsers.required = True

    # migrate command
    parser_migrate = subparsers.add_parser("migrate", help="Apply pending migrations")
    parser_migrate.add_argument("--dry-run", action="store_true", help="Print SQL without executing")
    
    # reset command
    subparsers.add_parser("reset", help="Drop target db, recreate, and apply all migrations")
    
    # status command
    subparsers.add_parser("status", help="Show applied vs pending migrations")
    
    # create command
    parser_create = subparsers.add_parser("create", help="Create a new numbered migration file")
    parser_create.add_argument("name", help="Name for the new migration (e.g. 'add_users_table')")

    args = parser.parse_args()

    # Determine migrations directory. 
    # Assume we run from project root, so 'database/migrations/'
    migrations_dir = os.environ.get("MIGRATIONS_DIR", "database/migrations")
    
    migrator = Migrator(migrations_dir=migrations_dir)

    if args.command == "migrate":
        # Hack to get subparser args
        dry_run = "--dry-run" in sys.argv
        migrator.migrate(dry_run=dry_run)
    elif args.command == "reset":
        migrator.reset()
    elif args.command == "status":
        migrator.status()
    elif args.command == "create":
        migrator.create(args.name)

if __name__ == "__main__":
    main()
