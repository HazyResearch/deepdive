import hashlib
import inspect
import json
import os
import sys
import time


def piggy_prepare_run(dir, script, source, target, is_pgxl):

    date = time.strftime('%Y%m%d')
    fp = date + '_' + hashlib.sha1('\t'.join([dir, script, source, target])).hexdigest()[:8]
    fname = 'piggy_func_' + fp
    sview = 'piggy_source_' + fp
    tview = 'piggy_target_' + fp

    if ' ' not in source:
        source_query = 'SELECT * FROM ' + source
    else:
        source_query = source

    if '(' not in target:
        target_query = 'SELECT * FROM ' + target
    else:
        tname, tcols = target.strip(' )').split('(')
        target_query = 'SELECT %s FROM %s' % (tcols, tname)

    logfile_path = os.path.join(dir, fname + '.log')
    if os.path.isfile(logfile_path):
        os.remove(logfile_path)

    ts_format = '%m-%d %H:%M:%S'

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

        # NULL input tuple indicates end of stream.
        # It's time to flush output and clean up.
        if record is None:
            import os
            if 'piggy' not in SD:
                raise StopIteration
            piggy = SD['piggy']
            try:
                piggy.log('Grabbing final tuples from the buffer.')
                for x in piggy.pull_final():
                    yield x
            finally:
                del SD['piggy']
            raise StopIteration

        # First input tuple; we setup the worker process.
        if 'piggy' not in SD:
            import errno
            import fcntl
            import json
            import os
            import socket
            import subprocess
            import time
            from datetime import datetime
            from Queue import Queue, Empty, Full
            from threading import Thread

            # Create a pipe for logs.
            # Only one process would succeed on the same host.
            log_pipe_name = os.path.join('%(dir)s', '%(fname)s.pipe')
            try:
                os.mkfifo(log_pipe_name)
            except OSError:
                pass
            log_pipe_fd = os.open(log_pipe_name, os.O_WRONLY)
            log_file = open(log_pipe_name + '.log', 'a')

            class Piggy(object):

                def __init__(self, proc):
                    self.proc = proc
                    self.proc_exited = False
                    self.input_count = 0
                    self.output_count = 0
                    self.start_time = time.time()
                    self.last_update_time = self.start_time
                    self.input_ended = False
                    self.input_queue = Queue(1000)
                    self.output_queue = Queue(1000)
                    port = plpy.execute("select inet_server_addr() || ':' || inet_server_port() as p")[0]['p']
                    self.log_prefix = '[PIGGY][' + str(os.getpid()) + '@' + port + '] '

                    # We could have used the following for non-blocking reads and writes,
                    # but we are not -- because non-blocking writes could result in partial writes.
                    # http://eyalarubas.com/python-subproc-nonblock.html

                    # flags = fcntl.fcntl(proc.stdout, fcntl.F_GETFL)
                    # fcntl.fcntl(proc.stdout, fcntl.F_SETFL, flags | os.O_NONBLOCK)
                    # flags = fcntl.fcntl(proc.stdin, fcntl.F_GETFL)
                    # fcntl.fcntl(proc.stdin, fcntl.F_SETFL, flags | os.O_NONBLOCK)

                    # Send data to stdin in another thread
                    def proc_push(piggy):
                        while True:
                            ended_already = piggy.input_ended
                            try:
                                item = piggy.input_queue.get(block=True, timeout=0.1)
                                piggy.proc.stdin.write(json.dumps(item) + '\\n')
                            except Empty:
                                if ended_already:
                                    piggy.proc.stdin.close()
                                    break

                    # Grab output from stdout in another thread
                    def proc_pull(piggy):
                        for line in iter(piggy.proc.stdout.readline, ''):
                            item = json.loads(line.rstrip())
                            piggy.output_queue.put(item)
                        piggy.proc_exited = True

                    # Monitor process liveness in another thread
                    # (killing process doesn't seem to break stdin or stdout)
                    def proc_heartbeat(piggy):
                        while piggy.proc.poll() is None:
                            time.sleep(0.25)
                        piggy.proc_exited = True

                    for p in [proc_push, proc_pull, proc_heartbeat]:
                        t = Thread(target=p, args=(self,))
                        t.daemon = True
                        t.start()

                def log(self, line):
                    ts = datetime.now().strftime('%(ts_format)s')
                    content = self.log_prefix + ts + ' ' + line + '\\n'
                    os.write(log_pipe_fd, content)
                    log_file.write(content)

                def log_stats(self, cur_time=None):
                    if not cur_time:
                        cur_time = time.time()
                    self.log('STATS: in = ' + str(self.input_count) +
                             '; out = ' + str(self.output_count) +
                             '; time = ' + str(int(cur_time - self.start_time)) + ' sec.')

                def push(self, item):
                    try:
                        self.input_queue.put_nowait(item)
                        self.input_count += 1
                        cur_time = time.time()
                        if cur_time - self.last_update_time >= 5:
                            self.log_stats(cur_time)
                            self.last_update_time = cur_time
                        return True
                    except Full:
                        return False

                def pull(self):
                    try:
                        item = self.output_queue.get_nowait()
                        self.output_count += 1
                        return item
                    except Empty:
                        return None

                def pull_all(self):
                    item = self.pull()
                    while item is not None:
                        yield item
                        item = self.pull()

                def pull_final(self):
                    self.input_ended = True

                    # wait till process has exited AND we have exhausted stdout
                    while True:
                        exited_already = self.proc_exited
                        for x in self.pull_all():
                            yield x
                        if exited_already:
                            break
                        time.sleep(0.1)

                    self.log_stats()
                    os.close(log_pipe_fd)
                    log_file.close()

            # Spawn the worker process.
            # TODO: tune the buffer size.
            proc = subprocess.Popen('%(script)s',
                                    stdin=subprocess.PIPE,
                                    stdout=subprocess.PIPE,
                                    stderr=log_pipe_fd,
                                    cwd='%(dir)s',
                                    bufsize=-1,  # system default buffer size
                                    shell=True)

            piggy = Piggy(proc)
            piggy.log('Worker started at ' + str(datetime.now()))
            SD['piggy'] = piggy

        piggy = SD['piggy']
        import os
        plpy.info('ONE TUPLE AT ' + str(os.getpid()))

        pushed = False
        while not pushed:
            if piggy.proc_exited:
                raise Exception('Worker process exited unexpectedly.')
            pushed = piggy.push(record)
            for x in piggy.pull_all():
                yield x

    $func$ LANGUAGE plpythonu VOLATILE;


    CREATE OR REPLACE FUNCTION %(fname)s_getlog ()
        RETURNS SETOF text
    AS $log$
        import os
        log_pipe_name = os.path.join('%(dir)s', '%(fname)s.pipe')
        if not os.path.exists(log_pipe_name):
            raise StopIteration

        if 'buffer' not in SD:
            import collections
            from threading import Thread

            # circular buffer
            buffer = collections.deque(maxlen=10000)

            def log_sucker():
                # log_fd = os.read(log_pipe_name, os.O_RDONLY)
                with open(log_pipe_name, 'r') as log:
                    for line in iter(log.readline, ''):
                        buffer.append(line.rstrip())

            t = Thread(target=log_sucker)
            t.daemon = True
            t.start()

            SD['buffer'] = buffer

        try:
            buf = SD['buffer']
            while True:
                yield buf.popleft()
        except IndexError:
            raise StopIteration

    $log$ LANGUAGE plpythonu VOLATILE;

    ''' % {
        'dir': dir,
        'script': script,
        'fname': fname,
        'sview': sview,
        'tview': tview,
        'ts_format': ts_format,
    }

    if is_pgxl:
        sql_dummy_source = 'FROM pgxl_dual'
        sql_getlog = '''
        SELECT %(fname)s_getlog() AS content FROM pgxl_dual_hosts;
        ''' % {'fname': fname}
    else:
        sql_dummy_source = ''
        sql_getlog = '''
        SELECT %(fname)s_getlog() AS content;
        ''' % {'fname': fname}

    # NOTE: relying on the assumption that the DB execution order
    # is the same as the order around UNION ALL.
    # Also tried splitting into two INSERT statements, but PGXL
    # seems to use different PG processes for remote subqueries and
    # so we lose access to the worker processes in the flush call.
    sql_insert = '''
    INSERT INTO %(target)s
        SELECT (_piggy_x).* FROM
        (
          SELECT %(fname)s((_piggy_s.*)) _piggy_x
          FROM (%(source_query)s) _piggy_s
            UNION ALL
          SELECT %(fname)s(null::%(sview)s) _piggy_x %(sql_dummy_source)s
        ) _piggy_body;
    ''' % {
        'fname': fname,
        'sview': sview,
        'target': target,
        'source_query': source_query,
        'sql_dummy_source': sql_dummy_source,
    }

    sql_clean = '''
    DROP FUNCTION %(fname)s (%(sview)s);
    DROP FUNCTION %(fname)s_getlog ();
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

    keys = inspect.getargspec(piggy_prepare_run).args

    if len(sys.argv) == 2:
        params = json.loads(sys.argv[1])
    else:
        print 'Usage:', sys.argv[0], '<params_json> with keys', keys
        sys.exit(1)

    for k in keys:
        if k not in params:
            print 'Params json must have these keys: %s' % str(keys)
            sys.exit(1)

    result = piggy_prepare_run(**params)
    print json.dumps(result)
