#!/bin/bash
# בדיקות על המחשב: המנוע (Scan) — בלי אנדרואיד. יוצא בכישלון אם בדיקה נכשלה.
set -e
DIR=$1
T=/home/claude/tools
W=$(mktemp -d)
export LANG=C.UTF-8
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -nowarn -d $W $DIR/src/com/tikiyot/lab/Scan.java $DIR/src/com/tikiyot/lab/Text.java $DIR/tests/ScanTest.java
java -Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp $W ScanTest
R=$?
rm -rf $W
exit $R
