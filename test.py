import psycopg2
import bcrypt
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

conn = psycopg2.connect(
    dbname=os.getenv("POSTGRES_DB"),
    user=os.getenv("POSTGRES_USER"),
    password=os.getenv("POSTGRES_PASSWORD"),
    host=os.getenv("POSTGRES_HOST"),
    port=os.getenv("POSTGRES_PORT")
)

cur = conn.cursor()

# Fetch user from DB
cur.execute("SELECT id, hashed_password FROM users WHERE email = %s;", ("immimatecanada@gmail.com",))
user = cur.fetchone()

if user:
    stored_password = user[1]  # Hashed password from DB
    entered_password = "Godiguis12"  # Change if testing different passwords

    if bcrypt.checkpw(entered_password.encode("utf-8"), stored_password.encode("utf-8")):
        print("✅ Password is correct! Authentication should work.")
    else:
        print("❌ Incorrect password! The stored hash doesn't match.")
else:
    print("❌ No user found with this email.")

cur.close()
conn.close()