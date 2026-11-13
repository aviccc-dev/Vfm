#!/bin/bash
# שומרי המבנה — אחרי צעד שפותר פניות "כלפי מעלה": כותב מחדש את הרשימה (tests/mivne/lemala.txt), מוודא שרק ירדו
# שורות (פנייה חדשה — מחזיר את הרשימה ויוצא בכישלון), ומריץ שוב את כל הבדיקות.
# שימוש: mivne.sh <תיקיית המקור המלאה>
DIR=$1
L=$DIR/tests/mivne/lemala.txt
cp $L /tmp/lemala-before.txt
JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dvfm.mivne.write=1" bash $DIR/build/test.sh $DIR 2>&1 | grep "נכתבה הרשימה"
ADDED=$(diff <(grep " > " /tmp/lemala-before.txt | sort) <(grep " > " $L | sort) | grep "^>")
echo "נוספו (חייב להיות ריק): [$ADDED]"
echo "ירדו: $(diff <(grep " > " /tmp/lemala-before.txt | sort) <(grep " > " $L | sort) | grep "^<" | sed 's/^< //' | tr '\n' ';')"
[ -n "$ADDED" ] && { cp /tmp/lemala-before.txt $L; echo "!!! נוספה פנייה חדשה כלפי מעלה — הרשימה הוחזרה"; exit 1; }
bash $DIR/build/test.sh $DIR 2>&1 | grep -v "JAVA_TOOL_OPTIONS\|deprecat\|Note:" | tail -3
