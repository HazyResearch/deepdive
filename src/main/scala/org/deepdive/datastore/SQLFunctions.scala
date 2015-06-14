package org.deepdive.datastore

object SQLFunctions {
  val radicalSequenceAssignForXL = """
    CREATE OR REPLACE FUNCTION copy_table_assign_ids_replace(
      schema_name character varying,
      table_name character varying,
      col_name character varying,
      start_id bigint,
      order_by character varying DEFAULT '')
    RETURNS TEXT AS
    $$
    DECLARE
      table_cur text := quote_ident(schema_name) || '.' || quote_ident(table_name);
      table_old text := quote_ident(schema_name) || '.' || quote_ident(table_name || '__old');
      table_new text := quote_ident(schema_name) || '.' || quote_ident(table_name || '__new');
      cols text[] := ARRAY(SELECT
                        CASE lower(attname)
                        WHEN lower(col_name) THEN start_id || ' + (row_number() over (' || order_by || ')) - 1'
                        ELSE quote_ident(attname)
                        END
                      FROM   pg_attribute
                      WHERE  attrelid = table_cur::regclass
                      AND    attnum > 0
                      AND    NOT attisdropped
                      ORDER  BY attnum);
    BEGIN
      RAISE NOTICE '%',  cols;
      EXECUTE 'drop table if exists ' || table_old || ' cascade;';
      EXECUTE 'drop table if exists ' || table_new || ' cascade;';
      EXECUTE 'create table ' || table_new || ' (like ' || table_cur || ' including all)';
      EXECUTE 'insert into ' || table_new || ' select ' || array_to_string(cols, ',') || ' from ' || table_cur;
      EXECUTE 'alter table ' || table_cur || ' rename to ' || table_name || '__old';
      EXECUTE 'alter table ' || table_new || ' rename to ' || table_name;
      RETURN '';
    END;
    $$ LANGUAGE 'plpgsql';
  """

        val fastSequenceAssignForGreenplum = """
    CREATE OR REPLACE FUNCTION clear_count_1(sid int) RETURNS int AS
    $$
    if '__count_1' in SD:
      SD['__count_1'] = -1
      return 1
    return 0
    $$ LANGUAGE plpythonu;


    CREATE OR REPLACE FUNCTION updateid(startid bigint, sid int, sids int[], base_ids bigint[], base_ids_noagg bigint[]) RETURNS bigint AS
    $$
    if '__count_1' in SD:
      a = SD['__count_2']
      b = SD['__count_1']
      SD['__count_2'] = SD['__count_2'] - 1
      if SD['__count_2'] < 0:
        SD.pop('__count_1')
      return startid+b-a
    else:
      for i in range(0, len(sids)):
        if sids[i] == sid:
          SD['__count_1'] = base_ids[i] - 1
          SD['__count_2'] = base_ids_noagg[i] - 1
      a = SD['__count_2']
      b = SD['__count_1']
      SD['__count_2'] = SD['__count_2'] - 1
      if SD['__count_2'] < 0:
        SD.pop('__count_1')
      return startid+b-a

    $$ LANGUAGE plpythonu;

    CREATE OR REPLACE FUNCTION fast_seqassign(tname character varying, startid bigint) RETURNS TEXT AS
    $$
    BEGIN
      EXECUTE 'drop table if exists tmp_gpsid_count cascade;';
      EXECUTE 'drop table if exists tmp_gpsid_count_noagg cascade;';
      EXECUTE 'create table tmp_gpsid_count as select gp_segment_id as sid, count(clear_count_1(gp_segment_id)) as base_id from ' || quote_ident(tname) || ' group by gp_segment_id order by sid distributed by (sid);';
      EXECUTE 'create table tmp_gpsid_count_noagg as select * from tmp_gpsid_count distributed by (sid);';
      EXECUTE 'update tmp_gpsid_count as t set base_id = (SELECT SUM(base_id) FROM tmp_gpsid_count as t2 WHERE t2.sid <= t.sid);';
      RAISE NOTICE 'EXECUTING _fast_seqassign()...';
      EXECUTE 'select * from _fast_seqassign(''' || quote_ident(tname) || ''', ' || startid || ');';
      RETURN '';
    END;
    $$ LANGUAGE 'plpgsql';

    CREATE OR REPLACE FUNCTION _fast_seqassign(tname character varying, startid bigint)
    RETURNS TEXT AS
    $$
    DECLARE
      sids int[] :=  ARRAY(SELECT sid FROM tmp_gpsid_count ORDER BY sid);
      base_ids bigint[] :=  ARRAY(SELECT base_id FROM tmp_gpsid_count ORDER BY sid);
      base_ids_noagg bigint[] :=  ARRAY(SELECT base_id FROM tmp_gpsid_count_noagg ORDER BY sid);
      tsids text;
      tbase_ids text;
      tbase_ids_noagg text;
    BEGIN
      SELECT INTO tsids array_to_string(sids, ',');
      SELECT INTO tbase_ids array_to_string(base_ids, ',');
      SELECT INTO tbase_ids_noagg array_to_string(base_ids_noagg, ',');
      if ('update ' || tname || ' set id = updateid(' || startid || ', gp_segment_id, ARRAY[' || tsids || '], ARRAY[' || tbase_ids || '], ARRAY[' || tbase_ids_noagg || ']);')::text is not null then
        EXECUTE 'update ' || tname || ' set id = updateid(' || startid || ', gp_segment_id, ARRAY[' || tsids || '], ARRAY[' || tbase_ids || '], ARRAY[' || tbase_ids_noagg || ']);';
      end if;
      RETURN '';
    END;
    $$
    LANGUAGE 'plpgsql';
    """

