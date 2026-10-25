/* Run the individual-seat model in an actual JavaScript runtime. */
(function(root){
 "use strict";
 const M=root.CineSeatEditor,results=[];
 function check(condition){if(!condition)throw new Error("Assertion failed");}
 function test(name,action){action();results.push(name);}
 const seats=[{id:'9007199254740993',rowLabel:'A',seatNumber:1,seatType:'NORMAL',active:true},{id:'2',rowLabel:'A',seatNumber:2,seatType:'VIP',active:false}];
 test('Select exact Long ID without numeric rounding',()=>check(M.selectSeat(seats,'9007199254740993').id==='9007199254740993'));
 test('Selection preserves identity and physical position',()=>{const s=M.selectSeat(seats,'2');check(s.rowLabel==='A'&&s.seatNumber===2&&s.id==='2');});
 test('Selection never reactivates an inactive seat',()=>check(M.selectSeat(seats,'2').active===false));
 test('Selecting returns a copy without changing saved data',()=>{const s=M.selectSeat(seats,'2');s.active=true;check(seats[1].active===false);});
 test('NORMAL changes to VIP with the same ID',()=>{const s=M.changeSeat(seats[0],'VIP',true);check(s.id===seats[0].id&&s.seatType==='VIP'&&s.rowLabel==='A'&&s.seatNumber===1);});
 test('VIP changes back to NORMAL',()=>check(M.changeSeat(seats[1],'NORMAL',false).seatType==='NORMAL'));
 test('Deactivate and explicitly reactivate',()=>{const off=M.changeSeat(seats[0],'NORMAL',false);check(!off.active);check(M.changeSeat(off,'NORMAL',true).active);});
 test('Editing one seat leaves another unchanged',()=>{const original=JSON.stringify(seats);M.changeSeat(seats[0],'VIP',false);check(JSON.stringify(seats)===original);});
 test('Reject COUPLE, unknown and missing types',()=>{for(const type of ['COUPLE','UNKNOWN','',null]){let failed=false;try{M.changeSeat(seats[0],type,true);}catch(e){failed=true;}check(failed);}});
 test('Reject non boolean active state',()=>{let failed=false;try{M.changeSeat(seats[0],'NORMAL','false');}catch(e){failed=true;}check(failed);});
 test('Unknown seat cannot edit another ID',()=>{let failed=false;try{M.selectSeat(seats,'3');}catch(e){failed=true;}check(failed);});
 test('Legacy data read does not silently migrate',()=>{const old={id:'3',rowLabel:'Z9',seatNumber:212,seatType:'COUPLE',active:false};const copy=M.selectSeat([old],'3');check(copy.seatType==='COUPLE'&&!copy.active&&copy.seatNumber===212);});
 test('Explicit legacy conversion keeps ID and inactive state',()=>{const old={id:'3',rowLabel:'A',seatNumber:12,seatType:'COUPLE',active:false};const s=M.changeSeat(old,'NORMAL',false);check(s.id==='3'&&!s.active&&s.seatNumber===12&&old.seatType==='COUPLE');});
 root.CineSeatDesignerTestResults=results;
 test('Create five by eight preview with all NORMAL seats',()=>{const p=M.previewLayout([],5,8);check(p.length===40&&p[0].rowLabel==='A'&&p.at(-1).rowLabel==='E'&&p.at(-1).seatNumber===8&&p.every(s=>s.id===null&&s.seatType==='NORMAL'&&s.active));});
 test('Growing to six by eight and six by ten has exact bounds',()=>{check(M.previewLayout([],6,8).length===48);const p=M.previewLayout([],6,10);check(p.length===60&&p.at(-1).rowLabel==='F'&&p.at(-1).seatNumber===10&&!p.some(s=>s.seatNumber>10));});
 test('Preview preserves existing IDs type and inactive state',()=>{const p=M.previewLayout(seats,6,10);check(p[0].id===seats[0].id&&p[1].seatType==='VIP'&&!p[1].active);check(seats[1].active===false);});
 test('Shrink identifies only outside persisted seats without modifying source',()=>{const saved=M.previewLayout([],6,10).map((s,i)=>({...s,id:String(i+1)}));const original=JSON.stringify(saved);const removed=M.removedSeats(saved,5,8);check(removed.length===20&&removed.every(s=>s.rowLabel==='F'||s.seatNumber>8));check(JSON.stringify(saved)===original);});
 test('Missing positions are new only after explicit size preview',()=>{const p=M.previewLayout([seats[0]],1,3);check(p.length===3&&p[0].id===seats[0].id&&p[1].id===null);});
 test('Unsupported dimensions cannot produce a partial preview',()=>{for(const [rows,count] of [[0,8],[31,8],[5,0],[5,201],[6,200],[1.5,8]]){let failed=false;try{M.previewLayout(seats,rows,count);}catch(e){failed=true;}check(failed);}});
})(typeof window!=="undefined"?window:globalThis);
