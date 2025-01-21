package ho.seong.cho.users;

import java.util.Optional;

public interface UserService {

  Optional<User> findByEmail(final String email);
}
