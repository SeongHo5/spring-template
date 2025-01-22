package ho.seong.cho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

/**
 * {@link Entity}의 생성자와 수정자를 자동으로 관리하기 위한 추상 클래스 <br>
 * 모든 {@link Entity} 클래스는 이 클래스 또는 {@link BaseEntity}를 상속받도록 구성해야 한다.
 */
@Getter
@SuperBuilder
@MappedSuperclass
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SecuredBaseEntity extends BaseEntity {

  @CreatedBy
  @Column(nullable = false, updatable = false)
  private Long createdBy;

  @LastModifiedBy
  @Column(nullable = false)
  private Long updatedBy;
}
