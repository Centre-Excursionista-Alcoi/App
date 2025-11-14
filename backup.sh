#!/bin/bash

# === Configuration ===

# The name of your running Docker container
export CONTAINER_NAME="cea-db"

# The PostgreSQL user for the dump
export PG_USER="postgres"

# The directory on the HOST machine where backups will be stored
export BACKUP_DIR="./backups"

# === Script Logic ===

# Exit immediately if a command exits with a non-zero status.
set -e

# Exit if any command in a pipeline fails (e.g., if pg_dumpall fails)
set -o pipefail

# Treat unset variables as an error
set -u

# 1. Create the backup directory if it doesn't already exist
# The '-p' flag creates parent directories as needed and
# doesn't error if the directory already exists.
echo "Ensuring backup directory exists: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"

# 2. Generate the filename with the current date (YYYY-MM-DD)
CURRENT_DATE=$(date +"%Y-%m-%d")
FILE_NAME="db_dump-$CURRENT_DATE.sql.gz"
DEST_FILE="$BACKUP_DIR/$FILE_NAME"

echo "Starting backup of '$CONTAINER_NAME' to '$DEST_FILE'..."

# 3. Execute the dump, compress, and save
#
# - `docker exec -t "$CONTAINER_NAME"`: Runs the command inside the specified container.
# - `pg_dumpall ...`: Dumps all databases using the specified user.
# - `| gzip`: Pipes the SQL output directly to gzip for compression.
# - `> "$DEST_FILE"`: Redirects the compressed output to the destination file on the host.

docker exec -t "$CONTAINER_NAME" \
    pg_dumpall --clean --if-exists --username="$PG_USER" \
    | gzip > "$DEST_FILE"

# 4. Confirmation message
FILE_SIZE=$(du -h "$DEST_FILE" | cut -f1)

echo ""
echo "@  Backup complete!"
echo "   File: $DEST_FILE"
echo "   Size: $FILE_SIZE"
