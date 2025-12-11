package codesAndStandards.springboot.userApp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "GroupUser",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_UserGroup",
                        columnNames = {"user_id", "groupId"}
                )
        }
)
public class GroupUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "groupUserId")
    private Long groupUserId;

    // FK: User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_GroupUser_User")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // FK: Group
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "groupId",
            referencedColumnName = "groupId",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_GroupUser_Group")
    )
    private codesAndStandards.springboot.userApp.entity.Group group;

    // created_by WITHOUT FK constraint
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            nullable = true,
//            referencedColumnName = "user_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
