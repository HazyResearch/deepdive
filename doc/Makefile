# Makefile for DeepDive website maintenance
#
# We use jekyll (http://jekyllrb.com/) to build the website from the Markdown docs.
# You can install jekyll with "gem install jekyll".
#

# Run "make help" to get documentaton.

# Get the deploying user in env var $DEPLOY_USER, default is `whoami`
ifeq ($(origin DEPLOY_USER), undefined)
  DEPLOY_USER = `whoami`
endif
DEPLOY_USER_HOST = $(DEPLOY_USER)@whale.stanford.edu
DEPLOY_DIR = /afs/cs/group/infolab/deepdive/www
GITHUB_REPO_URL = https://github.com/HazyResearch/deepdive
WEBSITE_CNAME = deepdive.stanford.edu
WEBSITE_BASEURL = http://$(WEBSITE_CNAME)
TEST_PORT = 4000

.PHONY: test
test: jekyll
	# Launching a server for testing...
	jekyll serve --host localhost --port $(TEST_PORT) --baseurl "" --watch

.PHONY: linkcheck linkcheck-deployed
linkcheck: jekyll linkchecker
	# Checking broken links locally
	jekyll serve --host localhost --port $(TEST_PORT) --baseurl "" & \
	    trap "kill -TERM $$!" EXIT; sleep 3; \
	linkchecker --timeout 30 --check-extern http://localhost:$(TEST_PORT)/
linkcheck-deployed: jekyll linkchecker
	# Checking broken links from deployed website
	linkchecker --timeout 30 --check-extern $(WEBSITE_BASEURL)/

.PHONY: deploy
deploy: jekyll
	######################################################################
	# NOTE: `make linkcheck` before every deployment highly recommended! #
	######################################################################
	# Building site...
	echo 'baseurl:	$(WEBSITE_BASEURL)' >_config.baseurl.yml
	jekyll build --config _config.yml,_config.baseurl.yml
	rm -f _config.baseurl.yml
	echo $(WEBSITE_CNAME) > _site/CNAME # create a CNAME file to put on server
	# Deploying site...
	@read -p"WARNING! Continue to overwrite all files in $(DEPLOY_USER_HOST):$(DEPLOY_DIR)? "
	ssh $(DEPLOY_USER_HOST) 'mkdir -p $(DEPLOY_DIR)'
	rsync -av --delete-after _site/ $(DEPLOY_USER_HOST):$(DEPLOY_DIR)/
	@echo "Deployed to $(WEBSITE_BASEURL)"

.PHONY: jekyll linkchecker
jekyll:
	type jekyll || gem install jekyll
linkchecker:
	type linkchecker >/dev/null || pip install linkchecker --use-mirrors

.PHONY: help
help:
	@cat README

.DEFAULT_GOAL := help
