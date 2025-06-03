package com.doma.artserver.service;

import com.doma.artserver.api.ApiClient;
import com.doma.artserver.api.munhwa.exhibition.MunwhaExhibitionDTO;
import com.doma.artserver.domain.exhibition.entity.Exhibition;
import com.doma.artserver.domain.exhibition.entity.ExhibitionStatus;
import com.doma.artserver.domain.exhibition.repository.ExhibitionRepository;
import com.doma.artserver.domain.museum.entity.Museum;
import com.doma.artserver.domain.museum.repository.MuseumRepository;
import com.doma.artserver.dto.exhibition.ExhibitionDTO;
import com.doma.artserver.service.exhibition.ExhibitionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ExhibitionServiceImplTest {

    @Mock
    private ApiClient<MunwhaExhibitionDTO> apiClient;

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @Mock
    private MuseumRepository museumRepository;

    @InjectMocks
    private ExhibitionServiceImpl exhibitionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFetchExhibitions() {
        // Given
        MunwhaExhibitionDTO exhibitionDTO1 = new MunwhaExhibitionDTO();
        exhibitionDTO1.setTitle("Exhibition A");
        exhibitionDTO1.setPlace("Museum A");
        MunwhaExhibitionDTO exhibitionDTO2 = new MunwhaExhibitionDTO();
        exhibitionDTO2.setTitle("Exhibition B");
        exhibitionDTO2.setPlace("Museum B");

        when(apiClient.fetchItems(1)).thenReturn(Arrays.asList(exhibitionDTO1, exhibitionDTO2));
        when(exhibitionRepository.findByTitle("Exhibition A")).thenReturn(Optional.empty());
        when(exhibitionRepository.findByTitle("Exhibition B")).thenReturn(Optional.empty());
        when(museumRepository.findByName("Museum A")).thenReturn(Optional.of(new Museum()));
        when(museumRepository.findByName("Museum B")).thenReturn(Optional.of(new Museum()));

        // When
        exhibitionService.fetchExhibitions();

        // Then
        verify(exhibitionRepository, times(2)).save(any(Exhibition.class));
    }

    @Test
    public void testGetExhibitions() {
        // Given
        Exhibition exhibition1 = Exhibition.builder()
                .title("Exhibition A")
                .place("Museum A")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(5))
                .status(ExhibitionStatus.ONGOING)
                .build();

        Exhibition exhibition2 = Exhibition.builder()
                .title("Exhibition B")
                .place("Museum B")
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(1))
                .status(ExhibitionStatus.COMPLETED)
                .build();

        List<Exhibition> exhibitionList = Arrays.asList(exhibition1, exhibition2);
        Page<Exhibition> exhibitionPage = new PageImpl<>(exhibitionList);
        Pageable pageable = PageRequest.of(0, 10);

        when(exhibitionRepository.findAllByStatusAndOrderByStartDate(pageable)).thenReturn(exhibitionPage);

        // When
        Page<ExhibitionDTO> result = exhibitionService.getExhibitions(0, 10);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals("Exhibition A", result.getContent().get(0).getTitle());
        assertEquals("Exhibition B", result.getContent().get(1).getTitle());
    }

    @Test
    public void testUpdateExhibitions() {
        // Given
        Exhibition exhibition1 = Exhibition.builder()
                .title("Exhibition A")
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(10))
                .status(ExhibitionStatus.SCHEDULED)
                .build();

        Exhibition exhibition2 = Exhibition.builder()
                .title("Exhibition B")
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(1))
                .status(ExhibitionStatus.ONGOING)
                .build();

        List<Exhibition> exhibitions = Arrays.asList(exhibition1, exhibition2);
        when(exhibitionRepository.findAll()).thenReturn(exhibitions);

        // When
        exhibitionService.updateExhibitions();

        // Then
        verify(exhibitionRepository, times(2)).save(any(Exhibition.class));
        assertEquals(ExhibitionStatus.SCHEDULED, exhibition1.getStatus());
        assertEquals(ExhibitionStatus.COMPLETED, exhibition2.getStatus());
    }

    @Test
    public void testFetchExhibitionsWithNullStartDate() {
        // Given
        MunwhaExhibitionDTO validExhibition = new MunwhaExhibitionDTO();
        validExhibition.setTitle("Valid Exhibition");
        validExhibition.setPlace("Museum A");
        validExhibition.setStartDate(LocalDate.now());
        validExhibition.setEndDate(LocalDate.now().plusDays(10));
        validExhibition.setSeq(1L);

        MunwhaExhibitionDTO nullStartDateExhibition = new MunwhaExhibitionDTO();
        nullStartDateExhibition.setTitle("Null StartDate Exhibition");
        nullStartDateExhibition.setPlace("Museum B");
        nullStartDateExhibition.setStartDate(null); // Explicitly set null startDate
        nullStartDateExhibition.setEndDate(LocalDate.now().plusDays(10));
        nullStartDateExhibition.setSeq(2L);

        when(apiClient.fetchItems(1)).thenReturn(Arrays.asList(validExhibition, nullStartDateExhibition));
        when(exhibitionRepository.findByApiId(1L)).thenReturn(Optional.empty());
        when(exhibitionRepository.findByApiId(2L)).thenReturn(Optional.empty());
        when(museumRepository.findByName("Museum A")).thenReturn(Optional.of(new Museum()));
        when(museumRepository.findByName("Museum B")).thenReturn(Optional.of(new Museum()));

        // When
        exhibitionService.fetchExhibitions();

        // Then
        // Verify that only the valid exhibition was saved
        verify(exhibitionRepository, times(1)).save(any(Exhibition.class));
    }
}
