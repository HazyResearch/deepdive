import os
import getpass
import psycopg2
from sets import Set

# Error threshold
eps = 0.5

# Get the environment variables
DBNAME = os.environ['DBNAME']
PGUSER = os.environ['PGUSER']
PGPASSWORD = os.environ['PGPASSWORD']
PGHOST = os.environ['PGHOST']
PGPORT = os.environ['PGPORT']

# Stanfard status
std = dict([])


std_res = open('smoke.dat', 'r')

for row in std_res:
    if (len(row) < 2): continue
    dat = row.strip().split(' ')
    std[str(dat[0])] = dat[1]
std_res.close()

# Connect database
conn = psycopg2.connect(database = DBNAME, user = PGUSER, password = PGPASSWORD, host = PGHOST, port = PGPORT)

cur = conn.cursor()

# Check table status
cur.execute("SELECT COUNT(*) FROM friends")
for row in cur.fetchall(): num = row[0]
if (std["friends"] != str(num)):
    print "Error in Table friends"
    exit(0)

cur.execute("SELECT COUNT(*) FROM person")
for row in cur.fetchall(): num = row[0]
if (std["person"] != str(num)):
    print "Error in Table person"
    exit(0)

cur.execute("SELECT COUNT(*) FROM person_has_cancer")
for row in cur.fetchall(): num = row[0]
if (std["person_has_cancer"] != str(num)):
    print "Error in Table person_has_cancer"
    exit(0)

cur.execute("SELECT COUNT(*) FROM person_smokes")
for row in cur.fetchall(): num = row[0]
if (std["person_smokes"] != str(num)):
    print "Error in Table person_smokes"
    exit(0)

# Check result
cur.execute("SELECT person_id, expectation FROM person_has_cancer_has_cancer_inference")

rows = cur.fetchall()
for row in rows:
    if (abs(float(std["person_has_cancer_" + str(row[0])]) - float(row[1])) > eps):
        print "Error result!"
        exit(0)

cur.execute("SELECT person_id, expectation FROM person_smokes_smokes_inference")

rows = cur.fetchall()
for row in rows:
    if (abs(float(std["person_smokes_" + str(row[0])]) - float(row[1])) > eps):
        print "Error result!"
        exit(0)


print "Test passed!"
