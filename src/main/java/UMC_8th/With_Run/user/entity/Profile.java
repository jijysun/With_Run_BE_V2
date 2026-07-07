package UMC_8th.With_Run.user.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "town_id")
    private Long townId;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "province_id")
    private Long provinceId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "gender", length = 255)
    private String gender;

    @Column(name = "birth", length = 255)
    private String birth;

    @Column(name = "breed", length = 255)
    private String breed;

    @Column(name = "size", length = 255)
    private String size;

    @Column(name = "profile_image", length = 2083) // URL 길이 고려
    private String profileImage;

    @Column(name = "characters", columnDefinition = "json")
    private String characters;

    @Column(name = "style", columnDefinition = "json")
    private String style;

    @Column(name = "introduction", length = 255)
    private String introduction;

    @Column(name = "created_at", columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", columnDefinition = "datetime")
    private LocalDateTime deletedAt;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
