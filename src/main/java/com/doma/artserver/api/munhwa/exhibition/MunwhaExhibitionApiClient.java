package com.doma.artserver.api.munhwa.exhibition;

import com.doma.artserver.api.ApiClient;
import com.doma.artserver.api.XMLParser;
import com.doma.artserver.util.storage.StorageService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MunwhaExhibitionApiClient implements ApiClient<MunwhaExhibitionDTO> {

    private static final Logger logger = LoggerFactory.getLogger(MunwhaExhibitionApiClient.class);
    private final RestTemplate restTemplate;
    private final XMLParser<MunwhaExhibitionDTO> xmlParser;
    private final CloseableHttpClient httpClient;
    private final StorageService storageService;

    @Value("${spring.api.gonggongkey}")
    private String API_KEY;
    private static final String BASE_URL = "https://apis.data.go.kr/B553457/nopenapi/rest/publicperformancedisplays/realm";

    MunwhaExhibitionApiClient(RestTemplate restTemplate,
                              XMLParser<MunwhaExhibitionDTO> xmlParser,
                              CloseableHttpClient httpClient,
                              StorageService<byte[]> storageService) {
        this.restTemplate = restTemplate;
        this.xmlParser = xmlParser;
        this.httpClient = httpClient;
        this.storageService = storageService;
    }

    @Override
    public List<MunwhaExhibitionDTO> fetchItems(int page) {
        logger.info("Fetching exhibition data from API - page {}", page);
        URI url = generateUrl(page);
        logger.debug("API URL: {}", url);

        String response = restTemplate.getForObject(url, String.class);
        logger.debug("Received raw response from API");
        logger.info("API Response data: {}", response);

        List<MunwhaExhibitionDTO> exhibitionList;
        try {
            exhibitionList = xmlParser.parse(response);
            logger.info("Successfully parsed XML response, found {} exhibitions", exhibitionList.size());

            // 파싱된 모든 전시회 정보 로깅
            if (!exhibitionList.isEmpty()) {
                // 처음 5개 전시회 정보 상세 로깅
                exhibitionList.stream().limit(5).forEach(exhibition ->
                        logger.info("Exhibition details - seq: {}, title: {}, place: {}, realmName: {}",
                                exhibition.getSeq(),
                                exhibition.getTitle(),
                                exhibition.getPlace(),
                                exhibition.getRealmName())
                );
            }
        } catch (Exception e) {
            logger.error("Failed to parse XML response", e);
            throw new RuntimeException("파싱 실패", e);
        }

        return exhibitionList;

//        return exhibitionList.stream()
//                .filter(exhibition -> "미술".equals(exhibition.getRealmName()))
//                .peek(exhibition -> {
//                    byte[] imageData = null;
//                    if (exhibition.getThumbnail() != null) {
//                        imageData = fetchImageData(exhibition.getThumbnail());
//                    }
//                    if (imageData != null) {
//                        // 이미지 URL에서 확장자 추출
//                        String fileExtension = extractFileExtension(exhibition.getThumbnail());
//                        // 확장자가 추가된 파일 이름으로 이미지 업로드
//                        String storageUrl = storageService.uploadFile(exhibition.getTitle() + fileExtension, imageData);
//                        exhibition.setStorageUrl(storageUrl);
//                    }
//                })
//                .collect(Collectors.toList());
    }

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
    public List<MunwhaExhibitionDTO> fetchItems(Long apiId) {
        return List.of();
    }

    @Override
    public URI generateUrl(Long seq) {
        return null;
    }

    public byte[] fetchImageData(String imageUrl) {
        // imageUrl이 null이거나 빈 값인지 확인
        if (imageUrl == null || imageUrl.isEmpty()) {
            logger.warn("Image URL is null or empty");
            return null;
        }

        logger.debug("Fetching image data from URL: {}", imageUrl);
        HttpGet request = new HttpGet(imageUrl);
        request.setHeader("User-Agent", "Mozilla/5.0"); // User-Agent 설정

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity(); // 응답 엔티티 추출
            if (entity != null) {
                logger.debug("Received image entity: {}", entity);
                byte[] imageData = EntityUtils.toByteArray(entity);
                logger.debug("Successfully converted image entity to byte array, size: {} bytes",
                        imageData != null ? imageData.length : 0);
                return imageData; // 엔티티의 바이트 배열 반환
            } else {
                logger.warn("No entity found in the image response");
            }
        } catch (IOException e) {
            logger.error("Error fetching image data from URL: {}", imageUrl, e);
        }

        logger.warn("Failed to fetch image data from URL: {}", imageUrl);
        return null; // 실패 시 null 반환
    }

    // URL에서 파일 확장자를 추출하는 메서드
    private String extractFileExtension(String imageUrl) {
        try {
            // URL에서 확장자 추출
            String extension = imageUrl.substring(imageUrl.lastIndexOf("."));
            logger.debug("Extracted file extension '{}' from URL: {}", extension, imageUrl);
            return extension;
        } catch (Exception e) {
            // 확장자 추출 실패 시 기본값을 반환 (예: .jpg)
            logger.warn("Failed to extract file extension from URL: {}, using default extension .jpg", imageUrl);
            return ".jpg"; // 기본 확장자
        }
    }
}
