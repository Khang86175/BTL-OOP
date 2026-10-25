package com.example.movie.service;

import com.example.movie.model.Seat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import java.sql.*;
import java.util.*;

/** Checks actual FK references without depending on Member 4's entity classes. */
@Component
public class SeatReferenceGuard {
 private final JdbcTemplate jdbc;
 public SeatReferenceGuard(JdbcTemplate jdbc){this.jdbc=jdbc;}
 private record Table(String catalog,String schema,String name) {}
 private record Reference(Table table,String column) {}

 // Caller holds exclusive Room/Seat locks until the enclosing transaction commits.
 public void assertUnreferenced(List<Seat> candidates){
  if(candidates.isEmpty())return;
  try{jdbc.execute((ConnectionCallback<Void>) connection->{
   var metadata=connection.getMetaData();String catalog=connection.getCatalog();
   var tables=new ArrayList<Table>();
   try(var result=metadata.getTables(catalog,null,"%",new String[]{"TABLE"})){
    while(result.next())tables.add(new Table(result.getString("TABLE_CAT"),result.getString("TABLE_SCHEM"),result.getString("TABLE_NAME")));
   }
   var seats=tables.stream().filter(t->t.name().equalsIgnoreCase("seats")).findFirst()
    .orElseThrow(SeatReferenceGuard::unverifiable);
   var references=new LinkedHashSet<Reference>();
   try(var keys=metadata.getExportedKeys(seats.catalog(),seats.schema(),seats.name())){
    while(keys.next()){
     if(!"id".equalsIgnoreCase(keys.getString("PKCOLUMN_NAME")))throw unverifiable();
     var child=new Table(keys.getString("FKTABLE_CAT"),keys.getString("FKTABLE_SCHEM"),keys.getString("FKTABLE_NAME"));
     if(child.catalog()!=null&&catalog!=null&&!child.catalog().equalsIgnoreCase(catalog))throw unverifiable();
     references.add(new Reference(child,keys.getString("FKCOLUMN_NAME")));
    }
   }
   // A recognized Booking table without an enforcing FK cannot be checked race-safely.
   for(var table:tables)if(Set.of("booking_seats","showtime_seats").contains(table.name().toLowerCase(Locale.ROOT))
    &&references.stream().noneMatch(r->r.table().equals(table)&&r.column().equalsIgnoreCase("seat_id")))throw unverifiable();
   String quote=metadata.getIdentifierQuoteString().trim();
   if(quote.isEmpty())throw unverifiable();
   var ids=candidates.stream().map(Seat::getId).toList();
   for(var reference:references){
    var parts=new ArrayList<String>();var table=reference.table();
    if(metadata.supportsCatalogsInDataManipulation()&&table.catalog()!=null)parts.add(quoted(table.catalog(),quote));
    if(metadata.supportsSchemasInDataManipulation()&&table.schema()!=null)parts.add(quoted(table.schema(),quote));
    parts.add(quoted(table.name(),quote));
    for(int offset=0;offset<ids.size();offset+=500){
     var batch=ids.subList(offset,Math.min(offset+500,ids.size()));
     String sql="select 1 from "+String.join(".",parts)+" where "+quoted(reference.column(),quote)+" in ("+String.join(",",Collections.nCopies(batch.size(),"?"))+")";
     try(var statement=connection.prepareStatement(sql)){
      statement.setMaxRows(1);for(int i=0;i<batch.size();i++)statement.setLong(i+1,batch.get(i));
      try(var result=statement.executeQuery()){if(result.next())throw new ScheduleValidationException("seats","Có ghế đang được dữ liệu khác tham chiếu (Booking/giữ chỗ/lịch ghế). Chưa xóa hoặc thay đổi sơ đồ.");}
     }
    }
   }
   return null;
  });}catch(DataAccessException ex){throw unverifiable();}
 }
 private static String quoted(String name,String quote){return quote+name.replace(quote,quote+quote)+quote;}
 private static ScheduleValidationException unverifiable(){return new ScheduleValidationException("seats","Chưa xác minh được tham chiếu hoặc khóa ngoại Booking. Không xóa ghế; cần kiểm tra schema và quyền đọc metadata.");}
}
