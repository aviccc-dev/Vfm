#!/bin/bash
# בדיקות על המחשב (80). יוצא בכישלון אם בדיקה אחת נכשלה — ואז build.sh לא בונה.
set -e
DIR=$1
T=/home/claude/tools
W=$(mktemp -d)
mkdir -p $W/stubs $W/out $W/home
export LANG=C.UTF-8
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -nowarn -d $W/stubs $(find $DIR/tests/stubs -name "*.java")
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -nowarn -classpath $T/android.jar -d $W/out \
    $(find $DIR/src $DIR/tests/com -name "*.java")
set +e
java -Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8 -Dvfm.home=$W/home \
    -Dvfm.src=$DIR -cp $W/stubs:$W/out:$T/android.jar com.tikiyot.vfm.Tests
R=$?
rm -rf $W
exit $R
