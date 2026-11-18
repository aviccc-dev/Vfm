#!/bin/bash
# בדיקות על המחשב (80). יוצא בכישלון אם בדיקה אחת נכשלה — ואז build.sh לא בונה.
set -e
DIR=$1
echo "הוראות תכנות וסדר — CLAUDE.md בשורש המאגר. מחייבות כל מי שמשנה את הקוד."
T=/home/claude/tools
W=$(mktemp -d)
mkdir -p $W/stubs $W/out $W/home
export LANG=C.UTF-8
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -nowarn -classpath $T/android.jar -d $W/stubs $(find $DIR/tests/stubs -name "*.java")
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -nowarn -classpath $T/android.jar -d $W/out \
    $(find $DIR/src $DIR/tests/com -name "*.java")
set +e
java -Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8 -Dvfm.home=$W/home \
    -Dvfm.src=$DIR -cp $W/stubs:$W/out:$T/android.jar com.tikiyot.vfm.Tests
R=$?
rm -rf $W
[ $R -ne 0 ] && exit $R
# מבנה 259: בדיקות העמודים של התוספים שבאים עם התוכנה — חלק מהבדיקות; עמוד שנכשל עוצר את הבנייה
bash "$DIR/build/amudim.sh" "$DIR"
