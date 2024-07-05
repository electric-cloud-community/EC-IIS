#
# Makefile responsible for building the EC-IIS plugin
#
# Copyright (c) 2005-2012 Electric Cloud, Inc.
# All rights reserved

SRCTOP=..

include $(SRCTOP)/build/vars.mak

build: buildJavaPlugin
unittest:
systemtest: systemtest-setup mysystemtest-run

systemtest-setup:
	$(EC_PERL) systemtest/setup.pl $(TEST_SERVER) $(PLUGINS_ARTIFACTS)

mysystemtest-run: NTESTFILES ?= systemtest

mysystemtest-run:
	@-mkdir -p "$(OUTDIR)/$(ARCH)"
	COMMANDER_PLUGIN_PERL="../../../staging/agent/perl" \
	COMMANDER_JOBSTEPID="" \
	COMMANDER_DEBUG=1 \
	COMMANDER_DEBUGFILE="$(OUTDIR)/$(ARCH)/systemtest.log" \
	$(EC_PERL) $(NTEST) --testout $(OUTDIR)/$(ARCH)/systemtest \
		--target $(TEST_SERVER) $(NTESTARGS) $(NTESTFILES)

include $(SRCTOP)/build/rules.mak
