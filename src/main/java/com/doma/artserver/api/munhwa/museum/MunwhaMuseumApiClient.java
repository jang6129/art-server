package com.doma.artserver.api.munhwa.museum;

import com.doma.artserver.api.ApiClient;
import com.doma.artserver.api.XMLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MunwhaMuseumApiClient implements ApiClient<MunwhaMuseumDTO> {

    private static final Logger logger = LoggerFactory.getLogger(MunwhaMuseumApiClient.class);
    private final RestTemplate restTemplate;
    private final XMLParser<MunwhaMuseumDTO> xmlParser;

    @Value("${spring.api.gonggongkey}")
    private String API_KEY;
    private static final String BASE_URL = "https://apis.data.go.kr/B553457/nopenapi/rest/publicperformancedisplays/realm";

    public MunwhaMuseumApiClient(RestTemplate restTemplate,
                                 XMLParser<MunwhaMuseumDTO> xmlParser) {
        this.restTemplate = restTemplate;
        this.xmlParser = xmlParser;
    }


    @Override
    public List<MunwhaMuseumDTO> fetchItems(int page) {
        logger.info("Fetching museum data from API - page {}", page);
        URI url = generateUrl(page);
        logger.debug("API URL: {}", url);

        String response = restTemplate.getForObject(url, String.class);
        logger.debug("Received raw response from API");
        logger.info("API Response data: {}", response);

        // XML 응답을 바로 파싱
        List<MunwhaMuseumDTO> museumList;
        try {
            logger.info("Parsing XML response");
            museumList = xmlParser.parse(response);
            logger.info("XML parse result: found {} museums", museumList.size());
        } catch (Exception e) {
            logger.error("Failed to parse XML response: {}", e.getMessage());
            throw new RuntimeException("파싱 실패", e);
        }

        // 파싱 성공 - 데이터 확인 로깅
        if (!museumList.isEmpty()) {
            logger.info("First item details - seq: {}, title: {}, place: {}, realmName: '{}'",
                    museumList.get(0).getSeq(),
                    museumList.get(0).getTitle(),
                    museumList.get(0).getPlace(),
                    museumList.get(0).getRealmName());

            List<String> realmNames = museumList.stream()
                    .map(MunwhaMuseumDTO::getRealmName)
                    .distinct()
                    .collect(Collectors.toList());
            logger.info("Available realmName values: {}", realmNames);
        } else {
            logger.warn("Parsed museum list is empty even though XML parsing succeeded");
        }

        // 모든 항목 그대로 반환 (필터링 제거)
        List<MunwhaMuseumDTO> filteredList = museumList;

        // 파싱된 모든 항목 로깅
        logger.info("Returning all museums without filtering");

        logger.info("Filtered museum data: {} total museums, {} exhibition museums",
                museumList.size(), filteredList.size());

        return filteredList;
    }

    // 특정 페이지에 해당하는 URL을 동적으로 생성
    @Override
    public URI generateUrl(int page) {
        try {
            return new URI(BASE_URL + "?realmCode=D000&PageNo=" + page +
                    "&numOfrows=50&sortStdr=1&serviceKey=" + API_KEY);
        } catch (URISyntaxException e) {
            throw new RuntimeException("URL 생성 실패");
        }
    }

    @Override
    public List<MunwhaMuseumDTO> fetchItems(Long apiId) {
        return List.of();
    }

    @Override
    public URI generateUrl(Long seq) {
        return null;
    }


}
