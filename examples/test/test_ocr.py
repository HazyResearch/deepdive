import os
import getpass
import psycopg2
from sets import Set

# Error threshold
eps = 0.3

# Get the environment variables
DBNAME = os.environ['DBNAME']
PGUSER = os.environ['PGUSER']
PGPASSWORD = os.environ['PGPASSWORD']
PGHOST = os.environ['PGHOST']
PGPORT = os.environ['PGPORT']

# Stanfard status
std = dict([])


std_res = open('ocr.dat', 'r')

for row in std_res:
    if (len(row) < 2): continue
    dat = row.strip().split(' ')
    std[str(dat[0])] = dat[1]
std_res.close()

# Connect database
conn = psycopg2.connect(database = DBNAME, user = PGUSER, password = PGPASSWORD, host = PGHOST, port = PGPORT)

cur = conn.cursor()

# Check table status
cur.execute("SELECT COUNT(*) FROM features")
for row in cur.fetchall(): num = row[0]
if (std["features"] != str(num)):
    print "Error in Table features"
    exit(0)

cur.execute("SELECT COUNT(*) FROM label1")
for row in cur.fetchall(): num = row[0]
if (std["label1"] != str(num)):
    print "Error in Table label1"
    exit(0)

cur.execute("SELECT COUNT(*) FROM label2")
for row in cur.fetchall(): num = row[0]
if (std["label2"] != str(num)):
    print "Error in Table label2"
    exit(0)

# Check result
cur.execute("SELECT wid, expectation FROM label1_val_inference")

rows = cur.fetchall()
for row in rows:
    if (abs(float(std["label1_" + str(row[0])]) - float(row[1])) > eps):
        print "Error result in label1_val_inference!"
        exit(0)

cur.execute("SELECT wid, expectation FROM label2_val_inference")

rows = cur.fetchall()
for row in rows:
    if (abs(float(std["label2_" + str(row[0])]) - float(row[1])) > eps):
        print "Error result in label1_val_inference!"
        exit(0)

print "Test passed!"
