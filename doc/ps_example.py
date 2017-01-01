"""Example parameter server.
1. Run this.
2. Run dw with "--parameter_server tcp://localhost:8888 --worker_id strongman"
"""

import msgpack
import zmq
import numpy as np

PORT = 8888


# see dimmwitted.cc DimmWitted::ps_update_weights for client code
def main():
    context = zmq.Context()
    socket = context.socket(zmq.REP)
    socket.bind("tcp://*:%s" % PORT)
    weights = None
    print('parameter server is up now')
    while True:
        buf = socket.recv()
        unpacker = msgpack.Unpacker()
        unpacker.feed(buf)
        worker_id, epochs, delta = unpacker
        worker_id = worker_id.decode('utf-8')
        print('\tgot msg: <%s, %d, %d>' % (worker_id, epochs, len(delta)))
        if weights is None:
            weights = np.zeros(len(delta))
        weights += delta

        packer = msgpack.Packer(use_bin_type=True)
        command = 'STOP' if epochs >= 95 else 'CONTINUE'
        weights64 = list(map(np.float64, weights))
        payload = b''.join(map(packer.pack, [command, weights64]))
        socket.send(payload)
        print('\tsent msg: <%s, %d>' % (command, len(payload)))


if __name__ == '__main__':
    main()
