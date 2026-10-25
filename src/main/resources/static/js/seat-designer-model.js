/* Individual-seat editing only. Identity and physical position are immutable. */
(function(root){
 "use strict";
 function selectSeat(seats,id){
  const seat=seats.find(s=>String(s.id)===String(id));
  if(!seat)throw new Error("Không tìm thấy ghế.");
  return {...seat,id:String(seat.id)};
 }
 function changeSeat(seat,type,active){
  if(type!=="NORMAL"&&type!=="VIP")throw new Error("Chỉ hỗ trợ ghế Thường và VIP.");
  if(typeof active!=="boolean")throw new Error("Trạng thái ghế không hợp lệ.");
  return {...seat,seatType:type,active};
 }
 function dimensions(rows,count){
  rows=Number(rows);count=Number(count);
  if(!Number.isInteger(rows)||rows<1||rows>30||!Number.isInteger(count)||count<1||count>200||rows*count>1000)throw new Error("Số hàng 1–30, số ghế 1–200; tổng tối đa 1.000 ghế.");
  const positions=[];
  for(let r=1;r<=rows;r++){
   let n=r,label="";while(n>0){n--;label=String.fromCharCode(65+n%26)+label;n=Math.floor(n/26);}
   for(let number=1;number<=count;number++)positions.push({rowLabel:label,seatNumber:number});
  }
  return positions;
 }
 const position=seat=>seat.rowLabel.trim().toUpperCase()+":"+seat.seatNumber;
 function previewLayout(saved,rows,count){
  const current=new Map(saved.map(s=>[position(s),s]));
  return dimensions(rows,count).map(p=>current.has(position(p))?{...current.get(position(p))}:{...p,id:null,seatType:"NORMAL",active:true});
 }
 function removedSeats(saved,rows,count){
  const keep=new Set(dimensions(rows,count).map(position));return saved.filter(s=>!keep.has(position(s)));
 }
 root.CineSeatEditor={selectSeat,changeSeat,previewLayout,removedSeats};
})(typeof window!=="undefined"?window:globalThis);
