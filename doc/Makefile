# Makefile for DeepDive website maintenance
#
# We use jekyll (http://jekyllrb.com/) to build the website from the Markdown docs.
# You can install jekyll with "gem install jekyll".
#

DEPLOY_USER_HOST = rionda@whale.stanford.edu
DEPLOY_DIR = /afs/cs/group/infolab/deepdive/www/
GITHUB_REPO_URL = https://github.com/HazyResearch/deepdive

TEST_PORT = 4000
test:
	# Launching a server for testing...
	jekyll serve --host localhost --port $(TEST_PORT) --baseurl "" --watch

deploy:
	# Building site...
	jekyll build
	# Deploying site...
	scp -r _site/* $(DEPLOY_HOST):$(DEPLOY_DIR)

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

