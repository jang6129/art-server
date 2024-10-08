package com.doma.artserver.service.museum;

import com.doma.artserver.domain.museum.entity.Museum;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MuseumService<T> {
    void fetchMuseum();
    Page<T> getMuseums(int page, int pageSize);
    List<Museum> findMuseumsByName(List<String> names);
}
