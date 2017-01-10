# Meta-Makefile that simplifies build with submodules
#
# Including this file defines make targets build-submodule-* and
# clean-submodule-* for every submodule at PATH/TO/SUBMODULE that has the
# following lines defined:
#
#   include path/to/this/build-submodules.mk
#   $(BUILD_SUBMODULE)/PATH/TO/A/SUBMODULE.mk: COPY = foo bar/
#   $(BUILD_SUBMODULE)/PATH/TO/ANOTHER/SUBMODULE.mk: COPY = target/baz.jar
#
# Each submodule should have its actual build steps written in a script next to
# the submodule following a naming convention: PATH/TO/SUBMODULE.build.sh
#
# This requires build-submodule-if-needed script to be next to it.

# some configurable path names
BUILD_DIR ?= .build
BUILD_SUBMODULE ?= $(BUILD_DIR)/submodule

# define aggregate targets
.PHONY: build-submodules clean-submodules rebuild-submodules
build-submodules:
clean-submodules:
rebuild-submodules:
rebuild-submodule-%: export ALWAYS_BUILD_SUBMODULE := true

# hook them up to standard targets
build test-build: build-submodules
clean: clean-submodules

# find where this Makefile is located
BUILD_SUBMODULE_MK := $(lastword $(MAKEFILE_LIST))
BUILD_SUBMODULE_HOME := $(dir $(BUILD_SUBMODULE_MK))

# define how to generate the make recipes for each submodule
$(BUILD_SUBMODULE)/%.mk: SUBMODULE_NAME = $(notdir $*)
$(BUILD_SUBMODULE)/%.mk: $(MAKEFILE_LIST)
	@mkdir -p $(@D) && rm -f $@ && : >$@
	@echo >>$@ '.PHONY: build-submodule-$(SUBMODULE_NAME) clean-submodule-$(SUBMODULE_NAME) rebuild-submodule-$(SUBMODULE_NAME)'
	@echo >>$@
	@echo >>$@ 'build-submodule-$(SUBMODULE_NAME): export BUILD_SUBMODULE := $$(BUILD_SUBMODULE)'
	@echo >>$@ 'build-submodule-$(SUBMODULE_NAME):'
	@echo >>$@ '	@$(BUILD_SUBMODULE_HOME)/build-submodule-if-needed $* $(COPY)'
	@echo >>$@ 'build-submodules: build-submodule-$(SUBMODULE_NAME)'
	@echo >>$@
	@echo >>$@ 'clean-submodule-$(SUBMODULE_NAME):'
	@echo >>$@ '	rm -rfv $$(BUILD_SUBMODULE)/$* $$(BUILD_SUBMODULE)/$*.built'
	@echo >>$@ 'clean-submodules: clean-submodule-$(SUBMODULE_NAME)'
	@echo >>$@
	@echo >>$@ 'rebuild-submodule-$(SUBMODULE_NAME): build-submodule-$(SUBMODULE_NAME)'
	@echo >>$@ 'rebuild-submodules: rebuild-submodule-$(SUBMODULE_NAME)'
	@chmod a-w $@

# define a shorthand for both generating and including the submodule Makefile and specifying the files to copy
#
# Usage: call the following Make function from anywhere in your Makefile
#   $(call BUILD_SUBMODULE_AND_COPY, path_to_the_submodule, paths_within_the_submodule_separated_by_whitespace)
#
# - first argument is the relative path to the submodule under the parent repo
# - second argument is a list of paths to copy out from the submodule after build
# - optional third argument is a mnemonic name for Make targets (without any
#   slashes) which defaults to the basename of the first argument
BUILD_SUBMODULE_AND_COPY = \
    $(eval $(call _BUILD_SUBMODULE_AND_COPY_,$(strip $(1)),$(strip $(2)),$(strip $(3))))
define _BUILD_SUBMODULE_AND_COPY_
include $(BUILD_SUBMODULE)/$(1).mk
$(BUILD_SUBMODULE)/$(strip $(1)).mk: COPY = $(2)
ifneq ($(3),)
$(BUILD_SUBMODULE)/$(strip $(1)).mk: SUBMODULE_NAME = $(3)
endif
endef
