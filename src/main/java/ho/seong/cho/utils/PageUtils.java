package ho.seong.cho.utils;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public final class PageUtils {

  private PageUtils() {}

  public static <T> Page<T> toPage(
      final List<T> content, final Pageable pageable, final long totalCount) {
    return new PageImpl<>(content, pageable, totalCount);
  }
}
