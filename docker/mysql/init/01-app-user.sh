#!/bin/bash
# Runs once when the MySQL data volume is first created. Creates the least-privilege app user.
# The app user gets rights on the fruitmachine schema only (Flyway needs DDL there).
set -euo pipefail
# Escape backslash and single quote so any password is a valid SQL string literal.
pw=${APP_DB_PASSWORD//\\/\\\\}
pw=${pw//\'/\'\'}
mysql --user=root --password="$MYSQL_ROOT_PASSWORD" <<SQL
CREATE USER IF NOT EXISTS 'fruitmachine_app'@'%' IDENTIFIED BY '$pw';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON \`$MYSQL_DATABASE\`.* TO 'fruitmachine_app'@'%';
FLUSH PRIVILEGES;
SQL
