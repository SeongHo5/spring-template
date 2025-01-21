package ho.seong.cho.users;

import ho.seong.cho.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@Inheritance(strategy = InheritanceType.JOINED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  @Serial private static final long serialVersionUID = 2025011001L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", columnDefinition = "int UNSIGNED not null")
  private Long id;

  @Size(max = 100)
  @NotNull @Column(name = "name", nullable = false, length = 100)
  protected String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "role_type")
  protected RoleType roleType;

  // Other fields(columns)

  public boolean isSuperAdmin() {
    return this.roleType.isSuperAdmin();
  }

  public boolean isAdmin() {
    return this.roleType.isAdmin();
  }
}
