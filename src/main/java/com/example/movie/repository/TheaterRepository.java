package com.example.movie.repository;
import com.example.movie.model.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TheaterRepository extends JpaRepository<Theater,Long> {
 List<Theater> findByActiveTrueOrderByNameAsc();
}
