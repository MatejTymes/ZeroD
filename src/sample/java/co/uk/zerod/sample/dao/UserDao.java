package co.uk.zerod.sample.dao;

import co.uk.zerod.AccessGuide;
import co.uk.zerod.sample.domain.User;
import co.uk.zerod.sample.domain.UserId;

import java.util.Map;

import static co.uk.zerod.ReadState.ReadOld;
import static co.uk.zerod.WriteState.*;
import static co.uk.zerod.sample.MigrationHelper.FULL_NAME_MIGRATION;
import static co.uk.zerod.sample.domain.UserId.userId;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.UUID.randomUUID;

public class UserDao {

    private final VersionedStore<UserId> store;
    private final AccessGuide accessGuide;

    public UserDao(VersionedStore<UserId> store, AccessGuide accessGuide) {
        this.store = store;
        this.accessGuide = accessGuide;
    }

    public UserId storeUser(User user) {
        UserId userId = userId(randomUUID());

        accessGuide.write(FULL_NAME_MIGRATION, writeState -> {
            Map<String, String> values = newHashMap();
            if (writeState == WriteOld || writeState == WriteBoth) {
                values.put("fullName", user.firstName + " " + user.lastName);
            }
            if (writeState == WriteNew || writeState == WriteBoth) {
                values.put("firstName", user.firstName);
                values.put("lastName", user.lastName);
            }

            store.insert(userId, values);
        });

        return userId;
    }

    public User findUser(UserId userId) {
        return accessGuide.read(FULL_NAME_MIGRATION, readState -> {
            Map<String, String> values = store.getValue(userId);

            String firstName;
            String lastName;
            if (readState == ReadOld) {
                String[] fullNamePieces = values.get("fullName").split(" ");
                firstName = fullNamePieces[0];
                lastName = fullNamePieces[1];
            } else {
                firstName = values.get("firstName");
                lastName = values.get("lastName");
            }

            return new User(firstName, lastName);
        });
    }
}
