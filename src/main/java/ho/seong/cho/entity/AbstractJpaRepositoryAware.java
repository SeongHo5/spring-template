package ho.seong.cho.entity;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 특정 엔티티의 {@link JpaRepository}를 {@link ApplicationContext}로부터 주입받아 사용할 수 있도록 지원하는 추상 클래스
 *
 * @param <T> 사용할 {@link JpaRepository} 타입
 */
public abstract class AbstractJpaRepositoryAware<T extends JpaRepository<? extends BaseEntity, ?>>
    implements ApplicationContextAware {

  protected T repository;

  /**
   * 사용할 {@link JpaRepository} 타입을 반환한다.
   *
   * @return 주입받을 {@link JpaRepository} 타입
   * @implNote 구현체에서는 해당 메서드를 구현하여 사용할 {@link JpaRepository} 타입을 반환해야 한다.
   */
  protected abstract Class<T> repositoryType();

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.repository = applicationContext.getBean(this.repositoryType());
  }
}
