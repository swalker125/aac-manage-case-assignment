package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

@Component
public class IdamRepository {

    private final IdamClient idamClient;

    @Autowired
    public IdamRepository(IdamClient idamClient) {
        this.idamClient = idamClient;
    }

    @Cacheable("userInfoCache")
    public UserInfo getUserInfo(String bearerToken) {
        return idamClient.getUserInfo(bearerToken);
    }

    public UserDetails getUserByUserId(String bearerToken, String userId) {
        return idamClient.getUserByUserId(bearerToken, userId);
    }
}