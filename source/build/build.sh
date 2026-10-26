#!/bin/bash
set -e
DIR=$1; OUT=$2
T=/home/claude/tools
KEY=$DIR/signing/tv-key.jks
bash $DIR/build/test.sh $DIR || { echo "הבנייה נעצרה: בדיקה נכשלה"; exit 1; }
cd $DIR
rm -rf out gen bld; mkdir -p out gen bld
aapt package -f -m -M AndroidManifest.xml -S res -A assets -I $T/android.jar -J gen -F bld/res.apk
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -nowarn -bootclasspath $T/android.jar -classpath $T/android.jar -d out $(find src gen -name "*.java")
dalvik-exchange --dex --min-sdk-version=21 --output=bld/classes.dex out
cd bld
cp res.apk unsigned.apk
aapt add -f unsigned.apk classes.dex > /dev/null
zipalign -f -p 4 unsigned.apk aligned.apk
apksigner sign --ks $KEY --ks-pass pass:android --key-pass pass:android --ks-key-alias tv --out final.apk aligned.apk
apksigner verify final.apk && echo "SIGN OK"
aapt dump badging final.apk | grep -E "^package:|application-label:|sdkVersion"
cp final.apk $OUT
cd $DIR; rm -rf out gen bld
ls -la $OUT
