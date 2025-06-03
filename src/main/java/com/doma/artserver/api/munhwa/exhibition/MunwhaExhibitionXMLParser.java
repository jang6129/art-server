package com.doma.artserver.api.munhwa.exhibition;

import com.doma.artserver.api.XMLParser;
import com.doma.artserver.util.HtmlParser;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class MunwhaExhibitionXMLParser implements XMLParser<MunwhaExhibitionDTO> {
    @Override
    public List<MunwhaExhibitionDTO> parse(String xmlData) throws Exception {
        // XML 데이터를 UTF-8로 변환하여 파싱
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8)));

        document.getDocumentElement().normalize();

        // item 노드 가져오기
        NodeList nodeList = document.getElementsByTagName("item");
        List<MunwhaExhibitionDTO> exhibitionList = new ArrayList<>();

        // 각 item 노드 탐색
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                // 각 필드값 추출
                String place = getTagValue("place", element);
                String realmName = getTagValue("realmName", element);
                // title에 HTML 엔티티를 HtmlParser로 처리
                String title = HtmlParser.parseHtml(getTagValue("title", element));
                String area = getTagValue("area", element);
                String thumbnail = getTagValue("thumbnail", element);
                LocalDate startDate = parseDate(getTagValue("startDate", element));
                LocalDate endDate = parseDate(getTagValue("endDate", element));
                Long seq = Long.parseLong(getTagValue("seq", element));

                // DTO 생성 후 리스트에 추가
                MunwhaExhibitionDTO exhibition = MunwhaExhibitionDTO.builder()
                        .place(place)
                        .realmName(realmName)
                        .title(title)
                        .area(area)
                        .thumbnail(thumbnail)
                        .startDate(startDate)
                        .endDate(endDate)
                        .seq(seq)
                        .build();

                exhibitionList.add(exhibition);
            }
        }

        return exhibitionList;
    }

    // 특정 태그의 값을 가져오는 헬퍼 메서드
    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = nodeList.item(0);
        return node != null ? node.getNodeValue() : null;
    }

    // XML에서 날짜 형식을 LocalDate로 변환하는 메서드
    private LocalDate parseDate(String date) {
        if (date != null && !date.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            return LocalDate.parse(date, formatter);
        }
        return null;
    }

}
