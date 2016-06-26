import sys
import json

config = json.load(sys.stdin)
config['name'] = sys.argv[1]
config['config']['workspaces']['default']['location'] = sys.argv[2]
print(json.dumps(config))
