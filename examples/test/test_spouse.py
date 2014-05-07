import os
import getpass
import psycopg2
from sets import Set

# Error threshold
eps = 0.2

# Get the environment variables
DBNAME = os.environ['DBNAME']
PGUSER = os.environ['PGUSER']
PGPASSWORD = os.environ['PGPASSWORD']
PGHOST = os.environ['PGHOST']
PGPORT = os.environ['PGPORT']

# Stanfard status
std = dict([])


std_res = open('spouse.dat', 'r')

for row in std_res:
    if (len(row) < 2): continue
    dat = row.strip().split(' ')
    std[str(dat[0])] = dat[1]
std_res.close()

# Connect database
conn = psycopg2.connect(database = DBNAME, user = PGUSER, password = PGPASSWORD, host = PGHOST, port = PGPORT)
print DBNAME
cur = conn.cursor()

# Check table status
cur.execute("SELECT COUNT(*) FROM articles")
for row in cur.fetchall(): num = row[0]
if (std["articles"] != str(num)):
    print "Error in Table articles"
    exit(0)

cur.execute("SELECT COUNT(*) FROM sentences")
for row in cur.fetchall(): num = row[0]
if (std["sentences"] != str(num)):
    print "Error in Table sentences"
    exit(0)

cur.execute("SELECT COUNT(*) FROM people_mentions")
for row in cur.fetchall(): num = row[0]
if (std["people_mentions"] != str(num)):
    print "Error in Table people_mentions"
    exit(0)

cur.execute("SELECT COUNT(*) FROM has_spouse")
for row in cur.fetchall(): num = row[0]
if (std["has_spouse"] != str(num)):
    print "Error in Table has_spouse"
    exit(0)

cur.execute("SELECT COUNT(*) FROM has_spouse_features")
for row in cur.fetchall(): num = row[0]
if (std["has_spouse_features"] != str(num)):
    print "Error in Table has_spouse_features"
    exit(0)


print "Test passed!"
