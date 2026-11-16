const {JSDOM}=require('jsdom');const fs=require('fs');const errs=[];
const vc=new (require('jsdom').VirtualConsole)();vc.on('jsdomError',e=>errs.push(String(e.detail||e.message).slice(0,200)));
const dom=new JSDOM(fs.readFileSync(process.argv[2],'utf8'),{runScripts:'dangerously',pretendToBeVisual:true,virtualConsole:vc,beforeParse(window){const known={
 commandFolders:()=>JSON.stringify([{path:'/s/גיבויים',name:'גיבויים',count:2},{path:'/s/עבודה/פקודות',name:'פקודות',count:1}]),
 listDir:p=>JSON.stringify([{name:'שבועי.vbat',dir:false,size:5,time:2},{name:'יומי.vbat',dir:false,size:5,time:9},{name:'תמונה.jpg',dir:false,size:5,time:3},{name:'תת',dir:true,size:0,time:1}]),
 readText:p=>'הודעה '+p+'\n',myCommands:()=>'[]',commandFiles:()=>'[]',load:()=>null,save:()=>{},info:()=>'{}'};
 window.vfm=new Proxy(known,{get:(t,k)=>k in t?t[k]:()=>null});}});
const w=dom.window,d=w.document,click=el=>el.dispatchEvent(new w.MouseEvent('click',{bubbles:true}));
const items=()=>[...d.querySelectorAll('#sheetIn .sh-title, #sheetIn .grp, #sheetIn .it')].map(x=>x.textContent.trim());
const open=()=>{click(d.querySelector('#mbar button[data-m="file"]'));click([...d.querySelectorAll('#menu button')].find(b=>b.textContent.includes('פתיחה')));};
open();console.log('עם היכולת:',items().join(' | '));
click([...d.querySelectorAll('#sheetIn .it')].find(b=>b.textContent.includes('גיבויים')));console.log('בתוך התיקייה:',items().join(' | '));
click([...d.querySelectorAll('#sheetIn .it')].find(b=>b.textContent.includes('יומי')));
console.log('נפתח:',d.getElementById('fname').textContent,'· שורות:',d.querySelectorAll('#ed .row').length,'· החלון נסגר:',d.getElementById('sheet').hidden);
if(errs.length)console.log('שגיאות:',errs.join('\n'));
