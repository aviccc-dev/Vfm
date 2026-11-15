import io,sys
src='source/assets/plugins/orech-interaktivi.html'
s=io.open(src,encoding='utf-8').read()
def rep(a,b,n=1):
    global s
    assert s.count(a)==n,(s.count(a),a[:70]); s=s.replace(a,b)
NAME='עורך — בדיקת תפריטים'
rep('<meta name="vfm-plugin" content="עורך אינטראקטיבי">','<meta name="vfm-plugin" content="%s">'%NAME)
rep('<meta name="vfm-plugin-version" content="16">','<meta name="vfm-plugin-version" content="4">')
rep('<meta name="vfm-lab" content="עורך אינטראקטיבי">','<meta name="vfm-lab" content="%s">'%NAME)
rep('<title>עורך אינטראקטיבי</title>','<title>%s</title>'%NAME)
# ---- CSS
rep('.bar2{display:flex;','''/* בדיקת תפריטים 1: שורת תפריטים כמו בחלונות, ומתחתיה סרגל קיצורים שניתן לשינוי */
.fn{flex:1;min-width:0;align-self:center;text-align:end;padding:0 8px;font-size:.8rem;color:var(--muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.mbar{display:flex;gap:2px;padding:2px 6px 0;position:relative}
.mbar>button{font:inherit;font-size:.95rem;border:0;background:transparent;color:var(--fg);padding:8px 9px;border-radius:6px;cursor:pointer}
.mbar>button.on{background:var(--chip);color:var(--chip-fg)}
.menu{position:absolute;top:100%;inset-inline-start:6px;z-index:8;min-width:230px;max-width:92vw;max-height:70vh;overflow:auto;background:var(--bg);border:1px solid var(--rule);border-radius:10px;box-shadow:0 6px 24px rgba(0,0,0,.2);padding:4px 0}
.menu button{display:flex;gap:10px;align-items:center;width:100%;font:inherit;border:0;background:transparent;color:var(--fg);padding:11px 14px;text-align:start;cursor:pointer}
.menu button:disabled{color:var(--ghost);cursor:default}
.menu button .mk{width:1em;color:var(--accent);font-weight:700}
.menu hr{border:0;border-top:1px solid var(--rule);margin:4px 0}
.tbar{display:flex;gap:6px;padding:4px 10px 8px;overflow-x:auto}
.tbar .btn{flex:none}
.tbar .btn.real{background:var(--err);border-color:var(--err);color:var(--on)}
.bar2{display:flex;''')
# ---- HTML
a=s.index('<div class="top">'); b=s.index('<div id="ed"></div>')
s=s[:a]+'''<div class="top">
  <div class="mbar" id="mbar" role="menubar">
    <button type="button" data-m="file">קובץ</button><button type="button" data-m="edit">עריכה</button><button type="button" data-m="view">תצוגה</button><button type="button" data-m="run">הרצה</button><button type="button" data-m="help">עזרה</button><span class="fn" id="fname"></span>
    <div class="menu" id="menu" role="menu" hidden></div>
  </div>
  <div class="tbar" id="tbar"></div>
</div>
'''+s[b:]
# ---- JS
rep("const fname=$('fname'),saveBtn=$('save'),runBtn=$('run');","const fname=$('fname'),mbar=$('mbar'),menu=$('menu'),tbar=$('tbar');")
a=s.index('function upd(){'); b=s.index('function parseAll(content){')
s=s[:a]+'''function upd(){
  fname.innerHTML='';
  if(file){fname.append(document.createTextNode(file.name.replace(/\\.vbat$/,'')));if(dirty())fname.append(document.createTextNode(' ●'));}
  else fname.textContent='';
  track();drawTbar();
}
'''+s[b:]
rep("saveBtn.addEventListener('click',()=>save());\nrunBtn.addEventListener('click',run);\nfname.addEventListener('click',()=>{commit();openSheet({kind:'files'});});\n",'')
a=s.index("$('segRun').addEventListener"); b=s.index('function setLang(l){')
s=s[:a]+s[b:]
rep("function load(f,content){\n  file=f;","function load(f,content){\n  hist=[];hi=-1;\n  file=f;")
rep("exists:drawExists,oldsave:drawOldSave,choose:drawChoose,name:drawName,vars:drawVars,cmdfile:drawCmdFile})[sh.kind]();","exists:drawExists,oldsave:drawOldSave,choose:drawChoose,name:drawName,vars:drawVars,cmdfile:drawCmdFile,find:drawFind,tbedit:drawTbEdit})[sh.kind]();")
rep('לעריכה — "רגילה" בראש העמוד.','לעריכה — תצוגה ← רגילה.')
rep("top('קובצי הפקודות');\n  const fs=myFiles()","top('פתיחה');\n  const fs=myFiles()")
rep("  box.appendChild(B('it',[E('span',null,'+ קובץ חדש'),E('span','k','')],()=>{sh={kind:'newfile'};drawSheet();}));\n",'')
a=s.index("  if(file)box.appendChild(B('it',[E('span',null,'שמירה בשם…')"); b=s.index("  sheetIn.appendChild(box);\n  if(fs.length>10){")
s=s[:a]+s[b:]
# גרסה 3 (המשתמש: "צריך שיביא את כל התיקיות המכילות קבצי פקודות. אין עניין שיביא את כל התיקיות הקיימות"):
# בחלון "פתיחה" — התיקיות שיש בהן קובצי פקודות (מהאינדקס, info.features: cmdfolders); בתוכנה שעוד לא מכירה — הבורר הרגיל.
rep("  sheetIn.appendChild(E('div','grp','הפקודות שלי'));\n  const list=E('div','items');\n  fs.forEach(","""  const dirs=can('commandFolders')?J(V.commandFolders(),null):null;
  if(dirs&&!sh.dir){
    box.textContent='';
    sheetIn.appendChild(E('div','grp','תיקיות שיש בהן קובצי פקודות'));
    const dl=E('div','items');
    dirs.forEach(d=>dl.appendChild(B('it',[(()=>{const w=E('span',null,'📁 '+d.name),up=d.path.split('/').filter(Boolean).slice(-3,-1).join(' / ');if(up){w.append(E('br'));const sm=E('small',null,up);sm.style.color='var(--muted)';w.append(sm);}return w;})(),E('span','k',String(d.count))],()=>{sh={kind:'files',dir:d};drawSheet();},{title:d.path})));
    if(!dirs.length)dl.appendChild(E('div','it','אין'));
    sheetIn.appendChild(dl);
    box.appendChild(B('it',[E('span',null,'תיקייה אחרת…'),E('span','k','')],()=>pickTyped('קובץ פקודות',VBAT,'קובצי פקודות',p=>openFile({name:p.split('/').pop(),path:p}))));
  }
  if(sh.dir){
    sheetIn.textContent='';top('📁 '+sh.dir.name);
    sheetIn.appendChild(E('div','grp',sh.dir.path));
    const fl=E('div','items'),base=sh.dir.path.replace(/\\/$/,'');
    fl.appendChild(B('it',[E('span',null,'‹ כל התיקיות'),E('span','k','')],()=>{sh={kind:'files'};drawSheet();}));
    (J(V.listDir(sh.dir.path),[])||[]).filter(f=>!f.dir&&VBAT.test(f.name)).sort((a,b)=>b.time-a.time)
      .forEach(f=>fl.appendChild(B('it',[E('span',null,f.name.replace(/\\.vbat$/,'')),E('span','k',file&&file.path===base+'/'+f.name?'✓':'')],()=>openFile({name:f.name,path:base+'/'+f.name}))));
    sheetIn.appendChild(fl);return;
  }
  sheetIn.appendChild(E('div','grp','הפקודות שלי'));\n  const list=E('div','items');\n  fs.forEach(""")
