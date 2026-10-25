(() => {
 "use strict";
 const form=document.getElementById("seat-edit-form"),sizeForm=document.getElementById("create-layout-form");
 if(!form||!sizeForm)return;
 const M=window.CineSeatEditor,$=id=>document.getElementById(id);
 const saved=[...document.querySelectorAll('#saved-seat-data [data-seat-id]')].map(e=>({id:e.dataset.seatId,rowLabel:e.dataset.row,seatNumber:Number(e.dataset.number),seatType:e.dataset.type,active:e.dataset.active==="true"}));
 let selected=null;
 function select(id){
  selected=M.selectSeat(saved,id);form.action=form.dataset.baseUrl+"/"+encodeURIComponent(selected.id);
  $("seat-title").textContent="Ghế "+selected.rowLabel+selected.seatNumber;$("seat-id").textContent="ID: "+selected.id;
  $("edit-row").value=selected.rowLabel;$("edit-number").value=selected.seatNumber;$("edit-type").value=selected.seatType;$("edit-active").checked=selected.active;
  document.querySelectorAll('.seat-box').forEach(e=>e.classList.toggle("is-selected",e.dataset.seatId===selected.id));
  form.hidden=false;$("seat-hint").hidden=true;
  const removal=$("seat-delete-form");removal.action=removal.dataset.baseUrl+"/"+encodeURIComponent(selected.id)+"/delete";removal.hidden=false;
 }
 $("seat-canvas").addEventListener("click",event=>{
  const link=event.target.closest('.seat-box[data-seat-id]');if(!link)return;
  event.preventDefault();select(link.dataset.seatId);
 });
 function preview(){
  try{
   const seats=M.previewLayout(saved,$("rowCount").value,$("seatsPerRow").value),removed=M.removedSeats(saved,$("rowCount").value,$("seatsPerRow").value);
   const canvas=$("seat-canvas");canvas.replaceChildren();const rows=new Map();
   seats.forEach(seat=>{const label=seat.rowLabel.trim().toUpperCase();if(!rows.has(label))rows.set(label,[]);rows.get(label).push(seat);});
   rows.forEach((seats,label)=>{
    const row=document.createElement("div");row.className="seat-map-row";row.dataset.row=label;
    const name=document.createElement("strong");name.className="seat-map-label";name.textContent=label;row.append(name);
    const track=document.createElement("div");track.className="seat-map-track";
    seats.forEach(seat=>{
     const cell=document.createElement(seat.id?"a":"span"),supported=seat.seatType==="NORMAL"||seat.seatType==="VIP";
     cell.className="seat-box seat-"+(supported?seat.seatType:"legacy")+(seat.active?"":" seat-inactive");
     cell.dataset.row=seat.rowLabel;cell.dataset.number=seat.seatNumber;cell.dataset.type=seat.seatType;cell.dataset.active=seat.active;
     if(seat.id){cell.dataset.seatId=seat.id;cell.href=form.dataset.baseUrl+"/"+encodeURIComponent(seat.id)+"/edit";}
     const text=document.createElement("span");text.textContent=seat.rowLabel+seat.seatNumber;cell.append(text);
     const kind=document.createElement("small");kind.textContent=seat.seatType==="NORMAL"?"Thường":seat.seatType==="VIP"?"VIP":"Loại cũ";cell.append(kind);
     if(!seat.id){const tag=document.createElement("small");tag.textContent="Mới";cell.append(tag);}
     if(!seat.active){const tag=document.createElement("small");tag.textContent="Ngừng dùng";cell.append(tag);}
     track.append(cell);
    });row.append(track);canvas.append(row);
   });
   $("layout-message").textContent=seats.length+" vị trí · "+seats.filter(s=>!s.id).length+" ghế mới · "+removed.length+" ghế đã lưu sẽ bị xóa nếu đủ điều kiện. Nhấn nút tạo/cập nhật để áp dụng trực tiếp.";
   $("save-layout").disabled=false;
  }catch(error){$("layout-message").textContent=error.message;$("save-layout").disabled=true;}
 }
 [$("rowCount"),$("seatsPerRow")].forEach(input=>input.addEventListener("input",preview));
 if(!saved.length)preview();
 sizeForm.addEventListener("submit",event=>{
  $("confirm-removal").value="false";
  try{
   const removed=M.removedSeats(saved,$("rowCount").value,$("seatsPerRow").value);
   if(removed.length){if(!window.confirm("Xóa "+removed.length+" ghế ngoài kích thước mới? Server sẽ từ chối nếu ghế đang được tham chiếu.")){event.preventDefault();return;}$("confirm-removal").value="true";}
  }catch(error){event.preventDefault();$("layout-message").textContent=error.message;}
 });
 form.addEventListener("submit",event=>{
  try{M.changeSeat(selected,$("edit-type").value,$("edit-active").checked);}
  catch(error){event.preventDefault();window.alert(error.message);}
 });
 document.querySelectorAll('[data-confirm-delete]').forEach(removal=>removal.addEventListener("submit",event=>{
  const confirmed=window.confirm("Xóa "+removal.dataset.confirmDelete+"? Chỉ ghế chưa được tham chiếu mới có thể xóa.");
  removal.querySelector('[name=confirmed]').value=String(confirmed);if(!confirmed)event.preventDefault();
 }));
})();
