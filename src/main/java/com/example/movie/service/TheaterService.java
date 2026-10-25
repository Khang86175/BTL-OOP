package com.example.movie.service;
import com.example.movie.dto.TheaterForm;
import com.example.movie.model.Theater;
import com.example.movie.repository.TheaterRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @Validated @Transactional(readOnly=true)
public class TheaterService {
 private final TheaterRepository theaters;
 public TheaterService(TheaterRepository theaters){this.theaters=theaters;}
 public List<Theater> findActive(){return theaters.findByActiveTrueOrderByNameAsc();}
 public List<Theater> findForAdmin(){ScheduleAccess.admin();return theaters.findAll(org.springframework.data.domain.Sort.by("name","id"));}
 @Transactional public void setActive(Long id,boolean active){ScheduleAccess.admin();getById(id).setActive(active);}
 public Theater getById(Long id){return theaters.findById(id).orElseThrow(()->new ScheduleValidationException("theaterId","Không tìm thấy rạp."));}
 @Transactional public Theater create(@NotNull @Valid TheaterForm f){ScheduleAccess.admin();return save(new Theater(),f);}
 @Transactional public Theater update(Long id,@NotNull @Valid TheaterForm f){ScheduleAccess.admin();return save(getById(id),f);}
 private Theater save(Theater t,TheaterForm f){t.setName(f.name().strip());t.setAddress(f.address().strip());t.setActive(f.active());return theaters.saveAndFlush(t);}
}
