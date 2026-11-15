"""python3 source/build/haavara-tikun.py <הקובץ החדש> — מבנה 165: אחרי haavara.py — מקדים Batch. לשמות שהמהדר לא מצא, ופותח אותם ב־Batch. להריץ מתוך source."""
import io,re,sys,subprocess
new=sys.argv[1]; P='src/com/tikiyot/vfm/pkudot/'
for it in range(6):
    r=subprocess.run('javac -encoding UTF-8 -nowarn -Xmaxerrs 500 -d /tmp/jc -cp /home/claude/tools/android.jar $(find src -name "*.java")',shell=True,capture_output=True,text=True)
    errs=r.stdout+r.stderr
    if 'error:' not in errs: print('OK'); break
    miss=set()
    for m in re.finditer(r'/'+new+r'\.java:\d+: error: cannot find symbol\n.*\n.*\n\s*symbol:\s+(method|variable|class) (\w+)',errs): miss.add((m.group(1),m.group(2)))
    other=[l for l in errs.split('\n') if 'error:' in l and not ('/'+new+'.java' in l and 'cannot find symbol' in l)]
    if not miss: print('\n'.join(errs.split('\n')[:30])); break
    f=io.open(P+new+'.java',encoding='utf-8').read(); head,body=f.split('    private '+new+'() { }',1)
    b=io.open(P+'Batch.java',encoding='utf-8').read()
    for k,n in miss:
        if k=='method': body=re.sub(r'(?<![\w.])'+n+r'\(','Batch.'+n+'(',body); b=re.sub(r'\n    private static ([^\n]*\b'+n+r'\()',r'\n    static \1',b)
        else: body=re.sub(r'(?<![\w."])'+n+r'\b','Batch.'+n,body); b=re.sub(r'\n    private static final ([^\n]*\b'+n+r'\b)',r'\n    static final \1',b)
    io.open(P+new+'.java','w',encoding='utf-8').write(head+'    private '+new+'() { }'+body); io.open(P+'Batch.java','w',encoding='utf-8').write(b)
    print('fixed',sorted(miss))
