"""python3 source/build/haavara.py <קובץ חדש> "<הערת ראש>" שם1,שם2 — מבנה 165: מעביר מ־Batch פונקציות / מחלקות בשמן לקובץ חדש בתיקיית הפקודות, ומחליף את הפניות. להריץ מתוך source.
מבנה 178: גם לתיקיות אחרות — DIR=chomer/konan SRC=Physical (ברירת המחדל: pkudot, Batch)."""
import os
SRC=os.environ.get('SRC','Batch')
import io,re,glob,sys
new,hdr,names=sys.argv[1],sys.argv[2],sys.argv[3].split(',')
P='src/com/tikiyot/vfm/'+os.environ.get('DIR','pkudot')+'/'
s=io.open(P+SRC+'.java',encoding='utf-8').read(); L=s.split('\n')
def start_of(i):
    j=i
    while j>0 and (L[j-1].strip().startswith('*') or L[j-1].strip().startswith('/**') or (L[j-1].startswith('    //') and not L[j-1].startswith('    // ----------'))):
        j-=1
    return j
spans=[];i=0
while i<len(L):
    l=L[i]
    m=re.match(r'    (?:private\s+|public\s+)?static\s+(?:final\s+)?(?:(?:class|interface)\s+(\w+)|[\w<>\[\], .]+?\s+(\w+)\s*[(=])',l)
    if m:
        n=m.group(1) or m.group(2); e=i
        if l.rstrip().endswith('{') or (m.group(2) and '(' in l and '=' not in l.split('(')[0]):   # שיטה, גם כשהחתימה בכמה שורות
            while L[e]!='    }': e+=1
        else:
            while not L[e].split('//')[0].rstrip().endswith(';'): e+=1
        if n in names: spans.append((start_of(i),e,n))
        i=e+1; continue
    i+=1
got=sorted(set(n for *_,n in spans)); assert got==sorted(set(names)),(set(names)-set(got))
take=set()
for b,e,n in spans: take.update(range(b,e+1))
body=[];out=[]
for i,l in enumerate(L):
    (body if i in take else out).append(l)
    if i in take and i+1 not in take: body.append('')
s=re.sub(r'\n{3,}','\n\n','\n'.join(out))
fns=[n for n in names if not n[0].isupper()]; caps=[n for n in names if n[0].isupper()]
for n in fns: s=re.sub(r'(?<![\w.$])'+n+r'\(',new+'.'+n+'(',s)
for n in caps: s=re.sub(r'(?<![\w.$"])'+n+r'\b',new+'.'+n,s)
io.open(P+SRC+'.java','w',encoding='utf-8').write(s)
bt='\n'.join(body).replace('    private static ','    static ')
for n in (['Step','Plan','Selection','Listener','AsIfRow'] if SRC=='Batch' else []):
    bt=re.sub(r'(?<![\w.])'+n+r'\b(?=[\s.(<>\[\],)])',SRC+'.'+n,bt)
out='package com.tikiyot.vfm;\n\n'+hdr+'\n\nimport java.io.File;\nimport java.util.ArrayList;\nimport java.util.Arrays;\nimport java.util.HashMap;\nimport java.util.LinkedHashMap;\nimport java.util.List;\nimport java.util.Locale;\nimport java.util.Map;\n\nfinal class '+new+' {\n\n    private '+new+'() { }\n\n'+bt.rstrip()+'\n}\n'
io.open(P+new+'.java','w',encoding='utf-8').write(out)
for f in glob.glob('src/**/*.java',recursive=True)+glob.glob('tests/**/*.java',recursive=True):
    if f.endswith('/'+SRC+'.java') or f.endswith('/'+new+'.java'): continue
    t=io.open(f,encoding='utf-8').read(); t2=t
    for n in fns: t2=re.sub(r'\b'+SRC+r'\.'+n+r'\(',new+'.'+n+'(',t2)
    for n in caps: t2=re.sub(r'\b'+SRC+r'\.'+n+r'\b',new+'.'+n,t2)
    if t2!=t: io.open(f,'w',encoding='utf-8').write(t2); print('updated',f.split('/')[-1])
print('moved',got)
