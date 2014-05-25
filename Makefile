# Makefile for DeepDive website maintenance

TEST_PORT = 4000
test:
	# Launching a server for testing...
	jekyll serve --host localhost --port $(TEST_PORT) --baseurl "" --watch

DEPLOY_HOST = zifei@whale.stanford.edu
DEPLOY_DIR = /afs/cs/group/infolab/deepdive/www
deploy:
	# Building site...
	jekyll build
	# Deploying site...
	scp -r _site/* $(DEPLOY_HOST):$(DEPLOY_DIR)/

