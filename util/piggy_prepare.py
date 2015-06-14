import hashlib
import json
import os
import sys
import time


def piggy_prepare_run(dir, func, source, target, is_pgxl):

    date = time.strftime('%Y%m%d')
    fp = date + '_' + hashlib.sha1('\t'.join([dir, func, source, target])).hexdigest()[:8]
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

    logfile_path = os.path.join(dir, fname + '.log')
    if os.path.isfile(logfile_path):
        os.remove(logfile_path)

    ts_format = '%Y-%m-%d %H:%M:%S.%f'


    # Why TABLE not VIEW?
    # Because PGXL does not create views on data nodes.

    sql_create_tables = '''
    DROP TABLE IF EXISTS %(sview)s CASCADE;
    DROP TABLE IF EXISTS %(tview)s CASCADE;
    CREATE TABLE %(sview)s AS SELECT * FROM (%(source_query)s) _sview_source LIMIT 0;
    CREATE TABLE %(tview)s AS %(target_query)s LIMIT 0;
    ''' % {
        'sview': sview,
        'tview': tview,
        'source_query': source_query,
        'target_query': target_query,
    }


    # Why log to file instead of a DB table?
    # PGXL would complain something like 'no XID'.
    # This may have fixed it though: https://github.com/snaga/postgres-xl/commit/d4136935a1d741c61ada13ef8c5ae44f68162cc9

    sql_create_functions = '''
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
        if cur_time - piggy.last_update_time > 10:
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

    sql_insert = '''
    INSERT INTO %(target)s
    SELECT (_piggy_x).* FROM
    (
      SELECT %(fname)s((_piggy_s.*)) _piggy_x
      FROM (%(source_query)s) _piggy_s
    ) _piggy_t;
    ''' % {
        'fname': fname,
        'sview': sview,
        'target': target,
        'source_query': source_query,
    }

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

    return {
        'sql_create_tables': sql_create_tables,
        'sql_create_functions': sql_create_functions,
        'sql_insert': sql_insert,
        'sql_getlog': sql_getlog,
        'sql_clean': sql_clean
    }


if __name__ == '__main__':
    if len(sys.argv) == 2:
        params = json.loads(sys.argv[1])
    else:
        print 'Usage:', sys.argv[0], '<params_json>'
        sys.exit(1)

    keys = ['dir', 'func', 'source', 'target', 'is_pgxl']

    for k in keys:
        if k not in params:
            print 'Params json must have these keys: %s' % str(keys)
            sys.exit(1)

    result = piggy_prepare_run(**params)
    print json.dumps(result)
