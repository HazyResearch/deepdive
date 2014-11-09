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
WEBSITE_CNAME ="deepdive.stanford.edu"
TEST_PORT = 4000

.PHONY: test
test:
	# Launching a server for testing...
	jekyll serve --host localhost --port $(TEST_PORT) --baseurl "" --watch

.PHONY: serve
serve:
	# Launching a server and detach it to background
	jekyll serve --host localhost --port $(TEST_PORT) --baseurl "" --detach

.PHONY: deploy
deploy:
	@echo "Note that deploying will clean up previous files on site."
	# Building site...
	jekyll build
	# Deploying site...
	@echo "[DANGEROUS!] Will delete all files in $(DEPLOY_USER_HOST):$(DEPLOY_DIR)"
	ssh $(DEPLOY_USER_HOST) rm -rf $(DEPLOY_DIR)/*
	# create a CNAME file to put on server
	echo $(WEBSITE_CNAME) > _site/CNAME
	scp -r _site/* $(DEPLOY_USER_HOST):$(DEPLOY_DIR)

.PHONY: deploy-github
deploy-github:
	# Building site...
	jekyll build
	# Deploying the same contents to gh-pages
	rm -rf .gh-pages
	git clone $(GITHUB_REPO_URL) --branch gh-pages --reference .. .gh-pages
	rsync -avH --delete --exclude=/{.git,README.md} _site/ .gh-pages/
	cd .gh-pages && \
	  git add . && git add -u . && \
	  git commit -m "Deploying changes" && \
	  git push origin gh-pages
	rm -rf .gh-pages

.PHONY: help
help:
	@echo '======== DeepDive website maintenance Makefile documentation ========'
	@echo 'If you want to make changes, BE SURE TO TEST LOCALLY, and do a "git push" first and wait for the travis test to pass before you run "make deploy".'
	@echo ''
	@echo 'To test the docs locally, be sure to have dependency `jekyll` installed via `gem install jekyll`. Then under DEEPDIVE_HOME/doc/:'
	@echo ''
	@echo '    make test'
	@echo ''
	@echo 'This will host the docs on your http://localhost:4000. Keep it hosted, and open a new terminal, navigate into DEEPDIVE_HOME/src/test/bash/, then run:'
	@echo ''
	@echo '    ./linkcheck.sh http://localhost:4000 --check-extern'
	@echo ''
	@echo 'This will do a BETTER test than the travis one since it also checks external links. We have not figured out a good way to do this on Travis, since it occasionally suffers from a timeout problem.'
	@echo 'Ideally you will find the output like:'
	@echo ''
	@echo "    [`date`] No errors found."
	@echo ''
	@echo 'When all the tests are passed, you can run:'
	@echo ''
	@echo '    DEPLOY_USER=[yourUserName] make deploy'
	@echo ''
	@echo 'where [yourUserName] is your account to log into our server that hosts deepdive.'

.DEFAULT_GOAL:= help