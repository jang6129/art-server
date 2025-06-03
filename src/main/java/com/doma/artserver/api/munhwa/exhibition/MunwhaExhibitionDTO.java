package com.doma.artserver.api.munhwa.exhibition;

import com.doma.artserver.domain.exhibition.entity.Exhibition;
import com.doma.artserver.domain.museum.entity.Museum;
import com.doma.artserver.util.LocalDateAdapter;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Getter
@Setter
@XmlRootElement(name = "item")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Component
public class MunwhaExhibitionDTO {

    @XmlElement(name = "seq")
    private Long seq;

    @XmlElement(name = "place")
    private String place;

    @XmlElement(name = "realmName")
    private String realmName;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "area")
    private String area;

    @XmlElement(name = "thumbnail")
    private String thumbnail;

    @XmlElement(name = "startDate")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate startDate;

    @XmlElement(name = "endDate")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate endDate;


    private String storageUrl;

    public Exhibition toEntity(Museum museum) {
        if (museum == null) {
            return Exhibition.builder()
                    .imgUrl(this.thumbnail)
                    .startDate(this.startDate)
                    .endDate(this.endDate)
                    .title(this.title)
                    .area(this.area)
                    .storageUrl(this.storageUrl)
                    .apiId(this.seq)
                    .build();
        } else {
            return Exhibition.builder()
                    .place(this.place)
                    .imgUrl(this.thumbnail)
                    .startDate(this.startDate)
                    .endDate(this.endDate)
                    .title(this.title)
                    .area(this.area)
                    .museum(museum)
                    .storageUrl(this.storageUrl)
                    .apiId(this.seq)
                    .build();
        }
    }

}
