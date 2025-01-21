package ho.seong.cho.users;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 사용자 권한 타입 */
@Getter
@AllArgsConstructor
public enum RoleType {
  SUPER_ADMIN(0, "최고 관리자"),
  ADMIN(1, "관리자"),
  MANAGER(2, "매니저"),
  USER(10, "사용자");

  /** 권한 레벨 */
  private final int level;

  /** 권한 이름 */
  private final String roleName;

  public static RoleType fromLevel(final int level) {
    return Arrays.stream(values())
        .filter(userType -> userType.level == level)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid level"));
  }

  public boolean isSuperAdmin() {
    return this == SUPER_ADMIN;
  }

  public boolean isAdmin() {
    return this.level < USER.level;
  }
}
