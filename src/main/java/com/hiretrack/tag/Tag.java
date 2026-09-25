package com.hiretrack.tag;

import com.hiretrack.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_tag_name", columnNames = {"user_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
