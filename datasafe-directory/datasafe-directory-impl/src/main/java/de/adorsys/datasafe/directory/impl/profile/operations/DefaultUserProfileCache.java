package de.adorsys.datasafe.directory.impl.profile.operations;

import de.adorsys.datasafe.encrypiton.api.types.UserID;
import de.adorsys.datasafe.directory.api.types.UserPrivateProfile;
import de.adorsys.datasafe.directory.api.types.UserPublicProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class DefaultUserProfileCache implements UserProfileCache {

    private final Map<UserID, UserPublicProfile> publicProfile;
    private final Map<UserID, UserPrivateProfile> privateProfile;
}
