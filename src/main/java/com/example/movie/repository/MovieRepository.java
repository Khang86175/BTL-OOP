package com.example.movie.repository;

import com.example.movie.model.Movie;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    @Override
    @EntityGraph(attributePaths = "categories")
    List<Movie> findAll(Specification<Movie> specification, Sort sort);

    @Override
    @EntityGraph(attributePaths = "categories")
    Optional<Movie> findById(Long id);
}
