package com.doma.artserver.api.munhwa.museum;

import com.doma.artserver.api.XMLParser;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class MunwhaMuseumXMLParser implements XMLParser<MunwhaMuseumDTO> {

    @Override
    public List<MunwhaMuseumDTO> parse(String xmlData) throws Exception {
        // XML 데이터를 UTF-8로 변환하여 파싱
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8)));

        document.getDocumentElement().normalize();

        // item 노드 가져오기
        NodeList nodeList = document.getElementsByTagName("item");
        List<MunwhaMuseumDTO> museumList = new ArrayList<>();

        // 각 item 노드 탐색
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                // 각 필드값 추출
                String place = getTagValue("place", element);
                String area = getTagValue("area", element);
                String gpsX = getTagValue("gpsX", element);
                String gpsY = getTagValue("gpsY", element);
                String realmName = getTagValue("realmName", element);

                // DTO 생성 후 리스트에 추가
                MunwhaMuseumDTO museum = MunwhaMuseumDTO.builder()
                        .place(place)
                        .area(area)
                        .gpsX(gpsX)
                        .gpsY(gpsY)
                        .realmName(realmName)
                        .build();

                museumList.add(museum);
            }
        }

        return museumList;
    }

    // 특정 태그의 값을 가져오는 헬퍼 메서드
    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = nodeList.item(0);
        return node != null ? node.getNodeValue() : null;
    }
}
