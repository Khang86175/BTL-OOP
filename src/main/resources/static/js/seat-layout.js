/* Only form assistance. All validation, authorization and writes happen on the server. */
(() => {
 "use strict";
 const batch = document.getElementById("seat-batch-form");
 if (batch) {
  const rows = document.getElementById("batch-rows");
  const invalidate = () => {
   const preview = document.getElementById("seat-preview");
   if (preview) preview.hidden = true;
  };
  const reindex = () => {
   [...rows.children].forEach((row, index) => {
    row.querySelectorAll("input,select").forEach(input => {
     const field = input.dataset.field || input.dataset.marker || (input.name.match(/\.([^\.]+)$/) || [])[1];
     const marker = input.dataset.marker || input.name.startsWith("_");
     if (field) input.name = (marker ? "_" : "") + "rows[" + index + "]." + field;
    });
   });
   invalidate();
  };
  document.getElementById("add-batch-row").addEventListener("click", () => {
   if (rows.children.length >= 30) { window.alert("Mỗi lần cấu hình tối đa 30 khoảng ghế."); return; }
   const clone = document.getElementById("batch-row-template").content.cloneNode(true);
   clone.querySelector('[data-field="rowLabel"]').value = rows.children.length < 26
    ? String.fromCharCode(65 + rows.children.length) : "";
   rows.appendChild(clone);
   reindex();
  });
  rows.addEventListener("click", event => {
   const button = event.target.closest("[data-remove-row]");
   if (button) {
    button.closest("tr").remove(); reindex();
   }
  });
  batch.addEventListener("input", invalidate);
  batch.addEventListener("change", invalidate);
 }
 const bulk = document.getElementById("seat-bulk-form");
 if (bulk) {
  const boxes = [...bulk.querySelectorAll('input[name="seatIds"]')];
  const count = () => { document.getElementById("selected-seat-count").textContent = "Đã chọn: " + boxes.filter(b => b.checked).length; };
  document.getElementById("select-all-seats").addEventListener("click", () => { boxes.forEach(b => { b.checked = true; }); count(); });
  document.getElementById("clear-all-seats").addEventListener("click", () => { boxes.forEach(b => { b.checked = false; }); count(); });
  bulk.querySelectorAll("[data-select-row]").forEach(button => button.addEventListener("click", () => {
   const row = boxes.filter(b => b.dataset.seatRow === button.dataset.selectRow);
   const select = row.some(b => !b.checked);
   row.forEach(b => { b.checked = select; }); count();
  }));
  bulk.addEventListener("change", count);
  bulk.addEventListener("submit", event => {
   if (!boxes.some(b => b.checked)) { event.preventDefault(); window.alert("Hãy chọn ít nhất một ghế."); }
  });
  count();
 }
})();
