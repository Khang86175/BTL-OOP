/* Pure draft operations: no HTTP, DOM or persistence. */
(function (root) {
 "use strict";
 const TYPES = ["NORMAL", "VIP", "COUPLE"];
 const clone = rows => rows.map(r => ({label:r.label, seats:r.seats.map(s => ({...s}))}));
 const key = s => s.rowLabel + ":" + s.seatNumber;
 const normalize = label => String(label || "").trim().toUpperCase();
 const integer = (value, min, max, message) => {
  const n = Number(value);
  if (!Number.isInteger(n) || n < min || n > max) throw new Error(message);
  return n;
 };
 function ordinal(label) {
  return /^[A-Z]+$/.test(label) ? [...label].reduce((n,c) => n*26+c.charCodeAt(0)-64,0) : Number.MAX_SAFE_INTEGER;
 }
 function sortRows(rows) { return rows.sort((a,b) => ordinal(a.label)-ordinal(b.label) || a.label.localeCompare(b.label)); }
 function fromSeats(seats) {
  const rows = [];
  seats.forEach(s => {
   const label=normalize(s.rowLabel);
   let r=rows.find(r => r.label===label);
   if (!r) { r={label,seats:[]};rows.push(r); }
   r.seats.push({...s,rowLabel:label});
  });
  rows.forEach(r => r.seats.sort((a,b) => a.seatNumber-b.seatNumber));
  return sortRows(rows);
 }
 function labels(start,count) {
  start=normalize(start);
  if (!/^[A-Z]{1,3}$/.test(start)) throw new Error("Hàng bắt đầu cần là chữ A–Z hoặc AA–ZZZ.");
  count=integer(count,1,30,"Tổng số hàng phải từ 1 đến 30.");
  let n=ordinal(start);
  return Array.from({length:count},() => {
   let value=n++, label="";
   while(value>0){value--;label=String.fromCharCode(65+value%26)+label;value=Math.floor(value/26);}
   return label;
  });
 }
 function generate(rows,start,total,seatCount) {
  let result=clone(rows);const names=labels(start,total);
  seatCount=integer(seatCount,1,200,"Số ghế mặc định phải từ 1 đến 200.");
  names.forEach(label => {
   if (result.some(r => r.label===label)) {
    result=applyRow(result,label,{count:seatCount,first:1,gaps:"",type:"",active:null,override:false});
    return;
   }
   result.push({label,seats:Array.from({length:seatCount},(_,n) => ({id:null,rowLabel:label,seatNumber:n+1,seatType:"NORMAL",active:true}))});
  });
  if (result.reduce((n,r)=>n+r.seats.length,0)>1000) throw new Error("Sơ đồ tối đa 1.000 ghế.");
  return sortRows(result);
 }
 function applyRow(rows,label,options) {
  const result=clone(rows), row=result.find(r=>r.label===label);
  if(!row)throw new Error("Không tìm thấy hàng.");
  const count=integer(options.count,0,200,"Số ghế cần từ 0 đến 200.");
  const first=integer(options.first,1,200,"Số ghế bắt đầu cần từ 1 đến 200.");
  const gaps=new Set();
  String(options.gaps||"").split(",").filter(x=>x.trim()).forEach(x=>gaps.add(integer(x.trim(),1,200,"Lối đi là danh sách số vị trí 1–200, ngăn bằng dấu phẩy.")));
  if(options.type && !TYPES.includes(options.type))throw new Error("Loại ghế không hợp lệ.");
  const desired=[];
  for(let n=first;desired.length<count;n++){
   if(n>200)throw new Error("Khoảng ghế vượt vị trí 200. Hãy giảm số ghế hoặc lối đi.");
   if(!gaps.has(n))desired.push(n);
  }
  const old=new Map(row.seats.map(s=>[s.seatNumber,s])), wanted=new Set(desired);
  row.seats=desired.map(number=>{
   const s=old.get(number);
   if(s){
    const next={...s}; delete next.retired;
    if(!s.id || options.override){
     if(options.type)next.seatType=options.type;
     if(options.active!==null && options.active!==undefined)next.active=options.active;
    }
    return next;
   }
   return {id:null,rowLabel:label,seatNumber:number,seatType:options.type||"NORMAL",active:options.active??true};
  });
  old.forEach((s,number)=>{if(!wanted.has(number)&&s.id)row.seats.push({...s,active:false,retired:true});});
  row.seats.sort((a,b)=>a.seatNumber-b.seatNumber);
  if(result.reduce((n,r)=>n+r.seats.length,0)>1000)throw new Error("Sơ đồ tối đa 1.000 ghế.");
  return result;
 }
 function removeRow(rows,label){
  return clone(rows).map(r=>r.label===label?{label:r.label,seats:r.seats.filter(s=>s.id).map(s=>({...s,active:false,retired:true}))}:r)
   .filter(r=>r.seats.length);
 }
 function removeSeat(rows,seatKey){
  const result=clone(rows);
  result.forEach(r=>{r.seats=r.seats.filter(s=>{
   if(key(s)!==seatKey)return true;
   if(s.id){s.active=false;s.retired=true;return true;}
   return false;
  });});
  return result.filter(r=>r.seats.length);
 }
 function updateSelected(rows,selected,type,active){
  if(!selected.size)throw new Error("Hãy chọn ít nhất một ghế.");
  if(!type && active===null)throw new Error("Chọn loại hoặc trạng thái cần thay đổi.");
  if(type&&!TYPES.includes(type))throw new Error("Loại ghế không hợp lệ.");
  const result=clone(rows);
  result.forEach(r=>r.seats.forEach(s=>{
   if(selected.has(key(s))){if(type)s.seatType=type;if(active!==null)s.active=active;if(active===true)delete s.retired;}
  }));
  return result;
 }
 function flatten(rows){
  return rows.flatMap(r=>r.seats.map(s=>({id:s.id??null,rowLabel:s.rowLabel,seatNumber:s.seatNumber,seatType:s.seatType,active:s.active})));
 }
 function geometry(row){
  if(!row.seats.length)return {span:0,offsets:[]};
  const min=Math.min(...row.seats.map(s=>s.seatNumber)),max=Math.max(...row.seats.map(s=>s.seatNumber));
  const span=max-min+1;
  return {span,offsets:row.seats.map(s=>({key:key(s),offset:(s.seatNumber-min+.5-span/2)*64}))};
 }
 root.CineSeatDesigner={fromSeats,labels,generate,applyRow,removeRow,removeSeat,updateSelected,flatten,geometry,key};
})(typeof window!=="undefined"?window:globalThis);
