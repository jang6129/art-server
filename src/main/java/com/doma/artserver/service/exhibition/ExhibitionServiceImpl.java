package com.doma.artserver.service.exhibition;

import com.doma.artserver.api.ApiClient;
import com.doma.artserver.api.munhwa.exhibition.MunwhaExhibitionDTO;
import com.doma.artserver.domain.exhibition.entity.Exhibition;
import com.doma.artserver.domain.exhibition.entity.ExhibitionStatus;
import com.doma.artserver.domain.museum.entity.Museum;
import com.doma.artserver.domain.exhibition.repository.ExhibitionRepository;
import com.doma.artserver.domain.museum.repository.MuseumRepository;
import com.doma.artserver.dto.exhibition.ExhibitionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExhibitionServiceImpl implements ExhibitionService {

    private static final Logger logger = LoggerFactory.getLogger(ExhibitionServiceImpl.class);
    private static final int DEFAULT_MAX_PAGE = 25;
    private static final int NON_EMPTY_DB_MAX_PAGE = 2;

    private final ApiClient<MunwhaExhibitionDTO> apiClient;
    private final ExhibitionRepository exhibitionRepository;
    private final MuseumRepository museumRepository;

    public ExhibitionServiceImpl(ApiClient<MunwhaExhibitionDTO> apiClient,
                                 ExhibitionRepository exhibitionRepository,
                                 MuseumRepository museumRepository) {
        this.apiClient = apiClient;
        this.exhibitionRepository = exhibitionRepository;
        this.museumRepository = museumRepository;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"exhibitions", "exhibitionsByArea", "exhibitionsByMuseum", "searchResults"}, allEntries = true)
    public void fetchExhibitions() {
        fetchExhibitions(DEFAULT_MAX_PAGE);
    }

    @Transactional
    public void fetchExhibitions(int maxPage) {
        logger.info("Starting to fetch exhibition data with max page: {}", maxPage);
        int page = 1;
        boolean isDbEmpty = exhibitionRepository.count() == 0;
        logger.info("Current exhibition count in database: {}", exhibitionRepository.count());

        if (!isDbEmpty) {
            maxPage = NON_EMPTY_DB_MAX_PAGE;
            logger.info("Database is not empty, limiting fetch to {} pages", maxPage - 1);
        }

        int totalSaved = 0;
        int totalSkipped = 0;

        while (page < maxPage) {
            logger.info("Fetching exhibition data from API - page {}/{}", page, maxPage - 1);
            List<MunwhaExhibitionDTO> list = apiClient.fetchItems(page);
            logger.info("Received {} exhibitions from API on page {}", list.size(), page);

            int pageSaved = 0;
            int pageSkipped = 0;

            for (MunwhaExhibitionDTO dto : list) {
                Optional<Exhibition> existingExhibition = exhibitionRepository.findByApiId(dto.getSeq());

                if (existingExhibition.isEmpty()) {
                    if (dto.getStartDate() == null) {
                        logger.debug("Skipping exhibition with null startDate: {}", dto.getTitle());
                        pageSkipped++;
                        continue;
                    }

                    Optional<Museum> museum = museumRepository.findByName(dto.getPlace());
                    if (museum.isPresent()) {
                        logger.debug("Saving exhibition: {} at museum: {}", dto.getTitle(), museum.get().getName());
                        exhibitionRepository.save(dto.toEntity(museum.get()));
                    } else {
                        logger.debug("Saving exhibition: {} with unknown museum", dto.getTitle());
                        exhibitionRepository.save(dto.toEntity(Museum.builder().name("정보 없음").build()));
                    }
                    pageSaved++;
                } else {
                    logger.debug("Skipping existing exhibition: {}", dto.getTitle());
                    pageSkipped++;
                }
            }

            totalSaved += pageSaved;
            totalSkipped += pageSkipped;
            logger.info("Page {} processing complete: {} exhibitions saved, {} existing exhibitions skipped", 
                    page, pageSaved, pageSkipped);
            page++;
        }

        logger.info("Exhibition data fetch completed. Total: {} saved, {} skipped. Total in database: {}", 
                totalSaved, totalSkipped, exhibitionRepository.count());
    }

    @Override
    @Cacheable(value = "exhibitions", key = "#page + '-' + #pageSize")
    public Page<ExhibitionDTO> getExhibitions(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Exhibition> exhibitions = exhibitionRepository.findAllByStatusAndOrderByStartDate(pageable);
        return exhibitions.map(this::convertToDTO);
    }

    @Override
    @Cacheable(value = "exhibitionsByArea", key = "#area + '-' + #page + '-' + #pageSize")
    public Page<ExhibitionDTO> getExhibitionsByArea(String area, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Exhibition> exhibitions = exhibitionRepository.findByArea(area, pageable);
        return exhibitions.map(this::convertToDTO);
    }

    @Override
    @Cacheable(value = "exhibitionsByMuseum", key = "#museumIds.hashCode() + '-' + #page + '-' + #pageSize")
    public Page<ExhibitionDTO> getExhibitionsByMuseums(List<Long> museumIds, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Exhibition> exhibitions = exhibitionRepository.findByMuseumIdsAndOrderByStatusAndStartDate(museumIds, pageable);
        return exhibitions.map(this::convertToDTO);
    }

    @Override
    @Cacheable(value = "searchResults", key = "#keyword + '-' + #area + '-' + #page + '-' + #pageSize")
    public Page<ExhibitionDTO> searchExhibitions(String keyword, String area, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Exhibition> exhibitions = exhibitionRepository.searchExhibitions(keyword, area, pageable);
        return exhibitions.map(this::convertToDTO);
    }

    private ExhibitionDTO convertToDTO(Exhibition exhibition) {
        return ExhibitionDTO.builder()
                .id(exhibition.getId())
                .title(exhibition.getTitle())
                .area(exhibition.getArea())
                .imgUrl(exhibition.getImgUrl())
                .place(exhibition.getPlace())
                .status(exhibition.getStatus())
                .startDate(exhibition.getStartDate())
                .endDate(exhibition.getEndDate())
                .storageUrl(exhibition.getStorageUrl())
                .apiId(exhibition.getApiId())
                .url(exhibition.getUrl())
                .price(exhibition.getPrice())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"exhibitions", "exhibitionsByArea", "exhibitionsByMuseum", "searchResults"}, allEntries = true)
    public void updateExhibitions() {
        List<Exhibition> exhibitions = exhibitionRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Exhibition exhibition : exhibitions) {
            updateExhibitionStatus(exhibition, today);
            exhibitionRepository.save(exhibition);
        }
    }

    private void updateExhibitionStatus(Exhibition exhibition, LocalDate today) {
        // Skip status update if startDate or endDate is null
        if (exhibition.getStartDate() == null || exhibition.getEndDate() == null) {
            logger.warn("Exhibition with id {} has null startDate or endDate. Status not updated.", exhibition.getId());
            return;
        }

        if (exhibition.getStartDate().isAfter(today)) {
            exhibition.setStatus(ExhibitionStatus.SCHEDULED);
        } else if ((exhibition.getStartDate().isBefore(today) || exhibition.getStartDate().isEqual(today)) && exhibition.getEndDate().isAfter(today)) {
            exhibition.setStatus(ExhibitionStatus.ONGOING);
        } else if (exhibition.getEndDate().isBefore(today)) {
            exhibition.setStatus(ExhibitionStatus.COMPLETED);
        }
    }

    @Override
    public void cacheExhibitions() {
        // 캐시 관련 로직은 이제 어노테이션으로 처리됨
    }

    @Override
    @CacheEvict(value = {"exhibitions", "exhibitionsByArea", "exhibitionsByMuseum", "searchResults"}, allEntries = true)
    public void clearExhibition() {
        exhibitionRepository.deleteAll();
    }
}
