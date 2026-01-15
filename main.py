import mysql.connector
import re

# MySQL connection details
config = {
    'user': 'root',
    'password': '',
    'host': 'mysql',  # Use MySQL container name
    'port': 3306,
    'autocommit': True
}

# Path to your SQL file
sql_file_path = 'ewaterdb.sql'

def split_sql_statements(sql_content):
    """Split SQL content into individual statements, handling quoted strings properly."""
    statements = []
    current_statement = ""
    in_string = False
    string_char = None
    i = 0
    
    while i < len(sql_content):
        char = sql_content[i]
        
        # Handle string literals
        if char in ("'", '"') and not in_string:
            in_string = True
            string_char = char
            current_statement += char
        elif char == string_char and in_string:
            # Check if it's escaped
            if i > 0 and sql_content[i-1] == '\\':
                current_statement += char
            else:
                in_string = False
                string_char = None
                current_statement += char
        elif char == ';' and not in_string:
            # End of statement
            if current_statement.strip():
                statements.append(current_statement.strip())
            current_statement = ""
        else:
            current_statement += char
        
        i += 1
    
    # Add the last statement if it exists
    if current_statement.strip():
        statements.append(current_statement.strip())
    
    return statements

try:
    # Read the SQL file
    with open(sql_file_path, 'r', encoding='utf-8') as f:
        sql_dump = f.read()

    # Split by semicolon into individual statements, handling quoted strings
    statements = split_sql_statements(sql_dump)

    # Connect to MySQL
    conn = mysql.connector.connect(**config)
    cursor = conn.cursor()
    
    # Increase max_allowed_packet size (requires SUPER privilege)
    try:
        cursor.execute("SET GLOBAL max_allowed_packet=1073741824")  # 1GB
        print("✅ Increased max_allowed_packet to 1GB")
    except mysql.connector.Error as packet_err:
        print(f"⚠️  Could not increase max_allowed_packet: {packet_err}")
        print("   Large INSERT statements might fail. Consider increasing max_allowed_packet in MySQL config.")

    for stmt in statements:
        # Skip comments and empty lines
        if stmt.startswith('--') or stmt.startswith('/*') or stmt == '':
            continue
        print(f"Executing: {stmt[:80]}...")  # show first 80 chars for context
        cursor.execute(stmt)

    print("✅ All SQL statements executed successfully.")

except mysql.connector.Error as err:
    print(f"❌ MySQL Error: {err}")

except FileNotFoundError:
    print(f"❌ SQL file not found: {sql_file_path}")

finally:
    if 'cursor' in locals():
        cursor.close()
    if 'conn' in locals():
        conn.close()
