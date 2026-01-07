package ho.seong.cho.users;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

  @Override
  public Optional<User> findById(long id) {
    return Optional.empty();
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return Optional.empty();
  }
}
