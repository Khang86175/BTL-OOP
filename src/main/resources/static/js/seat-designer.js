/* UI holds a draft only. One confirmed server transaction persists the complete layout. */
(() => {
 "use strict";
 const root=document.getElementById("seat-designer");
 if(!root)return;
 const M=window.CineSeatDesigner, $=id=>document.getElementById(id);
 const typeLabels={NORMAL:"Thường",VIP:"VIP",COUPLE:"Ghế đôi"};
 const baseline=[...document.querySelectorAll("#designer-seed [data-seat-id]")].map(e=>({
  id:e.dataset.seatId,rowLabel:e.dataset.row,seatNumber:Number(e.dataset.number),seatType:e.dataset.type,active:e.dataset.active==="true"
 }));
 let rows=M.fromSeats(baseline),selected=new Set(),dirty=false,activeRow=null,activeSeat=null,busy=false;
 const message=(text,bad=false)=>{const el=$("designer-message");el.textContent=text;el.className="alert "+(bad?"alert-danger":"alert-info");el.hidden=false;};
 const optionalBoolean=value=>value===""?null:value==="true";
 const changed=()=>{dirty=true;$("designer-dirty").textContent="Bản nháp chưa lưu";render();};
 const attempt=action=>{try{action();}catch(error){message(error.message,true);}};
 function button(text,css,action){
  const b=document.createElement("button");b.type="button";b.className=css;b.textContent=text;b.addEventListener("click",action);return b;
 }
 function render(){
  const canvas=$("designer-canvas");canvas.replaceChildren();
  const maxSpan=Math.max(1,...rows.map(r=>M.geometry(r).span));
  $("designer-inner").style.minWidth=(64+maxSpan*64+24)+"px";
  $("designer-empty").hidden=rows.length>0;
  rows.forEach(row=>{
   const line=document.createElement("div");line.className="designer-row";line.dataset.row=row.label;
   const label=button(row.label,"designer-row-label",()=>openRow(row.label));
   label.setAttribute("aria-label","Thiết lập hàng "+row.label);line.append(label);
   const track=document.createElement("div");track.className="designer-track";
   const offsets=new Map(M.geometry(row).offsets.map(x=>[x.key,x.offset]));
   row.seats.forEach(s=>{
    const cell=document.createElement("div");cell.className="designer-seat-cell";cell.dataset.seatKey=M.key(s);
    cell.style.left="calc(50% + "+offsets.get(M.key(s))+"px)";
    const checkbox=document.createElement("input");checkbox.type="checkbox";checkbox.checked=selected.has(M.key(s));
    checkbox.setAttribute("aria-label","Chọn ghế "+s.rowLabel+s.seatNumber);
    checkbox.addEventListener("change",()=>{if(checkbox.checked)selected.add(M.key(s));else selected.delete(M.key(s));render();});
    cell.append(checkbox);
    const b=button(s.rowLabel+s.seatNumber,"designer-seat seat-"+s.seatType+(s.active?"":" seat-inactive"),()=>openSeat(M.key(s)));
    b.dataset.number=s.seatNumber;b.setAttribute("aria-label","Sửa ghế "+s.rowLabel+s.seatNumber+", "+typeLabels[s.seatType]+(s.active?"":", ngừng hoạt động"));
    const kind=document.createElement("small");kind.textContent=typeLabels[s.seatType];b.append(kind);
    if(!s.active){const status=document.createElement("small");status.textContent="Ngừng dùng";b.append(status);}
    if(!s.id){const tag=document.createElement("small");tag.textContent="Mới";b.append(tag);}
    if(checkbox.checked)b.classList.add("is-selected");
    cell.append(b);track.append(cell);
   });
   line.append(track);canvas.append(line);
  });
  const current=M.flatten(rows),newCount=current.filter(s=>!s.id).length;
  const modified=current.filter(s=>s.id&&baseline.some(b=>b.id===s.id&&(b.active!==s.active||b.seatType!==s.seatType))).length;
  const existingOff=current.filter(s=>s.id&&!s.active&&baseline.some(b=>b.id===s.id&&b.active)).length;
  $("designer-summary").textContent=rows.length+" hàng · "+current.filter(s=>s.active).length+" ghế hoạt động · "+newCount+" ghế mới · "+modified+" ghế cũ thay đổi · "+existingOff+" ghế chuyển ngừng hoạt động";
  $("selected-count").textContent="Đã chọn: "+selected.size;
  $("designer-save").disabled=busy;
 }
 function openRow(label){
  activeRow=label;activeSeat=null;$("seat-panel").hidden=true;$("row-panel").hidden=false;
  const row=rows.find(r=>r.label===label),numbers=row.seats.map(s=>s.seatNumber);
  $("row-title").textContent="Thiết lập hàng "+label;
  $("row-count").value=row.seats.filter(s=>!s.retired).length;
  $("row-first").value=numbers.length?Math.min(...numbers):1;
  const gaps=[];
  if(numbers.length)for(let n=Math.min(...numbers);n<=Math.min(200,Math.max(...numbers));n++)if(!numbers.includes(n))gaps.push(n);
  $("row-gaps").value=gaps.join(", ");
  $("row-type").value="";$("row-active").value="";$("row-override").checked=false;
  const mixed=new Set(row.seats.map(s=>s.seatType)).size>1||new Set(row.seats.map(s=>s.active)).size>1;
  $("row-description").textContent=(mixed?"Hàng có tùy chỉnh riêng từng ghế. ":"")+"Giữ nguyên từng ghế là mặc định. Ghế đã lưu bị bỏ sẽ còn lại màu xám, không xóa ID.";
  $("row-panel").scrollIntoView({block:"nearest",behavior:"smooth"});
 }
 function openSeat(key){
  activeSeat=key;activeRow=null;$("row-panel").hidden=true;$("seat-panel").hidden=false;
  const s=rows.flatMap(r=>r.seats).find(s=>M.key(s)===key);
  $("seat-title").textContent="Ghế "+s.rowLabel+s.seatNumber+(s.id?" · ID "+s.id:" · Chưa lưu");
  $("seat-type").value=s.seatType;$("seat-active").value=String(s.active);
  const edit=$("single-seat-edit");edit.hidden=!s.id;if(s.id)edit.href="/admin/seats/"+s.id+"/edit";
 }
 $("generate-layout").addEventListener("click",()=>attempt(()=>{
  const before=rows.length;
  rows=M.generate(rows,$("layout-start").value,$("layout-rows").value,$("layout-count").value);
  selected.clear();$("row-panel").hidden=true;$("seat-panel").hidden=true;
  message("Đã cập nhật số ghế cho các hàng đã có và thêm "+(rows.length-before)+" hàng mới trong bản nháp. ID và tùy chỉnh ghế cũ được giữ; ghế bị giảm chỉ chuyển xám.");
  changed();
 }));
 $("apply-row").addEventListener("click",()=>attempt(()=>{
  if(!activeRow)throw new Error("Hãy chọn hàng.");
  rows=M.applyRow(rows,activeRow,{count:$("row-count").value,first:$("row-first").value,gaps:$("row-gaps").value,
   type:$("row-type").value,active:optionalBoolean($("row-active").value),override:$("row-override").checked});
  selected.clear();message("Đã cập nhật hàng trong bản nháp. Ghế cũ ngoài khoảng chuyển xám; cần xác nhận khi lưu.");
  changed();
 }));
 $("remove-row").addEventListener("click",()=>attempt(()=>{
  const row=rows.find(r=>r.label===activeRow);if(!row)return;
  const persisted=row.seats.some(s=>s.id);
  rows=M.removeRow(rows,activeRow);selected.clear();$("row-panel").hidden=true;
  message(persisted?"Hàng đã lưu được ngừng hoạt động trong bản nháp; ghế mới trong hàng đã bỏ. ID cũ được bảo toàn.":"Đã bỏ hàng chưa lưu khỏi bản nháp.");
  changed();
 }));
 $("select-row").addEventListener("click",()=>{rows.find(r=>r.label===activeRow)?.seats.forEach(s=>selected.add(M.key(s)));render();});
 $("apply-seat").addEventListener("click",()=>attempt(()=>{
  rows=M.updateSelected(rows,new Set([activeSeat]),$("seat-type").value,optionalBoolean($("seat-active").value));changed();
 }));
 $("remove-seat").addEventListener("click",()=>attempt(()=>{
  rows=M.removeSeat(rows,activeSeat);selected.delete(activeSeat);$("seat-panel").hidden=true;
  message("Ghế chưa lưu được bỏ; ghế đã lưu chỉ ngừng hoạt động để bảo toàn ID.");changed();
 }));
 $("select-all").addEventListener("click",()=>{M.flatten(rows).forEach(s=>selected.add(M.key(s)));render();});
 $("clear-selection").addEventListener("click",()=>{selected.clear();render();});
 $("apply-selection").addEventListener("click",()=>attempt(()=>{
  rows=M.updateSelected(rows,selected,$("selection-type").value,optionalBoolean($("selection-active").value));changed();
 }));
 $("reset-draft").addEventListener("click",()=>{
  if(dirty&&!window.confirm("Bỏ mọi thay đổi chưa lưu và tải lại sơ đồ hiện có?"))return;
  window.location.reload();
 });
 $("designer-form").addEventListener("submit",async event=>{
  event.preventDefault();if(busy)return;
  const current=M.flatten(rows);
  if(current.length>1000){message("Sơ đồ tối đa 1.000 ghế.",true);return;}
  const modified=current.some(s=>s.id&&baseline.some(b=>b.id===s.id&&(b.active!==s.active||b.seatType!==s.seatType)));
  if(modified&&!$("confirm-existing").checked){message("Hãy tích xác nhận thay đổi ghế đã lưu trước khi lưu sơ đồ.",true);return;}
  busy=true;render();
  try{
   const form=event.currentTarget,token=form.querySelector('input[name="_csrf"]');
   const response=await fetch(form.action,{method:"POST",credentials:"same-origin",headers:{
    "Content-Type":"application/json",[form.dataset.csrfHeader]:token.value
   },body:JSON.stringify({roomId:root.dataset.roomId,snapshot:root.dataset.snapshot,seats:current,confirmExistingChanges:$("confirm-existing").checked})});
   if(response.redirected){throw new Error("Phiên đăng nhập đã thay đổi. Hãy tải lại trang và đăng nhập.");}
   if(!response.ok){
    const data=await response.json().catch(()=>({}));
    throw new Error(data.message||data.detail||(response.status===403?"Bạn không có quyền hoặc phiên CSRF đã hết hạn. Hãy tải lại trang.":"Sơ đồ không hợp lệ hoặc không lưu được. Hãy kiểm tra dữ liệu."));
   }
   dirty=false;
   const destination=new URL(root.dataset.returnUrl,window.location.href);destination.hash="designer-saved";
   if(window.location.pathname===destination.pathname&&window.location.search===destination.search){
    window.location.hash=destination.hash;window.location.reload();
   }else window.location.assign(destination.href);
  }catch(error){message(error.message,true);}
  finally{busy=false;render();}
 });
 window.addEventListener("beforeunload",event=>{if(dirty){event.preventDefault();event.returnValue="";}});
 if(window.location.hash==="#designer-saved")message("Đã lưu sơ đồ. Ghế cũ giữ nguyên ID; ghế bị bỏ chỉ ngừng hoạt động.");
 render();
})();