    val piggyExtractorDriverDeclaration = """

CREATE OR REPLACE FUNCTION piggy_setup_env (client_name text, zip_blob bytea)
  RETURNS text
AS $$

import hashlib
import os
import shutil
import subprocess
import tempfile
import zipfile

try:
    from cStringIO import StringIO
except:
    from StringIO import StringIO

# postgres on homebrew somehow does not have /usr/local/bin in PATH
os.environ['PATH'] += os.pathsep + '/usr/local/bin'

if not zip_blob:
    plpy.error('No blob provided')
    return

# create subdir for this version
fp = hashlib.sha1(zip_blob).hexdigest()[:8]
client_dir = os.path.join(tempfile.gettempdir(), 'piggy_envs', client_name)
dir = os.path.join(client_dir, fp)
try:
    # only one contending process would succeed here
    os.makedirs(dir)
except OSError:
    plpy.info('Directory already exists: ' + dir)
    return
plpy.info('Created ' + dir)

# remove the other versions
for x in os.listdir(client_dir):
    if x in ['env', fp]:
        continue
    plpy.info('Removing subdir: ' + fp)
    shutil.rmtree(os.path.join(client_dir, x))

# expand zip blob
content = StringIO(zip_blob)
zipf = zipfile.ZipFile(content, 'r')
zipf.extractall(dir)
plpy.info('Extracted zip content to ' + dir)

# make / update virtualenv -- only one per client
venv = os.path.join(client_dir, 'env')
plpy.info('Ensuring env: ' + venv)
subprocess.check_output('virtualenv "%s"' % venv,
                        stderr=subprocess.STDOUT,
                        shell=True)

# install pip libraries
reqfile = os.path.join(dir, 'requirements.txt')
if os.path.isfile(reqfile):
    plpy.info('Installing libs from ' + reqfile)
    output = subprocess.check_output('%s/bin/pip install -r "%s"' % (venv, reqfile),
                                     stderr=subprocess.STDOUT,
                                     shell=True)
    plpy.info(output)

return dir

$$ LANGUAGE plpythonu;



CREATE OR REPLACE FUNCTION piggy_prepare_run (dir text, func text, source text, target text)
  RETURNS text[]
AS $$

import hashlib
import os
import time

date = time.strftime('%Y%m%d')
fp = date + '_' + hashlib.sha1('\\t'.join([dir, func, source, target])).hexdigest()[:8]
fname = 'piggy_func_' + fp
sview = 'piggy_source_' + fp
tview = 'piggy_target_' + fp
pymodule, pyfunc = func.rsplit('.', 1)
venv = os.path.join(dir, os.pardir, 'env')
activate_this = os.path.join(venv, 'bin', 'activate_this.py')

if ' ' not in source:
    source_query = 'SELECT * FROM ' + source
else:
    source_query = source

if '(' not in target:
    target_query = 'SELECT * FROM ' + target
else:
    tname, tcols = target.strip(' )').split('(')
    target_query = 'SELECT %s FROM %s' (tcols, tname)

# Why do we have these in a separate call?
# Because PGXL would whine if you have them together with CREATE TABLE.
sql = '''
DROP TABLE IF EXISTS %(sview)s CASCADE;
DROP TABLE IF EXISTS %(tview)s CASCADE;
''' % {
    'sview': sview,
    'tview': tview,
}
plpy.execute(sql)

# Why TABLE not VIEW?
# Because PGXL does not create views on data nodes.
# Why do we need the column names?
# Because we need them to convert row type to composite type.
sql = '''
CREATE TABLE %(sview)s AS SELECT * FROM (%(source_query)s) _sview_source LIMIT 0;
CREATE TABLE %(tview)s AS %(target_query)s LIMIT 0;

SELECT string_agg(name, ', ') as scols
FROM
(
    SELECT attname as name
    FROM   pg_attribute
    WHERE  attrelid = '%(sview)s'::regclass
    AND    attnum > 0
    AND    NOT attisdropped
    ORDER  BY attnum
) t;
''' % {
    'sview': sview,
    'tview': tview,
    'source_query': source_query,
    'target_query': target_query,
}

plpy.info(sql)
scols = plpy.execute(sql)[0]['scols']

logfile_path = os.path.join(dir, fname + '.log')
if os.path.isfile(logfile_path):
    os.remove(logfile_path)

ts_format = '%Y-%m-%d %H:%M:%S.%f'


# Why not merge CREATE FUNCTION with CREATE TABLE?
# Because PGXL sucks.
#
# Why log to file instead of a DB table?
# PGXL would complain something like 'no XID'.
# This may have fixed it though: https://github.com/snaga/postgres-xl/commit/d4136935a1d741c61ada13ef8c5ae44f68162cc9

sql = '''
CREATE OR REPLACE FUNCTION %(fname)s (record %(sview)s)
    RETURNS SETOF %(tview)s
AS $func$

    if 'func' not in SD:
        import fcntl
        import os
        import sys
        import time
        from datetime import datetime

        activate_this = '%(activate_this)s'
        if os.path.isfile(activate_this):
            execfile(activate_this, dict(__file__=activate_this))
        sys.path.insert(0, '%(dir)s')
        module = __import__('%(pymodule)s')
        module = reload(module)
        SD['func'] = getattr(module, '%(pyfunc)s')

        # set up file-based logging
        logfile = open('%(logfile_path)s', 'a')

        def log_line(x):
            fcntl.flock(logfile, fcntl.LOCK_EX)
            ts = datetime.now().strftime('%(ts_format)s')
            content = str(x)
            for line in content.split('\\n'):
                logfile.write(ts + '\\t' + line + '\\n')
            # TODO: more efficient way to flush.
            # Difficulty is we don't know when input ends.
            logfile.flush()
            fcntl.flock(logfile, fcntl.LOCK_UN)

        class Piggy(object):
            pass

        piggy = Piggy()
        piggy.log = log_line
        piggy.input_count = 0
        piggy.output_count = 0
        piggy.start_time = time.time()
        piggy.last_update_time = piggy.start_time

        SD['piggy'] = piggy
        SD['time'] = time

    func, piggy = SD['func'], SD['piggy']

    try:
        result = list(func(record, piggy))
    except:
        import traceback
        tb = traceback.format_exc()
        piggy.log('*' * 80)
        piggy.log(tb)
        piggy.log('*' * 80)
        piggy.log('Offending input record:')
        piggy.log(record)
        piggy.log('*' * 80)
        raise

    piggy.input_count += 1
    piggy.output_count += len(result)
    cur_time = SD['time'].time()
    if cur_time - piggy.last_update_time > 1:
        piggy.log('STATS: in = ' + str(piggy.input_count) +
                  '; out = ' + str(piggy.output_count) +
                  '; time = ' + str(int(cur_time - piggy.start_time)) + ' sec.')
        piggy.last_update_time = cur_time

    return result

$func$ LANGUAGE plpythonu VOLATILE;


CREATE OR REPLACE FUNCTION %(fname)s_getlog (threshold text)
    RETURNS SETOF text
AS $log$
    import os

    file = '%(logfile_path)s'
    if not os.path.isfile(file):
        return

    with open(file) as logfile:
        for line in logfile:
            line = line.rstrip('\\n ')
            parts = line.split('\\t', 1)
            if len(parts) != 2:
                continue
            ts, content = parts
            if ts > threshold:
                yield line

$log$ LANGUAGE plpythonu VOLATILE;

''' % {
    'dir': dir,
    'pyfunc': pyfunc,
    'pymodule': pymodule,
    'fname': fname,
    'sview': sview,
    'tview': tview,
    'activate_this': activate_this,
    'logfile_path': logfile_path,
    'ts_format': ts_format,
}

plpy.info(sql)
plpy.execute(sql)

sql = '''
INSERT INTO %(target)s
SELECT (x).* FROM
(
  SELECT %(fname)s((%(scols)s)) x
  FROM (%(source_query)s) s
) t;
''' % {
    'fname': fname,
    'sview': sview,
    'scols': scols,
    'target': target,
    'source_query': source_query,
}

is_pgxl = False
try:
    plpy.execute('SELECT * from pgxl_dual_hosts LIMIT 1')
    is_pgxl = True
except plpy.SPIError:
    pass

if is_pgxl:
    sql_getlog = '''
    SELECT %(fname)s_getlog(?) AS line FROM pgxl_dual_hosts ORDER BY line;
    '''
else:
    sql_getlog = '''
    SELECT %(fname)s_getlog(?) AS line ORDER BY line;
    '''

sql_getlog = sql_getlog % {'fname': fname}

sql_clean = '''
DROP FUNCTION %(fname)s (%(sview)s);
DROP FUNCTION %(fname)s_getlog (text);
DROP TABLE IF EXISTS %(sview)s;
DROP TABLE IF EXISTS %(tview)s;
''' % {
    'fname': fname,
    'sview': sview,
    'tview': tview,
}

# The user is supposed to run the returned SQL.
# Why not execute it here?
# Because PGXL wouldn't recognize the existence of the newly created tables (and hence types)
# if we execute the dynamic SQL within this function call.
# Solution: wait for this function call finishes on all nodes before we run the insertion.

return (sql, sql_getlog, sql_clean)

$$ LANGUAGE plpythonu;

    """
}
