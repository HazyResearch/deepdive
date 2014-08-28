# Makefile for DeepDive website maintenance
#
# We use jekyll (http://jekyllrb.com/) to build the website from the Markdown docs.
# You can install jekyll with "gem install jekyll".
#

# DEPLOY_USER_HOST = ssh.cs.brown.edu
# DEPLOY_DIR = public_html/deepdive/

# Get the deploying user in env var $DEPLOY_USER, default is `whoami`
ifeq ($(origin DEPLOY_USER), undefined)
  DEPLOY_USER = `whoami`
endif
DEPLOY_USER_HOST = $(DEPLOY_USER)@whale.stanford.edu
DEPLOY_DIR = /afs/cs/group/infolab/deepdive/www
GITHUB_REPO_URL = https://github.com/HazyResearch/deepdive
WEBSITE_CNAME ="deepdive.stanford.edu"
TEST_PORT = 4000

test:
	# Launching a server for testing...
	jekyll serve --host localhost --port $(TEST_PORT) --baseurl "" --watch

serve:
	# Launching a server and detach it to background
	jekyll serve --host localhost --port $(TEST_PORT) --baseurl "" --detach

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

