# גשר בדיקה: מריץ פקודות SQL מול SQLite אמיתי של המחשב, בשביל תחליף SQLiteDatabase בבדיקות (tests/stubs).
# פרוטוקול: שורה לכל בקשה, שדות מופרדים בטאב; סוג בתחילת כל ארגומנט: I=מספר, S=טקסט, N=ריק.
import sqlite3
import sys


def unesc(s):
    out, i = [], 0
    while i < len(s):
        c = s[i]
        if c == "\\" and i + 1 < len(s):
            n = s[i + 1]
            out.append({"t": "\t", "n": "\n", "r": "\r", "\\": "\\"}.get(n, n))
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def esc(s):
    return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")


def arg(f):
    t, v = f[0], unesc(f[1:])
    if t == "I":
        return int(v)
    if t == "N":
        return None
    return v


def enc(v):
    if v is None:
        return "N"
    if isinstance(v, int):
        return "I%d" % v
    return "S" + esc(str(v))


def main():
    sys.stdout.reconfigure(encoding="utf-8", newline="\n")
    out = sys.stdout
    conn = None
    for line in sys.stdin.buffer:
        parts = line.decode("utf-8").rstrip("\n").split("\t")
        op = parts[0]
        try:
            if op == "open":
                conn = sqlite3.connect(unesc(parts[1]), isolation_level=None)
                out.write("OK\n")
            elif op in ("exec", "query"):
                cur = conn.execute(unesc(parts[1]), [arg(f) for f in parts[2:]])
                if op == "exec":
                    out.write("OK\n")
                else:
                    rows = cur.fetchall()
                    ncols = len(cur.description) if cur.description else 0
                    out.write("ROWS\t%d\t%d\n" % (ncols, len(rows)))
                    for r in rows:
                        out.write("\t".join(enc(v) for v in r) + "\n")
            elif op == "begin":
                conn.execute("BEGIN")
                out.write("OK\n")
            elif op == "commit":
                conn.execute("COMMIT")
                out.write("OK\n")
            elif op == "rollback":
                conn.execute("ROLLBACK")
                out.write("OK\n")
            elif op == "close":
                if conn is not None:
                    conn.close()
                out.write("OK\n")
                out.flush()
                return
            else:
                out.write("ERR\tunknown op %s\n" % esc(op))
        except Exception as e:  # noqa: BLE001 — כל שגיאת SQL חוזרת לצד ה־Java
            out.write("ERR\t%s\n" % esc(str(e)))
        out.flush()


main()
