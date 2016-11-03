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
$(BUILD_SUBMODULE)/%.mk: $(MAKEFILE_LIST)
	@mkdir -p $(@D) && rm -f $@ && : >$@
	@echo >>$@ '.PHONY: build-submodule-$(*F) clean-submodule-$(*F) rebuild-submodule-$(*F)'
	@echo >>$@
	@echo >>$@ 'build-submodule-$(*F): export BUILD_SUBMODULE := $$(BUILD_SUBMODULE)'
	@echo >>$@ 'build-submodule-$(*F):'
	@echo >>$@ '	@$(BUILD_SUBMODULE_HOME)/build-submodule-if-needed $* $(COPY)'
	@echo >>$@ 'build-submodules: build-submodule-$(*F)'
	@echo >>$@
	@echo >>$@ 'clean-submodule-$(*F):'
	@echo >>$@ '	rm -rfv $$(BUILD_SUBMODULE)/$* $$(BUILD_SUBMODULE)/$*.built'
	@echo >>$@ 'clean-submodules: clean-submodule-$(*F)'
	@echo >>$@
	@echo >>$@ 'rebuild-submodule-$(*F): build-submodule-$(*F)'
	@echo >>$@ 'rebuild-submodules: rebuild-submodule-$(*F)'
	@chmod a-w $@

# generate and include all recipes for submodules that are defined somewhere
# FIXME maybe switch to $(eval ...) to expand all variables not just BUILD_SUBMODULE
include $(subst $$(BUILD_SUBMODULE),$(BUILD_SUBMODULE), \
    $(shell sed '/^$$(BUILD_SUBMODULE)\/.*\.mk *: *COPY/ !d; s/:.*//' $(MAKEFILE_LIST)))
