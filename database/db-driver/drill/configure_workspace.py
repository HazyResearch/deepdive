import sys
import json

config = json.load(sys.stdin)
config['name'] = sys.argv[1]
config['config']['connection'] = sys.argv[2]
config['config']['workspaces']['default']['location'] = sys.argv[3]
print(json.dumps(config))
