/* Run against the pure production model in a JavaScript runtime. No DOM mocks. */
(function(root) {
 "use strict";
 const M=root.CineSeatDesigner,results=[];
 function test(name,fn){fn();results.push(name);}
 function check(ok,message){if(!ok)throw new Error(message||"Assertion failed");}
 const old=[
  {id:11,rowLabel:"A",seatNumber:1,seatType:"VIP",active:true},
  {id:12,rowLabel:"A",seatNumber:2,seatType:"NORMAL",active:false},
  {id:13,rowLabel:"A",seatNumber:3,seatType:"COUPLE",active:true}
 ];
 test("A–E generation",()=>{const r=M.generate([],"A",5,12);check(r.map(x=>x.label).join(",")==="A,B,C,D,E");check(M.flatten(r).length===60);});
 test("Five ten-seat rows initialize only NORMAL",()=>{const r=M.generate([],"A",5,10);check(r.every(row=>row.seats.length===10));check(M.flatten(r).length===50);check(M.flatten(r).every(s=>s.seatType==="NORMAL"));});
 test("Appending rows preserves existing VIP and COUPLE while new seats are NORMAL",()=>{const r=M.generate(M.fromSeats(old),"A",5,10);check(r[0].seats[0].seatType==="VIP");check(r[0].seats[2].seatType==="COUPLE");check(r.slice(1).every(row=>row.seats.every(s=>s.seatType==="NORMAL")));});
 test("Admin can explicitly assign VIP or COUPLE after generation",()=>{let r=M.generate([],"A",5,10);r=M.applyRow(r,"C",{count:10,first:1,gaps:"",type:"VIP",active:null,override:false});r=M.applyRow(r,"E",{count:10,first:1,gaps:"",type:"COUPLE",active:null,override:false});check(r[2].seats.every(s=>s.seatType==="VIP"));check(r[4].seats.every(s=>s.seatType==="COUPLE"));check(r[1].seats.every(s=>s.seatType==="NORMAL"));});
 test("Z continues AA",()=>check(M.labels("Z",3).join(",")==="Z,AA,AB"));
 test("Generating over existing A resizes it and keeps custom IDs/types",()=>{const r=M.generate(M.fromSeats(old),"A",2,12);check(r[0].seats[0].id===11&&r[0].seats[0].seatType==="VIP");check(r[0].seats.length===12);check(r[0].seats[1].active===false);});
 test("Existing demo A/B/C all resize to ten while D/E are added",()=>{const demo=["A","B","C"].flatMap((row,i)=>Array.from({length:8},(_,n)=>({id:100+i*8+n,rowLabel:row,seatNumber:n+1,seatType:i===1?"VIP":i===2?"COUPLE":"NORMAL",active:true})));const r=M.generate(M.fromSeats(demo),"A",5,10);check(r.length===5&&r.every(row=>row.seats.length===10));check(M.flatten(r).filter(s=>s.id).length===24);check(r[1].seats[0].seatType==="VIP"&&r[2].seats[0].seatType==="COUPLE");check(M.flatten(r).filter(s=>!s.id).every(s=>s.seatType==="NORMAL"));});
 test("Generation reduces old rows safely instead of ignoring them",()=>{const oldRow=Array.from({length:12},(_,n)=>({id:n+1,rowLabel:"A",seatNumber:n+1,seatType:"NORMAL",active:true}));const r=M.generate(M.fromSeats(oldRow),"A",1,10);check(r[0].seats.length===12);check(r[0].seats.filter(s=>s.active).length===10);check(r[0].seats[11].id===12&&!r[0].seats[11].active);});
 test("Resize existing A preserves identity and mixed types",()=>{const r=M.applyRow(M.fromSeats(old),"A",{count:5,first:1,gaps:"",type:"NORMAL",active:true,override:false});check(r[0].seats[0].id===11&&r[0].seats[0].seatType==="VIP");check(r[0].seats[1].active===false);check(r[0].seats.length===5);});
 test("Explicit override replaces existing custom types",()=>{const r=M.applyRow(M.fromSeats(old),"A",{count:3,first:1,gaps:"",type:"VIP",active:true,override:true});check(r[0].seats.every(s=>s.seatType==="VIP"&&s.active));});
 test("Reducing old row keeps tail IDs gray",()=>{const r=M.applyRow(M.fromSeats(old),"A",{count:1,first:1,gaps:"",type:"",active:null,override:false});check(r[0].seats.length===3);check(r[0].seats[2].id===13&&!r[0].seats[2].active);});
 test("Delete sole unsaved row leaves an empty draft",()=>check(M.removeRow(M.generate([],"A",1,12),"A").length===0));
 test("Delete persisted row deactivates safely",()=>{const r=M.removeRow(M.fromSeats(old),"A");check(r[0].seats.length===3&&r[0].seats.every(s=>s.id&&!s.active));});
 test("Deleting mixed row removes only new records",()=>{let r=M.applyRow(M.fromSeats(old),"A",{count:5,first:1,gaps:"",type:"",active:null,override:false});r=M.removeRow(r,"A");check(r[0].seats.length===3);});
 test("Aisle creates no fake seats",()=>{let r=M.generate([],"A",1,5);r=M.applyRow(r,"A",{count:5,first:1,gaps:"3",type:"NORMAL",active:true,override:false});check(r[0].seats.map(s=>s.seatNumber).join(",")==="1,2,4,5,6");});
 test("Individual changes survive later resize",()=>{let r=M.generate([],"A",1,5);r=M.updateSelected(r,new Set(["A:2"]),"VIP",false);r=M.applyRow(r,"A",{count:7,first:1,gaps:"",type:"",active:null,override:false});check(r[0].seats[1].seatType==="VIP"&&!r[0].seats[1].active);});
 test("Different row lengths share the same center axis",()=>{let rows=M.generate([],"A",3,12);rows=M.applyRow(rows,"C",{count:5,first:1,gaps:"",type:"",active:null,override:false});for(const r of rows){const o=M.geometry(r).offsets;check(Math.abs(o[0].offset+o[o.length-1].offset)<.001);}});
 test("Centering independent of starting seat number",()=>{let rows=M.generate([],"A",1,1);rows=M.applyRow(rows,"A",{count:8,first:10,gaps:"",type:"",active:null,override:false});const o=M.geometry(rows[0]).offsets;check(o[0].offset===-224&&o[7].offset===224);});
 test("Interior aisle preserves physical gap and center",()=>{const row={label:"A",seats:[1,2,4,5].map(n=>({rowLabel:"A",seatNumber:n}))};const o=M.geometry(row).offsets;check(o.map(x=>x.offset).join(",")==="-128,-64,64,128");});
 test("Serialization excludes draft-only flags",()=>{const r=M.removeRow(M.fromSeats(old),"A");check(M.flatten(r).every(s=>!Object.hasOwn(s,"retired")));});
 test("Invalid ranges fail without changing source",()=>{const r=M.fromSeats(old);let failed=false;try{M.applyRow(r,"A",{count:4,first:199,gaps:"",type:"",active:null,override:false});}catch(e){failed=true;}check(failed&&r[0].seats[0].active);});
 root.CineSeatDesignerTestResults=results;
})(typeof window!=="undefined"?window:globalThis);
