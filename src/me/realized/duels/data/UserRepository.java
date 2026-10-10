package me.realized.duels.data;

import java.util.Collection;
import java.util.UUID;

public interface UserRepository {

    UserData findByUuid(UUID uuid) throws Exception;

    UserData findByName(String name) throws Exception;

    void save(UserData user) throws Exception;

    void saveAll(Collection<UserData> users) throws Exception;

    void delete(UUID uuid) throws Exception;
}
