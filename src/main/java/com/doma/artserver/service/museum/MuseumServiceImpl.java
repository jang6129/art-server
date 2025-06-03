package com.doma.artserver.service.museum;

import com.doma.artserver.api.ApiClient;
import com.doma.artserver.api.munhwa.museum.MunwhaMuseumDTO;
import com.doma.artserver.domain.museum.entity.Museum;
import com.doma.artserver.domain.museum.repository.MuseumRepository;
import com.doma.artserver.dto.museum.MuseumDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MuseumServiceImpl implements MuseumService {

    private static final Logger logger = LoggerFactory.getLogger(MuseumServiceImpl.class);
    private final ApiClient<MunwhaMuseumDTO> apiClient;
    private final MuseumRepository museumRepository;

    public MuseumServiceImpl(ApiClient<MunwhaMuseumDTO> apiClient,
                             MuseumRepository museumRepository) {
        this.apiClient = apiClient;
        this.museumRepository = museumRepository;
    }

    @Override
    @Transactional
    public void fetchMuseum() {
        logger.info("Starting to fetch museum data");
        int page = 1;
        int maxPage = 12;

        boolean isDbEmpty = museumRepository.count() == 0;
        logger.info("Current museum count in database: {}", museumRepository.count());

        if (!isDbEmpty) {
            maxPage = 2;
            logger.info("Database is not empty, limiting fetch to {} pages", maxPage - 1);
        }

        while (page < maxPage) {
            logger.info("Fetching museum data from API - page {}/{}", page, maxPage - 1);
            List<MunwhaMuseumDTO> list = apiClient.fetchItems(page);
            logger.info("Received {} museums from API on page {}", list.size(), page);

            int savedCount = 0;
            int duplicateCount = 0;

            for (MunwhaMuseumDTO museumDTO : list) {
                // name (place) 값으로 중복 검사
                Optional<Museum> existingMuseum = museumRepository.findByName(museumDTO.getPlace());

                // 중복되지 않은 경우에만 저장
                if (existingMuseum.isEmpty()) {
                    museumRepository.save(museumDTO.toEntity());
                    savedCount++;
                } else {
                    logger.debug("Duplicate museum data found: {}", museumDTO.getPlace());
                    duplicateCount++;
                }
            }

            logger.info("Page {} processing complete: {} museums saved, {} duplicates skipped", page, savedCount, duplicateCount);
            page++;
        }

        logger.info("Museum data fetch completed. Total museums in database: {}", museumRepository.count());
    }

    @Override
    public Page<MuseumDTO> getMuseums(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);

        Page<Museum> museums = museumRepository.findAll(pageable);

        return museums.map(this::convertToDTO);
    }

    @Override
    public List<Museum> findMuseumsByName(List names) {
        return museumRepository.findByNameIn(names);
    }

    // Exhibition -> ExhibitionDTO로 변환하는 메소드
    private MuseumDTO convertToDTO(Museum museum) {
        return MuseumDTO.builder()
                .name(museum.getName())
                .area(museum.getArea())
                .gpsX(museum.getGpsX())
                .gpsY(museum.getGpsY())
                .id(museum.getId())
                .contactInfo(museum.getContactInfo())
                .website(museum.getWebsite())
                .build();
    }


} // MuseumService ends
