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
TEST_HOST = localhost
TEST_PORT = 4000

# localize required Ruby gems
GEM_HOME ?= $(shell pwd)/.gems
export GEM_HOME
PATH := $(GEM_HOME)/bin:$(PATH)
SHELL := $(shell type -p bash)

IGNORE_URL = $(shell sed <.linkchecker.ignore-urls -e 's/[[:space:]]*\#.*$$//' -e '/^[[:space:]]*$$/d')
LINKCHECKER_OPTIONS = --timeout 30 \
                      --check-extern --anchors \
                      $(IGNORE_URL:%=--ignore-url='%') \
                      #

.PHONY: test
define JEKYLL_SERVE
jekyll serve --host $(TEST_HOST) --port $(TEST_PORT) --baseurl "" --watch
endef
test: jekyll
	# Launching a server for testing...
ifeq ($(shell PATH="$(PATH)"; jekyll --version | grep -c 'jekyll 2'),0)
	$(JEKYLL_SERVE)
else
	$(JEKYLL_SERVE) --config _config.yml,_config_2.x.yml
endif

.PHONY: linkcheck linkcheck-deployed
linkcheck: jekyll linkchecker
	# Checking broken links locally
	jekyll serve --host $(TEST_HOST) --port $(TEST_PORT) --baseurl "" & \
	    trap "kill -TERM $$!" EXIT; sleep 3; \
	linkchecker $(LINKCHECKER_OPTIONS) http://$(TEST_HOST):$(TEST_PORT)/
linkcheck-deployed: jekyll linkchecker
	# Checking broken links from deployed website
	linkchecker $(LINKCHECKER_OPTIONS) $(WEBSITE_BASEURL)/

.PHONY: deploy
deploy: jekyll
	######################################################################
	# NOTE: `make linkcheck` before every deployment highly recommended! #
	######################################################################
	# Building site...
	echo 'baseurl:	$(WEBSITE_BASEURL)' >_config.baseurl.yml
	jekyll build --config _config.yml,_config.baseurl.yml
	echo $(WEBSITE_CNAME) > _site/CNAME # create a CNAME file to put on server
	# Deploying site...
	@read -p"WARNING! Continue to overwrite all files in $(DEPLOY_USER_HOST):$(DEPLOY_DIR)? "
	ssh $(DEPLOY_USER_HOST) 'mkdir -p $(DEPLOY_DIR)'
	rsync -rtlHv --delete-after _site/ $(DEPLOY_USER_HOST):$(DEPLOY_DIR)/
	@echo "Deployed to $(WEBSITE_BASEURL)"
	rm -f _config.baseurl.yml

.PHONY: jekyll linkchecker
jekyll:
	type jekyll || gem2.0 install jekyll || gem install jekyll || gem install jekyll --version 2.5.3
linkchecker:
	type linkchecker >/dev/null || pip install linkchecker --use-mirrors

.PHONY: help
help:
	@cat README

.DEFAULT_GOAL := help
