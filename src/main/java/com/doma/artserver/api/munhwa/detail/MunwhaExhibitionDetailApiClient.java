package com.doma.artserver.api.munhwa.detail;

import com.doma.artserver.api.ApiClient;
import com.doma.artserver.api.XMLParser;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Component
public class MunwhaExhibitionDetailApiClient implements ApiClient<MunwhaExhibitionDetailDTO> {

    private static final Logger logger = LoggerFactory.getLogger(MunwhaExhibitionDetailApiClient.class);
    private final RestTemplate restTemplate;
    private final XMLParser<MunwhaExhibitionDetailDTO> xmlParser;
    private final CloseableHttpClient httpClient;

    MunwhaExhibitionDetailApiClient(RestTemplate restTemplate,
                                    XMLParser<MunwhaExhibitionDetailDTO> xmlParser,
                                    CloseableHttpClient httpClient) {
        this.restTemplate = restTemplate;
        this.xmlParser = xmlParser;
        this.httpClient = httpClient;
    }

    @Value("${spring.api.gonggongkey}")
    private String API_KEY;
    private static final String BASE_URL = "https://apis.data.go.kr/B553457/nopenapi/rest/publicperformancedisplays/detail";

    @Override
    public List<MunwhaExhibitionDetailDTO> fetchItems(int page) {
        return List.of();
    }

    @Override
    public URI generateUrl(int page) {
        return null;
    }

    @Override
    public List<MunwhaExhibitionDetailDTO> fetchItems(Long apiId) {
        URI url = generateUrl(apiId);
        logger.info("Fetching exhibition detail data from API - apiId {}", apiId);
        logger.debug("API URL: {}", url);

        String response = restTemplate.getForObject(url, String.class);
        logger.debug("Received raw response from API");
        logger.info("API Response data: {}", response);

        List<MunwhaExhibitionDetailDTO> detailList;

        try {
            detailList = xmlParser.parse(response);
            logger.info("Successfully parsed detail XML response, found {} details", detailList.size());

            // 파싱된 상세 정보 로깅
            if (!detailList.isEmpty()) {
                detailList.forEach(detail -> 
                    logger.info("Detail information - seq: {}, url: {}, price: {}", 
                        detail.getSeq(), 
                        detail.getUrl(), 
                        detail.getPrice())
                );
            } else {
                logger.warn("No detail information found for apiId: {}", apiId);
            }
        } catch (Exception e) {
            logger.error("Failed to parse detail XML response for apiId: {}", apiId, e);
            throw new RuntimeException("파싱 실패", e);
        }

        return detailList;
    }

    @Override
    public URI generateUrl(Long apiId) {
        try {
            return new URI(BASE_URL + "?serviceKey=" + API_KEY + "&seq=" + apiId);
        } catch (URISyntaxException e) {
            throw new RuntimeException("URL 생성 실패");
        }
    }
}
