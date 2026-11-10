#!/bin/bash
# שימוש: build.sh <תיקיית המקור המלאה> <קובץ יעד> [m]
# מ0.29: בנייה אחת — מנוע המסד (com.tikiyot.vfm.m); המנוע הישן נמחק, ואין עוד "בנייה ישנה". הפרמטר השלישי חייב להיות "m".
# עותק זמני שבו הסקריפט מתאים את המניפסט (חבילה, רשות, גרסה), את שם התוכנה ואת הצלמית.
# הגרסה של בניית מ — build/m-version.txt: "<versionCode> <versionName>".
set -e
DIR=$1; OUT=$2; FLAVOR=${3:-m}
[ "$FLAVOR" = "m" ] || { echo "הבנייה נעצרה: יש רק בניית מ"; exit 1; }
T=/home/claude/tools
KEY=$DIR/signing/tv-key.jks
bash $DIR/build/test.sh $DIR || { echo "הבנייה נעצרה: בדיקה נכשלה"; exit 1; }

# מקור הבנייה: הישנה — התיקייה עצמה; מ — עותק זמני
if [ "$FLAVOR" = "m" ]; then
  S=$(mktemp -d)
  cp -r $DIR/src $DIR/res $DIR/assets $DIR/AndroidManifest.xml $S/
  VC=$(cut -d' ' -f1 $DIR/build/m-version.txt); VN=$(cut -d' ' -f2- $DIR/build/m-version.txt | tr -d '\n')
  sed -i -e 's|package="com.tikiyot.vfm"|package="com.tikiyot.vfm.m"|' \
         -e 's|android:name="\.|android:name="com.tikiyot.vfm.|g' \
         -e 's|com\.tikiyot\.vfm\.files|com.tikiyot.vfm.m.files|' \
         -e 's|com\.tikiyot\.vfm\.share|com.tikiyot.vfm.m.share|' \
         -e "s|android:versionCode=\"[0-9]*\"|android:versionCode=\"$VC\"|" \
         -e "s|android:versionName=\"[^\"]*\"|android:versionName=\"$VN\"|" $S/AndroidManifest.xml
  sed -i 's|>מנהל קבצים וירטואלי<|>מנהל קבצים · מ<|' $S/res/values/strings.xml
  cp -r $DIR/res-m/. $S/res/
else
  S=$DIR
fi

cd $S
rm -rf out gen bld; mkdir -p out gen bld
aapt package -f -m --custom-package com.tikiyot.vfm -M AndroidManifest.xml -S res -A assets -I $T/android.jar -J gen -F bld/res.apk
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -nowarn -bootclasspath $T/android.jar -classpath $T/android.jar -d out $(find src gen -name "*.java")
dalvik-exchange --dex --min-sdk-version=21 --output=bld/classes.dex out
cd bld
cp res.apk unsigned.apk
aapt add -f unsigned.apk classes.dex > /dev/null
zipalign -f -p 4 unsigned.apk aligned.apk
apksigner sign --ks $KEY --ks-pass pass:android --key-pass pass:android --ks-key-alias tv --out final.apk aligned.apk
apksigner verify final.apk && echo "SIGN OK"
aapt dump badging final.apk | grep -E "^package:|application-label:|sdkVersion"
if [ "$FLAVOR" = "m" ]; then
  aapt dump badging final.apk | grep -q "package: name='com.tikiyot.vfm.m'" || { echo "הבנייה נעצרה: החבילה אינה com.tikiyot.vfm.m"; exit 1; }
  aapt dump xmltree final.apk AndroidManifest.xml | grep -q 'com.tikiyot.vfm.m.files' || { echo "הבנייה נעצרה: הרשות לא הוחלפה"; exit 1; }
fi
cp final.apk $OUT
cd $S; rm -rf out gen bld
[ "$S" != "$DIR" ] && rm -rf $S
ls -la $OUT
