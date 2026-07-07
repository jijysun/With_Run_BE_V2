package UMC_8th.With_Run.course.entity;

import UMC_8th.With_Run.map.entity.RegionProvince;
import UMC_8th.With_Run.map.entity.RegionsCity;
import UMC_8th.With_Run.map.entity.RegionsTown;
import UMC_8th.With_Run.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;

    @Column(columnDefinition = "json")
    private String keyWord;
    private Integer time;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String courseImage;
    private String location;
    private String overviewPolyline; //프론트에서 암호화해서주는 최적코스 좌표

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_province_id")
    private RegionProvince regionProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regions_city_id")
    private RegionsCity regionsCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regions_town_id")
    private RegionsTown regionsTown;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