rep("B('primary','קובצי הפקודות',()=>openSheet({kind:'files'}))","B('primary','פתיחה…',()=>openSheet({kind:'files'}))")
rep("'בחר קובץ פקודות, או צור חדש'","'אין קובץ פתוח. קובץ ← פתיחה, או קובץ ← חדש'")
NEW=r'''
// ====================== בדיקת תפריטים 1: התפריטים, הקיצורים, ביטול, ופעולות על שורות ======================
// המשתמש (14.11.2026): "תפריט למעלה — קובץ, עריכה, ועוד לפי הצורך. מתחת לזה — קיצורים ניתנים לשינוי" · "לפי הסדר המקובל בחלונות".
// ---------- ביטול וחזרה: כל מצב של השורות נרשם כשהוא משתנה (upd) ----------
let hist=[],hi=-1,histLock=false,clip=null;
function track(){
  if(histLock||!file||view==='plain')return;
  const t=JSON.stringify(lines);
  if(hi>=0&&hist[hi]===t)return;
  hist=hist.slice(0,hi+1);hist.push(t);if(hist.length>200)hist.shift();hi=hist.length-1;
}
function restore(t){histLock=true;act=-1;M=null;lines=JSON.parse(t);models=lines.map(Mo.fromLine);histLock=false;draw();}
function undo(){commit();if(hi>0){hi--;restore(hist[hi]);}}
function redo(){commit();if(hi<hist.length-1){hi++;restore(hist[hi]);}}
// ---------- השורה שעובדים בה, והלולאה שלה כיחידה ("חזור" עד "סוף") ----------
function blockOf(i){const p=pairOf(i);return p>i?[i,p]:[i,i];}
function lineOps(){return view==='inter'&&!!file&&act>=0;}
function setLines(at){act=-1;M=null;models=lines.map(Mo.fromLine);draw();if(at!=null&&at>=0&&at<lines.length&&models[at]&&models[at].cmd!=='סוף')activate(at);}
function copyLines(){commit();const b=blockOf(act);clip=lines.slice(b[0],b[1]+1);toast(clip.length>1?'הועתקו '+clip.length+' שורות (הלולאה כולה)':'השורה הועתקה');}
function cutLines(){commit();const b=blockOf(act);clip=lines.splice(b[0],b[1]-b[0]+1);if(!lines.length)lines=[''];setLines(null);toast(clip.length>1?'נגזרו '+clip.length+' שורות':'השורה נגזרה');}
function pasteLines(){if(!clip)return;commit();const at=act>=0?blockOf(act)[1]+1:lines.length;lines.splice.apply(lines,[at,0].concat(clip));setLines(at);}
function dupLines(){commit();const b=blockOf(act),c=lines.slice(b[0],b[1]+1);lines.splice.apply(lines,[b[1]+1,0].concat(c));setLines(b[1]+1);}
function moveLines(d){
  commit();const b=blockOf(act);
  if(d<0){if(b[0]===0)return;const x=lines.splice(b[0]-1,1)[0];lines.splice(b[1],0,x);setLines(b[0]-1);}
  else{if(b[1]>=lines.length-1)return;const x=lines.splice(b[1]+1,1)[0];lines.splice(b[0],0,x);setLines(b[0]+1);}
}
function delLine(){commit();const i=act;removeLine(i);}
// ---------- הפעולות — הגדרה אחת; התפריטים והקיצורים לוקחים ממנה ----------
const A={
  new:{t:'חדש',f:()=>openSheet({kind:'newfile'})},
  open:{t:'פתיחה…',s:'פתיחה',f:()=>openSheet({kind:'files'})},
  save:{t:'שמירה',f:()=>save(),on:()=>!!dirty()},
  saveas:{t:'שמירה בשם…',s:'שמירה בשם',f:()=>openSheet({kind:'newfile',saveAs:true}),on:()=>!!file},
  savetpl:{t:'שמירה כפקודה נפוצה…',s:'☆ נפוצה',f:()=>openSheet({kind:'name',title:'שם הפקודה הנפוצה',ph:'למשל גיבוי שבועי',ok:n=>{commit();toast(V.saveTemplate(n,text())?'נשמרה בפקודות הנפוצות שלי':'לא נשמרה');}}),on:()=>!!file&&lines.length>0&&can('saveTemplate')},
  close:{t:'סגירה',f:()=>{if(dirty())openSheet({kind:'leave'});else V.close();}},
  undo:{t:'ביטול',s:'↶ ביטול',f:undo,on:()=>view==='inter'&&hi>0},
  redo:{t:'חזרה',s:'↷ חזרה',f:redo,on:()=>view==='inter'&&hi<hist.length-1},
  cut:{t:'גזירה',f:cutLines,on:lineOps},
  copy:{t:'העתקה',f:copyLines,on:lineOps},
  paste:{t:'הדבקה',f:pasteLines,on:()=>view==='inter'&&!!file&&!!clip},
  del:{t:'מחיקה',f:delLine,on:lineOps},
  line:{t:'שורה חדשה',s:'+ שורה',f:()=>newLineAfter(),on:()=>view==='inter'&&!!file},
  dup:{t:'שכפול',f:dupLines,on:lineOps},
  up:{t:'הזזה למעלה',s:'▲',f:()=>moveLines(-1),on:lineOps},
  down:{t:'הזזה למטה',s:'▼',f:()=>moveLines(1),on:lineOps},
  find:{t:'חיפוש והחלפה…',s:'חיפוש',f:()=>{commit();openSheet({kind:'find'});},on:()=>!!file},
  vinter:{t:'אינטראקטיבית',f:()=>{if(view!=='inter')setView('inter');},mk:()=>view==='inter'?'•':''},
  vplain:{t:'רגילה',f:()=>{if(view!=='plain')setView('plain');},mk:()=>view==='plain'?'•':''},
  lhe:{t:'עברית',f:()=>{if(lang!=='he')setLang('he');},mk:()=>lang==='he'?'•':''},
  len:{t:'English',f:()=>{if(lang!=='en')setLang('en');},mk:()=>lang==='en'?'•':''},
  tb:{t:'סרגל קיצורים',f:()=>{setPref('tbshow',tbShown()?'0':'1');drawTbar();},mk:()=>tbShown()?'✓':''},
  tbedit:{t:'התאמת הקיצורים…',f:()=>openSheet({kind:'tbedit'})},
  asif:{t:'הרצה בכאילו',s:'▶ בכאילו',f:()=>{real=false;run();},on:()=>!!file},
  real:{t:'הרצה אמיתית…',s:'▶ אמיתית',f:()=>{real=true;run();},on:()=>!!file,cls:'real'},
  cmds:{t:'רשימת הפקודות',f:()=>{if(view==='inter'&&file&&act<0)newLineAfter();openSheet({kind:'cmd'});},on:()=>view==='inter'&&!!file},
  about:{t:'על העורך',f:()=>toast('עורך — בדיקת תפריטים 4 · על בסיס העורך האינטראקטיבי 16')}
};
const MENUS={file:['new','open','-','save','saveas','savetpl','-','close'],
  edit:['undo','redo','-','cut','copy','paste','del','-','line','dup','up','down','-','find'],
  view:['vinter','vplain','-','lhe','len','-','tb','tbedit'],run:['asif','real'],help:['cmds','about']};
const TB_DEFAULT=['save','undo','redo','line','asif'];
function tbShown(){return pref('tbshow','1')!=='0';}
function tbList(){const l=J(pref('tbar',null),null);return Array.isArray(l)?l.filter(k=>A[k]):TB_DEFAULT.slice();}
function able(k){return !A[k].on||!!A[k].on();}
function doAct(k){closeMenu();if(!able(k))return;A[k].f();upd();}
function drawTbar(){
  tbar.hidden=!tbShown();if(tbar.hidden)return;
  tbar.textContent='';
  tbList().forEach(k=>{const b=B('btn'+(A[k].cls?' '+A[k].cls:''),A[k].s||A[k].t.replace('…',''),()=>doAct(k));b.disabled=!able(k);b.title=A[k].t;tbar.appendChild(b);});
}
let openM=null;
function closeMenu(){openM=null;menu.hidden=true;[...mbar.querySelectorAll('button[data-m]')].forEach(b=>b.classList.remove('on'));}
function openMenu(m,btn){
  commit();openM=m;menu.textContent='';
  MENUS[m].forEach(k=>{
    if(k==='-'){menu.appendChild(E('hr'));return;}
    const b=E('button');b.type='button';b.setAttribute('role','menuitem');b.append(E('span','mk',A[k].mk?A[k].mk():''),E('span',null,A[k].t));
    b.disabled=!able(k);b.addEventListener('click',()=>doAct(k));menu.appendChild(b);
  });
  [...mbar.querySelectorAll('button[data-m]')].forEach(b=>b.classList.toggle('on',b===btn));
  menu.style.insetInlineStart=Math.max(6,Math.min(btn.offsetParent?mbar.clientWidth-(btn.offsetLeft+btn.offsetWidth):6,mbar.clientWidth-240))+'px';
  menu.hidden=false;
}
mbar.addEventListener('click',e=>{const b=e.target.closest('button[data-m]');if(!b)return;if(openM===b.dataset.m)closeMenu();else openMenu(b.dataset.m,b);});
document.addEventListener('pointerdown',e=>{if(openM&&!e.target.closest('#mbar'))closeMenu();},true);
// ---------- חיפוש והחלפה: בכל השורות של הקובץ ----------
function drawFind(){
  top('חיפוש והחלפה');
  const a=E('input','fi wide'),b=E('input','fi wide'),out=E('div','grp','');
  a.type='text';a.placeholder='מה לחפש';a.dir='auto';b.type='text';b.placeholder='במה להחליף';b.dir='auto';
  const src=()=>view==='plain'?plain.value.replace(/\r/g,'').split('\n'):lines;
  const count=()=>{const q=a.value;let n=0,rows=0;if(q)src().forEach(l=>{const c=l.split(q).length-1;if(c){n+=c;rows++;}});out.textContent=q?(n?'נמצא '+n+' פעמים, ב־'+rows+' שורות':'לא נמצא'):'';return n;};
  a.addEventListener('input',count);
  sheetIn.append(a,b,out);
  sheetIn.appendChild(B('primary','להחליף הכול',()=>{
    const q=a.value,n=count();if(!q||!n){toast(q?'לא נמצא':'צריך מה לחפש');return;}
    if(view==='plain'){plain.value=plain.value.split(q).join(b.value);hideSheet();upd();}
    else{lines=lines.map(l=>l.split(q).join(b.value));hideSheet();setLines(null);}
    toast('הוחלפו '+n+' — אפשר לבטל (עריכה ← ביטול)');
  }));
  setTimeout(()=>a.focus(),50);
}
// ---------- התאמת הקיצורים: כל פריט מהתפריטים יכול להיות קיצור ----------
function drawTbEdit(){
  top('התאמת הקיצורים');
  let cur=tbList();
  const names={file:'קובץ',edit:'עריכה',view:'תצוגה',run:'הרצה',help:'עזרה'};
  const saveL=()=>{setPref('tbar',JSON.stringify(cur));drawTbar();sheetIn.textContent='';drawTbEdit();};
  sheetIn.appendChild(E('div','grp','בסרגל עכשיו, לפי הסדר (נגיעה — מוציאה; ▲ — מקדימה):'));
  const now=E('div','items');
  cur.forEach((k,i)=>{const row=E('div','it');row.append(B('x',A[k].t+'  ✕',()=>{cur.splice(i,1);saveL();}),B('x','▲',()=>{if(i>0){cur.splice(i-1,0,cur.splice(i,1)[0]);saveL();}}));now.appendChild(row);});
  if(!cur.length)now.appendChild(E('div','it','ריק'));
  sheetIn.appendChild(now);
  Object.keys(MENUS).forEach(m=>{
    const rest=MENUS[m].filter(k=>k!=='-'&&k!=='tb'&&k!=='tbedit'&&cur.indexOf(k)<0);if(!rest.length)return;
    sheetIn.appendChild(E('div','grp','להוסיף מתוך "'+names[m]+'":'));
    const box=E('div','items');rest.forEach(k=>box.appendChild(B('it',[E('span',null,A[k].t),E('span','k','+')],()=>{cur.push(k);saveL();})));sheetIn.appendChild(box);
  });
  sheetIn.appendChild(B('secondary','חזרה לברירת המחדל',()=>{cur=TB_DEFAULT.slice();saveL();}));
}
'''
rep("// ---------- שמירה ----------\nfunction save(then){", NEW+"\n// ---------- שמירה ----------\nfunction save(then){")
out='lab-pages/תוסף עורך בדיקת תפריטים 4.html'
io.open(out,'w',encoding='utf-8').write(s); print(out,len(s.encode('utf-8')))
