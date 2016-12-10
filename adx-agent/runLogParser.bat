#!/bin/bash
#
# Usage
#   sh ./runServer.sh
#

TACAA_HOME=`pwd`
echo $TACAA_HOME
echo $CLASSPATH
java -cp "lib/*" se.sics.tasim.logtool.Main -handler tau.tac.adx.parser.GeneralHandler -file "C:\Users\Tom\Documents\4thYear\IntelligentAgents\game.slg.gz" -ucs -rating -bank -campaign -adnet > parserlog.txt

