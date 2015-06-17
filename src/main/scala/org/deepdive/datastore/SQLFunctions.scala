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

CREATE OR REPLACE FUNCTION piggy_setup_package (package_name text, zip_blob bytea)
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
client_dir = os.path.join(tempfile.gettempdir(), 'piggy_packages', package_name)
dir = os.path.join(client_dir, fp)
try:
    # only one contending process would succeed here
    os.makedirs(dir)
except OSError:
    plpy.info('Directory already exists: ' + dir)
    return
plpy.info('Created ' + dir)

# remove older versions
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

return dir

$$ LANGUAGE plpythonu;
    """
}
